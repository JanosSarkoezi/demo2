package de.fmc.editor.controller;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.state.EditorState;
import de.fmc.editor.state.MouseEventData;
import de.fmc.editor.view.GraphView;
import de.fmc.editor.view.ViewMapper;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.UUID;

public class CanvasController {

    @FXML
    private GraphView drawingPane;

    private CoreRegistry registry;
    private ToolbarController toolbarController;
    private ViewMapper viewMapper;
    private EditorState currentState = new de.fmc.editor.state.IdleState();

    private final de.fmc.editor.core.command.CommandHistory commandHistory = new de.fmc.editor.core.command.CommandHistory();

    public de.fmc.editor.core.command.CommandHistory getCommandHistory() {
        return commandHistory;
    }

    public EditorState getCurrentState() {
        return currentState;
    }

    public void setRegistry(CoreRegistry registry) {
        this.registry = registry;
    }

    public void setToolbarController(ToolbarController toolbarController) {
        this.toolbarController = toolbarController;
    }

    public void setViewMapper(ViewMapper viewMapper) {
        this.viewMapper = viewMapper;
    }

    public void setCurrentState(EditorState state) {
        if (this.currentState != null) {
            this.currentState.exitState(this);
        }
        this.currentState = state;
        if (this.currentState != null) {
            this.currentState.enterState(this);
        }

        // Wenn wir den ResizeState verlassen, Handles löschen
        if (viewMapper != null && !(state instanceof de.fmc.editor.state.ResizeState)) {
            viewMapper.setSelectedObject(null, null);
        }
    }

    public ViewMapper getViewMapper() {
        return viewMapper;
    }

    public CoreRegistry getRegistry() {
        return registry;
    }

    public ToolbarController getToolbarController() {
        return toolbarController;
    }

    private final java.util.Set<UUID> selectedObjectIds = new java.util.HashSet<>();

    public java.util.Set<UUID> getSelectedObjectIds() {
        return selectedObjectIds;
    }

    public void updateSelectionInView() {
        if (viewMapper != null) {
            viewMapper.setSelectedObjects(selectedObjectIds);
        }
    }

    @FXML
    public void onMousePressed(MouseEvent event) {
        javafx.geometry.Point2D worldPos = drawingPane.getMouseInWorld(event.getSceneX(), event.getSceneY());
        MouseEventData data = new MouseEventData(
            worldPos.getX(), worldPos.getY(),
            event.getSceneX(), event.getSceneY(),
            event.getClickCount(),
            event.isPrimaryButtonDown(),
            event.isControlDown()
        );
        currentState.handleMousePressed(data, this);
    }

    @FXML
    public void onMouseDragged(MouseEvent event) {
        javafx.geometry.Point2D worldPos = drawingPane.getMouseInWorld(event.getSceneX(), event.getSceneY());
        MouseEventData data = new MouseEventData(
            worldPos.getX(), worldPos.getY(),
            event.getSceneX(), event.getSceneY(),
            event.getClickCount(),
            event.isPrimaryButtonDown(),
            event.isControlDown()
        );
        currentState.handleMouseDragged(data, this);
    }

    @FXML
    public void onMouseReleased(MouseEvent event) {
        javafx.geometry.Point2D worldPos = drawingPane.getMouseInWorld(event.getSceneX(), event.getSceneY());
        MouseEventData data = new MouseEventData(
            worldPos.getX(), worldPos.getY(),
            event.getSceneX(), event.getSceneY(),
            event.getClickCount(),
            event.isPrimaryButtonDown(),
            event.isControlDown()
        );
        currentState.handleMouseReleased(data, this);
    }

    @FXML
    public void handleScroll(ScrollEvent event) {
        drawingPane.handleZoom(event);
    }

    public GraphView getDrawingPane() {
        return drawingPane;
    }

    public UUID findConnectionAt(double sceneX, double sceneY) {
        for (javafx.scene.Node node : drawingPane.getConnectionLayer().getChildren()) {
            if (node instanceof javafx.scene.shape.Path path) {
                if (path.isVisible() && path.contains(path.sceneToLocal(sceneX, sceneY))) {
                    return (UUID) path.getProperties().get("UUID");
                }
            }
        }
        return null;
    }

    public java.util.List<UUID> findObjectsInBounds(double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2);
        double maxY = Math.max(y1, y2);

        return registry.getObjects().stream()
            .filter(obj -> {
                // Check layer visibility
                var layer = registry.getLayers().get(obj.layerId());
                if (layer != null && !layer.visible()) {
                    return false;
                }

                // Wir prüfen einfach, ob das Zentrum des Objekts im Rahmen liegt
                return obj.x() >= minX && obj.x() <= maxX &&
                       obj.y() >= minY && obj.y() <= maxY;
            })
            .map(de.fmc.editor.core.model.FmcObject::id)
            .toList();
    }

    public FmcObject findObjectAt(double x, double y) {
        // x und y sind hier bereits Weltkoordinaten
        return registry.getObjects().stream()
            .filter(obj -> {
                // Check layer visibility
                var layer = registry.getLayers().get(obj.layerId());
                if (layer != null && !layer.visible()) {
                    return false;
                }

                if (obj.type() == de.fmc.editor.core.model.FmcType.KREIS ||
                    obj.type() == de.fmc.editor.core.model.FmcType.WEGPUNKT) {

                    // Für Wegpunkte geben wir eine etwas größere Klick-Zone (10px statt 5px)
                    double radius = (obj.type() == de.fmc.editor.core.model.FmcType.WEGPUNKT) ? 12.0 : (obj.width() / 2);
                    double dx = obj.x() - x;
                    double dy = obj.y() - y;
                    return (dx * dx + dy * dy) <= (radius * radius);
                } else if (obj.type() == de.fmc.editor.core.model.FmcType.QUADRAT) {
                    double halfW = obj.width() / 2;
                    double halfH = obj.height() / 2;
                    return x >= (obj.x() - halfW) && x <= (obj.x() + halfW) &&
                           y >= (obj.y() - halfH) && y <= (obj.y() + halfH);
                }
                return false;
            })
            .findFirst()
            .orElse(null);
    }
}
