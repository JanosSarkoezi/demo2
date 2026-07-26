package de.fmc.editor.core.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add((Consumer<Object>) listener);
    }

    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        if (event == null) return;
        
        // Direct class listeners
        List<Consumer<?>> targets = listeners.get(event.getClass());
        if (targets != null) {
            for (Consumer<?> target : targets) {
                ((Consumer<T>) target).accept(event);
            }
        }
        
        // Interface / superclass listeners
        for (Map.Entry<Class<?>, List<Consumer<?>>> entry : listeners.entrySet()) {
            if (entry.getKey().isInstance(event) && !entry.getKey().equals(event.getClass())) {
                for (Consumer<?> target : entry.getValue()) {
                    ((Consumer<T>) target).accept(event);
                }
            }
        }
    }
}
