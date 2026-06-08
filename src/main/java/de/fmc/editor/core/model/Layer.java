package de.fmc.editor.core.model;

import java.util.Objects;
import java.util.UUID;

public record Layer(
    UUID id,
    String name,
    boolean visible
) {
    public Layer {
        Objects.requireNonNull(id, "ID darf nicht null sein");
        Objects.requireNonNull(name, "Name darf nicht null sein");
    }
}
