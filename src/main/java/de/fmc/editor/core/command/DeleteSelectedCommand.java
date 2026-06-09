package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class DeleteSelectedCommand implements Command {
    private final List<Command> subCommands = new ArrayList<>();

    public DeleteSelectedCommand(CoreRegistry registry, Collection<UUID> selectedIds) {
        for (UUID id : selectedIds) {
            FmcObject obj = registry.getObject(id);
            if (obj != null) {
                if (obj.type() == de.fmc.editor.core.model.FmcType.WEGPUNKT) {
                    subCommands.add(new DeleteWaypointCommand(registry, id));
                } else {
                    subCommands.add(new DeleteObjectCommand(registry, id));
                }
            }
        }
    }

    @Override
    public void execute() {
        for (Command cmd : subCommands) {
            cmd.execute();
        }
    }

    @Override
    public void undo() {
        // Undo in umgekehrter Reihenfolge
        for (int i = subCommands.size() - 1; i >= 0; i--) {
            subCommands.get(i).undo();
        }
    }

    @Override
    public void redo() {
        for (Command cmd : subCommands) {
            cmd.redo();
        }
    }
}
