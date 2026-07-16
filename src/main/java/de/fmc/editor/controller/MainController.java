package de.fmc.editor.controller;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.DeleteSelectedCommand;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.view.ViewMapper;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
//        System.out.println("MainController initialized");
    }

    public void init(CoreRegistry registry) {
        canvasController.setRegistry(registry);
        canvasController.setToolbarController(toolbarController);
        toolbarController.setCanvasController(canvasController);
        
        // Da der Kreis standardmäßig in der Toolbar selektiert ist (FXML),
        // setzen wir auch den passenden Startzustand.
        canvasController.setActiveTool(Tool.CIRCLE_CREATE);
        
        // Den ViewMapper hier initialisieren, damit er sicher die Pane vom CanvasController nutzt
        this.viewMapper = new ViewMapper(canvasController.getDrawingPane(), registry);
        registry.addListener(viewMapper);
        
        canvasController.setViewMapper(viewMapper);

        // --- Demo-Daten für Etappe 4 ---
        var k1 = FmcFactory.createObject(FmcType.KREIS, 200, 200, CoreRegistry.DEFAULT_LAYER_ID);
        var q1 = FmcFactory.createObject(FmcType.QUADRAT, 400, 200, CoreRegistry.DEFAULT_LAYER_ID);
        registry.addObject(k1);
        registry.addObject(q1);
        registry.addConnection(k1.id(), q1.id(), java.util.Collections.emptyList());
    }

    public void setupShortcuts(Scene scene) {
        var accelerators = scene.getAccelerators();

        // 1. Globale Aktionen
        Shortcut.UNDO.register(accelerators, () -> canvasController.getCommandHistory().undo());
        Shortcut.REDO.register(accelerators, () -> canvasController.getCommandHistory().redo());
        Shortcut.DELETE.register(accelerators, this::deleteSelected);
        Shortcut.DELETE_ALT.register(accelerators, this::deleteSelected);
        Shortcut.SELECT_ALL.register(accelerators, this::selectAll);
        Shortcut.SAVE.register(accelerators, this::saveDiagram);
        Shortcut.LOAD.register(accelerators, this::loadDiagram);

        // 2. Werkzeug-Wechsel
        Shortcut.TOOL_CIRCLE.register(accelerators, () -> switchTool(Tool.CIRCLE_CREATE));
        Shortcut.TOOL_RECTANGLE.register(accelerators, () -> switchTool(Tool.RECTANGLE_CREATE));
        Shortcut.TOOL_CONNECTION.register(accelerators, () -> switchTool(Tool.CONNECTION_CREATE));

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                canvasController.onKeyPressed(event);
                event.consume(); // Verhindert, dass andere Komponenten ESC ebenfalls verarbeiten
            }
        });
    }

    private void deleteSelected() {
        var selectedIds = canvasController.getSelectedObjectIds();
        if (!selectedIds.isEmpty()) {
            var cmd = new DeleteSelectedCommand(
                canvasController.getRegistry(), 
                new java.util.ArrayList<>(selectedIds)
            );
            canvasController.getCommandHistory().executeCommand(cmd);
            
            // Auswahl leeren
            selectedIds.clear();
            canvasController.updateSelectionInView();
        }
    }

    private void selectAll() {
        var allIds = canvasController.getRegistry().getObjects().stream()
            .map(FmcObject::id)
            .toList();
        canvasController.getSelectedObjectIds().clear();
        canvasController.getSelectedObjectIds().addAll(allIds);
        canvasController.updateSelectionInView();
    }

    private void saveDiagram() {
        toolbarController.onSaveClick(null);
    }

    private void loadDiagram() {
        toolbarController.onLoadClick(null);
    }

    private void switchTool(Tool tool) {
        if (canvasController.getActiveTool() == tool) {
            tool = Tool.SELECT;
        }
        toolbarController.clearSelection();
        canvasController.setActiveTool(tool);
        toolbarController.selectTool(tool);
    }
}
