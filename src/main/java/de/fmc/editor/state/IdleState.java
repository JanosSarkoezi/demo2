package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.AddWaypointCommand;
import de.fmc.editor.core.event.EventBus;
import de.fmc.editor.core.event.EditorActionEvent;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.FmcText;
import de.fmc.editor.core.model.SelectionModel;
import de.fmc.editor.core.util.GeometryUtils;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IdleState implements EditorState {

    private final EventBus eventBus;
    private InteractionMap bindings;

    public IdleState() {
        this(null);
    }

    public IdleState(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private void initBindings(CanvasController context) {
        if (bindings != null) return;

        bindings = new InteractionMap();

        // 1. ESC – Tool reaktivieren
        bindings.on(EventMatcher.keyPressed(KeyCode.ESCAPE), event -> {
            if (eventBus != null) {
                eventBus.publish(new EditorActionEvent.ReactivateTool());
            } else {
                context.reactivateCurrentTool();
            }
        });

        // 2. Doppelklick Aktionen
        bindings.on(EventMatcher.primaryDoubleClick(), event -> {
            // a) Doppelklick auf einen Text → EditTextState
            FmcText hitText = context.findTextAt(event.worldX(), event.worldY());
            if (hitText != null) {
                if (eventBus != null) {
                    eventBus.publish(new EditorActionEvent.ChangeState(new EditTextState(hitText.id())));
                } else {
                    context.setCurrentState(new EditTextState(hitText.id()));
                }
                return;
            }

            // b) Doppelklick auf ein Objekt (kein Wegpunkt) → ResizeState
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null && hit.type() != FmcType.WAYPOINT) {
                if (eventBus != null) {
                    eventBus.publish(new EditorActionEvent.ChangeState(new ResizeState(hit.id(), eventBus)));
                } else {
                    context.setCurrentState(new ResizeState(hit.id()));
                }
                return;
            }

            // c) Doppelklick auf eine Verbindung → Wegpunkt hinzufügen
            UUID clickedConnectionId = context.findConnectionNear(event.worldX(), event.worldY(), 10.0);
            if (clickedConnectionId != null) {
                if (eventBus != null) {
                    eventBus.publish(new EditorActionEvent.AddWaypoint(clickedConnectionId, event.worldX(), event.worldY()));
                } else {
                    handleAddWaypointOnDoubleClick(event, context, clickedConnectionId);
                }
            }
        });

        // 3. Wegpunkte einblenden bei einfachem Klick auf eine Verbindung (ohne Drag-Start)
        bindings.on(
            EventMatcher.primaryClick().and(event -> context.findConnectionNear(event.worldX(), event.worldY(), 10.0) != null),
            event -> {
                if (eventBus != null) {
                    eventBus.publish(new EditorActionEvent.SetLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true));
                } else {
                    context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
                }
            }
        );

        // 4. Drag Aktionen (Continuous Events)
        
        // a) Drag auf Text -> Text verschieben
        bindings.onDrag(
            EventMatcher.primaryClick().and(event -> context.findTextAt(event.worldX(), event.worldY()) != null),
            startEvent -> {
                FmcText hitText = context.findTextAt(startEvent.worldX(), startEvent.worldY());
                if (hitText != null) {
                    handleTextSelection(startEvent, context, hitText);
                    return new MoveTextDragHandler(hitText.id(), context);
                }
                return null;
            }
        );

        // b) Drag auf Objekt -> Objekte verschieben
        bindings.onDrag(
            EventMatcher.primaryClick().and(event -> context.findObjectAt(event.worldX(), event.worldY()) != null),
            startEvent -> {
                FmcObject hit = context.findObjectAt(startEvent.worldX(), startEvent.worldY());
                if (hit != null) {
                    handleSelection(startEvent, context, hit);
                    return new MoveObjectsDragHandler(hit.id(), context);
                }
                return null;
            }
        );

        // c) Ctrl + Drag ins Leere -> BoxSelection (Rechteck aufspannen)
        bindings.onDrag(
            EventMatcher.primaryClick().and(event -> event.isControlDown() &&
                context.findObjectAt(event.worldX(), event.worldY()) == null &&
                context.findTextAt(event.worldX(), event.worldY()) == null),
            startEvent -> new BoxSelectionDragHandler(context)
        );

        // d) Normaler Drag ins Leere -> Panning (Kamera bewegen) + Auswahl leeren
        bindings.onDrag(
            EventMatcher.primaryClick().and(event -> !event.isControlDown() &&
                context.findObjectAt(event.worldX(), event.worldY()) == null &&
                context.findTextAt(event.worldX(), event.worldY()) == null),
            startEvent -> {
                // Auswahl leeren bei Klick ins Leere
                if (eventBus != null) {
                    eventBus.publish(new EditorActionEvent.ClearSelection());
                    eventBus.publish(new EditorActionEvent.SetLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false));
                } else {
                    context.getSelectionModel().clearAll();
                    if (!context.getToolbarController().isWaypointsVisible()) {
                        context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
                    }
                }
                return new PanningDragHandler(context);
            }
        );
    }

    @Override
    public InteractionMap getInteractionMap() {
        return bindings;
    }

    @Override
    public void enterState(CanvasController context) {
        initBindings(context);
    }

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        initBindings(context);
        // Falls handleInput direkt aufgerufen wird, leiten wir an die Map weiter
        if (event.activeKey().isPresent()) {
            bindings.handlePress(event);
        } else if (event.isPrimaryButtonDown()) {
            bindings.handlePress(event);
        } else {
            bindings.handleRelease(event);
        }
    }

    private void handleAddWaypointOnDoubleClick(InteractionEventData event, CanvasController context, UUID connId) {
        var waypoint = FmcFactory.createObject(
                FmcType.WAYPOINT,
                event.worldX(), event.worldY(),
                CoreRegistry.WAYPOINT_LAYER_ID
        );

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

        var cmd = new AddWaypointCommand(context.getRegistry(), connId, waypoint, index);
        context.getCommandHistory().executeCommand(cmd);

        SelectionModel selectionModel = context.getSelectionModel();
        selectionModel.clearObjectSelection();
        selectionModel.addObjectToSelection(waypoint.id());

        context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
    }

    private void handleTextSelection(InteractionEventData event, CanvasController context, FmcText hitText) {
        if (eventBus != null) {
            eventBus.publish(new EditorActionEvent.SelectText(hitText.id(), event.isControlDown()));
        } else {
            SelectionModel selectionModel = context.getSelectionModel();

            if (!event.isControlDown()) {
                selectionModel.clearObjectSelection();
            }

            if (event.isControlDown()) {
                selectionModel.toggleTextSelection(hitText.id());
            } else {
                if (!selectionModel.isTextSelected(hitText.id())) {
                    selectionModel.selectText(hitText.id());
                }
            }
        }
    }

    private void handleSelection(InteractionEventData event, CanvasController context, FmcObject hit) {
        if (eventBus != null) {
            eventBus.publish(new EditorActionEvent.SelectObject(hit.id(), event.isControlDown()));
        } else {
            SelectionModel selectionModel = context.getSelectionModel();

            if (!event.isControlDown()) {
                selectionModel.clearTextSelection();
            }

            if (event.isControlDown()) {
                selectionModel.toggleObjectSelection(hit.id());
            } else {
                if (!selectionModel.isObjectSelected(hit.id())) {
                    selectionModel.selectObject(hit.id()); // Löscht automatisch die alte Objektauswahl
                }
            }

            if (hit.type() != FmcType.WAYPOINT && !context.getToolbarController().isWaypointsVisible()) {
                context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
            }
        }
    }
}