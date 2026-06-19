package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.CreateConnectionCommand;
import de.fmc.editor.core.command.CreateObjectCommand;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateConnectionState extends AbstractEditorState {
    private UUID sourceObjectId = null;
    private final List<UUID> collectedWaypointIds = new ArrayList<>();
    private boolean connectionFinished = true;

    public CreateConnectionState() {
        // 1. Schritt: Startobjekt auswählen
        pressedActions.add(new MouseAction(
            MouseMatchers.all(
                MouseMatchers.primaryButton(),
                (e, ctx) -> sourceObjectId == null,
                MouseMatchers.objectHit()
            ),
            (event, context) -> {
                FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
                if (hit != null && hit.type() != FmcType.WEGPUNKT) {
                    sourceObjectId = hit.id();
                    connectionFinished = false;

                    context.getSelectedObjectIds().clear();
                    context.getSelectedObjectIds().add(sourceObjectId);
                    context.updateSelectionInView();

                    System.out.println("Start-Objekt fuer Verbindung gewaehlt: " + sourceObjectId);
                }
            }
        ));

        // 2a. Schritt: Wenn bereits ein Startobjekt existiert und ins Leere geklickt wird -> Wegpunkt hinzufügen
        pressedActions.add(new MouseAction(
            MouseMatchers.all(
                MouseMatchers.primaryButton(),
                (e, ctx) -> sourceObjectId != null,
                MouseMatchers.noObjectHit()
            ),
            (event, context) -> {
                UUID wpId = UUID.randomUUID();
                var wegpunkt = new FmcObject(wpId, FmcType.WEGPUNKT, event.worldX(), event.worldY(), 10, 10, CoreRegistry.WAYPOINT_LAYER_ID);

                var cmd = new CreateObjectCommand(context.getRegistry(), wegpunkt);
                context.getCommandHistory().executeCommand(cmd);

                collectedWaypointIds.add(wpId);
                System.out.println("Wegpunkt hinzugefuegt an: " + event.worldX() + ", " + event.worldY());
            }
        ));

        // 2b. Schritt: Wenn bereits ein Startobjekt existiert und auf ein anderes Objekt geklickt wird -> Verbindung abschließen
        pressedActions.add(new MouseAction(
            MouseMatchers.all(
                MouseMatchers.primaryButton(),
                (e, ctx) -> sourceObjectId != null,
                MouseMatchers.objectHit()
            ),
            (event, context) -> {
                FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
                if (hit != null && !hit.id().equals(sourceObjectId) && hit.type() != FmcType.WEGPUNKT) {
                    var cmd = new CreateConnectionCommand(context.getRegistry(), sourceObjectId, hit.id(), collectedWaypointIds);
                    
                    cmd.execute();

                    if (cmd.isSuccess()) {
                        context.getCommandHistory().addExecutedCommand(cmd);
                        System.out.println("Polygon-Verbindung erfolgreich via Command erstellt!");
                        connectionFinished = true;

                        context.getSelectedObjectIds().clear();
                        context.updateSelectionInView();

                        sourceObjectId = null;
                        collectedWaypointIds.clear();

                        context.reactivateCurrentTool();
                    } else {
                        System.out.println("Verbindung existiert bereits oder ist ungueltig! Raeume Wegpunkte auf...");
                        cleanupCollectedWaypoints(context);
                    }
                }
            }
        ));
    }

    @Override
    public void enterState(CanvasController context) {
        System.out.println("Verbindungs-Modus aktiv: Blende Wegpunkte ein.");
        context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
    }

    @Override
    public void exitState(CanvasController context) {
        if (!connectionFinished) {
            cleanupCollectedWaypoints(context);
        }
        
        context.getSelectedObjectIds().clear();
        context.updateSelectionInView();

        System.out.println("Verbindungs-Modus verlassen: Blende Wegpunkte aus.");
        if (!context.getToolbarController().isWaypointsVisible()) {
            context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
        }
    }

    private void cleanupCollectedWaypoints(CanvasController context) {
        int count = collectedWaypointIds.size();
        for (int i = 0; i < count; i++) {
            context.getCommandHistory().undo();
        }
        
        context.getCommandHistory().clearRedoStack();

        if (sourceObjectId != null) {
            context.getSelectedObjectIds().remove(sourceObjectId);
            context.updateSelectionInView();
        }

        sourceObjectId = null;
        collectedWaypointIds.clear();
        connectionFinished = true;
    }
}
