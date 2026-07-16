package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.command.MoveMultipleObjectsCommand;
import de.fmc.editor.core.model.FmcObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DragObjectsState implements EditorState {
    private final UUID primaryDraggedId;
    private final Map<UUID, MoveMultipleObjectsCommand.Position> initialPositions = new HashMap<>();
    private final double startMouseWorldX;
    private final double startMouseWorldY;
    private boolean hasMoved = false;
    private boolean isDragging = false;

    public DragObjectsState(UUID primaryId, InteractionEventData event, CanvasController context) {
        this.primaryDraggedId = primaryId;
        this.startMouseWorldX = event.worldX();
        this.startMouseWorldY = event.worldY();
        this.isDragging = true;

        var selectedIds = context.getSelectedObjectIds();

        // OPTIMIERUNG 1: Direkter Zugriff über getObject() in O(1) statt Stream-Suche
        for (UUID id : selectedIds) {
            FmcObject obj = context.getRegistry().getObject(id);
            if (obj != null) {
                initialPositions.put(id, new MoveMultipleObjectsCommand.Position(obj.x(), obj.y()));
            }
        }

        // OPTIMIERUNG 2: Auch hier für das primär gezogene Objekt direkter Zugriff
        if (!initialPositions.containsKey(primaryDraggedId)) {
            FmcObject hit = context.getRegistry().getObject(primaryDraggedId);
            if (hit != null) {
                initialPositions.put(primaryId, new MoveMultipleObjectsCommand.Position(hit.x(), hit.y()));
            }
        }
    }

    @Override
    public void enterState(CanvasController context) {}

    @Override
    public void exitState(CanvasController context) {}

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        // Drag
        if (event.isPrimaryButtonDown() && isDragging && event.activeKey().isEmpty()) {
            if (initialPositions.isEmpty()) return;

            hasMoved = true;
            double deltaX = event.worldX() - startMouseWorldX;
            double deltaY = event.worldY() - startMouseWorldY;

            if (context.getToolbarController().isSnapToGrid()) {
                double primaryInitialX = initialPositions.get(primaryDraggedId).x();
                double primaryInitialY = initialPositions.get(primaryDraggedId).y();
                double targetX = primaryInitialX + deltaX;
                double targetY = primaryInitialY + deltaY;

                int gridSize = 20;
                double snappedX = Math.round(targetX / gridSize) * gridSize;
                double snappedY = Math.round(targetY / gridSize) * gridSize;

                deltaX = snappedX - primaryInitialX;
                deltaY = snappedY - primaryInitialY;
            }

            for (var entry : initialPositions.entrySet()) {
                double newX = entry.getValue().x() + deltaX;
                double newY = entry.getValue().y() + deltaY;
                context.getRegistry().moveObject(entry.getKey(), newX, newY);
            }
            return;
        }

        // Release
        if (!event.isPrimaryButtonDown() && isDragging) {
            isDragging = false;
            if (hasMoved) {
                Map<UUID, MoveMultipleObjectsCommand.Position> currentPositions = new HashMap<>();

                // OPTIMIERUNG 3: Ermittlung der Endpositionen ebenfalls per Direktzugriff in O(1)
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
}