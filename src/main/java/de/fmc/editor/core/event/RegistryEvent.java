package de.fmc.editor.core.event;

import java.util.UUID;
import de.fmc.editor.core.model.FmcObject;

public sealed interface RegistryEvent {
    record ObjectAdded(FmcObject object) implements RegistryEvent {}
    record ObjectRemoved(UUID id) implements RegistryEvent {}
    record ObjectMoved(UUID id, double newX, double newY) implements RegistryEvent {}
    record ObjectResized(UUID id, double newW, double newH) implements RegistryEvent {}
}
