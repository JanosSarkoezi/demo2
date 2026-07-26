package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;

public interface EditorState {
    default void enterState(CanvasController context) {}
    default void exitState(CanvasController context) {}
    void handleInput(InteractionEventData event, CanvasController context);

    default InteractionMap getInteractionMap() {
        return null;
    }
}

