package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.command.UpdateTextCommand;
import de.fmc.editor.core.event.EventBus;
import de.fmc.editor.core.event.EditorActionEvent;
import de.fmc.editor.core.model.FmcText;
import javafx.geometry.Bounds;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import java.util.UUID;

public class EditTextState implements EditorState {

    private final UUID textId;
    private final EventBus eventBus;
    private TextArea editor;
    private String originalText;
    private boolean discardEdits = false;

    public EditTextState(UUID textId) {
        this(textId, null);
    }

    public EditTextState(UUID textId, EventBus eventBus) {
        this.textId = textId;
        this.eventBus = eventBus;
    }

    @Override
    public void enterState(CanvasController context) {
        FmcText text = context.getRegistry().getText(textId);
        if (text == null) {
            if (eventBus != null) {
                eventBus.publish(new EditorActionEvent.ResetToIdle());
            } else {
                context.resetToIdleState();
            }
            return;
        }

        originalText = text.text();

        Text textNode = context.getViewMapper().getTextMapper().getTextNode(textId);
        if (textNode == null) {
            if (eventBus != null) {
                eventBus.publish(new EditorActionEvent.ResetToIdle());
            } else {
                context.resetToIdleState();
            }
            return;
        }

        editor = new TextArea(text.text());
        Bounds bounds = textNode.getBoundsInParent();
        editor.setLayoutX(bounds.getMinX() - 2);
        editor.setLayoutY(bounds.getMinY() - 2);
        editor.setPrefWidth(bounds.getWidth() + 4);
        editor.setPrefHeight(bounds.getHeight() + 4);
        editor.setWrapText(true);

        editor.setStyle(
                "-fx-font-family: '" + text.fontFamily() + "'; " +
                        "-fx-font-size: " + text.fontSize() + "px; " +
                        "-fx-font-weight: " + text.fontWeight() + "; " +
                        "-fx-text-fill: " + text.textFill() + "; " +
                        "-fx-border-color: #3498db; " +
                        "-fx-border-width: 1.5px; " +
                        "-fx-background-color: white;"
        );

        context.getDrawingPane().getUiLayer().getChildren().add(editor);

        editor.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode() == KeyCode.ENTER && !ke.isShiftDown()) {
                if (eventBus != null) {
                    eventBus.publish(new EditorActionEvent.ChangeState(new IdleState(eventBus)));
                } else {
                    context.setCurrentState(new IdleState());
                }
                ke.consume();
            } else if (ke.getCode() == KeyCode.ESCAPE) {
                discardEdits = true;
                if (eventBus != null) {
                    eventBus.publish(new EditorActionEvent.ChangeState(new IdleState(eventBus)));
                } else {
                    context.setCurrentState(new IdleState());
                }
                ke.consume();
            }
        });

        editor.requestFocus();
        editor.selectAll();
    }

    @Override
    public void exitState(CanvasController context) {
        if (editor != null) {
            context.getDrawingPane().getUiLayer().getChildren().remove(editor);

            if (!discardEdits) {
                String newText = editor.getText();
                if (!newText.equals(originalText)) {
                    FmcText oldText = context.getRegistry().getText(textId);
                    if (oldText != null) {
                        FmcText updated = new FmcText(
                                oldText.id(), newText,
                                oldText.x(), oldText.y(),
                                oldText.width(),
                                oldText.fontFamily(), oldText.fontSize(),
                                oldText.fontWeight(), oldText.fontStyle(),
                                oldText.textFill(),
                                oldText.parentObjectId(), oldText.layerId()
                        );
                        var cmd = new UpdateTextCommand(context.getRegistry(), oldText, updated);
                        context.getCommandHistory().executeCommand(cmd);
                    }
                }
            }
            editor = null;
        }
    }

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        if (event.isPrimaryButtonDown()) {
            if (eventBus != null) {
                eventBus.publish(new EditorActionEvent.ChangeState(new IdleState(eventBus)));
            } else {
                context.setCurrentState(new IdleState());
            }
        }
    }
}