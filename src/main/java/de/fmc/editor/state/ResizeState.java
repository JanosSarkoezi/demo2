package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.Handle;
import de.fmc.editor.core.model.HandleType;
import de.fmc.editor.view.ViewMapper;
import java.util.UUID;

public class ResizeState extends AbstractEditorState {
    private final UUID targetObjectId;
    private HandleType activeHandle = null;
    private double lastMouseX;
    private double lastMouseY;
    
    private double startW;
    private double startH;
    private double lastKnownW;
    private double lastKnownH;

    public ResizeState(UUID targetObjectId) {
        this.targetObjectId = targetObjectId;

        // --- PRESSED ACTIONS ---

        // Failsafe: Target object no longer exists
        pressedActions.add(new MouseAction(
            (e, ctx) -> getTargetObject(ctx) == null,
            (event, context) -> context.reactivateCurrentTool()
        ));

        // Clicked on a resize handle
        pressedActions.add(new MouseAction(
            (e, ctx) -> {
                FmcObject obj = getTargetObject(ctx);
                return obj != null && findHandleAt(obj, e.worldX(), e.worldY()) != null;
            },
            (event, context) -> {
                FmcObject obj = getTargetObject(context);
                if (obj != null) {
                    activeHandle = findHandleAt(obj, event.worldX(), event.worldY());
                    lastMouseX = event.worldX();
                    lastMouseY = event.worldY();
                    
                    startW = obj.width();
                    startH = obj.height();
                    lastKnownW = obj.width();
                    lastKnownH = obj.height();
                }
            }
        ));

        // Clicked on the same object (not on handle) -> Switch to Idle and let it process the event (e.g. for drag)
        pressedActions.add(new MouseAction(
            (e, ctx) -> {
                FmcObject hit = ctx.findObjectAt(e.worldX(), e.worldY());
                return hit != null && hit.id().equals(targetObjectId);
            },
            (event, context) -> {
                context.reactivateCurrentTool();
                context.getCurrentState().handleMousePressed(event, context);
            }
        ));

        // Clicked on a different object -> Transition to ResizeState for that object
        pressedActions.add(new MouseAction(
            (e, ctx) -> {
                FmcObject hit = ctx.findObjectAt(e.worldX(), e.worldY());
                return hit != null && !hit.id().equals(targetObjectId);
            },
            (event, context) -> {
                ResizeState nextState = new ResizeState(context.findObjectAt(event.worldX(), event.worldY()).id());
                context.setCurrentState(nextState);
            }
        ));

        // Clicked into empty space -> Cancel resize and go to Idle
        pressedActions.add(new MouseAction(
            MouseMatchers.alwaysTrue(),
            (event, context) -> context.reactivateCurrentTool()
        ));


        // --- DRAGGED ACTIONS ---
        draggedActions.add(new MouseAction(
            (e, ctx) -> activeHandle != null,
            (event, context) -> {
                FmcObject obj = getTargetObject(context);
                if (obj == null) return;

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
        ));


        // --- RELEASED ACTIONS ---
        releasedActions.add(new MouseAction(
            MouseMatchers.alwaysTrue(),
            (event, context) -> {
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
        ));
    }

    @Override
    public void enterState(CanvasController context) {
        System.out.println("Entering ResizeState for: " + targetObjectId);
        FmcObject obj = getTargetObject(context);
        if (obj != null && context.getViewMapper() != null) {
            context.getViewMapper().setSelectedObject(targetObjectId, ViewMapper.getHandles(obj));
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
