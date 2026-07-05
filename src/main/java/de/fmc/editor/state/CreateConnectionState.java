package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.CreateConnectionCommand;
import de.fmc.editor.core.command.CreateObjectCommand;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import javafx.scene.input.KeyCode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateConnectionState implements EditorState {
    private UUID sourceObjectId = null;
    private final List<UUID> collectedWaypointIds = new ArrayList<>();
    private boolean connectionFinished = true;

    @Override
    public void enterState(CanvasController context) {
//        System.out.println("Verbindungs-Modus aktiv: Blende Wegpunkte ein.");
        context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
    }

    @Override
    public void exitState(CanvasController context) {
        if (!connectionFinished) {
            cleanupCollectedWaypoints(context);
        }
        
        context.getSelectedObjectIds().clear();
        context.updateSelectionInView();

//        System.out.println("Verbindungs-Modus verlassen: Blende Wegpunkte aus.");
        if (!context.getToolbarController().isWaypointsVisible()) {
            context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
        }
    }

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        // ESC -> Tool wechseln/abbrechen
        if (event.activeKey().isPresent() && event.activeKey().get() == KeyCode.ESCAPE) {
            context.reactivateCurrentTool();
            return;
        }

        if (event.clickCount() == 1 && event.isPrimaryButtonDown() && event.activeKey().isEmpty()) {
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());

            if (sourceObjectId == null) {
                // 1. Schritt: Startobjekt auswählen
                if (hit != null && hit.type() != FmcType.WEGPUNKT) {
                    sourceObjectId = hit.id();
                    connectionFinished = false;

                    context.getSelectedObjectIds().clear();
                    context.getSelectedObjectIds().add(sourceObjectId);
                    context.updateSelectionInView();

//                    System.out.println("Start-Objekt fuer Verbindung gewaehlt: " + sourceObjectId);
                }
            } else {
                // 2. Schritt: Wenn bereits ein Startobjekt existiert
                if (hit == null) {
                    // Klick ins Leere -> Wegpunkt hinzufügen
                    UUID wpId = UUID.randomUUID();
                    var wegpunkt = new FmcObject(wpId, FmcType.WEGPUNKT, event.worldX(), event.worldY(), 10, 10, CoreRegistry.WAYPOINT_LAYER_ID);

                    var cmd = new CreateObjectCommand(context.getRegistry(), wegpunkt);
                    context.getCommandHistory().executeCommand(cmd);

                    collectedWaypointIds.add(wpId);
//                    System.out.println("Wegpunkt hinzugefuegt an: " + event.worldX() + ", " + event.worldY());
                } else if (!hit.id().equals(sourceObjectId) && hit.type() != FmcType.WEGPUNKT) {
                    // Klick auf anderes Objekt -> Verbindung abschließen
                    var cmd = new CreateConnectionCommand(context.getRegistry(), sourceObjectId, hit.id(), collectedWaypointIds);
                    cmd.execute();

                    if (cmd.isSuccess()) {
                        context.getCommandHistory().addExecutedCommand(cmd);
//                        System.out.println("Polygon-Verbindung erfolgreich via Command erstellt!");
                        connectionFinished = true;

                        context.getSelectedObjectIds().clear();
                        context.updateSelectionInView();

                        sourceObjectId = null;
                        collectedWaypointIds.clear();

                        context.reactivateCurrentTool();
                    } else {
                        // System.out.println("Verbindung existiert bereits oder ist ungueltig! Raeume Wegpunkte auf...");
                        cleanupCollectedWaypoints(context);
                    }
                }
            }
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
