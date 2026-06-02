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
        UUID layerId = UUID.randomUUID();

        FmcObject k1 = FmcFactory.createObject(FmcType.KREIS, 0, 0, layerId);
        FmcObject k2 = FmcFactory.createObject(FmcType.KREIS, 50, 50, layerId);
        FmcObject q1 = FmcFactory.createObject(FmcType.QUADRAT, 100, 100, layerId);

        registry.addObject(k1);
        registry.addObject(k2);
        registry.addObject(q1);

        assertFalse(registry.addConnection(k1.id(), k2.id()), "Verbindung Kreis-Kreis hätte fehlschlagen müssen!");
        assertTrue(registry.addConnection(k1.id(), q1.id()), "Verbindung Kreis-Quadrat hätte klappen müssen!");
    }
}
