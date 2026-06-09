package de.fmc.editor.view;

import de.fmc.editor.core.model.FmcObject;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import java.util.List;

public class StraightLineRouting implements RoutingStrategy {
    @Override
    public Path calculatePath(FmcObject source, FmcObject target, List<FmcObject> waypoints) {
//        System.out.println("✏️ === RoutingStrategy aufgerufen ===");
//        System.out.println("     Verbindung von ID: " + source.id());
//        System.out.println("     Verbindung zu  ID: " + target.id());
//        System.out.println("     Anzahl Wegpunkte im Datenmodell: " + waypoints.size());
//        waypoints.forEach(w -> System.out.println("     WID " + w.id()));
//
//        // Stacktrace ausgeben, um zu sehen, welches Event diesen Neuzeichen-Aufruf getriggert hat
//        StackWalker.getInstance().walk(frames -> frames
//                .filter(f -> f.getClassName().startsWith("de.fmc"))
//                .limit(10)
//                .peek(f -> System.out.println("       -> " + f.getClassName() + "." + f.getMethodName() + ":" + f.getLineNumber()))
//                .toList());

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
