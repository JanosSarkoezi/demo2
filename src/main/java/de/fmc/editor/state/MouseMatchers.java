package de.fmc.editor.state;

import de.fmc.editor.controller.CanvasController;

public class MouseMatchers {

    public static MouseEventMatcher primaryButton() {
        return (e, ctx) -> e.isPrimaryButtonDown();
    }

    public static MouseEventMatcher clickCount(int count) {
        return (e, ctx) -> e.clickCount() == count;
    }

    public static MouseEventMatcher controlDown() {
        return (e, ctx) -> e.isControlDown();
    }

    public static MouseEventMatcher noControlDown() {
        return (e, ctx) -> !e.isControlDown();
    }

    public static MouseEventMatcher objectHit() {
        return (e, ctx) -> ctx.findObjectAt(e.worldX(), e.worldY()) != null;
    }

    public static MouseEventMatcher noObjectHit() {
        return (e, ctx) -> ctx.findObjectAt(e.worldX(), e.worldY()) == null;
    }

    public static MouseEventMatcher connectionHit() {
        return (e, ctx) -> ctx.findConnectionAt(e.sceneX(), e.sceneY()) != null;
    }

    public static MouseEventMatcher noConnectionHit() {
        return (e, ctx) -> ctx.findConnectionAt(e.sceneX(), e.sceneY()) == null;
    }

    public static MouseEventMatcher alwaysTrue() {
        return (e, ctx) -> true;
    }

    public static MouseEventMatcher all(MouseEventMatcher... matchers) {
        return (e, ctx) -> {
            for (MouseEventMatcher m : matchers) {
                if (!m.matches(e, ctx)) return false;
            }
            return true;
        };
    }

    public static MouseEventMatcher any(MouseEventMatcher... matchers) {
        return (e, ctx) -> {
            for (MouseEventMatcher m : matchers) {
                if (m.matches(e, ctx)) return true;
            }
            return false;
        };
    }
}
