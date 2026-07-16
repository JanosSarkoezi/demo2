package de.fmc.editor.view;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShapeViewMapper {

    private final GraphView graphView;
    private final Map<UUID, Shape> visualNodes = new HashMap<>();

    public ShapeViewMapper(GraphView graphView) {
        this.graphView = graphView;
    }

    public void handleObjectAdded(FmcObject obj) {
        Shape existingShape = visualNodes.get(obj.id());
        if (existingShape != null) {
            graphView.getShapeLayer().getChildren().remove(existingShape);
        }

        Shape shape = switch (obj.type()) {
            case KREIS -> new Circle(obj.x(), obj.y(), obj.width() / 2);
            case QUADRAT -> new Rectangle(obj.x() - (obj.width() / 2), obj.y() - (obj.height() / 2), obj.width(), obj.height());
            case WEGPUNKT -> {
                Circle c = new Circle(obj.x(), obj.y(), obj.width());
                c.setFill(Color.YELLOW);
                c.setStroke(Color.BLACK);
                c.setStrokeWidth(1.5);
                yield c;
            }
        };

        if (obj.type() != FmcType.WEGPUNKT) {
            shape.setFill(Color.WHITE);
            shape.setStroke(Color.BLACK);
            shape.setStrokeWidth(1.5);
            shape.setMouseTransparent(true);
        } else {
            shape.setMouseTransparent(false);
        }

        shape.getProperties().put("UUID", obj.id());
        visualNodes.put(obj.id(), shape);
        graphView.getShapeLayer().getChildren().add(shape);
    }

    public void handleObjectMoved(UUID id, double x, double y) {
        Shape shape = visualNodes.get(id);
        if (shape instanceof Circle circle) {
            circle.setCenterX(x);
            circle.setCenterY(y);
        } else if (shape instanceof Rectangle rect) {
            rect.setX(x - (rect.getWidth() / 2));
            rect.setY(y - (rect.getHeight() / 2));
        }
    }

    public void handleObjectResized(UUID id, double x, double y, double w, double h) {
        Shape shape = visualNodes.get(id);
        if (shape instanceof Circle circle) {
            circle.setRadius(w / 2);
        } else if (shape instanceof Rectangle rect) {
            rect.setWidth(w);
            rect.setHeight(h);
            rect.setX(x - (w / 2));
            rect.setY(y - (h / 2));
        }
    }

    public void handleObjectRemoved(UUID id) {
        Shape shape = visualNodes.remove(id);
        if (shape != null) {
            graphView.getShapeLayer().getChildren().remove(shape);
        }
    }

    public void clear() {
        visualNodes.values().forEach(node -> graphView.getShapeLayer().getChildren().remove(node));
        visualNodes.clear();
    }

    public Shape getShape(UUID id) {
        return visualNodes.get(id);
    }

    public Map<UUID, Shape> getVisualNodes() {
        return visualNodes;
    }
}
