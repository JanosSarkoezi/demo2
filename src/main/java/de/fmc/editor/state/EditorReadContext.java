package de.fmc.editor.state;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcText;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface EditorReadContext {
    FmcObject findObjectAt(double x, double y);
    FmcText findTextAt(double x, double y);
    UUID findConnectionNear(double x, double y, double tolerance);
    List<UUID> findObjectsInBounds(double x1, double y1, double x2, double y2);

    boolean isSnapToGrid();
    boolean isWaypointsVisible();
    boolean isSticky();

    Set<UUID> getSelectedObjectIds();
    Set<UUID> getSelectedTextIds();
}
