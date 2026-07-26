package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.command.MoveMultipleObjectsCommand;
import de.fmc.editor.core.model.FmcObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoveObjectsDragHandler implements DragHandler {
    private final CanvasController context;
    private final UUID primaryDraggedId;
    private final Map<UUID, MoveMultipleObjectsCommand.Position> initialPositions = new HashMap<>();
    private double startMouseWorldX;
    private double startMouseWorldY;
    private boolean hasMoved = false;

    public MoveObjectsDragHandler(UUID primaryId, CanvasController context) {
        this.primaryDraggedId = primaryId;
        this.context = context;
    }

    @Override
    public void onStart(InteractionEventData startEvent) {
        this.startMouseWorldX = startEvent.worldX();
        this.startMouseWorldY = startEvent.worldY();

        var selectedIds = context.getSelectionModel().getSelectedObjectIds();

        // Initiale Positionen aller selektierten Objekte sichern
        for (UUID id : selectedIds) {
            FmcObject obj = context.getRegistry().getObject(id);
            if (obj != null) {
                initialPositions.put(id, new MoveMultipleObjectsCommand.Position(obj.x(), obj.y()));
            }
        }

        // Sicherstellen, dass das primäre Objekt mitverschoben wird
        if (!initialPositions.containsKey(primaryDraggedId)) {
            FmcObject hit = context.getRegistry().getObject(primaryDraggedId);
            if (hit != null) {
                initialPositions.put(primaryDraggedId, new MoveMultipleObjectsCommand.Position(hit.x(), hit.y()));
            }
        }
    }

    @Override
    public void onDrag(InteractionEventData currentEvent, double deltaX, double deltaY) {
        if (initialPositions.isEmpty()) return;

        hasMoved = true;
        double currentDeltaX = currentEvent.worldX() - startMouseWorldX;
        double currentDeltaY = currentEvent.worldY() - startMouseWorldY;

        if (context.getToolbarController().isSnapToGrid()) {
            double primaryInitialX = initialPositions.get(primaryDraggedId).x();
            double primaryInitialY = initialPositions.get(primaryDraggedId).y();
            double targetX = primaryInitialX + currentDeltaX;
            double targetY = primaryInitialY + currentDeltaY;

            int gridSize = 20;
            double snappedX = Math.round(targetX / gridSize) * gridSize;
            double snappedY = Math.round(targetY / gridSize) * gridSize;

            currentDeltaX = snappedX - primaryInitialX;
            currentDeltaY = snappedY - primaryInitialY;
        }

        for (var entry : initialPositions.entrySet()) {
            double newX = entry.getValue().x() + currentDeltaX;
            double newY = entry.getValue().y() + currentDeltaY;
            context.getRegistry().moveObject(entry.getKey(), newX, newY);
        }
    }

    @Override
    public void onEnd(InteractionEventData endEvent) {
        if (hasMoved) {
            Map<UUID, MoveMultipleObjectsCommand.Position> currentPositions = new HashMap<>();
            for (UUID id : initialPositions.keySet()) {
                FmcObject obj = context.getRegistry().getObject(id);
                if (obj != null) {
                    currentPositions.put(id, new MoveMultipleObjectsCommand.Position(obj.x(), obj.y()));
                }
            }
            var cmd = new MoveMultipleObjectsCommand(context.getRegistry(), initialPositions, currentPositions);
            context.getCommandHistory().addExecutedCommand(cmd);
        }
        context.reactivateCurrentTool();
    }
}
