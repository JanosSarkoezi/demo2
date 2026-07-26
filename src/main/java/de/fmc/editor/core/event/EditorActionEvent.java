package de.fmc.editor.core.event;

import de.fmc.editor.state.EditorState;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.FmcText;
import java.util.List;
import java.util.UUID;

public sealed interface EditorActionEvent {
    record SelectObject(UUID id, boolean isControlDown) implements EditorActionEvent {}
    record SelectText(UUID id, boolean isControlDown) implements EditorActionEvent {}
    record ClearSelection() implements EditorActionEvent {}
    record DeleteSelected() implements EditorActionEvent {}
    record ChangeState(EditorState newState) implements EditorActionEvent {}
    record MoveObject(UUID id, double deltaX, double deltaY) implements EditorActionEvent {}
    
    // Additional events for IdleState
    record ReactivateTool() implements EditorActionEvent {}
    record AddWaypoint(UUID connectionId, double x, double y) implements EditorActionEvent {}
    record SetLayerVisibility(UUID layerId, boolean visible) implements EditorActionEvent {}

    // Events for CreateObjectState
    record ResetToIdle() implements EditorActionEvent {}
    record CreateObject(FmcType type, double x, double y) implements EditorActionEvent {}

    // Events for CreateConnectionState
    record CreateWaypoint(UUID id, double x, double y) implements EditorActionEvent {}
    record CreateConnection(UUID sourceId, UUID targetId, List<UUID> waypointIds) implements EditorActionEvent {}

    // Events for CreateTextState
    record CreateText(FmcText text) implements EditorActionEvent {}

    // Events for ResizeState
    record ResizeObject(UUID id, double newW, double newH) implements EditorActionEvent {}
    record CommitResize(UUID id, double startW, double startH, double endW, double endH) implements EditorActionEvent {}
}
