package de.fmc.editor.view;

import de.fmc.editor.core.model.FmcObject;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import java.util.List;

public class OrthogonalRouting implements RoutingStrategy {
    @Override
    public Path calculatePath(FmcObject source, FmcObject target, List<FmcObject> waypoints) {
        Path path = new Path();
        
        if (waypoints == null || waypoints.isEmpty()) {
            path.getElements().add(new MoveTo(source.x(), source.y()));
            double midX = source.x() + (target.x() - source.x()) / 2;
            path.getElements().add(new LineTo(midX, source.y()));
            path.getElements().add(new LineTo(midX, target.y()));
            path.getElements().add(new LineTo(target.x(), target.y()));
        } else {
            // Wenn Wegpunkte da sind, verbinden wir sie einfach direkt (vereinfacht)
            path.getElements().add(new MoveTo(source.x(), source.y()));
            for (FmcObject wp : waypoints) {
                path.getElements().add(new LineTo(wp.x(), wp.y()));
            }
            path.getElements().add(new LineTo(target.x(), target.y()));
        }
        
        return path;
    }
}
