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
 * Command zum Löschen eines einzelnen Objekts (Kreis oder Quadrat).
 * Beinhaltet das kaskadierende Löschen aller anhängenden Verbindungen 
 * und deren Wegpunkte.
 */
public class DeleteObjectCommand implements Command {
    private final CoreRegistry registry;
    private final UUID objectId;

    private FmcObject deletedObject;
    private final Map<UUID, Connection> deletedConnections = new HashMap<>();
    private final List<FmcObject> deletedWaypoints = new ArrayList<>();

    public DeleteObjectCommand(CoreRegistry registry, UUID objectId) {
        this.registry = registry;
        this.objectId = objectId;
    }

    @Override
    public void execute() {
        if (deletedObject == null) {
            // Backup des Haupt-Objekts
            deletedObject = registry.getObject(objectId);

            if (deletedObject != null) {
                // Kaskade analysieren: Welche Verbindungen hängen an diesem Objekt?
                registry.getConnections().forEach((connId, conn) -> {
                    if (conn.sourceId().equals(objectId) || conn.targetId().equals(objectId)) {
                        // Verbindung sichern
                        deletedConnections.put(connId, new Connection(
                                conn.sourceId(), 
                                conn.targetId(), 
                                new ArrayList<>(conn.waypointIds())
                        ));

                        // Wegpunkte dieser Verbindung sichern
                        for (UUID wpId : conn.waypointIds()) {
                            FmcObject wp = registry.getObject(wpId);
                            if (wp != null) {
                                deletedWaypoints.add(wp);
                            }
                        }
                    }
                });
            }
        }

        // Ausführung: Objekt entfernen (Registry erledigt intern den Rest der Kaskade)
        registry.removeObject(objectId);
    }

    @Override
    public void undo() {
        if (deletedObject != null) {
            // 1. Haupt-Objekt wiederherstellen
            registry.addObject(deletedObject);

            // 2. Wegpunkte wiederherstellen
            for (FmcObject wp : deletedWaypoints) {
                registry.addObject(wp);
            }

            // 3. Verbindungen wiederherstellen
            deletedConnections.forEach(registry::addConnection);
        }
    }

    @Override
    public void redo() {
        registry.removeObject(objectId);
    }
}
