package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.model.FmcObject;
import java.util.UUID;

public class SelectOrMoveState implements EditorState {
    protected UUID draggedObjectId = null;
    protected double offsetX;
    protected double offsetY;
    
    // Command-Tracking
    private double startX;
    private double startY;
    private double lastKnownX;
    private double lastKnownY;

    // Panning Felder
    private boolean isPanning = false;
    private double startTranslateX;
    private double startTranslateY;
    private double startMouseX;
    private double startMouseY;

    @Override
    public void handleMousePressed(MouseEventData event, CanvasController context) {
        FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());

        if (hit != null) {
            isPanning = false;
            if (event.clickCount() == 2) {
                // Doppel-Klick wechselt in den Resize-Modus
                ResizeState nextState = new ResizeState(hit.id());
                context.setCurrentState(nextState);
                nextState.enterState(context);
            } else {
                draggedObjectId = hit.id();
                // Offset berechnen: Wo innerhalb des Objekts wurde geklickt?
                offsetX = event.worldX() - hit.x();
                offsetY = event.worldY() - hit.y();
                
                // Ursprung merken für Command
                startX = hit.x();
                startY = hit.y();
                lastKnownX = hit.x();
                lastKnownY = hit.y();
            }
        } else {
            // Panning starten
            draggedObjectId = null;
            isPanning = true;
            startMouseX = event.sceneX();
            startMouseY = event.sceneY();
            startTranslateX = context.getDrawingPane().getWorld().getTranslateX();
            startTranslateY = context.getDrawingPane().getWorld().getTranslateY();
        }
    }

    @Override
    public void handleMouseDragged(MouseEventData event, CanvasController context) {
        if (draggedObjectId != null) {
            context.getRegistry().getObjects().stream()
                .filter(obj -> obj.id().equals(draggedObjectId))
                .findFirst()
                .ifPresent(obj -> {
                    double newX = event.worldX() - offsetX;
                    double newY = event.worldY() - offsetY;

                    if (context.getToolbarController().isSnapToGrid()) {
                        int gridSize = 20;
                        newX = Math.round(newX / gridSize) * gridSize;
                        newY = Math.round(newY / gridSize) * gridSize;
                    }

                    // Vorschau (direktes Update der Registry für flüssige UI)
                    context.getRegistry().moveObject(draggedObjectId, newX, newY);
                    lastKnownX = newX;
                    lastKnownY = newY;
                });
        } else if (isPanning) {
            // Panning ausführen
            double deltaX = event.sceneX() - startMouseX;
            double deltaY = event.sceneY() - startMouseY;
            
            context.getDrawingPane().getWorld().setTranslateX(startTranslateX + deltaX);
            context.getDrawingPane().getWorld().setTranslateY(startTranslateY + deltaY);
        }
    }

    @Override
    public void handleMouseReleased(MouseEventData event, CanvasController context) {
        if (draggedObjectId != null) {
            if (startX != lastKnownX || startY != lastKnownY) {
                var cmd = new de.fmc.editor.core.command.MoveObjectCommand(
                    context.getRegistry(), draggedObjectId, startX, startY, lastKnownX, lastKnownY
                );
                context.getCommandHistory().executeCommand(cmd);
            }
        }
        draggedObjectId = null;
        isPanning = false;
    }
}
