package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;

public interface EditorState {
    default void enterState(CanvasController context) {}
    default void exitState(CanvasController context) {}
    void handleMousePressed(MouseEventData event, CanvasController context);
    void handleMouseDragged(MouseEventData event, CanvasController context);
    void handleMouseReleased(MouseEventData event, CanvasController context);
}
