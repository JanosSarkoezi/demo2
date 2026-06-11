package de.fmc.editor.controller;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.view.ViewMapper;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;

public class MainController {
    @FXML
    private VBox toolbar;
    
    @FXML
    private ToolbarController toolbarController;
    
    @FXML
    private CanvasController canvasController;

    private ViewMapper viewMapper;

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
        this.viewMapper = new ViewMapper(canvasController.getDrawingPane(), registry);
        registry.addListener(viewMapper);
        
        canvasController.setViewMapper(viewMapper);

        // --- Demo-Daten für Etappe 4 ---
        var k1 = de.fmc.editor.core.factory.FmcFactory.createObject(de.fmc.editor.core.model.FmcType.KREIS, 200, 200, CoreRegistry.DEFAULT_LAYER_ID);
        var q1 = de.fmc.editor.core.factory.FmcFactory.createObject(de.fmc.editor.core.model.FmcType.QUADRAT, 400, 200, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(k1);
        registry.addObject(q1);
        registry.addConnection(k1.id(), q1.id(), java.util.Collections.emptyList());
    }

    public void setupShortcuts(Scene scene) {
        var accelerators = scene.getAccelerators();

        // 1. STRG + Z -> Undo
        accelerators.put(
            new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
            () -> canvasController.getCommandHistory().undo()
        );

        // 2. STRG + SHIFT + Z -> Redo
        accelerators.put(
            new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
            () -> canvasController.getCommandHistory().redo()
        );

        // 3. ENTF -> Löschen
        accelerators.put(
            new KeyCodeCombination(KeyCode.DELETE),
            this::deleteSelected
        );

        // 4. BACKSPACE -> Alternativ auch Löschen
        accelerators.put(
            new KeyCodeCombination(KeyCode.BACK_SPACE),
            this::deleteSelected
        );
    }

    private void deleteSelected() {
        var selectedIds = canvasController.getSelectedObjectIds();
        if (!selectedIds.isEmpty()) {
            var cmd = new de.fmc.editor.core.command.DeleteSelectedCommand(
                canvasController.getRegistry(), 
                new java.util.ArrayList<>(selectedIds)
            );
            canvasController.getCommandHistory().executeCommand(cmd);
            
            // Auswahl leeren
            selectedIds.clear();
            canvasController.updateSelectionInView();
        }
    }
}
