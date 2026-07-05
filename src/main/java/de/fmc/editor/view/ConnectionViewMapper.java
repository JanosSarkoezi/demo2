package de.fmc.editor.view;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;
import javafx.geometry.Point2D;
import javafx.scene.shape.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConnectionViewMapper {

    private final GraphView graphView;
    private final CoreRegistry registry;
    private final Map<UUID, javafx.scene.Group> visualConnections = new HashMap<>();
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
        javafx.scene.Group group = visualConnections.remove(id);
        if (group != null) {
            graphView.getConnectionLayer().getChildren().remove(group);
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

        javafx.scene.Group oldGroup = visualConnections.get(connId);
        if (oldGroup != null) {
            graphView.getConnectionLayer().getChildren().remove(oldGroup);
        }

        Path newPath = routingStrategy.calculatePath(source, target, waypoints);
        javafx.scene.Group newGroup = createConnectionNode(connId, newPath, source, target, waypoints);

        visualConnections.put(connId, newGroup);
        graphView.getConnectionLayer().getChildren().add(newGroup);
    }

    private javafx.scene.Group createConnectionNode(UUID connId, Path path, FmcObject source, FmcObject target, List<FmcObject> waypoints) {
        // letzter Punkt vor dem Ziel (Quelle oder letzter Wegpunkt)
        double fromX, fromY;
        if (waypoints != null && !waypoints.isEmpty()) {
            FmcObject lastWp = waypoints.get(waypoints.size() - 1);
            fromX = lastWp.x();
            fromY = lastWp.y();
        } else {
            fromX = source.x();
            fromY = source.y();
        }

        // Schnittpunkt mit dem Rand des Zielobjekts
        Point2D intersection = getBoundaryIntersection(target, fromX, fromY);

        // Winkel des letzten Segments (von from nach intersection)
        double angle = Math.atan2(intersection.getY() - fromY, intersection.getX() - fromX);

        // Pfeillänge (muss mit der im Pfeil-Polygon übereinstimmen)
        double arrowSize = 12.0;

        // Endpunkt der Linie = Basis des Pfeils (um arrowSize zurückversetzt)
        double lineEndX = intersection.getX() - arrowSize * Math.cos(angle);
        double lineEndY = intersection.getY() - arrowSize * Math.sin(angle);

        // Letztes Liniensegment anpassen
        if (!path.getElements().isEmpty()) {
            var lastElement = path.getElements().get(path.getElements().size() - 1);
            if (lastElement instanceof javafx.scene.shape.LineTo lineTo) {
                lineTo.setX(lineEndX);
                lineTo.setY(lineEndY);
            }
        }

        path.setStroke(javafx.scene.paint.Color.DARKGRAY);
        path.setStrokeWidth(4.0);
        path.setMouseTransparent(false);

        // Pfeil erstellen (Spitze bei (0,0), Basis bei (-arrowSize, 0))
        javafx.scene.shape.Polygon arrowHead = new javafx.scene.shape.Polygon();
        arrowHead.getPoints().addAll(
                0.0, 0.0,
                -arrowSize, -arrowSize * 0.5,
                -arrowSize,  arrowSize * 0.5
        );
        arrowHead.setFill(javafx.scene.paint.Color.DARKGRAY);
        arrowHead.setStroke(javafx.scene.paint.Color.WHITE);
        arrowHead.setStrokeWidth(1.0);

        // Rotation und Positionierung des Pfeils
        javafx.scene.transform.Rotate rotate = new javafx.scene.transform.Rotate(Math.toDegrees(angle), 0, 0);
        arrowHead.getTransforms().add(rotate);
        arrowHead.setLayoutX(intersection.getX());
        arrowHead.setLayoutY(intersection.getY());

        javafx.scene.Group group = new javafx.scene.Group(path, arrowHead);
        group.getProperties().put("UUID", connId);
        group.getProperties().put("TYPE", "CONNECTION");

        return group;
    }

    private Point2D getBoundaryIntersection(FmcObject obj, double fromX, double fromY) {
        double cx = obj.x();
        double cy = obj.y();
        double w = obj.width();
        double h = obj.height();

        if (obj.type() == de.fmc.editor.core.model.FmcType.KREIS) {
            double r = w / 2.0;
            double dx = fromX - cx;
            double dy = fromY - cy;
            double dist = Math.hypot(dx, dy);
            if (dist < 0.001) {
                return new Point2D(cx, cy);
            }
            return new Point2D(cx + (dx / dist) * r, cy + (dy / dist) * r);
        } else {
            double dx = fromX - cx;
            double dy = fromY - cy;

            // Punkt liegt exakt im Mittelpunkt
            if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) {
                return new Point2D(cx, cy);
            }

            // Parameter t für vertikale Kante (rechts/links)
            double tX = (Math.abs(dx) < 0.001) ? Double.POSITIVE_INFINITY
                    : (w / 2.0) / Math.abs(dx);
            // Parameter t für horizontale Kante (oben/unten)
            double tY = (Math.abs(dy) < 0.001) ? Double.POSITIVE_INFINITY
                    : (h / 2.0) / Math.abs(dy);

            // Der Rand wird bei der zuerst erreichten Kante getroffen
            double t = Math.min(tX, tY);

            // Schnittpunkt auf dem Rechteckrand
            return new Point2D(cx + dx * t, cy + dy * t);

        }
    }

    public void clear() {
        visualConnections.values().forEach(group -> graphView.getConnectionLayer().getChildren().remove(group));
        visualConnections.clear();
    }

    public Map<UUID, javafx.scene.Group> getVisualConnections() {
        return visualConnections;
    }
}
