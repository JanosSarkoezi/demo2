package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MoveMultipleObjectsCommandTest {

    private CoreRegistry registry;
    private FmcObject obj1;
    private FmcObject obj2;

    @BeforeEach
    public void setUp() {
        registry = new CoreRegistry();
        obj1 = FmcFactory.createObject(FmcType.KREIS, 10, 10, CoreRegistry.DEFAULT_LAYER_ID);
        obj2 = FmcFactory.createObject(FmcType.QUADRAT, 100, 100, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(obj1);
        registry.addObject(obj2);
    }

    @Test
    public void testExecuteAndUndo() {
        Map<UUID, MoveMultipleObjectsCommand.Position> oldPositions = new HashMap<>();
        oldPositions.put(obj1.id(), new MoveMultipleObjectsCommand.Position(10, 10));
        oldPositions.put(obj2.id(), new MoveMultipleObjectsCommand.Position(100, 100));

        Map<UUID, MoveMultipleObjectsCommand.Position> newPositions = new HashMap<>();
        newPositions.put(obj1.id(), new MoveMultipleObjectsCommand.Position(20, 20));
        newPositions.put(obj2.id(), new MoveMultipleObjectsCommand.Position(110, 110));

        MoveMultipleObjectsCommand cmd = new MoveMultipleObjectsCommand(registry, oldPositions, newPositions);

        // Execute
        cmd.execute();
        
        FmcObject moved1 = registry.getObjects().stream().filter(o -> o.id().equals(obj1.id())).findFirst().orElseThrow();
        FmcObject moved2 = registry.getObjects().stream().filter(o -> o.id().equals(obj2.id())).findFirst().orElseThrow();
        
        assertEquals(20, moved1.x());
        assertEquals(20, moved1.y());
        assertEquals(110, moved2.x());
        assertEquals(110, moved2.y());

        // Undo
        cmd.undo();
        
        FmcObject undone1 = registry.getObjects().stream().filter(o -> o.id().equals(obj1.id())).findFirst().orElseThrow();
        FmcObject undone2 = registry.getObjects().stream().filter(o -> o.id().equals(obj2.id())).findFirst().orElseThrow();
        
        assertEquals(10, undone1.x());
        assertEquals(10, undone1.y());
        assertEquals(100, undone2.x());
        assertEquals(100, undone2.y());
    }
}
