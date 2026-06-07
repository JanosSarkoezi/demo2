package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.Handle;
import de.fmc.editor.core.model.HandleType;
import de.fmc.editor.view.ViewMapper;
import java.util.UUID;

public class ResizeState implements EditorState {
    private final UUID targetObjectId;
    private HandleType activeHandle = null;
    private double lastMouseX;
    private double lastMouseY;
    
    // Command-Tracking
    private double startW;
    private double startH;
    private double lastKnownW;
    private double lastKnownH;

    public ResizeState(UUID targetObjectId) {
        this.targetObjectId = targetObjectId;
    }

    public void enterState(CanvasController context) {
        System.out.println("Entering ResizeState for: " + targetObjectId);
        FmcObject obj = getTargetObject(context);
        if (obj != null && context.getViewMapper() != null) {
            context.getViewMapper().setSelectedObject(targetObjectId, ViewMapper.getHandles(obj));
        }
    }

    @Override
    public void handleMousePressed(MouseEventData event, CanvasController context) {
        FmcObject obj = getTargetObject(context);
        if (obj == null) {
            context.setCurrentState(new SelectOrMoveState());
            return;
        }

        activeHandle = findHandleAt(obj, event.worldX(), event.worldY());
        if (activeHandle == null) {
            // Wenn kein Handle getroffen wurde, prüfen wir ob ein anderes Objekt getroffen wurde
            FmcObject hit = context.findObjectAt(event.worldX(), event.worldY());
            if (hit != null) {
                if (hit.id().equals(targetObjectId)) {
                    // Wieder das gleiche Objekt -> Move-Modus innerhalb von Resize? 
                    context.setCurrentState(new SelectOrMoveState());
                    context.getCurrentState().handleMousePressed(event, context);
                } else {
                    // Ein neues Objekt -> Zu diesem wechseln
                    ResizeState nextState = new ResizeState(hit.id());
                    context.setCurrentState(nextState);
                    nextState.enterState(context);
                }
            } else {
                // Ins Leere geklickt -> Deselektieren
                context.setCurrentState(new SelectOrMoveState());
            }
        } else {
            lastMouseX = event.worldX();
            lastMouseY = event.worldY();
            
            // Ursprung merken für Command
            startW = obj.width();
            startH = obj.height();
            lastKnownW = obj.width();
            lastKnownH = obj.height();
        }
    }

    @Override
    public void handleMouseDragged(MouseEventData event, CanvasController context) {
        if (activeHandle == null) return;

        FmcObject obj = getTargetObject(context);
        if (obj == null) return;

        double deltaX = event.worldX() - lastMouseX;
        double deltaY = event.worldY() - lastMouseY;

        double newW = obj.width();
        double newH = obj.height();

        // Einfache Resize-Logik (Mittelpunkt bleibt fix für diese Demo)
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

        // Für Kreise: Gleichmäßiges Skalieren erzwingen
        if (obj.type() == FmcType.KREIS) {
            if (activeHandle == HandleType.N || activeHandle == HandleType.S) {
                newW = newH;
            } else {
                newH = newW;
            }
        }

        // Mindestgröße
        newW = Math.max(10, newW);
        newH = Math.max(10, newH);

        // Bei Kreisen nach der Mindestgröße nochmal synchronisieren
        if (obj.type() == FmcType.KREIS) {
            newW = Math.max(newW, newH);
            newH = newW;
        }

        context.getRegistry().resizeObject(targetObjectId, newW, newH);
        lastKnownW = newW;
        lastKnownH = newH;
        
        // Handles nach dem Resize neu zeichnen
        if (context.getViewMapper() != null) {
            FmcObject updated = getTargetObject(context);
            context.getViewMapper().setSelectedObject(targetObjectId, ViewMapper.getHandles(updated));
        }
        
        lastMouseX = event.worldX();
        lastMouseY = event.worldY();
    }

    @Override
    public void handleMouseReleased(MouseEventData event, CanvasController context) {
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

    private HandleType findHandleAt(FmcObject obj, double x, double y) {
        for (Handle h : ViewMapper.getHandles(obj)) {
            double dx = h.x() - x;
            double dy = h.y() - y;
            if ((dx * dx + dy * dy) <= (10 * 10)) { // 10px Radius für Handles
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
