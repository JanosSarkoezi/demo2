package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddWaypointCommand implements Command {
    private final CoreRegistry registry;
    private final UUID connectionId;
    private final FmcObject waypoint;
    private final int index;
    private List<UUID> previousWaypointIds;

    public AddWaypointCommand(CoreRegistry registry, UUID connectionId, FmcObject waypoint, int index) {
        this.registry = registry;
        this.connectionId = connectionId;
        this.waypoint = waypoint;
        this.index = index;
    }

    @Override
    public void execute() {
        Connection conn = registry.getConnections().get(connectionId);

        if (conn != null) {
            // 1. Wegpunkt zur Registry hinzufügen (damit er im ViewMapper erscheint)
            registry.addObject(waypoint);
            
            // 2. Alten Zustand NUR beim ersten Mal sichern
            if (previousWaypointIds == null) {
                this.previousWaypointIds = new ArrayList<>(conn.waypointIds());
            }
            
            // 3. Neuen Wegpunkt an der gewünschten Stelle einfügen
            List<UUID> updatedList = new ArrayList<>(previousWaypointIds);
            if (index >= 0 && index <= updatedList.size()) {
                updatedList.add(index, waypoint.id());
            } else {
                updatedList.add(waypoint.id());
            }
            
            registry.updateConnectionWaypoints(connectionId, updatedList);
        }
    }

    @Override
    public void undo() {
        if (previousWaypointIds != null) {
            // 1. Verbindung zurücksetzen
            registry.updateConnectionWaypoints(connectionId, previousWaypointIds);
            
            // 2. Wegpunkt aus der Registry entfernen
            registry.removeObject(waypoint.id());
        }
    }

    @Override
    public void redo() {
        execute();
    }
}
