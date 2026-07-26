package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;

public class PanningDragHandler implements DragHandler {
    private final CanvasController context;
    private double startTranslateX;
    private double startTranslateY;
    private double startMouseX;
    private double startMouseY;

    public PanningDragHandler(CanvasController context) {
        this.context = context;
    }

    @Override
    public void onStart(InteractionEventData startEvent) {
        this.startMouseX = startEvent.sceneX();
        this.startMouseY = startEvent.sceneY();
        this.startTranslateX = context.getDrawingPane().getWorld().getTranslateX();
        this.startTranslateY = context.getDrawingPane().getWorld().getTranslateY();
    }

    @Override
    public void onDrag(InteractionEventData currentEvent, double deltaX, double deltaY) {
        double currentDeltaX = currentEvent.sceneX() - startMouseX;
        double currentDeltaY = currentEvent.sceneY() - startMouseY;

        context.getDrawingPane().getWorld().setTranslateX(startTranslateX + currentDeltaX);
        context.getDrawingPane().getWorld().setTranslateY(startTranslateY + currentDeltaY);
    }

    @Override
    public void onEnd(InteractionEventData endEvent) {
        context.reactivateCurrentTool();
    }
}
