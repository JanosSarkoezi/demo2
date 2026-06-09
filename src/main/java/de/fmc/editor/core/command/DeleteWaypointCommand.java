package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Command zum Löschen eines einzelnen Wegpunktes.
 * Die Verbindung bleibt bestehen, aber der Wegpunkt wird aus ihrer Liste entfernt.
 */
// NEUER, VEREINFACHTER CODE:
public class DeleteWaypointCommand implements Command {
    private final CoreRegistry registry;
    private final UUID waypointId;
    private FmcObject deletedWaypoint;
    private final Map<UUID, List<UUID>> connectionBackups = new HashMap<>();

    public DeleteWaypointCommand(CoreRegistry registry, UUID waypointId) {
        this.registry = registry;
        this.waypointId = waypointId;
    }

    @Override
    public void execute() {
        if (deletedWaypoint == null) { // Nur beim allerersten Mal Backups machen
            deletedWaypoint = registry.getObject(waypointId);
            registry.getConnections().forEach((connId, conn) -> {
                if (conn.waypointIds().contains(waypointId)) {
                    connectionBackups.put(connId, new ArrayList<>(conn.waypointIds()));
                }
            });
        }
        registry.removeObject(waypointId);
    }

    @Override
    public void undo() {
        if (deletedWaypoint != null) {
            registry.addObject(deletedWaypoint);
            connectionBackups.forEach(registry::updateConnectionWaypoints);
        }
    }

    @Override
    public void redo() {
        // Keine Neuberechnung, kein Suchen im Core nötig!
        // Da die IDs stabil sind, führen wir einfach exakt dieselbe Löschung nochmal aus.
        registry.removeObject(waypointId);
    }
}