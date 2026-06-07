package de.fmc.editor.view;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.event.RegistryEvent;
import de.fmc.editor.core.event.RegistryListener;
import de.fmc.editor.core.model.Handle;
import de.fmc.editor.core.model.HandleType;
import de.fmc.editor.state.ResizeState;
import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ViewMapper implements RegistryListener {

    private final GraphView graphView;
    private final CoreRegistry registry;
    private final Map<UUID, Shape> visualNodes = new HashMap<>();
    private final List<Shape> activeHandles = new ArrayList<>();
    private UUID selectedObjectId = null;

    public ViewMapper(GraphView graphView, de.fmc.editor.core.CoreRegistry registry) {
        this.graphView = graphView;
        this.registry = registry;
    }

    public static List<Handle> getHandles(de.fmc.editor.core.model.FmcObject obj) {
        List<Handle> handles = new ArrayList<>();
        double hw = obj.width() / 2;
        double hh = obj.height() / 2;

        if (obj.type() == de.fmc.editor.core.model.FmcType.QUADRAT) {
            handles.add(new Handle(HandleType.NW, obj.x() - hw, obj.y() - hh));
            handles.add(new Handle(HandleType.N,  obj.x(),      obj.y() - hh));
            handles.add(new Handle(HandleType.NE, obj.x() + hw, obj.y() - hh));
            handles.add(new Handle(HandleType.E,  obj.x() + hw, obj.y()));
            handles.add(new Handle(HandleType.SE, obj.x() + hw, obj.y() + hh));
            handles.add(new Handle(HandleType.S,  obj.x(),      obj.y() + hh));
            handles.add(new Handle(HandleType.SW, obj.x() - hw, obj.y() + hh));
            handles.add(new Handle(HandleType.W,  obj.x() - hw, obj.y()));
        } else if (obj.type() == de.fmc.editor.core.model.FmcType.KREIS) {
            handles.add(new Handle(HandleType.N, obj.x(),      obj.y() - hh));
            handles.add(new Handle(HandleType.E, obj.x() + hw, obj.y()));
            handles.add(new Handle(HandleType.S, obj.x(),      obj.y() + hh));
            handles.add(new Handle(HandleType.W, obj.x() - hw, obj.y()));
        }
        return handles;
    }

    @Override
    public void handleEvent(RegistryEvent event) {
        // Java 21 Pattern Matching für sealed-Interfaces:
        switch (event) {
            case RegistryEvent.ObjectAdded(var obj) -> handleObjectAdded(obj);
            case RegistryEvent.ObjectRemoved(var id) -> handleObjectRemoved(id);
            case RegistryEvent.ObjectMoved(var id, var x, var y) -> handleObjectMoved(id, x, y);
            case RegistryEvent.ObjectResized(var id, var w, var h) -> handleObjectResized(id, w, h);
        }

        // Falls das selektierte Objekt geändert wurde (z.B. durch Undo/Redo), Handles refreshen
        if (selectedObjectId != null) {
            UUID affectedId = switch (event) {
                case RegistryEvent.ObjectMoved(var id, var x, var y) -> id;
                case RegistryEvent.ObjectResized(var id, var w, var h) -> id;
                default -> null;
            };

            if (selectedObjectId.equals(affectedId)) {
                registry.getObjects().stream()
                    .filter(o -> o.id().equals(selectedObjectId))
                    .findFirst()
                    .ifPresent(obj -> refreshHandles(getHandles(obj)));
            }
        }
    }

    // Hilfsmethode um Handles anzuzeigen (wird von außen gesteuert)
    public void setSelectedObject(UUID id, List<Handle> handles) {
        this.selectedObjectId = id;
        refreshHandles(handles);
    }

    private void refreshHandles(List<Handle> handles) {
        graphView.getUiLayer().getChildren().clear();
        activeHandles.clear();

        if (selectedObjectId != null && handles != null) {
            for (var h : handles) {
                var rect = new javafx.scene.shape.Rectangle(h.x() - 4, h.y() - 4, 8, 8);
                rect.setFill(javafx.scene.paint.Color.BLACK);
                rect.setStroke(javafx.scene.paint.Color.WHITE);
                rect.setStrokeWidth(1.0);
                graphView.getUiLayer().getChildren().add(rect);
                activeHandles.add(rect);
            }
        }
    }

    private void handleObjectAdded(de.fmc.editor.core.model.FmcObject obj) {
        Shape shape = switch (obj.type()) {
            case KREIS -> new Circle(obj.x(), obj.y(), obj.width() / 2);
            case QUADRAT -> new Rectangle(obj.x() - (obj.width() / 2), obj.y() - (obj.height() / 2), obj.width(), obj.height());
            case WEGPUNKT -> new Circle(obj.x(), obj.y(), 5);
        };

        shape.setFill(javafx.scene.paint.Color.WHITE);
        shape.setStroke(javafx.scene.paint.Color.BLACK);
        shape.setStrokeWidth(1.5);

        // Klicks gehen durch das Shape auf das darunterliegende Canvas (GraphView/world)
        shape.setMouseTransparent(true); 

        // ID in den Properties speichern
        shape.getProperties().put("UUID", obj.id());

        visualNodes.put(obj.id(), shape);
        graphView.getShapeLayer().getChildren().add(shape);
    }

    private void handleObjectMoved(UUID id, double x, double y) {
        Shape shape = visualNodes.get(id);
        if (shape instanceof Circle circle) {
            circle.setCenterX(x);
            circle.setCenterY(y);
        } else if (shape instanceof javafx.scene.shape.Rectangle rect) {
            rect.setX(x - (rect.getWidth() / 2));
            rect.setY(y - (rect.getHeight() / 2));
        }
    }

    private void handleObjectResized(UUID id, double w, double h) {
        Shape shape = visualNodes.get(id);
        if (shape instanceof Circle circle) {
            circle.setRadius(w / 2);
        } else if (shape instanceof javafx.scene.shape.Rectangle rect) {
            rect.setWidth(w);
            rect.setHeight(h);
            de.fmc.editor.core.model.FmcObject obj = registry.getObjects().stream()
                    .filter(o -> o.id().equals(id))
                    .findFirst().orElse(null);
            if (obj != null) {
                rect.setX(obj.x() - (w / 2));
                rect.setY(obj.y() - (h / 2));
            }
        }
    }

    private void handleObjectRemoved(UUID id) {
        Shape shape = visualNodes.remove(id);
        if (shape != null) {
            graphView.getShapeLayer().getChildren().remove(shape);
        }
    }
}
