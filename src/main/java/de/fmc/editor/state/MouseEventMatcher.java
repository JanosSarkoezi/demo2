package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;

@FunctionalInterface
public interface MouseEventMatcher {
    boolean matches(MouseEventData event, CanvasController context);
}
