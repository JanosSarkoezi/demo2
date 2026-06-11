package de.fmc.editor.core.event;

import java.util.UUID;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.Connection;
import de.fmc.editor.core.model.Layer;

public sealed interface RegistryEvent {
    record ObjectAdded(FmcObject object) implements RegistryEvent {}
    record ObjectRemoved(UUID id) implements RegistryEvent {}
    record ObjectMoved(UUID id, double newX, double newY) implements RegistryEvent {}
    record ObjectResized(UUID id, double newW, double newH) implements RegistryEvent {}
    record ObjectTextUpdated(UUID id, String newText) implements RegistryEvent {}

    record ConnectionAdded(UUID id, Connection connection) implements RegistryEvent {}
    record ConnectionRemoved(UUID id) implements RegistryEvent {}
    record ConnectionUpdated(UUID id, Connection connection) implements RegistryEvent {}

    record LayerAdded(Layer layer) implements RegistryEvent {}
    record LayerRemoved(UUID id) implements RegistryEvent {}
    record LayerVisibilityChanged(UUID id, boolean visible) implements RegistryEvent {}

    record RegistryReset() implements RegistryEvent {}
}
