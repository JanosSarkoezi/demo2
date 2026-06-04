package de.fmc.editor.controller;

import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.state.CreateState;
import de.fmc.editor.state.SelectOrMoveState;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class ToolbarController {

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

    @FXML
    public void onCircleClick(ActionEvent event) {
        if (circleButton.isSelected()) {
            canvasController.setCurrentState(new CreateState());
        } else {
            canvasController.setCurrentState(new SelectOrMoveState());
        }
    }

    @FXML
    public void onRectClick(ActionEvent event) {
        if (rectButton.isSelected()) {
            canvasController.setCurrentState(new CreateState());
        } else {
            canvasController.setCurrentState(new SelectOrMoveState());
        }
    }

    @FXML
    public void onConnClick(ActionEvent event) {
        // Handle connection tool selection
    }

    @FXML
    public void onSnapToGridAction(ActionEvent event) {
        // Handle snap to grid setting
    }
}
