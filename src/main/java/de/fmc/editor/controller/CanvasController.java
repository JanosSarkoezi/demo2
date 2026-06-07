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
        javafx.geometry.Point2D worldPos = drawingPane.getMouseInWorld(event.getSceneX(), event.getSceneY());
        // Wir könnten hier worldPos an den State übergeben, aber wir müssten das Interface ändern.
        // Vorerst lassen wir findObjectAt die Transformation intern machen oder wir ändern das Interface.
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
        drawingPane.handleZoom(event);
    }

    public GraphView getDrawingPane() {
        return drawingPane;
    }

    public FmcObject findObjectAt(double x, double y) {
        // Da die Events von der Pane kommen, müssen wir sie erst in Weltkoordinaten umrechnen,
        // falls sie noch nicht umgerechnet sind.
        // In den States wird aktuell event.getX()/getY() verwendet.
        // Wir transformieren hier sicherheitshalber von Scene-Koordinaten, 
        // falls wir findObjectAt von woanders aufrufen.
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

    public javafx.geometry.Point2D getWorldPoint(MouseEvent event) {
        return drawingPane.getMouseInWorld(event.getSceneX(), event.getSceneY());
    }
}
