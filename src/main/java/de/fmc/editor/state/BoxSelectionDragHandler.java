package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.model.SelectionModel;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class BoxSelectionDragHandler implements DragHandler {
    private final CanvasController context;
    private Rectangle selectionRect;
    private double selectionStartWorldX;
    private double selectionStartWorldY;

    public BoxSelectionDragHandler(CanvasController context) {
        this.context = context;
    }

    @Override
    public void onStart(InteractionEventData startEvent) {
        this.selectionStartWorldX = startEvent.worldX();
        this.selectionStartWorldY = startEvent.worldY();

        this.selectionRect = new Rectangle(selectionStartWorldX, selectionStartWorldY, 0, 0);
        this.selectionRect.setFill(Color.web("#0078D7", 0.3));
        this.selectionRect.setStroke(Color.web("#0078D7"));
        this.selectionRect.getStrokeDashArray().addAll(5.0, 5.0);

        context.getDrawingPane().getUiLayer().getChildren().add(selectionRect);
    }

    @Override
    public void onDrag(InteractionEventData currentEvent, double deltaX, double deltaY) {
        double currentX = currentEvent.worldX();
        double currentY = currentEvent.worldY();

        double minX = Math.min(selectionStartWorldX, currentX);
        double minY = Math.min(selectionStartWorldY, currentY);
        double width = Math.abs(selectionStartWorldX - currentX);
        double height = Math.abs(selectionStartWorldY - currentY);

        selectionRect.setX(minX);
        selectionRect.setY(minY);
        selectionRect.setWidth(width);
        selectionRect.setHeight(height);
    }

    @Override
    public void onEnd(InteractionEventData endEvent) {
        context.getDrawingPane().getUiLayer().getChildren().remove(selectionRect);

        var foundIds = context.findObjectsInBounds(
                selectionStartWorldX, selectionStartWorldY,
                endEvent.worldX(), endEvent.worldY()
        );

        SelectionModel selectionModel = context.getSelectionModel();
        selectionModel.clearObjectSelection();
        selectionModel.addAllObjectsToSelection(foundIds);

        context.reactivateCurrentTool();
    }
}
