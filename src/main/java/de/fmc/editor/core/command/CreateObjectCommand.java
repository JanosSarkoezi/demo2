package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcObject;

public class CreateObjectCommand implements Command {
    private final CoreRegistry registry;
    private final FmcObject objectToCreate;

    public CreateObjectCommand(CoreRegistry registry, FmcObject objectToCreate) {
        this.registry = registry;
        this.objectToCreate = objectToCreate;
    }

    @Override
    public void execute() {
        registry.addObject(objectToCreate);
    }

    @Override
    public void undo() {
        registry.removeObject(objectToCreate.id());
    }
}
