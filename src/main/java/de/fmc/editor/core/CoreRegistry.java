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
    private final Map<UUID, FmcObject> objects = new HashMap<>();
    private final Map<UUID, Connection> connections = new HashMap<>();
    private final List<RegistryListener> listeners = new ArrayList<>();

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
            fireEvent(new RegistryEvent.ObjectRemoved(id));
        }
    }

    public boolean addConnection(UUID sourceId, UUID targetId) {
        FmcObject source = objects.get(sourceId);
        FmcObject target = objects.get(targetId);

        if (source == null || target == null) return false;
        if (source.type() == target.type()) return false;

        Connection connection = new Connection(sourceId, targetId);
        connections.put(UUID.randomUUID(), connection);
        return true;
    }

    public Collection<FmcObject> getObjects() {
        return Collections.unmodifiableCollection(objects.values());
    }
}
