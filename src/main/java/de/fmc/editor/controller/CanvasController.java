package de.fmc.editor.controller;

import de.fmc.editor.view.GraphView;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class CanvasController {

    @FXML
    private GraphView drawingPane;

    @FXML
    public void onMousePressed(MouseEvent event) {
        // Handle mouse press on canvas
    }

    @FXML
    public void onMouseDragged(MouseEvent event) {
        // Handle mouse drag on canvas
    }

    @FXML
    public void onMouseReleased(MouseEvent event) {
        // Handle mouse release on canvas
    }

    @FXML
    public void handleScroll(ScrollEvent event) {
        // Handle scroll (zoom?)
    }

    public GraphView getDrawingPane() {
        return drawingPane;
    }
}
