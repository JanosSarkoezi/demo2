package de.fmc.editor.controller;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.UpdateTextCommand;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.FmcText;
import de.fmc.editor.core.persistence.PersistenceService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.FileWriter;
import java.util.UUID;

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
    @FXML
    private ToggleButton textButton;
    @FXML
    private ComboBox<String> fontFamilyCombo;
    @FXML
    private Spinner<Integer> fontSizeSpinner;
    @FXML
    private CheckBox boldCheckbox;
    @FXML
    private CheckBox italicCheckbox;
    @FXML
    private ColorPicker textColorPicker;

    private CanvasController canvasController;

    @FXML
    public void initialize() {
        if (fontFamilyCombo != null) {
            fontFamilyCombo.getItems().addAll("System", "Arial", "Courier New", "Georgia", "Times New Roman", "Verdana");
            fontFamilyCombo.setValue("System");
            fontFamilyCombo.setOnAction(this::onTextAttributesChanged);
        }
        if (fontSizeSpinner != null) {
            fontSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 72, 14));
            fontSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyTextAttributesToSelection());
        }
        if (boldCheckbox != null) {
            boldCheckbox.setOnAction(this::onTextAttributesChanged);
        }
        if (italicCheckbox != null) {
            italicCheckbox.setOnAction(this::onTextAttributesChanged);
        }
        if (textColorPicker != null) {
            textColorPicker.setOnAction(this::onTextAttributesChanged);
        }
    }

    public void setCanvasController(CanvasController canvasController) {
        this.canvasController = canvasController;
    }

    public FmcType getSelectedType() {
        if (circleButton.isSelected()) return FmcType.CIRCLE;
        if (rectButton.isSelected()) return FmcType.RECTANGLE;
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
                CoreRegistry.WAYPOINT_LAYER_ID,
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
    public void onTextClick(ActionEvent event) {
        if (textButton.isSelected()) {
            canvasController.setActiveTool(Tool.TEXT_CREATE);
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Diagramm speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Dateien", "*.json"));
        java.io.File file = fileChooser.showSaveDialog(toolbarContainer.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                var data = canvasController.getRegistry().exportData();
                PersistenceService.saveDiagram(data, writer);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onLoadClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Diagramm laden");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Dateien", "*.json"));
        java.io.File file = fileChooser.showOpenDialog(toolbarContainer.getScene().getWindow());

        if (file != null) {
            try (java.io.FileReader reader = new java.io.FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
                var data = PersistenceService.loadDiagram(reader);
                canvasController.getRegistry().loadData(data);
                // History leeren nach dem Laden
                canvasController.getCommandHistory().clear();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void selectTool(Tool tool) {
        toolGroup.selectToggle(null);
        switch (tool) {
            case SELECT -> {
                // Kein ToggleButton für SELECT, alle abgewählt
            }
            case CIRCLE_CREATE -> circleButton.setSelected(true);
            case RECTANGLE_CREATE -> rectButton.setSelected(true);
            case CONNECTION_CREATE -> connectButton.setSelected(true);
            case TEXT_CREATE -> textButton.setSelected(true);
        }
    }

    @FXML
    public void onTextAttributesChanged(ActionEvent event) {
        applyTextAttributesToSelection();
    }

    public void applyTextAttributesToSelection() {
        if (canvasController == null) return;
        var selectedTextIds = canvasController.getSelectionModel().getSelectedTextIds();
        if (selectedTextIds.isEmpty()) return;

        String family = fontFamilyCombo.getValue() != null ? fontFamilyCombo.getValue() : "System";
        double size = fontSizeSpinner.getValue() != null ? fontSizeSpinner.getValue().doubleValue() : 14.0;
        String weight = boldCheckbox.isSelected() ? "bold" : "normal";
        String style = italicCheckbox.isSelected() ? "italic" : "normal";
        String color = "#" + textColorPicker.getValue().toString().substring(2, 8);

        for (UUID id : selectedTextIds) {
            FmcText old = canvasController.getRegistry().getText(id);
            if (old != null) {
                var updated = new FmcText(
                        old.id(), old.text(), old.x(), old.y(), old.width(),
                        family, size, weight, style, color,
                        old.parentObjectId(), old.layerId()
                );
                // Hier der entscheidende Unterschied: jetzt mit alt und neu
                var cmd = new UpdateTextCommand(canvasController.getRegistry(), old, updated);
                canvasController.getCommandHistory().executeCommand(cmd);
            }
        }
    }
}
