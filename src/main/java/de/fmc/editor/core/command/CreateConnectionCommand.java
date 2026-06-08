package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import java.util.List;
import java.util.UUID;

public class CreateConnectionCommand implements Command {
    private final CoreRegistry registry;
    private final UUID sourceId;
    private final UUID targetId;
    private final List<UUID> waypointIds;
    private UUID createdConnectionId;

    public CreateConnectionCommand(CoreRegistry registry, UUID sourceId, UUID targetId, List<UUID> waypointIds) {
        this.registry = registry;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.waypointIds = List.copyOf(waypointIds);
    }

    @Override
    public void execute() {
        createdConnectionId = registry.addConnection(sourceId, targetId, waypointIds);
    }

    @Override
    public void undo() {
        if (createdConnectionId != null) {
            registry.removeConnection(createdConnectionId);
        }
    }
}
