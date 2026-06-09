package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import java.util.UUID;

public class MoveObjectCommand implements Command {
    private final CoreRegistry registry;
    private final UUID objectId;
    private final double oldX, oldY;
    private final double newX, newY;

    public MoveObjectCommand(CoreRegistry registry, UUID objectId, double oldX, double oldY, double newX, double newY) {
        this.registry = registry;
        this.objectId = objectId;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
    }

    @Override
    public void execute() {
        registry.moveObject(objectId, newX, newY);
    }

    @Override
    public void undo() {
        registry.moveObject(objectId, oldX, oldY);
    }

    @Override
    public void redo() {
        execute();
    }
}
