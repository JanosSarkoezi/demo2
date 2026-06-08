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

public class CreateConnectionState implements EditorState {
    private UUID sourceObjectId = null;
    private final List<UUID> collectedWaypointIds = new ArrayList<>();
    private boolean connectionFinished = true;

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
        System.out.println("Verbindungs-Modus verlassen: Blende Wegpunkte aus.");
        context.getRegistry().setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
    }

    @Override
    public void handleMousePressed(MouseEventData event, CanvasController context) {
        FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());

        // 1. Schritt: Startobjekt auswählen
        if (sourceObjectId == null) {
            if (hit != null && hit.type() != FmcType.WEGPUNKT) {
                sourceObjectId = hit.id();
                connectionFinished = false;
                System.out.println("Start-Objekt fuer Verbindung gewaehlt: " + sourceObjectId);
            }
            return;
        }

        // 2. Schritt: Wenn bereits ein Startobjekt existiert...
        if (hit == null) {
            // Klick ins Leere -> Gelben Wegpunkt erzeugen!
            UUID wpId = UUID.randomUUID();
            // Kleine Standardgröße für den Wegpunkt (z.B. 10x10)
            var wegpunkt = new FmcObject(wpId, FmcType.WEGPUNKT, event.worldX(), event.worldY(), 10, 10, CoreRegistry.WAYPOINT_LAYER_ID);

            // Per Command in die Registry einfügen (wichtig für Undo/Redo!)
            var cmd = new CreateObjectCommand(context.getRegistry(), wegpunkt);
            context.getCommandHistory().executeCommand(cmd);

            // In unserer temporären Pfad-Liste merken
            collectedWaypointIds.add(wpId);
            System.out.println("Wegpunkt hinzugefuegt an: " + event.worldX() + ", " + event.worldY());

        } else if (!hit.id().equals(sourceObjectId) && hit.type() != FmcType.WEGPUNKT) {
            // Klick auf ein ANDERES valides FMC-Objekt -> Pfad abschließen!
            var cmd = new CreateConnectionCommand(context.getRegistry(), sourceObjectId, hit.id(), collectedWaypointIds);
            
            // Erst ausführen, dann prüfen. Wenn es fehlschlägt, landet es NICHT in der History.
            cmd.execute();

            if (cmd.isSuccess()) {
                context.getCommandHistory().addExecutedCommand(cmd);
                System.out.println("Polygon-Verbindung erfolgreich via Command erstellt!");
                connectionFinished = true;
                // Reset für die nächste Verbindung im selben State
                sourceObjectId = null;
                collectedWaypointIds.clear();
            } else {
                System.out.println("Verbindung existiert bereits oder ist ungueltig! Raeume Wegpunkte auf...");
                cleanupCollectedWaypoints(context);
            }
        }
    }

    @Override
    public void handleMouseDragged(MouseEventData event, CanvasController context) {
    }

    @Override
    public void handleMouseReleased(MouseEventData event, CanvasController context) {
    }

    private void cleanupCollectedWaypoints(CanvasController context) {
        // Rückgängig machen der Wegpunkt-Erstellungen über die CommandHistory,
        // damit der Undo-Stack sauber bleibt.
        int count = collectedWaypointIds.size();
        for (int i = 0; i < count; i++) {
            context.getCommandHistory().undo();
        }
        
        // Da diese Wegpunkte verworfen wurden, löschen wir sie auch aus dem Redo-Stack
        context.getCommandHistory().clearRedoStack();

        sourceObjectId = null;
        collectedWaypointIds.clear();
        connectionFinished = true;
    }
}
