package de.fmc.editor.core;

import de.fmc.editor.core.event.RegistryEvent;
import de.fmc.editor.core.event.RegistryListener;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CoreRegistry {
    public static final UUID DEFAULT_LAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final UUID WAYPOINT_LAYER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final Map<UUID, FmcObject> objects = new HashMap<>();
    private final Map<UUID, Connection> connections = new HashMap<>();
    private final Map<UUID, de.fmc.editor.core.model.Layer> layers = new HashMap<>();
    private final List<RegistryListener> listeners = new ArrayList<>();

    public CoreRegistry() {
        // Default Layer anlegen
        addLayer(new de.fmc.editor.core.model.Layer(DEFAULT_LAYER_ID, "Standard", true));
        // Wegpunkt-Layer initial unsichtbar anlegen
        addLayer(new de.fmc.editor.core.model.Layer(WAYPOINT_LAYER_ID, "Wegpunkte", false));
    }

    public void addListener(RegistryListener listener) {
        this.listeners.add(listener);
    }

    private void fireEvent(RegistryEvent event) {
        for (RegistryListener listener : listeners) {
            listener.handleEvent(event);
        }
    }

    public void addObject(FmcObject obj) {
        objects.put(obj.id(), obj);
        fireEvent(new RegistryEvent.ObjectAdded(obj));
    }

    public void moveObject(UUID id, double newX, double newY) {
        FmcObject original = objects.get(id);
        if (original != null) {
            // Nur ändern, wenn wirklich eine Bewegung stattfindet
            if (original.x() != newX || original.y() != newY) {
                FmcObject updated = FmcFactory.moveObject(original, newX, newY);
                objects.put(id, updated);
                fireEvent(new RegistryEvent.ObjectMoved(id, newX, newY));
            }
        }
    }

    public void resizeObject(UUID id, double newW, double newH) {
        FmcObject original = objects.get(id);
        if (original != null) {
            if (original.width() != newW || original.height() != newH) {
                FmcObject updated = FmcFactory.resizeObject(original, newW, newH);
                objects.put(id, updated);
                fireEvent(new RegistryEvent.ObjectResized(id, newW, newH));
            }
        }
    }

    public void removeObject(UUID id) {
        if (objects.containsKey(id)) {
            objects.remove(id);
            
            // 1. Verbindungen finden, die dieses Objekt als Source oder Target nutzen -> Löschen
            List<UUID> toRemove = connections.entrySet().stream()
                    .filter(e -> e.getValue().sourceId().equals(id) || 
                                 e.getValue().targetId().equals(id))
                    .map(Map.Entry::getKey)
                    .toList();
            
            // Bevor wir die Verbindung löschen, müssen wir ihre Wegpunkte aus dem System werfen!
            toRemove.forEach(connId -> {
                Connection conn = connections.get(connId);
                if (conn != null) {
                    for (UUID wpId : conn.waypointIds()) {
                        if (objects.remove(wpId) != null) {
                            fireEvent(new RegistryEvent.ObjectRemoved(wpId));
                        }
                    }
                }
                removeConnection(connId);
            });

            // 2. Verbindungen finden, die dieses Objekt als Wegpunkt nutzen -> Update (entfernen aus Liste)
            connections.forEach((connId, conn) -> {
                if (conn.waypointIds().contains(id)) {
                    List<UUID> updatedWaypoints = new ArrayList<>(conn.waypointIds());
                    updatedWaypoints.remove(id);
                    updateConnectionWaypoints(connId, updatedWaypoints);
                }
            });

            fireEvent(new RegistryEvent.ObjectRemoved(id));
        }
    }

    public UUID addConnection(UUID sourceId, UUID targetId, List<UUID> waypointIds) {
        FmcObject source = objects.get(sourceId);
        FmcObject target = objects.get(targetId);

        if (source == null || target == null) return null;
        if (source.type() == target.type()) return null;

        // Validierung: Keine doppelten Verbindungen (A -> B oder B -> A)
//        boolean connectionExists = connections.values().stream().anyMatch(c ->
//            (c.sourceId().equals(sourceId) && c.targetId().equals(targetId)) ||
//            (c.sourceId().equals(targetId) && c.targetId().equals(sourceId))
//        );
//        if (connectionExists) return null;

        UUID connId = UUID.randomUUID();
        Connection connection = new Connection(sourceId, targetId, new ArrayList<>(waypointIds));
        connections.put(connId, connection);
        fireEvent(new RegistryEvent.ConnectionAdded(connId, connection));
        return connId;
    }

    public void addConnection(UUID id, Connection connection) {
        connections.put(id, connection);
        fireEvent(new RegistryEvent.ConnectionAdded(id, connection));
    }

    public void removeConnection(UUID id) {
        if (connections.containsKey(id)) {
            connections.remove(id);
            fireEvent(new RegistryEvent.ConnectionRemoved(id));
        }
    }

    public void updateConnectionWaypoints(UUID connectionId, List<UUID> waypointIds) {
        Connection oldConn = connections.get(connectionId);
        if (oldConn != null) {
            Connection updatedConn = new Connection(oldConn.sourceId(), oldConn.targetId(), new ArrayList<>(waypointIds));
            connections.put(connectionId, updatedConn);
            fireEvent(new RegistryEvent.ConnectionUpdated(connectionId, updatedConn));
        }
    }

    public void addLayer(de.fmc.editor.core.model.Layer layer) {
        layers.put(layer.id(), layer);
        fireEvent(new RegistryEvent.LayerAdded(layer));
    }

    public void setLayerVisibility(UUID id, boolean visible) {
        de.fmc.editor.core.model.Layer layer = layers.get(id);
        if (layer != null && layer.visible() != visible) {
            layers.put(id, new de.fmc.editor.core.model.Layer(id, layer.name(), visible));
            fireEvent(new RegistryEvent.LayerVisibilityChanged(id, visible));
        }
    }

    public Collection<FmcObject> getObjects() {
        return Collections.unmodifiableCollection(objects.values());
    }

    public FmcObject getObject(UUID id) {
        return objects.get(id);
    }

    public Map<UUID, Connection> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    public Map<UUID, de.fmc.editor.core.model.Layer> getLayers() {
        return Collections.unmodifiableMap(layers);
    }
}
