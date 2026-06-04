package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcType;
import javafx.scene.input.MouseEvent;
import java.util.UUID;

public class CreateState implements EditorState {

    @Override
    public void handleMousePressed(MouseEvent event, CanvasController context) {
        if (event.isPrimaryButtonDown()) {
            FmcType selectedType = context.getToolbarController().getSelectedType();
            
            if (selectedType != null) {
                UUID layerId = UUID.randomUUID(); 
                var obj = FmcFactory.createObject(selectedType, event.getX(), event.getY(), layerId);
                context.getRegistry().addObject(obj);
                
                // Falls "sticky" (Checkbox in Toolbar) NICHT aktiv ist, 
                // wechseln wir nach dem Klick sofort zurück in den Auswahlmodus
                if (!context.getToolbarController().isSticky()) {
                    context.getToolbarController().clearSelection();
                    context.setCurrentState(new SelectOrMoveState());
                }
            }
        }
    }

    @Override
    public void handleMouseDragged(MouseEvent event, CanvasController context) {}

    @Override
    public void handleMouseReleased(MouseEvent event, CanvasController context) {}
}
