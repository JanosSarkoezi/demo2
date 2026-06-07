package de.fmc.editor.state;

public record MouseEventData(
    double worldX,
    double worldY,
    double sceneX,
    double sceneY,
    int clickCount,
    boolean isPrimaryButtonDown
) {}
