package de.fmc.editor.state;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class InteractionMap {

    private final List<Binding> singleBindings = new ArrayList<>();
    private final List<DragBinding> dragBindings = new ArrayList<>();

    // Aktiver Drag-Zustand (null, wenn gerade kein Drag läuft)
    private ActiveDrag activeDrag = null;

    private record Binding(EventMatcher matcher, Consumer<InteractionEventData> action) {}
    private record DragBinding(EventMatcher startMatcher, Function<InteractionEventData, DragHandler> handlerSupplier) {}
    private record ActiveDrag(DragHandler handler, InteractionEventData startEvent) {}

    public InteractionEventData getActiveDragStartEvent() {
        return activeDrag != null ? activeDrag.startEvent() : null;
    }

    // --- Standard-Aktionen ---
    public InteractionMap on(EventMatcher matcher, java.util.function.Consumer<InteractionEventData> action) {
        singleBindings.add(new Binding(matcher, action));
        return this;
    }

    // --- Continuous Drag-Aktionen ---
    public InteractionMap onDrag(EventMatcher startMatcher, java.util.function.Function<InteractionEventData, DragHandler> handlerSupplier) {
        dragBindings.add(new DragBinding(startMatcher, handlerSupplier));
        return this;
    }

    // --- Event Handling ---

    /** Aufzurufen bei MOUSE_PRESSED */
    public boolean handlePress(InteractionEventData event) {
        // Prüfen, ob ein Drag gestartet werden soll
        for (DragBinding binding : dragBindings) {
            if (binding.startMatcher().test(event)) {
                DragHandler handler = binding.handlerSupplier().apply(event);
                if (handler != null) {
                    handler.onStart(event);
                    activeDrag = new ActiveDrag(handler, event);
                    return true;
                }
            }
        }

        // Falls kein Drag-Start matcht, normale Klick-Aktionen prüfen
        return handleSingleEvent(event);
    }

    /** Aufzurufen bei MOUSE_DRAGGED */
    public boolean handleDrag(InteractionEventData event) {
        if (activeDrag != null) {
            double deltaX = event.worldX() - activeDrag.startEvent().worldX();
            double deltaY = event.worldY() - activeDrag.startEvent().worldY();

            activeDrag.handler().onDrag(event, deltaX, deltaY);
            return true;
        }
        return false;
    }

    /** Aufzurufen bei MOUSE_RELEASED */
    public boolean handleRelease(InteractionEventData event) {
        if (activeDrag != null) {
            activeDrag.handler().onEnd(event);
            activeDrag = null; // Drag beendet
            return true;
        }
        return false;
    }

    private boolean handleSingleEvent(InteractionEventData event) {
        for (Binding binding : singleBindings) {
            if (binding.matcher().test(event)) {
                binding.action().accept(event);
                return true;
            }
        }
        return false;
    }
}