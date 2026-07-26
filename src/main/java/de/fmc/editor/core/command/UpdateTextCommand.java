package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcText;

public class UpdateTextCommand implements Command {
    private final CoreRegistry registry;
    private final FmcText oldText;
    private final FmcText newText;

    // Neuer Konstruktor mit beiden Zuständen
    public UpdateTextCommand(CoreRegistry registry, FmcText oldText, FmcText newText) {
        this.registry = registry;
        this.oldText = oldText;
        this.newText = newText;
    }

    @Override
    public void execute() {
        registry.updateText(newText.id(), newText);
    }

    @Override
    public void undo() {
        registry.updateText(oldText.id(), oldText);
    }

    @Override
    public void redo() {
        registry.updateText(newText.id(), newText);
    }
}