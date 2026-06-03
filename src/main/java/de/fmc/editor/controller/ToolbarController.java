package de.fmc.editor.controller;

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

    @FXML
    public void onCircleClick(ActionEvent event) {
        // Handle circle tool selection
    }

    @FXML
    public void onRectClick(ActionEvent event) {
        // Handle rectangle tool selection
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
