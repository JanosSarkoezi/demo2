package de.fmc.editor.controller;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import java.util.Map;

/**
 * Zentrale Definition aller Tastaturkürzel des Editors.
 * Jeder Eintrag besteht aus einem sprechenden Namen, der Tastenkombination
 * und einer optionalen Beschreibung (für ein späteres Shortcut-Menü).
 */
public enum Shortcut {

    // --- Globale Aktionen (immer verfügbar) ---
    UNDO("Rückgängig", 
         new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN)),
    REDO("Wiederherstellen", 
         new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)),
    DELETE("Löschen", 
         new KeyCodeCombination(KeyCode.DELETE)),
    DELETE_ALT("Löschen (alternativ)", 
         new KeyCodeCombination(KeyCode.BACK_SPACE)),
    SELECT_ALL("Alle auswählen", 
         new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN)),
    SAVE("Speichern", 
         new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)),
    LOAD("Laden", 
         new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN)),

    // --- Werkzeug-Switches ---
    TOOL_CIRCLE("Kreis-Werkzeug", 
         new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.ALT_DOWN)),
    TOOL_RECTANGLE("Rechteck-Werkzeug", 
         new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.ALT_DOWN)),
    TOOL_CONNECTION("Verbindungs-Werkzeug", 
         new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.ALT_DOWN)),

    // --- State-spezifische Shortcuts ---
    CANCEL("Abbrechen", 
         new KeyCodeCombination(KeyCode.ESCAPE));

    private final String name;
    private final KeyCodeCombination combination;

    Shortcut(String name, KeyCodeCombination combination) {
        this.name = name;
        this.combination = combination;
    }

    public String getName() {
        return name;
    }

    public KeyCodeCombination getCombination() {
        return combination;
    }

    /**
     * Praktische Hilfsmethode zum Registrieren in einer Map.
     */
    public void register(Map<KeyCombination, Runnable> acceleratorMap, Runnable action) {
        acceleratorMap.put(this.combination, action);
    }

    @Override
    public String toString() {
        return name + " (" + combination.getDisplayText() + ")";
    }
}
