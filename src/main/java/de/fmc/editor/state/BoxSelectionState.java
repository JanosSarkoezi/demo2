package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import javafx.scene.shape.Rectangle;
import java.util.UUID;

public class BoxSelectionState implements EditorState {
    private final Rectangle selectionRect;
    private final double selectionStartWorldX;
    private final double selectionStartWorldY;

    public BoxSelectionState(MouseEventData event, CanvasController context) {
        this.selectionStartWorldX = event.worldX();
        this.selectionStartWorldY = event.worldY();
        
        this.selectionRect = new Rectangle(selectionStartWorldX, selectionStartWorldY, 0, 0);
        this.selectionRect.setFill(javafx.scene.paint.Color.web("#0078D7", 0.3));
        this.selectionRect.setStroke(javafx.scene.paint.Color.web("#0078D7"));
        this.selectionRect.getStrokeDashArray().addAll(5.0, 5.0);
        
        context.getDrawingPane().getUiLayer().getChildren().add(selectionRect);
    }

    @Override
    public void enterState(CanvasController context) {}

    @Override
    public void exitState(CanvasController context) {
        context.getDrawingPane().getUiLayer().getChildren().remove(selectionRect);
    }

    @Override
    public void handleMousePressed(MouseEventData event, CanvasController context) {}

    @Override
    public void handleMouseDragged(MouseEventData event, CanvasController context) {
        double currentX = event.worldX();
        double currentY = event.worldY();
        
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
    public void handleMouseReleased(MouseEventData event, CanvasController context) {
        var foundIds = context.findObjectsInBounds(
            selectionStartWorldX, selectionStartWorldY, 
            event.worldX(), event.worldY()
        );
        
        // Da wir zum Starten CTRL brauchen, fügen wir zur bestehenden Auswahl hinzu
        context.getSelectedObjectIds().addAll(foundIds);
        context.updateSelectionInView();
        
        context.setCurrentState(new IdleState());
    }
}
