package de.fmc.editor.core.command;

import java.util.Stack;

public class CommandHistory {
    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

    public void executeCommand(Command cmd) {
        cmd.execute();
        addExecutedCommand(cmd);

    }

    public void addExecutedCommand(Command cmd) {
        undoStack.push(cmd);
        redoStack.clear();

//        System.out.println("Neuer Undo-Punkt registriert: " + cmd.getClass().getSimpleName());
//        StackWalker.getInstance().walk(frames -> frames
//                .filter(f -> f.getClassName().startsWith("de.fmc"))
//                .limit(10)
//                .peek(f -> System.out.println("   -> Verursacht durch: " + f.getMethodName() + " in " + f.getClassName()))
//                .toList());
    }

    public void clearRedoStack() {
        redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
            System.out.println("Undo: " + cmd.getClass().getSimpleName() + " (UndoStack: " + undoStack.size() + ", RedoStack: " + redoStack.size() + ")");
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command cmd = redoStack.pop();
            cmd.redo();
            undoStack.push(cmd);
            System.out.println("Redo: " + cmd.getClass().getSimpleName() + " (UndoStack: " + undoStack.size() + ", RedoStack: " + redoStack.size() + ")");
        }
    }
}
