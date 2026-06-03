package de.fmc.editor.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class MainController {
    @FXML
    private VBox toolbar;
    
    @FXML
    private ToolbarController toolbarController;
    
    @FXML
    private CanvasController canvasController;

    @FXML
    public void initialize() {
        // Initialization logic if needed
    }
}
