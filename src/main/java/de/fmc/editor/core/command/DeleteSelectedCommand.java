package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeleteSelectedCommand implements Command {
    private final CoreRegistry registry;
    private final List<UUID> idsToDelete;

    // Backups for Undo
    private final List<FmcObject> deletedObjects = new ArrayList<>();
    private final Map<UUID, Connection> deletedConnections = new HashMap<>();
    private final Map<UUID, Connection> updatedConnections = new HashMap<>(); // connectionId -> OLD state

    public DeleteSelectedCommand(CoreRegistry registry, Collection<UUID> selectedIds) {
        this.registry = registry;
        this.idsToDelete = new ArrayList<>(selectedIds);
    }

    @Override
    public void execute() {
        // 1. Analyse and Backup
        for (UUID id : idsToDelete) {
            // Backup object
            FmcObject obj = registry.getObjects().stream()
                    .filter(o -> o.id().equals(id))
                    .findFirst().orElse(null);
            
            if (obj != null) {
                deletedObjects.add(obj);
                
                // Backup connections that will be deleted (cascading)
                registry.getConnections().forEach((connId, conn) -> {
                    if (conn.sourceId().equals(id) || conn.targetId().equals(id)) {
                        deletedConnections.put(connId, conn);
                    }
                });
            }
        }

        // Backup connections that will be updated (waypoint removal)
        for (UUID id : idsToDelete) {
            registry.getConnections().forEach((connId, conn) -> {
                if (conn.waypointIds().contains(id) && !deletedConnections.containsKey(connId)) {
                    // Only backup if not already slated for deletion
                    updatedConnections.putIfAbsent(connId, conn);
                }
            });
        }

        // 2. Perform deletion
        // We use the registry's own logic which now handles cascading and waypoint updates
        for (UUID id : idsToDelete) {
            registry.removeObject(id);
        }
    }

    @Override
    public void undo() {
        // 1. Restore objects
        for (FmcObject obj : deletedObjects) {
            registry.addObject(obj);
        }

        // 2. Restore updated connections (restore original waypoint lists)
        updatedConnections.forEach((connId, oldConn) -> {
            registry.updateConnectionWaypoints(connId, oldConn.waypointIds());
        });

        // 3. Restore deleted connections
        deletedConnections.forEach((connId, conn) -> {
            registry.addConnection(connId, conn);
        });
    }
}
