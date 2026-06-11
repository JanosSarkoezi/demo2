package de.fmc.editor.core.persistence;

import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.Layer;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) für die Serialisierung des Diagramm-Zustands.
 */
public record DiagramData(
    Collection<FmcObject> objects,
    Map<UUID, Connection> connections,
    Collection<Layer> layers
) {}
