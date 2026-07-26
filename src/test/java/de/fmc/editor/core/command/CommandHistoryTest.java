package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class CommandHistoryTest {

    private CoreRegistry registry;
    private CommandHistory history;

    @BeforeEach
    public void setUp() {
        registry = new CoreRegistry();
        history = new CommandHistory();
    }

    @Test
    public void testCreateObjectCommandExecuteAndUndo() {
        FmcObject obj = FmcFactory.createObject(FmcType.CIRCLE, 100, 100, CoreRegistry.DEFAULT_LAYER_ID);
        CreateObjectCommand cmd = new CreateObjectCommand(registry, obj);

        history.executeCommand(cmd);
        assertEquals(1, registry.getObjects().size(), "Object should be added to registry");
        assertTrue(registry.getObjects().contains(obj));

        history.undo();
        assertEquals(0, registry.getObjects().size(), "Object should be removed after undo");

        history.redo();
        assertEquals(1, registry.getObjects().size(), "Object should be re-added after redo");
        assertTrue(registry.getObjects().contains(obj));
    }

    @Test
    public void testMoveObjectCommandExecuteAndUndo() {
        FmcObject obj = FmcFactory.createObject(FmcType.CIRCLE, 100, 100, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(obj);

        MoveObjectCommand cmd = new MoveObjectCommand(registry, obj.id(), 100, 100, 200, 300);
        history.executeCommand(cmd);

        FmcObject moved = registry.getObject(obj.id());
        assertEquals(200, moved.x(), 0.001);
        assertEquals(300, moved.y(), 0.001);

        history.undo();
        FmcObject undone = registry.getObject(obj.id());
        assertEquals(100, undone.x(), 0.001);
        assertEquals(100, undone.y(), 0.001);

        history.redo();
        FmcObject redone = registry.getObject(obj.id());
        assertEquals(200, redone.x(), 0.001);
        assertEquals(300, redone.y(), 0.001);
    }

    @Test
    public void testCreateConnectionCommandValidationAndUndo() {
        FmcObject source = FmcFactory.createObject(FmcType.CIRCLE, 100, 100, CoreRegistry.DEFAULT_LAYER_ID);
        FmcObject target = FmcFactory.createObject(FmcType.RECTANGLE, 300, 300, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(source);
        registry.addObject(target);

        // Valid connection: Kreis to Quadrat
        CreateConnectionCommand validCmd = new CreateConnectionCommand(registry, source.id(), target.id(), Collections.emptyList());
        history.executeCommand(validCmd);

        assertTrue(validCmd.isSuccess());
        assertEquals(1, registry.getConnections().size(), "Connection should be created");

        history.undo();
        assertEquals(0, registry.getConnections().size(), "Connection should be removed on undo");

        // Invalid connection: Kreis to Kreis (Bipartite validation)
        FmcObject anotherCircle = FmcFactory.createObject(FmcType.CIRCLE, 400, 400, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(anotherCircle);

        CreateConnectionCommand invalidCmd = new CreateConnectionCommand(registry, source.id(), anotherCircle.id(), Collections.emptyList());
        history.executeCommand(invalidCmd);

        assertFalse(invalidCmd.isSuccess(), "Bipartite validation should fail for Kreis-to-Kreis");
        assertEquals(0, registry.getConnections().size(), "Invalid connection should not be in registry");
    }

    @Test
    public void testUndoRedoBoundaries() {
        // Calling undo/redo on empty history should not throw exceptions
        assertDoesNotThrow(() -> history.undo());
        assertDoesNotThrow(() -> history.redo());

        FmcObject obj = FmcFactory.createObject(FmcType.CIRCLE, 100, 100, CoreRegistry.DEFAULT_LAYER_ID);
        CreateObjectCommand cmd = new CreateObjectCommand(registry, obj);

        history.executeCommand(cmd);
        history.undo();
        
        // Double undo on single command
        assertDoesNotThrow(() -> history.undo());
        assertEquals(0, registry.getObjects().size());

        history.redo();
        assertEquals(1, registry.getObjects().size());

        // Double redo on single command
        assertDoesNotThrow(() -> history.redo());
        assertEquals(1, registry.getObjects().size());
    }
}
