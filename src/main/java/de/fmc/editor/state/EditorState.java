package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import javafx.scene.input.MouseEvent;

public interface EditorState {
    void handleMousePressed(MouseEvent event, CanvasController context);
    void handleMouseDragged(MouseEvent event, CanvasController context);
    void handleMouseReleased(MouseEvent event, CanvasController context);
}
