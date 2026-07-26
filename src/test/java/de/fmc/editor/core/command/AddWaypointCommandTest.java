package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.util.GeometryUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AddWaypointCommandTest {

    private CoreRegistry registry;
    private FmcObject source;
    private FmcObject target;
    private UUID connId;

    @BeforeEach
    public void setUp() {
        registry = new CoreRegistry();
        source = FmcFactory.createObject(FmcType.RECTANGLE, 0, 0, CoreRegistry.DEFAULT_LAYER_ID);
        target = FmcFactory.createObject(FmcType.CIRCLE, 100, 0, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(source);
        registry.addObject(target);
        connId = registry.addConnection(source.id(), target.id(), List.of());
    }

    @Test
    public void testCascadingWaypointInsertionIndices() {
        // 1. Einfügen bei X=50 (Mitte) -> Sollte an Index 0 landen
        FmcObject w1 = FmcFactory.createObject(FmcType.WAYPOINT, 50, 0, CoreRegistry.WAYPOINT_LAYER_ID);
        int index1 = getInsertionIndex(50, 0);
        assertEquals(0, index1, "Erster Wegpunkt muss an Index 0 eingefügt werden");
        
        var cmd1 = new AddWaypointCommand(registry, connId, w1, index1);
        cmd1.execute();
        
        // 2. Einfügen bei X=75 (Zwischen W1 und Target) -> Sollte an Index 1 landen
        FmcObject w2 = FmcFactory.createObject(FmcType.WAYPOINT, 75, 0, CoreRegistry.WAYPOINT_LAYER_ID);
        int index2 = getInsertionIndex(75, 0);
        assertEquals(1, index2, "Wegpunkt zwischen W1 und Target muss an Index 1 eingefügt werden");
        
        var cmd2 = new AddWaypointCommand(registry, connId, w2, index2);
        cmd2.execute();

        // Check Connection: [w1, w2]
        Connection conn = registry.getConnections().get(connId);
        assertEquals(List.of(w1.id(), w2.id()), conn.waypointIds());

        // 3. Einfügen bei X=25 (Zwischen Source und W1) -> Sollte an Index 0 landen
        FmcObject w3 = FmcFactory.createObject(FmcType.WAYPOINT, 25, 0, CoreRegistry.WAYPOINT_LAYER_ID);
        int index3 = getInsertionIndex(25, 0);
        assertEquals(0, index3, "Wegpunkt zwischen Source und W1 muss an Index 0 eingefügt werden");
        
        var cmd3 = new AddWaypointCommand(registry, connId, w3, index3);
        cmd3.execute();

        // Check Connection: [w3, w1, w2]
        conn = registry.getConnections().get(connId);
        assertEquals(List.of(w3.id(), w1.id(), w2.id()), conn.waypointIds());

        // 4. Einfügen bei X=62.5 (Zwischen W1 und W2) -> Sollte an Index 2 landen (da w3 an 0, w1 an 1, w2 an 3)
        FmcObject w4 = FmcFactory.createObject(FmcType.WAYPOINT, 62.5, 0, CoreRegistry.WAYPOINT_LAYER_ID);
        int index4 = getInsertionIndex(62.5, 0);
        assertEquals(2, index4, "Wegpunkt zwischen W1 und W2 muss an Index 2 eingefügt werden");
        
        var cmd4 = new AddWaypointCommand(registry, connId, w4, index4);
        cmd4.execute();

        // Check Connection: [w3, w1, w4, w2]
        conn = registry.getConnections().get(connId);
        assertEquals(List.of(w3.id(), w1.id(), w4.id(), w2.id()), conn.waypointIds());
    }

    private int getInsertionIndex(double clickX, double clickY) {
        Connection conn = registry.getConnections().get(connId);
        List<FmcObject> currentWps = new ArrayList<>();
        for (UUID id : conn.waypointIds()) {
            FmcObject wp = registry.getObject(id);
            if (wp != null) currentWps.add(wp);
        }
        return GeometryUtils.calculateInsertionIndex(clickX, clickY, source, target, currentWps);
    }
}
