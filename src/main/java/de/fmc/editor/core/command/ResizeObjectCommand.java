package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import java.util.UUID;

public class ResizeObjectCommand implements Command {
    private final CoreRegistry registry;
    private final UUID objectId;
    private final double oldW, oldH;
    private final double newW, newH;

    public ResizeObjectCommand(CoreRegistry registry, UUID objectId, double oldW, double oldH, double newW, double newH) {
        this.registry = registry;
        this.objectId = objectId;
        this.oldW = oldW;
        this.oldH = oldH;
        this.newW = newW;
        this.newH = newH;
    }

    @Override
    public void execute() {
        registry.resizeObject(objectId, newW, newH);
    }

    @Override
    public void undo() {
        registry.resizeObject(objectId, oldW, oldH);
    }

    @Override
    public void redo() {
        execute();
    }
}
