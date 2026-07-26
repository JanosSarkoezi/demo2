package de.fmc.editor.state;

import javafx.scene.input.KeyCode;
import java.util.function.Predicate;

@FunctionalInterface
public interface EventMatcher extends Predicate<InteractionEventData> {

    // --- Nützliche Factory-Methoden für lesbare Matcher ---

    static EventMatcher keyPressed(KeyCode key) {
        return data -> data.activeKey().filter(k -> k == key).isPresent();
    }

    static EventMatcher shortcut(KeyCode key) {
        return data -> data.isShortcut(key);
    }

    static EventMatcher primaryClick() {
        return data -> data.isPrimaryButtonDown() && data.clickCount() == 1;
    }

    static EventMatcher primaryDoubleClick() {
        return data -> data.isPrimaryButtonDown() && data.clickCount() == 2;
    }

    static EventMatcher shiftClick() {
        return data -> data.isPrimaryButtonDown() && data.isShiftDown();
    }

    // Kombinationen von Matchern (z.B. Tastatur + Maus)
    default EventMatcher and(EventMatcher other) {
        return data -> this.test(data) && other.test(data);
    }
}