package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import java.util.function.BiConsumer;

public record MouseAction(
    MouseEventMatcher matcher, 
    BiConsumer<MouseEventData, CanvasController> consumer
) {}
