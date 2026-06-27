package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.AddWaypointCommand;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.util.GeometryUtils;
import javafx.scene.input.KeyCode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IdleState implements EditorState {

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        // A. Tastatur-Shortcuts
        if (event.activeKey().isPresent()) {
            KeyCode key = event.activeKey().get();
            // ESC -> Tool reaktivieren
            if (key == KeyCode.ESCAPE) {
                context.reactivateCurrentTool();
                return;
            }
        }

        // B. Einstieg in die Textbearbeitung oder ResizeState: Doppelklick
        if (event.clickCount() == 2 && event.isPrimaryButtonDown()) {
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null) {
                if (hit.type() == FmcType.WEGPUNKT) {
                    // Wegpunkte werden nicht skaliert
                    return;
                }
                // Wechsel in den ResizeState
                context.setCurrentState(new ResizeState(hit.id()));
                return;
            }

            // Doppelklick auf eine Verbindung -> Wegpunkt hinzufügen
            UUID clickedConnectionId = context.findConnectionAt(event.sceneX(), event.sceneY());
            if (clickedConnectionId != null) {
                var waypoint = FmcFactory.createObject(
                    FmcType.WEGPUNKT,
                    event.worldX(), event.worldY(),
                    CoreRegistry.WAYPOINT_LAYER_ID
                );

                var conn = context.getRegistry().getConnections().get(clickedConnectionId);
                int index = 0;
                if (conn != null) {
                    var source = context.getRegistry().getObject(conn.sourceId());
                    var target = context.getRegistry().getObject(conn.targetId());
                    List<FmcObject> currentWps = new ArrayList<>();
                    for (UUID id : conn.waypointIds()) {
                        FmcObject wp = context.getRegistry().getObject(id);
                        if (wp != null) currentWps.add(wp);
                    }
                    index = GeometryUtils.calculateInsertionIndex(event.worldX(), event.worldY(), source, target, currentWps);
                }

                var cmd = new AddWaypointCommand(
                    context.getRegistry(), clickedConnectionId, waypoint, index
                );
                context.getCommandHistory().executeCommand(cmd);

                var selectedIds = context.getSelectedObjectIds();
                selectedIds.clear();
                selectedIds.add(waypoint.id());
                context.updateSelectionInView();

                context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
                return;
            }
        }

        // C. Klick auf eine Verbindung -> Wegpunkte einblenden
        if (event.clickCount() == 1 && event.isPrimaryButtonDown()) {
            UUID clickedConnectionId = context.findConnectionAt(event.sceneX(), event.sceneY());
            if (clickedConnectionId != null) {
                context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
                return;
            }
        }

        // D. Klick auf ein Objekt -> Selektion + Drag-Merkung
        if (event.clickCount() == 1 && event.isPrimaryButtonDown()) {
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null) {
                var selectedIds = context.getSelectedObjectIds();

                if (hit.type() != FmcType.WEGPUNKT && !context.getToolbarController().isWaypointsVisible()) {
                    context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
                }

                if (event.isControlDown()) {
                    if (selectedIds.contains(hit.id())) {
                        selectedIds.remove(hit.id());
                    } else {
                        selectedIds.add(hit.id());
                    }
                } else {
                    if (!selectedIds.contains(hit.id())) {
                        selectedIds.clear();
                        selectedIds.add(hit.id());
                    }
                }

                context.updateSelectionInView();
                context.setCurrentState(new DragObjectsState(hit.id(), event, context));
                return;
            }
        }

        // E. STRG + Klick ins Leere -> BoxSelectionState (Gummiband)
        if (event.clickCount() == 1 && event.isPrimaryButtonDown() && event.isControlDown()) {
            if (context.findObjectAt(event.worldX(), event.worldY()) == null) {
                context.setCurrentState(new BoxSelectionState(event, context));
                return;
            }
        }

        // F. Normaler Klick ins Leere -> PanningState
        if (event.clickCount() == 1 && event.isPrimaryButtonDown() && !event.isControlDown()) {
            if (context.findObjectAt(event.worldX(), event.worldY()) == null) {
                context.getSelectedObjectIds().clear();
                context.updateSelectionInView();

                if (!context.getToolbarController().isWaypointsVisible()) {
                    context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
                }
                context.setCurrentState(new PanningState(event, context));
                return;
            }
        }
    }
}
