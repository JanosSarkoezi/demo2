package de.fmc.editor.state;

@FunctionalInterface
public interface StateAction {
    void execute(InteractionEventData event, EditorReadContext context);
}