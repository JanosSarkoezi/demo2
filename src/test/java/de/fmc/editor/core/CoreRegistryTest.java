package de.fmc.editor.core;

import de.fmc.editor.core.event.RegistryEvent;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

public class CoreRegistryTest {

    @Test
    public void testAddObjectAndFireEvent() {
        CoreRegistry registry = new CoreRegistry();
        UUID layerId = UUID.randomUUID();
        FmcObject obj = FmcFactory.createObject(FmcType.KREIS, 100, 150, layerId);

        AtomicBoolean eventFired = new AtomicBoolean(false);
        registry.addListener(event -> {
            if (event instanceof RegistryEvent.ObjectAdded added) {
                assertEquals(obj.id(), added.object().id());
                eventFired.set(true);
            }
        });

        registry.addObject(obj);
        assertTrue(eventFired.get(), "Das ObjectAdded-Event wurde nicht gefeuert!");
    }

    @Test
    public void testBipartiteValidation() {
        CoreRegistry registry = new CoreRegistry();
        UUID layerId = CoreRegistry.DEFAULT_LAYER_ID;

        FmcObject k1 = FmcFactory.createObject(FmcType.KREIS, 0, 0, layerId);
        FmcObject k2 = FmcFactory.createObject(FmcType.KREIS, 50, 50, layerId);
        FmcObject q1 = FmcFactory.createObject(FmcType.QUADRAT, 100, 100, layerId);

        registry.addObject(k1);
        registry.addObject(k2);
        registry.addObject(q1);

        assertNull(registry.addConnection(k1.id(), k2.id(), java.util.Collections.emptyList()), "Verbindung Kreis-Kreis hätte fehlschlagen müssen!");
        assertNotNull(registry.addConnection(k1.id(), q1.id(), java.util.Collections.emptyList()), "Verbindung Kreis-Quadrat hätte klappen müssen!");
    }

    @Test
    public void testConnectionEvents() {
        CoreRegistry registry = new CoreRegistry();
        FmcObject k1 = FmcFactory.createObject(FmcType.KREIS, 0, 0, CoreRegistry.DEFAULT_LAYER_ID);
        FmcObject q1 = FmcFactory.createObject(FmcType.QUADRAT, 100, 100, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(k1);
        registry.addObject(q1);

        AtomicBoolean addedFired = new AtomicBoolean(false);
        AtomicBoolean removedFired = new AtomicBoolean(false);

        registry.addListener(event -> {
            if (event instanceof RegistryEvent.ConnectionAdded added) {
                assertEquals(k1.id(), added.connection().sourceId());
                assertEquals(q1.id(), added.connection().targetId());
                addedFired.set(true);
            }
            if (event instanceof RegistryEvent.ConnectionRemoved) {
                removedFired.set(true);
            }
        });

        assertNotNull(registry.addConnection(k1.id(), q1.id(), java.util.Collections.emptyList()));
        assertTrue(addedFired.get(), "ConnectionAdded-Event wurde nicht gefeuert!");

        UUID connId = registry.getConnections().keySet().iterator().next();
        registry.removeConnection(connId);
        assertTrue(removedFired.get(), "ConnectionRemoved-Event wurde nicht gefeuert!");
    }

    @Test
    public void testLayerVisibility() {
        CoreRegistry registry = new CoreRegistry();
        AtomicBoolean visibilityFired = new AtomicBoolean(false);

        registry.addListener(event -> {
            if (event instanceof RegistryEvent.LayerVisibilityChanged changed) {
                assertEquals(CoreRegistry.DEFAULT_LAYER_ID, changed.id());
                assertFalse(changed.visible());
                visibilityFired.set(true);
            }
        });

        registry.setLayerVisibility(CoreRegistry.DEFAULT_LAYER_ID, false);
        assertTrue(visibilityFired.get(), "LayerVisibilityChanged-Event wurde nicht gefeuert!");
    }
}
