package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcText;

public class CreateTextCommand implements Command {
    private final CoreRegistry registry;
    private final FmcText text;

    public CreateTextCommand(CoreRegistry registry, FmcText text) {
        this.registry = registry;
        this.text = text;
    }

    @Override
    public void execute() {
        registry.addText(text);
    }

    @Override
    public void undo() {
        registry.removeText(text.id());
    }

    @Override
    public void redo() {
        execute();
    }
}
