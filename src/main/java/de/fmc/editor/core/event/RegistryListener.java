package de.fmc.editor.core.event;

@FunctionalInterface
public interface RegistryListener {
    void handleEvent(RegistryEvent event);
}
