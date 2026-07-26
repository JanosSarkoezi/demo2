package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.CreateConnectionCommand;
import de.fmc.editor.core.command.CreateObjectCommand;
import de.fmc.editor.core.event.EventBus;
import de.fmc.editor.core.event.EditorActionEvent;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.SelectionModel;
import javafx.scene.input.KeyCode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateConnectionState implements EditorState {
    private final EventBus eventBus;
    private UUID sourceObjectId = null;
    private final List<UUID> collectedWaypointIds = new ArrayList<>();

    public CreateConnectionState() {
        this(null);
    }

    public CreateConnectionState(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void enterState(CanvasController context) {
        if (eventBus != null) {
            eventBus.publish(new EditorActionEvent.SetLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true));
        } else {
            context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
        }
    }

    @Override
    public void exitState(CanvasController context) {
        if (eventBus != null) {
            eventBus.publish(new EditorActionEvent.ClearSelection());
            eventBus.publish(new EditorActionEvent.SetLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false));
        } else {
            context.getSelectionModel().clearObjectSelection();
            if (!context.getToolbarController().isWaypointsVisible()) {
                context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
            }
        }
    }

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        // ESC -> Tool wechseln/abbrechen
        if (event.activeKey().isPresent() && event.activeKey().get() == KeyCode.ESCAPE) {
            if (eventBus != null) {
                eventBus.publish(new EditorActionEvent.ResetToIdle());
            } else {
                context.resetToIdleState();
            }
            return;
        }

        if (event.clickCount() == 1 && event.isPrimaryButtonDown() && event.activeKey().isEmpty()) {
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());

            if (sourceObjectId == null) {
                // 1. Schritt: Startobjekt auswählen
                if (hit != null && hit.type() != FmcType.WAYPOINT) {
                    sourceObjectId = hit.id();

                    if (eventBus != null) {
                        eventBus.publish(new EditorActionEvent.SelectObject(sourceObjectId, false));
                    } else {
                        SelectionModel selectionModel = context.getSelectionModel();
                        selectionModel.clearObjectSelection();
                        selectionModel.addObjectToSelection(sourceObjectId);
                    }
                }
            } else {
                // 2. Schritt: Wenn bereits ein Startobjekt existiert
                if (hit == null) {
                    // Klick ins Leere -> Wegpunkt hinzufügen
                    UUID wpId = UUID.randomUUID();
                    if (eventBus != null) {
                        eventBus.publish(new EditorActionEvent.CreateWaypoint(wpId, event.worldX(), event.worldY()));
                        collectedWaypointIds.add(wpId);
                    } else {
                        var wegpunkt = new FmcObject(wpId, FmcType.WAYPOINT, event.worldX(), event.worldY(), 10, 10, CoreRegistry.WAYPOINT_LAYER_ID);
                        var cmd = new CreateObjectCommand(context.getRegistry(), wegpunkt);
                        context.getCommandHistory().executeCommand(cmd);
                        collectedWaypointIds.add(wpId);
                    }
                } else if (!hit.id().equals(sourceObjectId) && hit.type() != FmcType.WAYPOINT) {
                    // Klick auf anderes Objekt -> Verbindung abschließen
                    if (eventBus != null) {
                        eventBus.publish(new EditorActionEvent.CreateConnection(sourceObjectId, hit.id(), new ArrayList<>(collectedWaypointIds)));
                    } else {
                        var cmd = new CreateConnectionCommand(context.getRegistry(), sourceObjectId, hit.id(), collectedWaypointIds);
                        cmd.execute();

                        if (cmd.isSuccess()) {
                            context.getCommandHistory().addExecutedCommand(cmd);
                            context.getSelectionModel().clearObjectSelection();
                            sourceObjectId = null;
                            collectedWaypointIds.clear();
                            context.reactivateCurrentTool();
                        }
                    }
                }
            }
        }
    }
}
