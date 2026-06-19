package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;

public class PanningState implements EditorState {
    private final double startTranslateX;
    private final double startTranslateY;
    private final double startMouseX;
    private final double startMouseY;

    public PanningState(MouseEventData event, CanvasController context) {
        this.startMouseX = event.sceneX();
        this.startMouseY = event.sceneY();
        this.startTranslateX = context.getDrawingPane().getWorld().getTranslateX();
        this.startTranslateY = context.getDrawingPane().getWorld().getTranslateY();
    }

    @Override
    public void enterState(CanvasController context) {}

    @Override
    public void exitState(CanvasController context) {}

    @Override
    public void handleMousePressed(MouseEventData event, CanvasController context) {}

    @Override
    public void handleMouseDragged(MouseEventData event, CanvasController context) {
        double deltaX = event.sceneX() - startMouseX;
        double deltaY = event.sceneY() - startMouseY;

        context.getDrawingPane().getWorld().setTranslateX(startTranslateX + deltaX);
        context.getDrawingPane().getWorld().setTranslateY(startTranslateY + deltaY);
    }

    @Override
    public void handleMouseReleased(MouseEventData event, CanvasController context) {
        context.reactivateCurrentTool();
    }
}
