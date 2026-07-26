package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.CreateTextCommand;
import de.fmc.editor.core.event.EventBus;
import de.fmc.editor.core.event.EditorActionEvent;
import de.fmc.editor.core.model.FmcText;
import javafx.scene.input.KeyCode;
import java.util.UUID;

public class CreateTextState implements EditorState {
    private final EventBus eventBus;
    private InteractionMap bindings;

    public CreateTextState() {
        this(null);
    }

    public CreateTextState(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private void initBindings(CanvasController context) {
        if (bindings != null) return;

        bindings = new InteractionMap();

        // ESC -> abbrechen
        bindings.on(EventMatcher.keyPressed(KeyCode.ESCAPE), event -> {
            if (eventBus != null) {
                eventBus.publish(new EditorActionEvent.ResetToIdle());
            } else {
                context.resetToIdleState();
            }
        });

        // Primärklick -> Text erzeugen
        bindings.on(EventMatcher.primaryClick(), event -> {
            double x = event.worldX();
            double y = event.worldY();
            UUID parentObjectId = null;

            // Prüfen, ob auf ein Objekt geklickt wurde
            var hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null && hit.type() != de.fmc.editor.core.model.FmcType.WAYPOINT) {
                parentObjectId = hit.id();
                // Text wird relativ zum Objekt platziert
                x = hit.x();
                y = hit.y() + hit.height() / 2 + 25;
            } else {
                // Snap‑to‑Grid für die Mitte des Textes
                if (context.isSnapToGrid()) {
                    int gridSize = 20;
                    double snappedCenterX = Math.round(x / gridSize) * gridSize;
                    double snappedCenterY = Math.round(y / gridSize) * gridSize;

                    double fontSize = 14.0;
                    double lineHeight = fontSize * 1.2;
                    double visualHeight = lineHeight;

                    x = snappedCenterX;
                    y = snappedCenterY + (visualHeight / 2);
                }
            }

            // FmcText erzeugen
            var text = new FmcText(
                    UUID.randomUUID(),
                    "Doppelklick zum Bearbeiten",
                    x, y,
                    150,
                    "System",
                    14.0,
                    "normal",
                    "normal",
                    "#000000",
                    parentObjectId,
                    CoreRegistry.DEFAULT_LAYER_ID
            );

            if (eventBus != null) {
                eventBus.publish(new EditorActionEvent.CreateText(text));
                eventBus.publish(new EditorActionEvent.ChangeState(new EditTextState(text.id(), eventBus)));
            } else {
                var cmd = new CreateTextCommand(context.getRegistry(), text);
                context.getCommandHistory().executeCommand(cmd);
                context.setCurrentState(new EditTextState(text.id()));
            }
        });
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