package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.CreateObjectCommand;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcType;
import java.util.UUID;

public class CreateState extends SelectOrMoveState {

    @Override
    public void handleMousePressed(MouseEventData event, CanvasController context) {
        if (!event.isPrimaryButtonDown()) return;

        var hit = context.findObjectAt(event.worldX(), event.worldY());
        if (hit != null) {
            // Wir nutzen die Bewegungs-Logik der Basisklasse,
            // bleiben aber in diesem Zustand und lassen die Toolbar aktiv.
            super.handleMousePressed(event, context);
        } else {
            FmcType selectedType = context.getToolbarController().getSelectedType();
            if (selectedType != null) {
                double createX = event.worldX();
                double createY = event.worldY();

                if (context.getToolbarController().isSnapToGrid()) {
                    int gridSize = 20;
                    createX = Math.round(createX / gridSize) * gridSize;
                    createY = Math.round(createY / gridSize) * gridSize;
                }

                UUID layerId = CoreRegistry.DEFAULT_LAYER_ID; 
                var obj = FmcFactory.createObject(selectedType, createX, createY, layerId);
                
                // Über CommandHistory ausführen statt direkt adden
                var cmd = new CreateObjectCommand(context.getRegistry(), obj);
                context.getCommandHistory().executeCommand(cmd);
            }
        }
    }
}
