package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import java.util.Map;
import java.util.UUID;

public class MoveMultipleObjectsCommand implements Command {
    private final CoreRegistry registry;
    private final Map<UUID, Position> oldPositions;
    private final Map<UUID, Position> newPositions;

    public record Position(double x, double y) {}

    public MoveMultipleObjectsCommand(CoreRegistry registry, Map<UUID, Position> oldPositions, Map<UUID, Position> newPositions) {
        this.registry = registry;
        this.oldPositions = Map.copyOf(oldPositions);
        this.newPositions = Map.copyOf(newPositions);
    }

    @Override
    public void execute() {
        newPositions.forEach((id, pos) -> registry.moveObject(id, pos.x(), pos.y()));
    }

    @Override
    public void undo() {
        oldPositions.forEach((id, pos) -> registry.moveObject(id, pos.x(), pos.y()));
    }
}
