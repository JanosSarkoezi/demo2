package de.fmc.editor.view;

import de.fmc.editor.core.model.FmcObject;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import java.util.List;

public class StraightLineRouting implements RoutingStrategy {
    @Override
    public Path calculatePath(FmcObject source, FmcObject target, List<FmcObject> waypoints) {
        Path path = new Path();
        path.getElements().add(new MoveTo(source.x(), source.y()));

        if (waypoints != null) {
            for (FmcObject wp : waypoints) {
                path.getElements().add(new LineTo(wp.x(), wp.y()));
            }
        }

        path.getElements().add(new LineTo(target.x(), target.y()));
        return path;
    }
}
