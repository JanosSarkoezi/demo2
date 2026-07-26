package de.fmc.editor.core.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Zentrale Verwaltung der aktuellen Auswahl im Editor.
 * Benachrichtigt automatisch einen Listener, wenn sich die Auswahl ändert,
 * sodass die View (ViewMapper) sich selbst aktualisieren kann.
 */
public class SelectionModel {

    // Auswahl für Objekte (Kreise, Rechtecke, Wegpunkte)
    private final Set<UUID> selectedObjectIds = new HashSet<>();
    // Auswahl für Texte (unabhängig von Objekten)
    private final Set<UUID> selectedTextIds = new HashSet<>();

    // Callback, der bei jeder Änderung der Auswahl ausgelöst wird
    private Runnable onChangeListener = () -> {};

    // =========================================================================
    // 1. Setter für den Listener (wird vom CanvasController gesetzt)
    // =========================================================================

    public void setOnChangeListener(Runnable listener) {
        this.onChangeListener = listener != null ? listener : () -> {};
    }

    // =========================================================================
    // 2. Objekt-Auswahl (Object Selection)
    // =========================================================================

    public void selectObject(UUID id) {
        selectedObjectIds.clear();
        if (id != null) {
            selectedObjectIds.add(id);
        }
        notifyChange();
    }

    public void addObjectToSelection(UUID id) {
        if (id != null) {
            selectedObjectIds.add(id);
            notifyChange();
        }
    }

    public void addAllObjectsToSelection(Collection<UUID> ids) {
        if (ids != null && !ids.isEmpty()) {
            selectedObjectIds.addAll(ids);
            notifyChange();
        }
    }

    public void toggleObjectSelection(UUID id) {
        if (id == null) return;
        if (selectedObjectIds.contains(id)) {
            selectedObjectIds.remove(id);
        } else {
            selectedObjectIds.add(id);
        }
        notifyChange();
    }

    public void deselectObject(UUID id) {
        if (id != null && selectedObjectIds.remove(id)) {
            notifyChange();
        }
    }

    public void clearObjectSelection() {
        if (!selectedObjectIds.isEmpty()) {
            selectedObjectIds.clear();
            notifyChange();
        }
    }

    public boolean isObjectSelected(UUID id) {
        return selectedObjectIds.contains(id);
    }

    public Set<UUID> getSelectedObjectIds() {
        return Collections.unmodifiableSet(selectedObjectIds);
    }

    public boolean hasObjectSelection() {
        return !selectedObjectIds.isEmpty();
    }

    // =========================================================================
    // 3. Text-Auswahl (Text Selection)
    // =========================================================================

    public void selectText(UUID id) {
        selectedTextIds.clear();
        if (id != null) {
            selectedTextIds.add(id);
        }
        notifyChange();
    }

    public void addTextToSelection(UUID id) {
        if (id != null) {
            selectedTextIds.add(id);
            notifyChange();
        }
    }

    public void addAllTextsToSelection(Collection<UUID> ids) {
        if (ids != null && !ids.isEmpty()) {
            selectedTextIds.addAll(ids);
            notifyChange();
        }
    }

    public void toggleTextSelection(UUID id) {
        if (id == null) return;
        if (selectedTextIds.contains(id)) {
            selectedTextIds.remove(id);
        } else {
            selectedTextIds.add(id);
        }
        notifyChange();
    }

    public void deselectText(UUID id) {
        if (id != null && selectedTextIds.remove(id)) {
            notifyChange();
        }
    }

    public void clearTextSelection() {
        if (!selectedTextIds.isEmpty()) {
            selectedTextIds.clear();
            notifyChange();
        }
    }

    public boolean isTextSelected(UUID id) {
        return selectedTextIds.contains(id);
    }

    public Set<UUID> getSelectedTextIds() {
        return Collections.unmodifiableSet(selectedTextIds);
    }

    public boolean hasTextSelection() {
        return !selectedTextIds.isEmpty();
    }

    // =========================================================================
    // 4. Globale Auswahl-Aktionen (für "Alles löschen" / "Alle auswählen")
    // =========================================================================

    public void clearAll() {
        boolean changed = !selectedObjectIds.isEmpty() || !selectedTextIds.isEmpty();
        selectedObjectIds.clear();
        selectedTextIds.clear();
        if (changed) {
            notifyChange();
        }
    }

    public boolean isEmpty() {
        return selectedObjectIds.isEmpty() && selectedTextIds.isEmpty();
    }

    // =========================================================================
    // 5. Interner Benachrichtigungsmechanismus
    // =========================================================================

    private void notifyChange() {
        onChangeListener.run();
    }
}