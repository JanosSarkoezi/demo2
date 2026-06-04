package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.model.FmcObject;
import javafx.scene.input.MouseEvent;
import java.util.UUID;

public class SelectOrMoveState implements EditorState {
    private UUID draggedObjectId = null;
    private double lastMouseX;
    private double lastMouseY;

    @Override
    public void handleMousePressed(MouseEvent event, CanvasController context) {
        FmcObject hit = context.findObjectAt(event.getX(), event.getY());
        
        if (hit != null) {
            draggedObjectId = hit.id();
            lastMouseX = event.getX();
            lastMouseY = event.getY();
        } else {
            draggedObjectId = null;
        }
    }

    @Override
    public void handleMouseDragged(MouseEvent event, CanvasController context) {
        if (draggedObjectId != null) {
            double deltaX = event.getX() - lastMouseX;
            double deltaY = event.getY() - lastMouseY;

            context.getRegistry().getObjects().stream()
                .filter(obj -> obj.id().equals(draggedObjectId))
                .findFirst()
                .ifPresent(obj -> {
                    double newX = obj.x() + deltaX;
                    double newY = obj.y() + deltaY;
                    context.getRegistry().moveObject(draggedObjectId, newX, newY);
                });

            lastMouseX = event.getX();
            lastMouseY = event.getY();
        }
    }

    @Override
    public void handleMouseReleased(MouseEvent event, CanvasController context) {
        draggedObjectId = null;
    }
}
