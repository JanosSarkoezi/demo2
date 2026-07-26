package de.fmc.editor.state;

import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

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
        Optional<KeyCode> activeKey,
        LogicalKey logicalKey
) {
    public InteractionEventData {
        Objects.requireNonNull(activeKey, "activeKey must not be null (use Optional.empty())");
        if (logicalKey == null) {
            logicalKey = LogicalKey.NONE;
        }
    }

    public InteractionEventData(
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
        this(worldX, worldY, sceneX, sceneY, clickCount,
             isPrimaryButtonDown, isSecondaryButtonDown, isMiddleButtonDown,
             isControlDown, isShiftDown, isAltDown, activeKey,
             activeKey.map(InteractionEventData::mapKeyCode).orElse(LogicalKey.NONE));
    }

    public boolean isShortcut(KeyCode key) {
        return isControlDown && activeKey.filter(key::equals).isPresent();
    }

    public boolean isShortcut(LogicalKey key) {
        return isControlDown && logicalKey == key;
    }

    public static LogicalKey mapKeyCode(KeyCode code) {
        if (code == null) return LogicalKey.NONE;
        return switch (code) {
            case ESCAPE -> LogicalKey.ESCAPE;
            case ENTER -> LogicalKey.ENTER;
            case DELETE -> LogicalKey.DELETE;
            case BACK_SPACE -> LogicalKey.BACK_SPACE;
            case CONTROL -> LogicalKey.CONTROL;
            case SHIFT -> LogicalKey.SHIFT;
            case ALT -> LogicalKey.ALT;
            case A -> LogicalKey.A;
            case B -> LogicalKey.B;
            case C -> LogicalKey.C;
            case D -> LogicalKey.D;
            case E -> LogicalKey.E;
            case F -> LogicalKey.F;
            case G -> LogicalKey.G;
            case H -> LogicalKey.H;
            case I -> LogicalKey.I;
            case J -> LogicalKey.J;
            case K -> LogicalKey.K;
            case L -> LogicalKey.L;
            case M -> LogicalKey.M;
            case N -> LogicalKey.N;
            case O -> LogicalKey.O;
            case P -> LogicalKey.P;
            case Q -> LogicalKey.Q;
            case R -> LogicalKey.R;
            case S -> LogicalKey.S;
            case T -> LogicalKey.T;
            case U -> LogicalKey.U;
            case V -> LogicalKey.V;
            case W -> LogicalKey.W;
            case X -> LogicalKey.X;
            case Y -> LogicalKey.Y;
            case Z -> LogicalKey.Z;
            case DIGIT0 -> LogicalKey.DIGIT0;
            case DIGIT1 -> LogicalKey.DIGIT1;
            case DIGIT2 -> LogicalKey.DIGIT2;
            case DIGIT3 -> LogicalKey.DIGIT3;
            case DIGIT4 -> LogicalKey.DIGIT4;
            case DIGIT5 -> LogicalKey.DIGIT5;
            case DIGIT6 -> LogicalKey.DIGIT6;
            case DIGIT7 -> LogicalKey.DIGIT7;
            case DIGIT8 -> LogicalKey.DIGIT8;
            case DIGIT9 -> LogicalKey.DIGIT9;
            case F1 -> LogicalKey.F1;
            case F2 -> LogicalKey.F2;
            case F3 -> LogicalKey.F3;
            case F4 -> LogicalKey.F4;
            case F5 -> LogicalKey.F5;
            case F6 -> LogicalKey.F6;
            case F7 -> LogicalKey.F7;
            case F8 -> LogicalKey.F8;
            case F9 -> LogicalKey.F9;
            case F10 -> LogicalKey.F10;
            case F11 -> LogicalKey.F11;
            case F12 -> LogicalKey.F12;
            default -> LogicalKey.NONE;
        };
    }

    // =========================================================================
    // Statische Factory-Methoden für JavaFX Events
    // =========================================================================

    public static InteractionEventData from(MouseEvent event, BiFunction<Double, Double, Point2D> sceneToWorldTransformer) {
        Point2D worldPos = sceneToWorldTransformer.apply(event.getSceneX(), event.getSceneY());

        return builder()
                .scene(event.getSceneX(), event.getSceneY())
                .world(worldPos.getX(), worldPos.getY())
                .clickCount(event.getClickCount())
                .primaryButton(event.isPrimaryButtonDown())
                .secondaryButton(event.isSecondaryButtonDown())
                .middleButton(event.isMiddleButtonDown())
                .control(event.isControlDown())
                .shift(event.isShiftDown())
                .alt(event.isAltDown())
                .build();
    }

    public static InteractionEventData from(MouseEvent event) {
        return from(event, Point2D::new);
    }

    public static InteractionEventData from(KeyEvent event) {
        return builder()
                .control(event.isControlDown())
                .shift(event.isShiftDown())
                .alt(event.isAltDown())
                .activeKey(event.getCode())
                .build();
    }

    // =========================================================================
    // Builder
    // =========================================================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double worldX = 0.0;
        private double worldY = 0.0;
        private double sceneX = 0.0;
        private double sceneY = 0.0;
        private int clickCount = 1;
        private boolean isPrimaryButtonDown = false;
        private boolean isSecondaryButtonDown = false;
        private boolean isMiddleButtonDown = false;
        private boolean isControlDown = false;
        private boolean isShiftDown = false;
        private boolean isAltDown = false;
        private Optional<KeyCode> activeKey = Optional.empty();
        private LogicalKey logicalKey = LogicalKey.NONE;

        public Builder world(double x, double y) {
            this.worldX = x;
            this.worldY = y;
            return this;
        }

        public Builder scene(double x, double y) {
            this.sceneX = x;
            this.sceneY = y;
            return this;
        }

        public Builder clickCount(int clickCount) {
            this.clickCount = clickCount;
            return this;
        }

        public Builder primaryButton(boolean down) {
            this.isPrimaryButtonDown = down;
            return this;
        }

        public Builder secondaryButton(boolean down) {
            this.isSecondaryButtonDown = down;
            return this;
        }

        public Builder middleButton(boolean down) {
            this.isMiddleButtonDown = down;
            return this;
        }

        public Builder control(boolean down) {
            this.isControlDown = down;
            return this;
        }

        public Builder shift(boolean down) {
            this.isShiftDown = down;
            return this;
        }

        public Builder alt(boolean down) {
            this.isAltDown = down;
            return this;
        }

        public Builder activeKey(KeyCode key) {
            this.activeKey = Optional.ofNullable(key);
            if (key != null) {
                this.logicalKey = mapKeyCode(key);
            }
            return this;
        }

        public Builder logicalKey(LogicalKey key) {
            this.logicalKey = key != null ? key : LogicalKey.NONE;
            return this;
        }

        public InteractionEventData build() {
            return new InteractionEventData(
                    worldX, worldY, sceneX, sceneY, clickCount,
                    isPrimaryButtonDown, isSecondaryButtonDown, isMiddleButtonDown,
                    isControlDown, isShiftDown, isAltDown, activeKey, logicalKey
            );
        }
    }
}