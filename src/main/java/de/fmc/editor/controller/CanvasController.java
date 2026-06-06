package de.fmc.editor.controller;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.state.EditorState;
import de.fmc.editor.state.SelectOrMoveState;
import de.fmc.editor.view.GraphView;
import de.fmc.editor.view.ViewMapper;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class CanvasController {

    @FXML
    private GraphView drawingPane;

    private CoreRegistry registry;
    private ToolbarController toolbarController;
    private ViewMapper viewMapper;
    private EditorState currentState = new SelectOrMoveState();

    public void setRegistry(CoreRegistry registry) {
        this.registry = registry;
    }

    public void setToolbarController(ToolbarController toolbarController) {
        this.toolbarController = toolbarController;
    }

    public void setViewMapper(ViewMapper viewMapper) {
        this.viewMapper = viewMapper;
    }

    public void setCurrentState(EditorState state) {
        this.currentState = state;
        
        // Wenn wir den ResizeState verlassen, Handles löschen
        if (viewMapper != null && !(state instanceof de.fmc.editor.state.ResizeState)) {
            viewMapper.setSelectedObject(null, null);
        }
    }

    public ViewMapper getViewMapper() {
        return viewMapper;
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
                    double radius = obj.width() / 2;
                    double dx = obj.x() - x;
                    double dy = obj.y() - y;
                    return (dx * dx + dy * dy) <= (radius * radius);
                } else if (obj.type() == de.fmc.editor.core.model.FmcType.QUADRAT) {
                    double halfW = obj.width() / 2;
                    double halfH = obj.height() / 2;
                    return x >= (obj.x() - halfW) && x <= (obj.x() + halfW) &&
                           y >= (obj.y() - halfH) && y <= (obj.y() + halfH);
                }
                return false;
            })
            .findFirst()
            .orElse(null);
    }
}
