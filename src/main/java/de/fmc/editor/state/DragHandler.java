package de.fmc.editor.state;

public interface DragHandler {
    void onStart(InteractionEventData startEvent);
    void onDrag(InteractionEventData currentEvent, double deltaX, double deltaY);
    void onEnd(InteractionEventData endEvent);
    default void onCancel() {} // optional
}