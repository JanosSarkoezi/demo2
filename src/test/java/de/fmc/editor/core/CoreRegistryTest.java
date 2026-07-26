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
        FmcObject obj = FmcFactory.createObject(FmcType.CIRCLE, 100, 150, layerId);

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

        FmcObject k1 = FmcFactory.createObject(FmcType.CIRCLE, 0, 0, layerId);
        FmcObject k2 = FmcFactory.createObject(FmcType.CIRCLE, 50, 50, layerId);
        FmcObject q1 = FmcFactory.createObject(FmcType.RECTANGLE, 100, 100, layerId);

        registry.addObject(k1);
        registry.addObject(k2);
        registry.addObject(q1);

        assertNull(registry.addConnection(k1.id(), k2.id(), java.util.Collections.emptyList()), "Verbindung Kreis-Kreis hätte fehlschlagen müssen!");
        assertNotNull(registry.addConnection(k1.id(), q1.id(), java.util.Collections.emptyList()), "Verbindung Kreis-Quadrat hätte klappen müssen!");
    }

    @Test
    public void testConnectionEvents() {
        CoreRegistry registry = new CoreRegistry();
        FmcObject k1 = FmcFactory.createObject(FmcType.CIRCLE, 0, 0, CoreRegistry.DEFAULT_LAYER_ID);
        FmcObject q1 = FmcFactory.createObject(FmcType.RECTANGLE, 100, 100, CoreRegistry.DEFAULT_LAYER_ID);
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

    @Test
    public void testWaypointLayerInitialization() {
        CoreRegistry registry = new CoreRegistry();
        assertTrue(registry.getLayers().containsKey(CoreRegistry.WAYPOINT_LAYER_ID), "Waypoint-Layer sollte existieren!");
        assertFalse(registry.getLayers().get(CoreRegistry.WAYPOINT_LAYER_ID).visible(), "Waypoint-Layer sollte initial unsichtbar sein!");

        AtomicBoolean visibilityFired = new AtomicBoolean(false);
        registry.addListener(event -> {
            if (event instanceof RegistryEvent.LayerVisibilityChanged changed) {
                if (changed.id().equals(CoreRegistry.WAYPOINT_LAYER_ID)) {
                    assertTrue(changed.visible());
                    visibilityFired.set(true);
                }
            }
        });

        registry.setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
        assertTrue(visibilityFired.get(), "LayerVisibilityChanged-Event für Waypoint-Layer wurde nicht gefeuert!");
    }

    @Test
    public void testTextCRUDAndCascadeDelete() {
        CoreRegistry registry = new CoreRegistry();
        UUID textId = UUID.randomUUID();
        
        // 1. Check add text
        de.fmc.editor.core.model.FmcText text = new de.fmc.editor.core.model.FmcText(
            textId, "Hello", 100, 100, 150, "System", 14.0, "normal", "normal", "#000000", null, CoreRegistry.DEFAULT_LAYER_ID
        );
        
        java.util.List<RegistryEvent> firedEvents = new java.util.ArrayList<>();
        registry.addListener(firedEvents::add);
        
        registry.addText(text);
        assertEquals(1, firedEvents.size());
        assertTrue(firedEvents.get(0) instanceof RegistryEvent.TextAdded);
        assertEquals(textId, ((RegistryEvent.TextAdded) firedEvents.get(0)).text().id());
        assertEquals(1, registry.getTexts().size());
        
        // 2. Check update text
        firedEvents.clear();
        de.fmc.editor.core.model.FmcText updated = new de.fmc.editor.core.model.FmcText(
            textId, "Hello World", 100, 100, 150, "System", 14.0, "normal", "normal", "#000000", null, CoreRegistry.DEFAULT_LAYER_ID
        );
        
        registry.updateText(textId, updated);
        assertEquals(1, firedEvents.size());
        assertTrue(firedEvents.get(0) instanceof RegistryEvent.TextUpdated);
        assertEquals("Hello World", ((RegistryEvent.TextUpdated) firedEvents.get(0)).text().text());
        assertEquals("Hello World", registry.getText(textId).text());

        // 3. Check parent object movement delta propagation
        FmcObject parent = FmcFactory.createObject(FmcType.RECTANGLE, 100, 100, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(parent);
        
        de.fmc.editor.core.model.FmcText associatedText = new de.fmc.editor.core.model.FmcText(
            UUID.randomUUID(), "Associated", 100, 150, 150, "System", 14.0, "normal", "normal", "#000000", parent.id(), CoreRegistry.DEFAULT_LAYER_ID
        );
        registry.addText(associatedText);
        
        registry.moveObject(parent.id(), 150, 120); // deltaX = 50, deltaY = 20
        de.fmc.editor.core.model.FmcText movedText = registry.getText(associatedText.id());
        assertEquals(150.0, movedText.x(), 0.001);
        assertEquals(170.0, movedText.y(), 0.001);

        // 4. Check cascade delete when parent object is removed
        registry.removeObject(parent.id());
        assertNull(registry.getText(associatedText.id()), "Text should have been cascade deleted along with the parent shape!");
        
        // 5. Check manual remove text
        firedEvents.clear();
        registry.removeText(textId);
        assertFalse(firedEvents.isEmpty());
        assertTrue(firedEvents.stream().anyMatch(e -> e instanceof RegistryEvent.TextRemoved && ((RegistryEvent.TextRemoved) e).id().equals(textId)));
        assertEquals(0, registry.getTexts().size());
    }
}
