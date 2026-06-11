package de.fmc.editor.view;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;
import javafx.scene.Group;
import javafx.scene.shape.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the visual mapping of Connections to JavaFX Paths.
 */
public class ConnectionViewMapper {
    private final Group connectionLayer;
    private final CoreRegistry registry;
    private final Map<UUID, Path> visualConnections = new HashMap<>();
    private RoutingStrategy routingStrategy = new StraightLineRouting();

    public ConnectionViewMapper(Group connectionLayer, CoreRegistry registry) {
        this.connectionLayer = connectionLayer;
        this.registry = registry;
    }

    public void setRoutingStrategy(RoutingStrategy routingStrategy) {
        this.routingStrategy = routingStrategy;
        refreshAll();
    }

    public void handleAdd(UUID id, Connection conn) {
        refreshConnection(id);
    }

    public void handleRemove(UUID id) {
        Path path = visualConnections.remove(id);
        if (path != null) {
            connectionLayer.getChildren().remove(path);
        }
    }

    public void handleUpdate(UUID id, Connection conn) {
        refreshConnection(id);
    }

    public void refreshConnection(UUID connId) {
        var conn = registry.getConnections().get(connId);
        if (conn == null) return;

        var source = registry.getObject(conn.sourceId());
        var target = registry.getObject(conn.targetId());

        if (source == null || target == null) return;

        // Wegpunkte auflösen
        List<FmcObject> waypoints = new ArrayList<>();
        for (UUID wpId : conn.waypointIds()) {
            FmcObject wp = registry.getObject(wpId);
            if (wp != null) {
                waypoints.add(wp);
            }
        }

        Path oldPath = visualConnections.get(connId);
        if (oldPath != null) {
            connectionLayer.getChildren().remove(oldPath);
        }

        Path newPath = routingStrategy.calculatePath(source, target, waypoints);
        newPath.setStroke(javafx.scene.paint.Color.DARKGRAY);
        newPath.setStrokeWidth(4.0);
        newPath.setMouseTransparent(false);
        newPath.getProperties().put("UUID", connId);
        newPath.getProperties().put("TYPE", "CONNECTION");

        visualConnections.put(connId, newPath);
        connectionLayer.getChildren().add(newPath);
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

    public void refreshAll() {
        visualConnections.keySet().forEach(this::refreshConnection);
    }

    public void clear() {
        visualConnections.values().forEach(connectionLayer.getChildren()::remove);
        visualConnections.clear();
    }

    public void setVisible(UUID id, boolean visible) {
        Path path = visualConnections.get(id);
        if (path != null) {
            path.setVisible(visible);
        }
    }

    public Map<UUID, Path> getVisualConnections() {
        return visualConnections;
    }
}
