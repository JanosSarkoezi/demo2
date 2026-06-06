package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.model.FmcObject;
import javafx.scene.input.MouseEvent;
import java.util.UUID;

public class SelectOrMoveState implements EditorState {
    protected UUID draggedObjectId = null;
    protected double offsetX;
    protected double offsetY;

    @Override
    public void handleMousePressed(MouseEvent event, CanvasController context) {
        FmcObject hit = context.findObjectAt(event.getX(), event.getY());
        
        if (hit != null) {
            if (event.getClickCount() == 2) {
                // Doppel-Klick wechselt in den Resize-Modus
                ResizeState nextState = new ResizeState(hit.id());
                context.setCurrentState(nextState);
                nextState.enterState(context);
            } else {
                draggedObjectId = hit.id();
                // Offset berechnen: Wo innerhalb des Objekts wurde geklickt?
                offsetX = event.getX() - hit.x();
                offsetY = event.getY() - hit.y();
            }
        } else {
            draggedObjectId = null;
        }
    }

    @Override
    public void handleMouseDragged(MouseEvent event, CanvasController context) {
        if (draggedObjectId != null) {
            context.getRegistry().getObjects().stream()
                .filter(obj -> obj.id().equals(draggedObjectId))
                .findFirst()
                .ifPresent(obj -> {
                    // Neue Zielposition basierend auf Mausposition abzüglich des initialen Offsets
                    double newX = event.getX() - offsetX;
                    double newY = event.getY() - offsetY;

                    if (context.getToolbarController().isSnapToGrid()) {
                        int gridSize = 20;
                        newX = Math.round(newX / gridSize) * gridSize;
                        newY = Math.round(newY / gridSize) * gridSize;
                    }

                    context.getRegistry().moveObject(draggedObjectId, newX, newY);
                });
        }
    }

    @Override
    public void handleMouseReleased(MouseEvent event, CanvasController context) {
        draggedObjectId = null;
    }
}
