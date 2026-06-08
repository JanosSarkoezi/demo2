package de.fmc.editor.view;

import de.fmc.editor.core.model.FmcObject;
import javafx.geometry.Point2D;
import javafx.scene.shape.Path;
import java.util.List;

public interface RoutingStrategy {
    Path calculatePath(FmcObject source, FmcObject target, List<FmcObject> waypoints);
}
