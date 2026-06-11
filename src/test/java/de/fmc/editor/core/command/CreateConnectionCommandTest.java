package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CreateConnectionCommandTest {

    private CoreRegistry registry;
    private FmcObject source;
    private FmcObject target;

    @BeforeEach
    public void setUp() {
        registry = new CoreRegistry();
        source = FmcFactory.createObject(FmcType.KREIS, 0, 0, CoreRegistry.DEFAULT_LAYER_ID);
        target = FmcFactory.createObject(FmcType.QUADRAT, 100, 100, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(source);
        registry.addObject(target);
    }

    @Test
    public void testExecuteSuccess() {
        CreateConnectionCommand cmd = new CreateConnectionCommand(registry, source.id(), target.id(), Collections.emptyList());
        cmd.execute();
        
        assertTrue(cmd.isSuccess(), "Command should be successful");
        assertEquals(1, registry.getConnections().size(), "One connection should be in registry");
    }

//    @Test
//    public void testExecuteDuplicateFailure() {
//        // Create first connection
//        registry.addConnection(source.id(), target.id(), Collections.emptyList());
//        assertEquals(1, registry.getConnections().size());
//
//        // Attempt to create the same connection via command
//        CreateConnectionCommand cmd = new CreateConnectionCommand(registry, source.id(), target.id(), Collections.emptyList());
//        cmd.execute();
//
//        assertFalse(cmd.isSuccess(), "Command should fail for duplicate connection");
//        assertEquals(1, registry.getConnections().size(), "Registry should still have only one connection");
//    }

    @Test
    public void testUndo() {
        CreateConnectionCommand cmd = new CreateConnectionCommand(registry, source.id(), target.id(), Collections.emptyList());
        cmd.execute();
        assertTrue(cmd.isSuccess());
        assertEquals(1, registry.getConnections().size());

        cmd.undo();
        assertEquals(0, registry.getConnections().size(), "Connection should be removed after undo");
    }

    @Test
    public void testExecuteSameTypeFailure() {
        // Create another circle
        FmcObject anotherCircle = FmcFactory.createObject(FmcType.KREIS, 200, 200, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(anotherCircle);

        // Attempt to connect Kreis with Kreis
        CreateConnectionCommand cmd = new CreateConnectionCommand(registry, source.id(), anotherCircle.id(), Collections.emptyList());
        cmd.execute();

        assertFalse(cmd.isSuccess(), "Command should fail for same type connection (Kreis to Kreis)");
        assertEquals(0, registry.getConnections().size(), "No connection should be added");
    }

    @Test
    public void testExecuteSelfConnectionFailure() {
        // Attempt to connect source with itself
        CreateConnectionCommand cmd = new CreateConnectionCommand(registry, source.id(), source.id(), Collections.emptyList());
        cmd.execute();

        assertFalse(cmd.isSuccess(), "Command should fail for self-connection");
        assertEquals(0, registry.getConnections().size(), "No connection should be added");
    }
}
