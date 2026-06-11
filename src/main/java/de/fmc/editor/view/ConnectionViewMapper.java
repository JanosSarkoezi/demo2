package de.fmc.editor.view;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;
import javafx.scene.shape.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConnectionViewMapper {

    private final GraphView graphView;
    private final CoreRegistry registry;
    private final Map<UUID, Path> visualConnections = new HashMap<>();
    private RoutingStrategy routingStrategy = new StraightLineRouting();

    public ConnectionViewMapper(GraphView graphView, CoreRegistry registry) {
        this.graphView = graphView;
        this.registry = registry;
    }

    public void setRoutingStrategy(RoutingStrategy strategy) {
        this.routingStrategy = strategy;
        visualConnections.keySet().forEach(this::refreshConnection);
    }

    public void handleConnectionAdded(UUID id, Connection conn) {
        refreshConnection(id);
    }

    public void handleConnectionRemoved(UUID id) {
        Path path = visualConnections.remove(id);
        if (path != null) {
            graphView.getConnectionLayer().getChildren().remove(path);
        }
    }

    public void handleConnectionUpdated(UUID id, Connection conn) {
        refreshConnection(id);
    }

    public void updateConnectionsForObject(UUID objectId) {
        registry.getConnections().forEach((id, conn) -> {
            if (conn.sourceId().equals(objectId) ||
                conn.targetId().equals(objectId) ||
                conn.waypointIds().contains(objectId)) {
                refreshConnection(id);
            }
        });
    }

    public void refreshConnection(UUID connId) {
        var conn = registry.getConnections().get(connId);
        if (conn == null) return;

        var source = registry.getObjects().stream().filter(o -> o.id().equals(conn.sourceId())).findFirst().orElse(null);
        var target = registry.getObjects().stream().filter(o -> o.id().equals(conn.targetId())).findFirst().orElse(null);

        if (source == null || target == null) return;

        List<FmcObject> waypoints = new ArrayList<>();
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

    public void clear() {
        visualConnections.values().forEach(path -> graphView.getConnectionLayer().getChildren().remove(path));
        visualConnections.clear();
    }

    public Map<UUID, Path> getVisualConnections() {
        return visualConnections;
    }
}
