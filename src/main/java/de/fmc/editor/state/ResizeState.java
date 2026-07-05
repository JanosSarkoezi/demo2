package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.Handle;
import de.fmc.editor.core.model.HandleType;
import de.fmc.editor.view.ViewMapper;
import javafx.scene.input.KeyCode;
import java.util.UUID;

public class ResizeState implements EditorState {
    private final UUID targetObjectId;
    private HandleType activeHandle = null;
    private double lastMouseX;
    private double lastMouseY;
    
    private double startW;
    private double startH;
    private double lastKnownW;
    private double lastKnownH;
    private boolean isResizing = false;

    public ResizeState(UUID targetObjectId) {
        this.targetObjectId = targetObjectId;
    }

    @Override
    public void enterState(CanvasController context) {
        // System.out.println("Entering ResizeState for: " + targetObjectId);
        FmcObject obj = getTargetObject(context);
        if (obj != null && context.getViewMapper() != null) {
            context.getViewMapper().setSelectedObject(targetObjectId, ViewMapper.getHandles(obj));
        }
    }

    @Override
    public void exitState(CanvasController context) {
        // Handles are cleared in CanvasController.setCurrentState when leaving ResizeState
    }

    @Override
    public void handleInput(InteractionEventData event, CanvasController context) {
        // ESC -> cancel or return to tool
        if (event.activeKey().isPresent() && event.activeKey().get() == KeyCode.ESCAPE) {
            context.reactivateCurrentTool();
            return;
        }

        FmcObject obj = getTargetObject(context);
        if (obj == null) {
            context.reactivateCurrentTool();
            return;
        }

        // Press / Drag initiation
        if (event.isPrimaryButtonDown() && event.activeKey().isEmpty()) {
            if (!isResizing) {
                HandleType handle = findHandleAt(obj, event.worldX(), event.worldY());
                if (handle != null) {
                    activeHandle = handle;
                    lastMouseX = event.worldX();
                    lastMouseY = event.worldY();
                    
                    startW = obj.width();
                    startH = obj.height();
                    lastKnownW = obj.width();
                    lastKnownH = obj.height();
                    isResizing = true;
                } else {
                    // Clicked elsewhere
                    FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
                    if (hit != null) {
                        if (hit.id().equals(targetObjectId)) {
                            // Clicked on target (not handle) -> back to tool, let it handle the event (drag)
                            context.reactivateCurrentTool();
                            context.getCurrentState().handleInput(event, context);
                        } else {
                            // Clicked on different object -> Resize that instead
                            context.setCurrentState(new ResizeState(hit.id()));
                        }
                    } else {
                        // Clicked into empty space -> Cancel resize and go to standard tool
                        context.reactivateCurrentTool();
                    }
                }
            } else {
                // Dragging
                double deltaX = event.worldX() - lastMouseX;
                double deltaY = event.worldY() - lastMouseY;

                double newW = obj.width();
                double newH = obj.height();

                switch (activeHandle) {
                    case E -> newW += deltaX * 2;
                    case W -> newW -= deltaX * 2;
                    case S -> newH += deltaY * 2;
                    case N -> newH -= deltaY * 2;
                    case SE -> { newW += deltaX * 2; newH += deltaY * 2; }
                    case SW -> { newW -= deltaX * 2; newH += deltaY * 2; }
                    case NE -> { newW += deltaX * 2; newH -= deltaY * 2; }
                    case NW -> { newW -= deltaX * 2; newH -= deltaY * 2; }
                }

                if (obj.type() == FmcType.KREIS) {
                    double absDeltaX = Math.abs(deltaX);
                    double absDeltaY = Math.abs(deltaY);
                    
                    if (absDeltaX > absDeltaY) {
                        newH = newW;
                    } else {
                        newW = newH;
                    }
                }

                newW = Math.max(10, newW);
                newH = Math.max(10, newH);

                if (obj.type() == FmcType.KREIS) {
                    newW = Math.max(newW, newH);
                    newH = newW;
                }

                context.getRegistry().resizeObject(targetObjectId, newW, newH);
                lastKnownW = newW;
                lastKnownH = newH;
                
                lastMouseX = event.worldX();
                lastMouseY = event.worldY();
            }
            return;
        }

        // Release
        if (!event.isPrimaryButtonDown() && isResizing) {
            isResizing = false;
            if (activeHandle != null) {
                if (startW != lastKnownW || startH != lastKnownH) {
                    var cmd = new de.fmc.editor.core.command.ResizeObjectCommand(
                        context.getRegistry(), targetObjectId, startW, startH, lastKnownW, lastKnownH
                    );
                    context.getCommandHistory().executeCommand(cmd);
                }
            }
            activeHandle = null;
        }
    }

    private HandleType findHandleAt(FmcObject obj, double x, double y) {
        for (Handle h : ViewMapper.getHandles(obj)) {
            double dx = h.x() - x;
            double dy = h.y() - y;
            if ((dx * dx + dy * dy) <= (10 * 10)) { // 10px radius
                return h.type();
            }
        }
        return null;
    }

    private FmcObject getTargetObject(CanvasController context) {
        return context.getRegistry().getObjects().stream()
                .filter(o -> o.id().equals(targetObjectId))
                .findFirst()
                .orElse(null);
    }
}
