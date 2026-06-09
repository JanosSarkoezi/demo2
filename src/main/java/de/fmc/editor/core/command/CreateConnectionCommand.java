package de.fmc.editor.core.command;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.Connection;

import java.util.List;
import java.util.UUID;

public class CreateConnectionCommand implements Command {
    private final CoreRegistry registry;
    private final UUID connectionId;
    private final Connection connection;
    private boolean success = false;

    public CreateConnectionCommand(CoreRegistry registry, UUID sourceId, UUID targetId, List<UUID> waypointIds) {
        this.registry = registry;
        this.connectionId = UUID.randomUUID();
        this.connection = new Connection(sourceId, targetId, List.copyOf(waypointIds));
    }

    @Override
    public void execute() {
        // Wir prüfen, ob die Verbindung valide ist (Registry-Logik nutzen)
        // Eigentlich sollte die Validierung schon im State passieren, 
        // aber hier sichern wir uns ab.
        registry.addConnection(connectionId, connection);
        this.success = true;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public void undo() {
        registry.removeConnection(connectionId);
    }

    @Override
    public void redo() {
        execute();
    }
}
