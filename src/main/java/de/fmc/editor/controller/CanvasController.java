package de.fmc.editor.controller;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.state.EditorState;
import de.fmc.editor.state.SelectOrMoveState;
import de.fmc.editor.view.GraphView;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class CanvasController {

    @FXML
    private GraphView drawingPane;

    private CoreRegistry registry;
    private ToolbarController toolbarController;
    private EditorState currentState = new SelectOrMoveState();

    public void setRegistry(CoreRegistry registry) {
        this.registry = registry;
    }

    public void setToolbarController(ToolbarController toolbarController) {
        this.toolbarController = toolbarController;
    }

    public void setCurrentState(EditorState state) {
        this.currentState = state;
    }

    public CoreRegistry getRegistry() {
        return registry;
    }

    public ToolbarController getToolbarController() {
        return toolbarController;
    }

    @FXML
    public void onMousePressed(MouseEvent event) {
        currentState.handleMousePressed(event, this);
    }

    @FXML
    public void onMouseDragged(MouseEvent event) {
        currentState.handleMouseDragged(event, this);
    }

    @FXML
    public void onMouseReleased(MouseEvent event) {
        currentState.handleMouseReleased(event, this);
    }

    @FXML
    public void handleScroll(ScrollEvent event) {
        // Handle scroll (zoom?)
    }

    public GraphView getDrawingPane() {
        return drawingPane;
    }

    public FmcObject findObjectAt(double x, double y) {
        return registry.getObjects().stream()
            .filter(obj -> {
                if (obj.type() == de.fmc.editor.core.model.FmcType.KREIS) {
                    double dx = obj.x() - x;
                    double dy = obj.y() - y;
                    return (dx * dx + dy * dy) <= (20 * 20); // Radius 20
                } else if (obj.type() == de.fmc.editor.core.model.FmcType.QUADRAT) {
                    return x >= (obj.x() - 15) && x <= (obj.x() + 15) &&
                           y >= (obj.y() - 15) && y <= (obj.y() + 15);
                }
                return false;
            })
            .findFirst()
            .orElse(null);
    }
}
