package de.fmc.editor.core.util;

import de.fmc.editor.core.model.FmcObject;
import java.util.List;
import java.util.UUID;

/**
 * Utility-Klasse für geometrische Berechnungen im FMC-Core.
 * Absolut frei von JavaFX-Abhängigkeiten!
 */
public class GeometryUtils {

    /**
     * Berechnet den optimalen Einfüge-Index für einen neuen Punkt basierend auf der 
     * Distanz zu den Segmenten einer Verbindung.
     * 
     * @param clickX X-Koordinate des Klicks
     * @param clickY Y-Koordinate des Klicks
     * @param source Quell-Objekt
     * @param target Ziel-Objekt
     * @param waypoints Liste der aktuellen Wegpunkt-Objekte
     * @return Der Index (0-basiert), an dem der neue Punkt in die Wegpunkt-Liste eingefügt werden sollte.
     */
    public static int calculateInsertionIndex(double clickX, double clickY, FmcObject source, FmcObject target, List<FmcObject> waypoints) {
        if (source == null || target == null) return 0;

        // Wir bauen eine Liste aller Punkte der Verbindung: Source -> WP1 -> WP2 -> ... -> Target
        int pointCount = 2 + (waypoints != null ? waypoints.size() : 0);
        double[] xs = new double[pointCount];
        double[] ys = new double[pointCount];

        xs[0] = source.x();
        ys[0] = source.y();

        if (waypoints != null) {
            for (int i = 0; i < waypoints.size(); i++) {
                xs[i + 1] = waypoints.get(i).x();
                ys[i + 1] = waypoints.get(i).y();
            }
        }

        xs[pointCount - 1] = target.x();
        ys[pointCount - 1] = target.y();

        int bestIndex = 0;
        double minDistance = Double.MAX_VALUE;

        // Segmente prüfen
        for (int i = 0; i < pointCount - 1; i++) {
            double dist = distanceToSegment(clickX, clickY, xs[i], ys[i], xs[i+1], ys[i+1]);
            if (dist < minDistance) {
                minDistance = dist;
                bestIndex = i; // i=0 bedeutet nach Source, also Index 0 in der WP-Liste
            }
        }

        return bestIndex;
    }

    /**
     * Berechnet die minimale Distanz eines Punktes P zu einem Liniensegment AB.
     */
    public static double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double l2 = dx * dx + dy * dy;

        if (l2 == 0.0) return distance(px, py, ax, ay);

        // Projektion des Punktes P auf die Linie AB
        double t = ((px - ax) * dx + (py - ay) * dy) / l2;
        t = Math.max(0, Math.min(1, t));

        return distance(px, py, ax + t * dx, ay + t * dy);
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
