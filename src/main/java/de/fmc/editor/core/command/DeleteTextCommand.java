package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcText;

public class DeleteTextCommand implements Command {
    private final CoreRegistry registry;
    private final FmcText text;

    public DeleteTextCommand(CoreRegistry registry, FmcText text) {
        this.registry = registry;
        this.text = text;
    }

    @Override
    public void execute() {
        registry.removeText(text.id());
    }

    @Override
    public void undo() {
        registry.addText(text);
    }

    @Override
    public void redo() {
        execute();
    }
}
