package de.fmc.editor.view;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConnectionViewMapper {

    private final GraphView graphView;
    private final CoreRegistry registry;
    private final Map<UUID, Group> visualConnections = new HashMap<>();
    private RoutingStrategy routingStrategy = new RoundedCornerRouting(20);

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
        Group group = visualConnections.remove(id);
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

    /**
     * Aktualisiert eine Verbindung.
     * Wenn sie noch nicht existiert, wird sie neu gezeichnet.
     * Wenn sie bereits existiert, werden nur die inneren Elemente geupdatet (In-Place).
     */
    public void refreshConnection(UUID connId) {
        var conn = registry.getConnections().get(connId);
        if (conn == null) return;

        // OPTIMIERUNG: Direkter Map-Lookup über CoreRegistry anstatt langsamer Java-Streams
        var source = registry.getObject(conn.sourceId());
        var target = registry.getObject(conn.targetId());

        if (source == null || target == null) return;

        List<FmcObject> waypoints = new ArrayList<>();
        for (UUID wpId : conn.waypointIds()) {
            FmcObject wp = registry.getObject(wpId);
            if (wp != null) {
                waypoints.add(wp);
            }
        }

        Group existingGroup = visualConnections.get(connId);

        if (existingGroup == null) {
            // FALL 1: Die Verbindung wird das erste Mal gezeichnet
            Path newPath = routingStrategy.calculatePath(source, target, waypoints);
            Group newGroup = createConnectionNode(connId, newPath, source, target, waypoints);

            visualConnections.put(connId, newGroup);
            graphView.getConnectionLayer().getChildren().add(newGroup);
        } else {
            // FALL 2 (Optimiert): Die Verbindung existiert bereits.
            // Wir updaten nur die Eigenschaften der Nodes, anstatt die Gruppe zu löschen und neu zu erstellen!
            Path existingPath = (Path) existingGroup.getChildren().get(0);
            Polygon existingArrow = (Polygon) existingGroup.getChildren().get(1);

            // 1. Neuen Pfad berechnen
            Path calculatedPath = routingStrategy.calculatePath(source, target, waypoints);

            // 2. Bestehende Pfadelemente in-place überschreiben (triggert kein teures Node-Re-Layout im Szenengraphen)
            existingPath.getElements().setAll(calculatedPath.getElements());

            // 3. Pfeil und Endsegment anpassen
            updateArrowHead(existingPath, existingArrow, source, target, waypoints);
        }
    }

    /**
     * Erstellt die visuelle Repräsentanz (Gruppe) einer Verbindung einmalig beim ersten Zeichnen.
     */
    private Group createConnectionNode(UUID connId, Path path, FmcObject source, FmcObject target, List<FmcObject> waypoints) {
        // Styling der Linie
        path.setStroke(Color.BLACK);
        path.setStrokeWidth(1.0);
        path.setMouseTransparent(false);

        // Styling der Pfeilspitze (wird an Punkt 0,0 erstellt und später transformiert)
        Polygon arrowHead = new Polygon();
        double arrowSize = 12.0;         // Länge des Pfeils von Spitze bis zu den äußeren Ecken
        double indentRatio = 0.65;       // Wie tief die Einkerbung geht
        double indentSize = arrowSize * indentRatio;

        arrowHead.getPoints().addAll(
                0.0, 0.0,                                 // Spitze
                -arrowSize, -arrowSize * 0.55,            // Äußere Ecke links
                -indentSize, 0.0,                         // Einkerbung (Mitte hinten)
                -arrowSize, arrowSize * 0.55              // Äußere Ecke rechts
        );

        arrowHead.setFill(Color.BLACK);
        arrowHead.setStroke(Color.BLACK);
        arrowHead.setStrokeWidth(1.0);

        // Gruppe zusammenbauen
        Group group = new Group(path, arrowHead);
        group.getProperties().put("UUID", connId);
        group.getProperties().put("TYPE", "CONNECTION");

        // Einmalig initial positionieren und Pfad anpassen
        updateArrowHead(path, arrowHead, source, target, waypoints);

        return group;
    }

    /**
     * Berechnet die Ausrichtung des Pfeils, passt das Ende der Linie an (damit sie nicht
     * durch den Pfeil durchschaut) und verschiebt/rotiert den Pfeil an die Zielkante.
     */
    private void updateArrowHead(Path path, Polygon arrowHead, FmcObject source, FmcObject target, List<FmcObject> waypoints) {
        // Letzter Punkt vor dem Ziel (Quelle oder letzter Wegpunkt)
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

        // Winkel des letzten Segments
        double angle = Math.atan2(intersection.getY() - fromY, intersection.getX() - fromX);

        double arrowSize = 12.0;
        double indentRatio = 0.65;
        double indentSize = arrowSize * indentRatio;

        // Endpunkt der Linie anpassen (Knick des Pfeils)
        double lineEndX = intersection.getX() - indentSize * Math.cos(angle);
        double lineEndY = intersection.getY() - indentSize * Math.sin(angle);

        // Letztes Liniensegment im Pfad verändern, falls vorhanden
        if (!path.getElements().isEmpty()) {
            var lastElement = path.getElements().get(path.getElements().size() - 1);
            if (lastElement instanceof LineTo lineTo) {
                lineTo.setX(lineEndX);
                lineTo.setY(lineEndY);
            }
        }

        // Pfeil-Position verschieben
        arrowHead.setLayoutX(intersection.getX());
        arrowHead.setLayoutY(intersection.getY());

        // Rotation aktualisieren (wir nutzen die bestehende Transformation oder legen sie einmalig an)
        if (!arrowHead.getTransforms().isEmpty() && arrowHead.getTransforms().get(0) instanceof Rotate rotate) {
            rotate.setAngle(Math.toDegrees(angle));
        } else {
            Rotate newRotate = new Rotate(Math.toDegrees(angle), 0, 0);
            arrowHead.getTransforms().setAll(newRotate);
        }
    }

    private Point2D getBoundaryIntersection(FmcObject obj, double fromX, double fromY) {
        double cx = obj.x();
        double cy = obj.y();
        double w = obj.width();
        double h = obj.height();

        if (obj.type() == FmcType.CIRCLE) {
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

            if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) {
                return new Point2D(cx, cy);
            }

            double tX = (Math.abs(dx) < 0.001) ? Double.POSITIVE_INFINITY : (w / 2.0) / Math.abs(dx);
            double tY = (Math.abs(dy) < 0.001) ? Double.POSITIVE_INFINITY : (h / 2.0) / Math.abs(dy);

            double t = Math.min(tX, tY);
            return new Point2D(cx + dx * t, cy + dy * t);
        }
    }

    public void clear() {
        visualConnections.values().forEach(group -> graphView.getConnectionLayer().getChildren().remove(group));
        visualConnections.clear();
    }

    public Map<UUID, Group> getVisualConnections() {
        return visualConnections;
    }
}