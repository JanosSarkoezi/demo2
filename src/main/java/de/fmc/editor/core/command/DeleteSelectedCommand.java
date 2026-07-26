package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.FmcText;
import de.fmc.editor.core.model.SelectionModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class DeleteSelectedCommand implements Command {
    private final List<Command> subCommands = new ArrayList<>();

    public DeleteSelectedCommand(CoreRegistry registry, SelectionModel selectionModel) {
        Collection<UUID> selectedObjectIds = selectionModel.getSelectedObjectIds();
        Collection<UUID> selectedTextIds = selectionModel.getSelectedTextIds();

        if (selectedObjectIds != null) {
            for (UUID id : selectedObjectIds) {
                FmcObject obj = registry.getObject(id);
                if (obj != null) {
                    if (obj.type() == FmcType.WAYPOINT) {
                        subCommands.add(new DeleteWaypointCommand(registry, id));
                    } else {
                        subCommands.add(new DeleteObjectCommand(registry, id));
                    }
                }
            }
        }
        if (selectedTextIds != null) {
            for (UUID id : selectedTextIds) {
                FmcText txt = registry.getText(id);
                if (txt != null) {
                    subCommands.add(new DeleteTextCommand(registry, txt));
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
