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
        // 1. ESC – immer höchste Priorität
        if (handleEscape(event, context)) return;

        // 2. Doppelklick – vor Einfachklick
        if (handleDoubleClick(event, context)) return;

        // 3. Einfachklick (nur primäre Maustaste)
        if (event.isPrimaryButtonDown()) {
            if (handleClickOnConnection(event, context)) return;
            if (handleClickOnObject(event, context)) return;
            if (handleCtrlClickOnEmpty(event, context)) return;
            if (handleClickOnEmpty(event, context)) return;
        }
    }

    // -------------------------------------------------------------
    // 1. ESC – Tool reaktivieren
    // -------------------------------------------------------------
    private boolean handleEscape(InteractionEventData event, CanvasController context) {
        if (event.activeKey().isPresent() && event.activeKey().get() == KeyCode.ESCAPE) {
            context.reactivateCurrentTool();
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------
    // 2. Doppelklick – Objekt skalieren oder Wegpunkt hinzufügen
    // -------------------------------------------------------------
    private boolean handleDoubleClick(InteractionEventData event, CanvasController context) {
        if (event.clickCount() == 2 && event.isPrimaryButtonDown()) {
            // a) Doppelklick auf ein Objekt (kein Wegpunkt) → ResizeState
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null && hit.type() != FmcType.WEGPUNKT) {
                context.setCurrentState(new ResizeState(hit.id()));
                return true;
            }

            // b) Doppelklick auf eine Verbindung → Wegpunkt hinzufügen
            UUID clickedConnectionId = context.findConnectionAt(event.sceneX(), event.sceneY());
            if (clickedConnectionId != null) {
                handleAddWaypointOnDoubleClick(event, context, clickedConnectionId);
                return true;
            }
        }
        return false;
    }

    private void handleAddWaypointOnDoubleClick(InteractionEventData event, CanvasController context, UUID connId) {
        // Wegpunkt erzeugen
        var waypoint = FmcFactory.createObject(
                FmcType.WEGPUNKT,
                event.worldX(), event.worldY(),
                CoreRegistry.WAYPOINT_LAYER_ID
        );

        // Einfügeindex berechnen
        var conn = context.getRegistry().getConnections().get(connId);
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

        // Command ausführen
        var cmd = new AddWaypointCommand(context.getRegistry(), connId, waypoint, index);
        context.getCommandHistory().executeCommand(cmd);

        // Neuen Wegpunkt selektieren
        var selectedIds = context.getSelectedObjectIds();
        selectedIds.clear();
        selectedIds.add(waypoint.id());
        context.updateSelectionInView();

        // Wegpunkt-Layer sichtbar machen
        context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
    }

    // -------------------------------------------------------------
    // 3. Einfachklick auf eine Verbindung – nur Wegpunkte einblenden
    // -------------------------------------------------------------
    private boolean handleClickOnConnection(InteractionEventData event, CanvasController context) {
        if (event.clickCount() == 1 && event.isPrimaryButtonDown()) {
            UUID clickedConnectionId = context.findConnectionAt(event.sceneX(), event.sceneY());
            if (clickedConnectionId != null) {
                context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------
    // 4. Einfachklick auf ein Objekt – Selektion + Drag
    // -------------------------------------------------------------
    private boolean handleClickOnObject(InteractionEventData event, CanvasController context) {
        if (event.clickCount() == 1 && event.isPrimaryButtonDown()) {
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null) {
                handleSelection(event, context, hit);
                context.setCurrentState(new DragObjectsState(hit.id(), event, context));
                return true;
            }
        }
        return false;
    }

    private void handleSelection(InteractionEventData event, CanvasController context, FmcObject hit) {
        var selectedIds = context.getSelectedObjectIds();

        // Strg → toggle
        if (event.isControlDown()) {
            if (selectedIds.contains(hit.id())) {
                selectedIds.remove(hit.id());
            } else {
                selectedIds.add(hit.id());
            }
        } else {
            // Normal → nur dieses Objekt selektieren
            if (!selectedIds.contains(hit.id())) {
                selectedIds.clear();
                selectedIds.add(hit.id());
            }
        }

        // Wegpunkte ausblenden, wenn ein Nicht-Wegpunkt selektiert wurde und die Toolbar es nicht anders verlangt
        if (hit.type() != FmcType.WEGPUNKT && !context.getToolbarController().isWaypointsVisible()) {
            context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
        }

        context.updateSelectionInView();
    }

    // -------------------------------------------------------------
    // 5. Ctrl + Klick ins Leere → BoxSelection (Gummiband)
    // -------------------------------------------------------------
    private boolean handleCtrlClickOnEmpty(InteractionEventData event, CanvasController context) {
        if (event.clickCount() == 1 && event.isPrimaryButtonDown() && event.isControlDown()) {
            if (context.findObjectAt(event.worldX(), event.worldY()) == null) {
                context.setCurrentState(new BoxSelectionState(event, context));
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------
    // 6. Normaler Klick ins Leere → Auswahl leeren + Panning
    // -------------------------------------------------------------
    private boolean handleClickOnEmpty(InteractionEventData event, CanvasController context) {
        if (event.clickCount() == 1 && event.isPrimaryButtonDown() && !event.isControlDown()) {
            if (context.findObjectAt(event.worldX(), event.worldY()) == null) {
                context.getSelectedObjectIds().clear();
                context.updateSelectionInView();

                if (!context.getToolbarController().isWaypointsVisible()) {
                    context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
                }
                context.setCurrentState(new PanningState(event, context));
                return true;
            }
        }
        return false;
    }
}