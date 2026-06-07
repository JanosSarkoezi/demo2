package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.model.FmcObject;
import javafx.scene.input.MouseEvent;
import java.util.UUID;

public class SelectOrMoveState implements EditorState {
    protected UUID draggedObjectId = null;
    protected double offsetX;
    protected double offsetY;
    
    // Panning Felder
    private boolean isPanning = false;
    private double startTranslateX;
    private double startTranslateY;
    private double startMouseX;
    private double startMouseY;

    @Override
    public void handleMousePressed(MouseEvent event, CanvasController context) {
        javafx.geometry.Point2D worldPos = context.getWorldPoint(event);
        FmcObject hit = context.findObjectAt(worldPos.getX(), worldPos.getY());

        if (hit != null) {
            isPanning = false;
            if (event.getClickCount() == 2) {
                // Doppel-Klick wechselt in den Resize-Modus
                ResizeState nextState = new ResizeState(hit.id());
                context.setCurrentState(nextState);
                nextState.enterState(context);
            } else {
                draggedObjectId = hit.id();
                // Offset berechnen: Wo innerhalb des Objekts wurde geklickt?
                offsetX = worldPos.getX() - hit.x();
                offsetY = worldPos.getY() - hit.y();
            }
        } else {
            // Panning starten
            draggedObjectId = null;
            isPanning = true;
            startMouseX = event.getSceneX();
            startMouseY = event.getSceneY();
            startTranslateX = context.getDrawingPane().getWorld().getTranslateX();
            startTranslateY = context.getDrawingPane().getWorld().getTranslateY();
        }
    }

    @Override
    public void handleMouseDragged(MouseEvent event, CanvasController context) {
        if (draggedObjectId != null) {
            javafx.geometry.Point2D worldPos = context.getWorldPoint(event);
            context.getRegistry().getObjects().stream()
                .filter(obj -> obj.id().equals(draggedObjectId))
                .findFirst()
                .ifPresent(obj -> {
                    double newX = worldPos.getX() - offsetX;
                    double newY = worldPos.getY() - offsetY;

                    if (context.getToolbarController().isSnapToGrid()) {
                        int gridSize = 20;
                        newX = Math.round(newX / gridSize) * gridSize;
                        newY = Math.round(newY / gridSize) * gridSize;
                    }

                    context.getRegistry().moveObject(draggedObjectId, newX, newY);
                });
        } else if (isPanning) {
            // Panning ausführen
            double deltaX = event.getSceneX() - startMouseX;
            double deltaY = event.getSceneY() - startMouseY;
            
            context.getDrawingPane().getWorld().setTranslateX(startTranslateX + deltaX);
            context.getDrawingPane().getWorld().setTranslateY(startTranslateY + deltaY);
        }
    }

    @Override
    public void handleMouseReleased(MouseEvent event, CanvasController context) {
        draggedObjectId = null;
        isPanning = false;
    }
}
