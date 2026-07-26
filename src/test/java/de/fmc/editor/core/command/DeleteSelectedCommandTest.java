package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.SelectionModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DeleteSelectedCommandTest {

    private CoreRegistry registry;
    private FmcObject source;
    private FmcObject target;
    private FmcObject waypoint;
    private UUID connId;
    private SelectionModel selectionModel;  // NEU

    @BeforeEach
    public void setUp() {
        registry = new CoreRegistry();
        selectionModel = new SelectionModel();  // NEU
        source = FmcFactory.createObject(FmcType.CIRCLE, 0, 0, CoreRegistry.DEFAULT_LAYER_ID);
        target = FmcFactory.createObject(FmcType.RECTANGLE, 100, 100, CoreRegistry.DEFAULT_LAYER_ID);
        waypoint = FmcFactory.createObject(FmcType.WAYPOINT, 50, 50, CoreRegistry.WAYPOINT_LAYER_ID);

        registry.addObject(source);
        registry.addObject(target);
        registry.addObject(waypoint);

        connId = registry.addConnection(source.id(), target.id(), List.of(waypoint.id()));
    }

    @Test
    public void testDeleteObjectCascadesToConnection() {
        // NEU: Auswahl setzen
        selectionModel.clearAll();
        selectionModel.addObjectToSelection(source.id());

        DeleteSelectedCommand cmd = new DeleteSelectedCommand(registry, selectionModel);
        cmd.execute();

        assertEquals(1, registry.getObjects().size(), "Only target remains; source and cascading waypoint should be gone");
        assertEquals(0, registry.getConnections().size(), "Connection should be deleted kaskadierend");

        cmd.undo();
        assertEquals(3, registry.getObjects().size(), "All objects (source, target, waypoint) should be back");
        assertEquals(1, registry.getConnections().size(), "Connection should be back");
    }

    @Test
    public void testDeleteWaypointUpdatesConnection() {
        selectionModel.clearAll();
        selectionModel.addObjectToSelection(waypoint.id());

        DeleteSelectedCommand cmd = new DeleteSelectedCommand(registry, selectionModel);
        cmd.execute();

        assertEquals(2, registry.getObjects().size(), "Waypoint should be gone, source and target remain");
        assertEquals(1, registry.getConnections().size(), "Connection should still exist");

        Connection conn = registry.getConnections().get(connId);
        assertTrue(conn.waypointIds().isEmpty(), "Waypoint should be removed from connection list");

        cmd.undo();
        assertEquals(3, registry.getObjects().size(), "Waypoint should be back");
        conn = registry.getConnections().get(connId);
        assertEquals(1, conn.waypointIds().size(), "Waypoint should be back in connection list");
    }

    @Test
    public void testDeleteMultiple() {
        selectionModel.clearAll();
        selectionModel.addAllObjectsToSelection(List.of(source.id(), target.id()));

        DeleteSelectedCommand cmd = new DeleteSelectedCommand(registry, selectionModel);
        cmd.execute();

        assertEquals(0, registry.getObjects().size(), "Source, target, and cascading waypoint should all be gone");
        assertEquals(0, registry.getConnections().size(), "Connection gone");

        cmd.undo();
        assertEquals(3, registry.getObjects().size());
        assertEquals(1, registry.getConnections().size());
    }
}