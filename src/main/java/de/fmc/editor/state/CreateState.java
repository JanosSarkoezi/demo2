package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcType;
import javafx.scene.input.MouseEvent;
import java.util.UUID;

public class CreateState extends SelectOrMoveState {

    @Override
    public void handleMousePressed(MouseEvent event, CanvasController context) {
        if (!event.isPrimaryButtonDown()) return;

        var hit = context.findObjectAt(event.getX(), event.getY());
        if (hit != null) {
            // Wir nutzen die Bewegungs-Logik der Basisklasse,
            // bleiben aber in diesem Zustand und lassen die Toolbar aktiv.
            super.handleMousePressed(event, context);
        } else {
            FmcType selectedType = context.getToolbarController().getSelectedType();
            if (selectedType != null) {
                UUID layerId = UUID.randomUUID(); 
                var obj = FmcFactory.createObject(selectedType, event.getX(), event.getY(), layerId);
                context.getRegistry().addObject(obj);
            }
        }
    }
}
