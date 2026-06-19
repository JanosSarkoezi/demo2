package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.controller.Tool;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.CreateObjectCommand;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcType;
import java.util.UUID;

public class CreateState extends IdleState {

    public CreateState(FmcType typeToCreate) {
        super();

        // 0. Erstellungsregel mit hoher Priorität (Index 0): Führt Erstellung aus, wenn ins Leere geklickt wird
        pressedActions.add(0, new MouseAction(
            MouseMatchers.all(
                MouseMatchers.primaryButton(),
                MouseMatchers.noObjectHit()
            ),
            (event, context) -> {
                if (typeToCreate != null) {
                    double createX = event.worldX();
                    double createY = event.worldY();

                    if (context.getToolbarController().isSnapToGrid()) {
                        int gridSize = 20;
                        createX = Math.round(createX / gridSize) * gridSize;
                        createY = Math.round(createY / gridSize) * gridSize;
                    }

                    UUID layerId = CoreRegistry.DEFAULT_LAYER_ID; 
                    var obj = FmcFactory.createObject(typeToCreate, createX, createY, layerId);
                    
                    var cmd = new CreateObjectCommand(context.getRegistry(), obj);
                    context.getCommandHistory().executeCommand(cmd);
                    context.reactivateCurrentTool();
                }
            }
        ));
    }
}
