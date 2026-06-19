package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEditorState implements EditorState {
    protected final List<MouseAction> pressedActions = new ArrayList<>();
    protected final List<MouseAction> draggedActions = new ArrayList<>();
    protected final List<MouseAction> releasedActions = new ArrayList<>();

    @Override
    public void enterState(CanvasController context) {}

    @Override
    public void exitState(CanvasController context) {}

    @Override
    public void handleMousePressed(MouseEventData event, CanvasController context) {
        executeFirstMatching(pressedActions, event, context);
    }

    @Override
    public void handleMouseDragged(MouseEventData event, CanvasController context) {
        executeFirstMatching(draggedActions, event, context);
    }

    @Override
    public void handleMouseReleased(MouseEventData event, CanvasController context) {
        executeFirstMatching(releasedActions, event, context);
    }

    private void executeFirstMatching(List<MouseAction> actions, MouseEventData event, CanvasController context) {
        for (MouseAction action : actions) {
            if (action.matcher().matches(event, context)) {
                action.consumer().accept(event, context);
                return;
            }
        }
    }
}
