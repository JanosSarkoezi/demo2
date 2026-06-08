package de.fmc.editor.core.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Connection(
    UUID sourceId,
    UUID targetId,
    List<UUID> waypointIds
) {
    public Connection {
        Objects.requireNonNull(sourceId, "Source ID darf nicht null sein");
        Objects.requireNonNull(targetId, "Target ID darf nicht null sein");
        Objects.requireNonNull(waypointIds, "Waypoint-Liste darf nicht null sein");
    }
}
