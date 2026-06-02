package de.fmc.editor.core.model;

import java.util.Objects;
import java.util.UUID;

public record FmcObject(
    UUID id,
    FmcType type,
    double x,
    double y,
    UUID layerId
) {
    public FmcObject {
        Objects.requireNonNull(id, "ID darf nicht null sein");
        Objects.requireNonNull(type, "Typ darf nicht null sein");
    }
}
