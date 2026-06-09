package de.fmc.editor.controller;

import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.state.CreateConnectionState;
import de.fmc.editor.state.CreateState;
import de.fmc.editor.state.IdleState;
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
            canvasController.setCurrentState(new CreateState());
        } else {
            canvasController.setCurrentState(new IdleState());
        }
    }

    @FXML
    public void onRectClick(ActionEvent event) {
        if (rectButton.isSelected()) {
            canvasController.setCurrentState(new CreateState());
        } else {
            canvasController.setCurrentState(new IdleState());
        }
    }

    @FXML
    public void onConnClick(ActionEvent event) {
        if (connectButton.isSelected()) {
            canvasController.setCurrentState(new CreateConnectionState());
        } else {
            canvasController.setCurrentState(new IdleState());
        }
    }

    @FXML
    public void onSnapToGridAction(ActionEvent event) {
        // Handle snap to grid setting
    }
}
