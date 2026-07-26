package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.command.UpdateTextCommand;
import de.fmc.editor.core.model.FmcText;
import java.util.UUID;

public class MoveTextDragHandler implements DragHandler {
    private final CanvasController context;
    private final UUID textId;
    private double startMouseWorldX;
    private double startMouseWorldY;
    private double initialTextX;
    private double initialTextY;
    private boolean hasMoved = false;

    public MoveTextDragHandler(UUID textId, CanvasController context) {
        this.textId = textId;
        this.context = context;
    }

    @Override
    public void onStart(InteractionEventData startEvent) {
        this.startMouseWorldX = startEvent.worldX();
        this.startMouseWorldY = startEvent.worldY();

        FmcText text = context.getRegistry().getText(textId);
        if (text != null) {
            this.initialTextX = text.x();
            this.initialTextY = text.y();
        }
    }

    @Override
    public void onDrag(InteractionEventData currentEvent, double deltaX, double deltaY) {
        FmcText text = context.getRegistry().getText(textId);
        if (text == null) return;

        hasMoved = true;
        double currentDeltaX = currentEvent.worldX() - startMouseWorldX;
        double currentDeltaY = currentEvent.worldY() - startMouseWorldY;

        double targetX = initialTextX + currentDeltaX;
        double targetY = initialTextY + currentDeltaY;

        if (context.getToolbarController().isSnapToGrid()) {
            int gridSize = 20;
            targetX = Math.round(targetX / gridSize) * gridSize;
            targetY = Math.round(targetY / gridSize) * gridSize;
        }

        FmcText updated = new FmcText(
                text.id(), text.text(),
                targetX, targetY,
                text.width(), text.fontFamily(), text.fontSize(),
                text.fontWeight(), text.fontStyle(), text.textFill(),
                text.parentObjectId(), text.layerId()
        );
        context.getRegistry().updateText(textId, updated);
    }

    @Override
    public void onEnd(InteractionEventData endEvent) {
        if (hasMoved) {
            FmcText currentText = context.getRegistry().getText(textId);
            if (currentText != null) {
                FmcText initialText = new FmcText(
                        currentText.id(), currentText.text(),
                        initialTextX, initialTextY,
                        currentText.width(), currentText.fontFamily(), currentText.fontSize(),
                        currentText.fontWeight(), currentText.fontStyle(), currentText.textFill(),
                        currentText.parentObjectId(), currentText.layerId()
                );
                var cmd = new UpdateTextCommand(context.getRegistry(), initialText, currentText);
                context.getCommandHistory().addExecutedCommand(cmd);
            }
        }
        context.reactivateCurrentTool();
    }
}
