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
import javafx.scene.shape.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ViewMapper implements RegistryListener {

    private final GraphView graphView;
    private final CoreRegistry registry;
    private final Map<UUID, Shape> visualNodes = new HashMap<>();
    private final Map<UUID, Path> visualConnections = new HashMap<>();
    private final List<Shape> activeHandles = new ArrayList<>();
    private UUID selectedObjectId = null;
    private RoutingStrategy routingStrategy = new StraightLineRouting();

    public ViewMapper(GraphView graphView, de.fmc.editor.core.CoreRegistry registry) {
        this.graphView = graphView;
        this.registry = registry;
    }

    public void setRoutingStrategy(RoutingStrategy strategy) {
        this.routingStrategy = strategy;
        // Alle bestehenden Verbindungen neu zeichnen
        visualConnections.keySet().forEach(this::refreshConnection);
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
            case RegistryEvent.ConnectionAdded(var id, var conn) -> handleConnectionAdded(id, conn);
            case RegistryEvent.ConnectionRemoved(var id) -> handleConnectionRemoved(id);
            case RegistryEvent.ConnectionUpdated(var id, var conn) -> handleConnectionUpdated(id, conn);
            case RegistryEvent.LayerAdded(var layer) -> {} 
            case RegistryEvent.LayerRemoved(var id) -> {}
            case RegistryEvent.LayerVisibilityChanged(var id, var visible) -> handleLayerVisibilityChanged(id, visible);
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
            case WEGPUNKT -> {
                Circle c = new Circle(obj.x(), obj.y(), obj.width());
                c.setFill(javafx.scene.paint.Color.YELLOW);
                c.setStroke(javafx.scene.paint.Color.BLACK);
                c.setStrokeWidth(1.5);
                yield c;
            }
        };

        if (obj.type() != de.fmc.editor.core.model.FmcType.WEGPUNKT) {
            shape.setFill(javafx.scene.paint.Color.WHITE);
            shape.setStroke(javafx.scene.paint.Color.BLACK);
            shape.setStrokeWidth(1.5);
            shape.setMouseTransparent(true); 
        } else {
            shape.setMouseTransparent(false);
        }

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
        updateConnectionsForObject(id);
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
        updateConnectionsForObject(id);
    }

    private void handleObjectRemoved(UUID id) {
        Shape shape = visualNodes.remove(id);
        if (shape != null) {
            graphView.getShapeLayer().getChildren().remove(shape);
        }
    }

    private void handleConnectionAdded(UUID id, de.fmc.editor.core.model.Connection conn) {
        refreshConnection(id);
    }

    private void handleConnectionRemoved(UUID id) {
        Path path = visualConnections.remove(id);
        if (path != null) {
            graphView.getConnectionLayer().getChildren().remove(path);
        }
    }

    private void handleConnectionUpdated(UUID id, de.fmc.editor.core.model.Connection conn) {
        refreshConnection(id);
    }

    private void handleLayerVisibilityChanged(UUID layerId, boolean visible) {
        registry.getObjects().stream()
            .filter(obj -> obj.layerId().equals(layerId))
            .forEach(obj -> {
                Shape node = visualNodes.get(obj.id());
                if (node != null) node.setVisible(visible);
                
                // Auch Verbindungen dieses Objekts ausblenden (einfache Logik: wenn ein Ende weg ist, Linie weg)
                visualConnections.forEach((connId, path) -> {
                    var conn = registry.getConnections().get(connId);
                    if (conn != null && (conn.sourceId().equals(obj.id()) || conn.targetId().equals(obj.id()))) {
                        path.setVisible(visible);
                    }
                });
            });
    }

    private void updateConnectionsForObject(UUID objectId) {
        registry.getConnections().forEach((id, conn) -> {
            if (conn.sourceId().equals(objectId) || 
                conn.targetId().equals(objectId) ||
                conn.waypointIds().contains(objectId)) {
                refreshConnection(id);
            }
        });
    }

    private void refreshConnection(UUID connId) {
        var conn = registry.getConnections().get(connId);
        if (conn == null) return;

        var source = registry.getObjects().stream().filter(o -> o.id().equals(conn.sourceId())).findFirst().orElse(null);
        var target = registry.getObjects().stream().filter(o -> o.id().equals(conn.targetId())).findFirst().orElse(null);

        if (source == null || target == null) return;

        // Wegpunkte auflösen
        List<de.fmc.editor.core.model.FmcObject> waypoints = new ArrayList<>();
        for (UUID wpId : conn.waypointIds()) {
            registry.getObjects().stream()
                    .filter(o -> o.id().equals(wpId))
                    .findFirst()
                    .ifPresent(waypoints::add);
        }

        Path oldPath = visualConnections.get(connId);
        if (oldPath != null) {
            graphView.getConnectionLayer().getChildren().remove(oldPath);
        }

        Path newPath = routingStrategy.calculatePath(source, target, waypoints);
        newPath.setStroke(javafx.scene.paint.Color.DARKGRAY);
        newPath.setStrokeWidth(4.0);
        newPath.setMouseTransparent(false);
        newPath.getProperties().put("UUID", connId);
        newPath.getProperties().put("TYPE", "CONNECTION");
        
        visualConnections.put(connId, newPath);
        graphView.getConnectionLayer().getChildren().add(newPath);
    }
}
