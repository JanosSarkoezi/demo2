package de.fmc.editor.controller;

import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.state.CreateConnectionState;
import de.fmc.editor.state.IdleState;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public class ToolbarController {

    @FXML
    private VBox toolbarContainer;
    @FXML
    private ToggleGroup toolGroup;
    @FXML
    private ToggleButton circleButton;
    @FXML
    private ToggleButton rectButton;
    @FXML
    private ToggleButton connectButton;
    @FXML
    private CheckBox snapToGridCheckbox;
    @FXML
    private CheckBox waypointsCheckbox;
    @FXML
    private CheckBox stickyCheckbox;

    private CanvasController canvasController;

    public void setCanvasController(CanvasController canvasController) {
        this.canvasController = canvasController;
    }

    public FmcType getSelectedType() {
        if (circleButton.isSelected()) return FmcType.KREIS;
        if (rectButton.isSelected()) return FmcType.QUADRAT;
        return null;
    }

    public void clearSelection() {
        toolGroup.selectToggle(null);
    }

    public boolean isSticky() {
        return stickyCheckbox.isSelected();
    }

    public boolean isSnapToGrid() {
        return snapToGridCheckbox.isSelected();
    }

    public boolean isWaypointsVisible() {
        return waypointsCheckbox != null && waypointsCheckbox.isSelected();
    }

    @FXML
    public void onWaypointsAction(ActionEvent event) {
        if (canvasController != null) {
            canvasController.getRegistry().setLayerVisibility(
                de.fmc.editor.core.CoreRegistry.WAYPOINT_LAYER_ID, 
                waypointsCheckbox.isSelected()
            );
        }
    }

    @FXML
    public void onCircleClick(ActionEvent event) {
        if (circleButton.isSelected()) {
            canvasController.setActiveTool(Tool.CIRCLE_CREATE);
        } else {
            canvasController.setActiveTool(Tool.SELECT);
        }
    }

    @FXML
    public void onRectClick(ActionEvent event) {
        if (rectButton.isSelected()) {
            canvasController.setActiveTool(Tool.RECTANGLE_CREATE);
        } else {
            canvasController.setActiveTool(Tool.SELECT);
        }
    }

    @FXML
    public void onConnClick(ActionEvent event) {
        if (connectButton.isSelected()) {
            canvasController.setActiveTool(Tool.CONNECTION_CREATE);
        } else {
            canvasController.setActiveTool(Tool.SELECT);
        }
    }

    @FXML
    public void onSnapToGridAction(ActionEvent event) {
        // Handle snap to grid setting
    }

    @FXML
    public void onSaveClick(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Diagramm speichern");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON Dateien", "*.json"));
        java.io.File file = fileChooser.showSaveDialog(toolbarContainer.getScene().getWindow());

        if (file != null) {
            try (java.io.FileWriter writer = new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                var data = canvasController.getRegistry().exportData();
                de.fmc.editor.core.persistence.PersistenceService.saveDiagram(data, writer);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onLoadClick(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Diagramm laden");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON Dateien", "*.json"));
        java.io.File file = fileChooser.showOpenDialog(toolbarContainer.getScene().getWindow());

        if (file != null) {
            try (java.io.FileReader reader = new java.io.FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
                var data = de.fmc.editor.core.persistence.PersistenceService.loadDiagram(reader);
                canvasController.getRegistry().loadData(data);
                // History leeren nach dem Laden
                canvasController.getCommandHistory().clear();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }
}
