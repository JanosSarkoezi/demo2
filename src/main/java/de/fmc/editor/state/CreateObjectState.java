package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.controller.Tool;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.CreateObjectCommand;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcType;
import javafx.scene.input.KeyCode;
import java.util.UUID;

public class CreateObjectState implements EditorState {
    private final FmcType typeToCreate;

    public CreateObjectState(FmcType type) {
        this.typeToCreate = type;
    }

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        // ESC -> zurück zu Idle (Tool wechseln)
        if (event.activeKey().isPresent() && event.activeKey().get() == KeyCode.ESCAPE) {
//            context.reactivateCurrentTool();
            context.setActiveTool(Tool.SELECT);
            return;
        }

        // Wir reagieren auf den Klick beim Loslassen (oder Drücken)
        // Wenn es ein primärer Mausklick ist, erstellen wir das Objekt
        if (event.clickCount() == 1 && event.isPrimaryButtonDown() && event.activeKey().isEmpty()) {
            if (context.findObjectAt(event.worldX(), event.worldY()) == null) {
                double x = event.worldX();
                double y = event.worldY();

                if (context.getToolbarController().isSnapToGrid()) {
                    int gridSize = 20;
                    x = Math.round(x / gridSize) * gridSize;
                    y = Math.round(y / gridSize) * gridSize;
                }

                UUID layerId = CoreRegistry.DEFAULT_LAYER_ID;
                var obj = FmcFactory.createObject(typeToCreate, x, y, layerId);
                var cmd = new CreateObjectCommand(context.getRegistry(), obj);
                context.getCommandHistory().executeCommand(cmd);

                if (!context.getToolbarController().isSticky()) {
                    context.reactivateCurrentTool();
                }
            }
        }
    }
}
