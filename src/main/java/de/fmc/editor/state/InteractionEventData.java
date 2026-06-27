package de.fmc.editor.state;

import javafx.scene.input.KeyCode;
import java.util.Optional;

public record InteractionEventData(
    double worldX,
    double worldY,
    double sceneX,
    double sceneY,
    int clickCount,
    boolean isPrimaryButtonDown,
    boolean isSecondaryButtonDown,
    boolean isMiddleButtonDown,
    boolean isControlDown,
    boolean isShiftDown,
    boolean isAltDown,
    Optional<KeyCode> activeKey
) {
    public boolean isShortcut(KeyCode key) {
        return isControlDown && activeKey.isPresent() && activeKey.get() == key;
    }
}
