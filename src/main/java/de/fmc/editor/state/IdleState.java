package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.AddWaypointCommand;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.util.GeometryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IdleState extends AbstractEditorState {

    public IdleState() {
        // 1. Doppel-Klick auf ein Objekt -> ResizeState
        pressedActions.add(new MouseAction(
            MouseMatchers.all(
                MouseMatchers.primaryButton(),
                MouseMatchers.clickCount(2),
                MouseMatchers.objectHit()
            ),
            (event, context) -> {
                FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
                if (hit != null) {
                    context.setCurrentState(new ResizeState(hit.id()));
                }
            }
        ));

        // 2. Einfacher Klick auf ein Objekt -> Selektion + Drag-Merkung
        pressedActions.add(new MouseAction(
            MouseMatchers.all(
                MouseMatchers.primaryButton(),
                MouseMatchers.clickCount(1),
                MouseMatchers.objectHit()
            ),
            (event, context) -> {
                FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
                if (hit == null) return;
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
            }
        ));

        // 3. Doppelklick auf eine Verbindung -> Wegpunkt hinzufügen
        pressedActions.add(new MouseAction(
            MouseMatchers.all(
                MouseMatchers.primaryButton(),
                MouseMatchers.clickCount(2),
                MouseMatchers.connectionHit()
            ),
            (event, context) -> {
                UUID clickedConnectionId = context.findConnectionAt(event.sceneX(), event.sceneY());
                if (clickedConnectionId == null) return;

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
            }
        ));

        // 4. Einfacher Klick auf eine Verbindung -> Wegpunkte einblenden
        pressedActions.add(new MouseAction(
            MouseMatchers.all(
                MouseMatchers.primaryButton(),
                MouseMatchers.clickCount(1),
                MouseMatchers.connectionHit()
            ),
            (event, context) -> {
                context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
            }
        ));

        // 5. STRG + Klick ins Leere -> BoxSelectionState (Gummiband)
        pressedActions.add(new MouseAction(
            MouseMatchers.all(
                MouseMatchers.primaryButton(),
                MouseMatchers.controlDown(),
                MouseMatchers.noObjectHit(),
                MouseMatchers.noConnectionHit()
            ),
            (event, context) -> {
                context.setCurrentState(new BoxSelectionState(event, context));
            }
        ));

        // 6. Einfacher Klick ins Leere -> PanningState
        pressedActions.add(new MouseAction(
            MouseMatchers.all(
                MouseMatchers.primaryButton(),
                MouseMatchers.noControlDown(),
                MouseMatchers.noObjectHit(),
                MouseMatchers.noConnectionHit()
            ),
            (event, context) -> {
                context.getSelectedObjectIds().clear();
                context.updateSelectionInView();

                if (!context.getToolbarController().isWaypointsVisible()) {
                    context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
                }
                context.setCurrentState(new PanningState(event, context));
            }
        ));
    }
}
