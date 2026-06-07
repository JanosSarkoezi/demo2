package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResizeState implements EditorState {
    private final UUID targetObjectId;
    private HandleType activeHandle = null;
    private double lastMouseX;
    private double lastMouseY;

    public enum HandleType {
        N, S, E, W, NE, NW, SE, SW
    }

    public record Handle(HandleType type, double x, double y) {}

    public ResizeState(UUID targetObjectId) {
        this.targetObjectId = targetObjectId;
    }

    public void enterState(CanvasController context) {
        System.out.println("Entering ResizeState for: " + targetObjectId);
        FmcObject obj = getTargetObject(context);
        if (obj != null && context.getViewMapper() != null) {
            context.getViewMapper().setSelectedObject(targetObjectId, getHandles(obj));
        }
    }

    @Override
    public void handleMousePressed(MouseEvent event, CanvasController context) {
        FmcObject obj = getTargetObject(context);
        if (obj == null) {
            context.setCurrentState(new SelectOrMoveState());
            return;
        }

        javafx.geometry.Point2D worldPos = context.getWorldPoint(event);
        activeHandle = findHandleAt(obj, worldPos.getX(), worldPos.getY());
        if (activeHandle == null) {
            // Wenn kein Handle getroffen wurde, prüfen wir ob ein anderes Objekt getroffen wurde
            FmcObject hit = context.findObjectAt(worldPos.getX(), worldPos.getY());
            if (hit != null) {
                if (hit.id().equals(targetObjectId)) {
                    // Wieder das gleiche Objekt -> Move-Modus innerhalb von Resize? 
                    // Oder einfach zurück zu SelectOrMove
                    context.setCurrentState(new SelectOrMoveState());
                    context.onMousePressed(event);
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
            lastMouseX = worldPos.getX();
            lastMouseY = worldPos.getY();
        }
    }

    @Override
    public void handleMouseDragged(MouseEvent event, CanvasController context) {
        if (activeHandle == null) return;

        FmcObject obj = getTargetObject(context);
        if (obj == null) return;

        javafx.geometry.Point2D worldPos = context.getWorldPoint(event);
        double deltaX = worldPos.getX() - lastMouseX;
        double deltaY = worldPos.getY() - lastMouseY;

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
        
        // Handles nach dem Resize neu zeichnen
        if (context.getViewMapper() != null) {
            FmcObject updated = getTargetObject(context);
            context.getViewMapper().setSelectedObject(targetObjectId, getHandles(updated));
        }
        
        lastMouseX = worldPos.getX();
        lastMouseY = worldPos.getY();
    }

    @Override
    public void handleMouseReleased(MouseEvent event, CanvasController context) {
        activeHandle = null;
    }

    public List<Handle> getHandles(FmcObject obj) {
        List<Handle> handles = new ArrayList<>();
        double hw = obj.width() / 2;
        double hh = obj.height() / 2;

        if (obj.type() == FmcType.QUADRAT) {
            handles.add(new Handle(HandleType.NW, obj.x() - hw, obj.y() - hh));
            handles.add(new Handle(HandleType.N,  obj.x(),      obj.y() - hh));
            handles.add(new Handle(HandleType.NE, obj.x() + hw, obj.y() - hh));
            handles.add(new Handle(HandleType.E,  obj.x() + hw, obj.y()));
            handles.add(new Handle(HandleType.SE, obj.x() + hw, obj.y() + hh));
            handles.add(new Handle(HandleType.S,  obj.x(),      obj.y() + hh));
            handles.add(new Handle(HandleType.SW, obj.x() - hw, obj.y() + hh));
            handles.add(new Handle(HandleType.W,  obj.x() - hw, obj.y()));
        } else if (obj.type() == FmcType.KREIS) {
            handles.add(new Handle(HandleType.N, obj.x(),      obj.y() - hh));
            handles.add(new Handle(HandleType.E, obj.x() + hw, obj.y()));
            handles.add(new Handle(HandleType.S, obj.x(),      obj.y() + hh));
            handles.add(new Handle(HandleType.W, obj.x() - hw, obj.y()));
        }
        return handles;
    }

    private HandleType findHandleAt(FmcObject obj, double x, double y) {
        for (Handle h : getHandles(obj)) {
            double dx = h.x() - x;
            double dy = h.y() - y;
            if ((dx * dx + dy * dy) <= (10 * 10)) { // 10px Radius für Handles (leichter zu treffen)
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
