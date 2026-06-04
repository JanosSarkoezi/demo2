package de.fmc.editor.controller;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.view.ViewMapper;
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
        System.out.println("MainController initialized");
    }

    public void init(CoreRegistry registry) {
        canvasController.setRegistry(registry);
        canvasController.setToolbarController(toolbarController);
        toolbarController.setCanvasController(canvasController);
        
        // Da der Kreis standardmäßig in der Toolbar selektiert ist (FXML),
        // setzen wir auch den passenden Startzustand.
        canvasController.setCurrentState(new de.fmc.editor.state.CreateState());
        
        // Den ViewMapper hier initialisieren, damit er sicher die Pane vom CanvasController nutzt
        ViewMapper viewMapper = new ViewMapper(canvasController.getDrawingPane(), registry);
        registry.addListener(viewMapper);
    }
}
