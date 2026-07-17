package de.fmc.editor.view;

import de.fmc.editor.core.model.FmcObject;
import javafx.geometry.Point2D;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.util.ArrayList;
import java.util.List;

/**
 * Routingt-Strategie, die an jedem Wegpunkt (Ecke) einen abgerundeten Bogen einfügt.
 * Der Bogenradius kann beim Erstellen der Instanz gesetzt werden (Standard 20).
 */
public class RoundedCornerRouting implements RoutingStrategy {

    private final double radius;
    private static final int DEFAULT_SEGMENTS = 20;  // Anzahl der Segmente zur Bogen-Approximation

    public RoundedCornerRouting() {
        this(20.0);
    }

    public RoundedCornerRouting(double radius) {
        this.radius = radius;
    }

    @Override
    public Path calculatePath(FmcObject source, FmcObject target, List<FmcObject> waypoints) {
        // Alle Punkte in der richtigen Reihenfolge sammeln
        List<Point2D> points = new ArrayList<>();
        points.add(new Point2D(source.x(), source.y()));
        if (waypoints != null) {
            for (FmcObject wp : waypoints) {
                points.add(new Point2D(wp.x(), wp.y()));
            }
        }
        points.add(new Point2D(target.x(), target.y()));

        Path path = new Path();
        // Startpunkt setzen
        path.getElements().add(new MoveTo(points.get(0).getX(), points.get(0).getY()));
        Point2D currentPos = points.get(0);

        // Für jede Ecke (innere Punkte) den Bogen einfügen
        for (int i = 1; i < points.size() - 1; i++) {
            Point2D prev = points.get(i - 1);
            Point2D curr = points.get(i);
            Point2D next = points.get(i + 1);

            // Vektoren von curr zu prev und curr zu next
            Point2D v1 = prev.subtract(curr);
            Point2D v2 = next.subtract(curr);
            double len1 = v1.distance(0, 0);
            double len2 = v2.distance(0, 0);

            // Sicherheitscheck (kollineare oder identische Punkte)
            if (len1 < 1e-9 || len2 < 1e-9) {
                // Wenn keine sinnvolle Ecke, einfach gerade Linie zum nächsten Punkt
                path.getElements().add(new LineTo(curr.getX(), curr.getY()));
                currentPos = curr;
                continue;
            }

            // Winkel zwischen den beiden Vektoren (Innenwinkel der Ecke)
            double dot = v1.getX() * v2.getX() + v1.getY() * v2.getY();
            double cosAngle = Math.max(-1.0, Math.min(1.0, dot / (len1 * len2)));
            double angle = Math.acos(cosAngle);  // zwischen 0 und PI

            // NEUER CHECK: Wenn die Punkte (fast) auf einer Geraden liegen, gibt es keine Ecke!
            if (angle < 1e-4 || angle > (Math.PI - 1e-4)) {
                path.getElements().add(new LineTo(curr.getX(), curr.getY()));
                currentPos = curr;
                continue;
            }

            // Abstand vom Scheitelpunkt bis zu den Tangentialpunkten
            double d = radius * Math.cos(angle / 2.0) / Math.sin(angle / 2.0); // d = r * cot(angle/2)

            // Begrenzen, damit der Bogen nicht über die angrenzenden Segmente hinausragt
            double maxD = Math.min(len1, len2) * 0.5;
            double effectiveRadius = radius;
            if (d > maxD) {
                // Radius verkleinern, damit die Bogen in die vorhandenen Strecken passen
                effectiveRadius = maxD * Math.tan(angle / 2.0);
                d = maxD;
            }

            // Einheitsvektoren
            Point2D dir1 = v1.multiply(1.0 / len1);
            Point2D dir2 = v2.multiply(1.0 / len2);

            // Tangentialpunkte
            Point2D pStart = curr.add(dir1.multiply(d));
            Point2D pEnd   = curr.add(dir2.multiply(d));

            // Linie von aktueller Position zum Startpunkt des Bogens
            if (!currentPos.equals(pStart)) {
                path.getElements().add(new LineTo(pStart.getX(), pStart.getY()));
            }

            // Bogen approximieren (Kreissegment)
            // Mittelpunkt des Bogens berechnen
            Point2D midDir = dir1.add(dir2).normalize(); // Winkelhalbierende
            double distCenter = effectiveRadius / Math.sin(angle / 2.0);
            Point2D center = curr.add(midDir.multiply(distCenter));

            // Winkel der Tangentialpunkte zum Mittelpunkt
            double startAngle = Math.atan2(pStart.getY() - center.getY(), pStart.getX() - center.getX());
            double endAngle   = Math.atan2(pEnd.getY() - center.getY(), pEnd.getX() - center.getX());

            // Bestimme die Drehrichtung des Bogens (über Kreuzprodukt)
            double cross = (pStart.getX() - center.getX()) * (pEnd.getY() - center.getY())
                    - (pStart.getY() - center.getY()) * (pEnd.getX() - center.getX());

            // Bogenwinkel (im Zentrum) ist PI - angle
            double beta = Math.PI - angle;
            int sign = (cross > 0) ? 1 : -1; // positive Drehung = Gegenuhrzeigersinn

            // Segmente für den Bogen erzeugen
            int segments = DEFAULT_SEGMENTS;
            for (int s = 1; s <= segments; s++) {
                double t = (double) s / segments * beta;
                double theta = startAngle + sign * t;
                double px = center.getX() + effectiveRadius * Math.cos(theta);
                double py = center.getY() + effectiveRadius * Math.sin(theta);
                path.getElements().add(new LineTo(px, py));
            }

            // Aktuelle Position auf das Ende des Bogens setzen
            currentPos = pEnd;
        }

        // Letztes Teilstück zum Ziel
        Point2D last = points.get(points.size() - 1);
        if (!currentPos.equals(last)) {
            path.getElements().add(new LineTo(last.getX(), last.getY()));
        }

        return path;
    }
}