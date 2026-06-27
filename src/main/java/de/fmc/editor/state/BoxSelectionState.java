package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import javafx.scene.shape.Rectangle;

public class BoxSelectionState implements EditorState {
    private final Rectangle selectionRect;
    private final double selectionStartWorldX;
    private final double selectionStartWorldY;
    private boolean isDragging = false;

    public BoxSelectionState(InteractionEventData event, CanvasController context) {
        this.selectionStartWorldX = event.worldX();
        this.selectionStartWorldY = event.worldY();
        
        this.selectionRect = new Rectangle(selectionStartWorldX, selectionStartWorldY, 0, 0);
        this.selectionRect.setFill(javafx.scene.paint.Color.web("#0078D7", 0.3));
        this.selectionRect.setStroke(javafx.scene.paint.Color.web("#0078D7"));
        this.selectionRect.getStrokeDashArray().addAll(5.0, 5.0);
        
        context.getDrawingPane().getUiLayer().getChildren().add(selectionRect);
        this.isDragging = true;
    }

    @Override
    public void enterState(CanvasController context) {}

    @Override
    public void exitState(CanvasController context) {
        context.getDrawingPane().getUiLayer().getChildren().remove(selectionRect);
    }

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        // Drag: Wenn primäre Taste gedrückt bleibt und wir ziehen
        if (event.isPrimaryButtonDown() && isDragging && event.activeKey().isEmpty()) {
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
            return;
        }

        // Release: Wenn die primäre Taste losgelassen wird (oder drag beendet ist)
        if (!event.isPrimaryButtonDown() && isDragging) {
            isDragging = false;
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
}
