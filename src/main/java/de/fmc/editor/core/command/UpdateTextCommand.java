package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import java.util.UUID;

public class UpdateTextCommand implements Command {
    private final CoreRegistry registry;
    private final UUID objectId;
    private final String newText;
    private final String oldText;

    public UpdateTextCommand(CoreRegistry registry, UUID objectId, String newText) {
        this.registry = registry;
        this.objectId = objectId;
        this.newText = newText;
        // Alten Zustand für Undo merken
        this.oldText = registry.getObject(objectId).text();
    }

    @Override
    public void execute() {
        registry.updateObjectText(objectId, newText);
    }

    @Override
    public void undo() {
        registry.updateObjectText(objectId, oldText);
    }

    @Override
    public void redo() {
        execute();
    }
}
