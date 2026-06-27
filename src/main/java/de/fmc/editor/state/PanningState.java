package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;

public class PanningState implements EditorState {
    private final double startTranslateX;
    private final double startTranslateY;
    private final double startMouseX;
    private final double startMouseY;
    private boolean isPanning = false;

    public PanningState(InteractionEventData event, CanvasController context) {
        this.startMouseX = event.sceneX();
        this.startMouseY = event.sceneY();
        this.startTranslateX = context.getDrawingPane().getWorld().getTranslateX();
        this.startTranslateY = context.getDrawingPane().getWorld().getTranslateY();
        this.isPanning = true;
    }

    @Override
    public void enterState(CanvasController context) {}

    @Override
    public void exitState(CanvasController context) {}

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        if (event.isPrimaryButtonDown() && isPanning && event.activeKey().isEmpty()) {
            double deltaX = event.sceneX() - startMouseX;
            double deltaY = event.sceneY() - startMouseY;

            context.getDrawingPane().getWorld().setTranslateX(startTranslateX + deltaX);
            context.getDrawingPane().getWorld().setTranslateY(startTranslateY + deltaY);
            return;
        }

        if (!event.isPrimaryButtonDown() && isPanning) {
            isPanning = false;
            context.reactivateCurrentTool();
        }
    }
}
