package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.CreateObjectCommand;
import de.fmc.editor.core.event.EventBus;
import de.fmc.editor.core.event.EditorActionEvent;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcType;
import javafx.scene.input.KeyCode;
import java.util.UUID;

public class CreateObjectState implements EditorState {
    private final FmcType typeToCreate;
    private final EventBus eventBus;
    private InteractionMap bindings;

    public CreateObjectState(FmcType type) {
        this(type, null);
    }

    public CreateObjectState(FmcType type, EventBus eventBus) {
        this.typeToCreate = type;
        this.eventBus = eventBus;
    }

    private void initBindings(CanvasController context) {
        if (bindings != null) return;

        bindings = new InteractionMap();

        // ESC -> Zurück zu Idle (Tool wechseln)
        bindings.on(EventMatcher.keyPressed(KeyCode.ESCAPE), event -> {
            if (eventBus != null) {
                eventBus.publish(new EditorActionEvent.ResetToIdle());
            } else {
                context.resetToIdleState();
            }
        });

        // Primärklick -> Neues Objekt erzeugen
        bindings.on(
            EventMatcher.primaryClick().and(event -> context.findObjectAt(event.worldX(), event.worldY()) == null),
            event -> {
                double x = event.worldX();
                double y = event.worldY();

                if (context.isSnapToGrid()) {
                    int gridSize = 20;
                    x = Math.round(x / gridSize) * gridSize;
                    y = Math.round(y / gridSize) * gridSize;
                }

                if (eventBus != null) {
                    eventBus.publish(new EditorActionEvent.CreateObject(typeToCreate, x, y));
                } else {
                    UUID layerId = CoreRegistry.DEFAULT_LAYER_ID;
                    var obj = FmcFactory.createObject(typeToCreate, x, y, layerId);
                    var cmd = new CreateObjectCommand(context.getRegistry(), obj);
                    context.getCommandHistory().executeCommand(cmd);

                    if (!context.getToolbarController().isSticky()) {
                        context.reactivateCurrentTool();
                    }
                }
            }
        );
    }

    @Override
    public InteractionMap getInteractionMap() {
        return bindings;
    }

    @Override
    public void enterState(CanvasController context) {
        initBindings(context);
    }

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        initBindings(context);
        if (event.activeKey().isPresent()) {
            bindings.handlePress(event);
        } else if (event.isPrimaryButtonDown()) {
            bindings.handlePress(event);
        } else {
            bindings.handleRelease(event);
        }
    }
}
