package de.fmc.editor.controller;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.CommandHistory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.state.CreateConnectionState;
import de.fmc.editor.state.CreateObjectState;
import de.fmc.editor.state.EditorState;
import de.fmc.editor.state.IdleState;
import de.fmc.editor.state.InteractionEventData;
import de.fmc.editor.state.ResizeState;
import de.fmc.editor.view.GraphView;
import de.fmc.editor.view.ViewMapper;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.shape.Path;

import java.util.Optional;
import java.util.UUID;

public class CanvasController {

    @FXML
    private GraphView drawingPane;

    private Tool activeTool;
    private CoreRegistry registry;
    private ToolbarController toolbarController;
    private ViewMapper viewMapper;
    private EditorState currentState = new IdleState();

    private final CommandHistory commandHistory = new CommandHistory();

    public CommandHistory getCommandHistory() {
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

    private void applyStateForTool(Tool tool) {
        switch (tool) {
            case CIRCLE_CREATE -> setCurrentState(new CreateObjectState(FmcType.KREIS));
            case RECTANGLE_CREATE -> setCurrentState(new CreateObjectState(FmcType.QUADRAT));
            case CONNECTION_CREATE -> setCurrentState(new CreateConnectionState());
            case SELECT -> setCurrentState(new IdleState());
        }
    }

    public void setActiveTool(Tool tool) {
        this.activeTool = tool; // Setzt bei normalem Klick das Tool
        applyStateForTool(tool);
    }

    public Tool getActiveTool() {
        return activeTool;
    }

    public void reactivateCurrentTool() {
        applyStateForTool(this.activeTool); // Reaktiviert nach dem Draggen
    }

    public void resetToIdleState() {
        setCurrentState(new IdleState());
        setActiveTool(Tool.SELECT);
        if (toolbarController != null) {
            toolbarController.clearSelection();
        }
        getSelectedObjectIds().clear();
        updateSelectionInView();
    }

    public void setCurrentState(EditorState state) {
        if (this.currentState != null) {
            this.currentState.exitState(this);
        }
        // --- NEU: Logging des State-Wechsels ---
        System.out.println("🔄 State-Wechsel: "
                + (this.currentState != null ? this.currentState.getClass().getSimpleName() : "null")
                + " → "
                + (state != null ? state.getClass().getSimpleName() : "null"));
        // ---------------------------------------
        this.currentState = state;
        if (this.currentState != null) {
            this.currentState.enterState(this);
        }

        // Wenn wir den ResizeState verlassen, Handles löschen
        if (viewMapper != null && !(state instanceof ResizeState)) {
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
        Point2D worldPos = drawingPane.getMouseInWorld(event.getSceneX(), event.getSceneY());
        InteractionEventData data = new InteractionEventData(
            worldPos.getX(), worldPos.getY(),
            event.getSceneX(), event.getSceneY(),
            event.getClickCount(),
            event.isPrimaryButtonDown(),
            event.isSecondaryButtonDown(),
            event.isMiddleButtonDown(),
            event.isControlDown(),
            event.isShiftDown(),
            event.isAltDown(),
            Optional.empty()
        );
        currentState.handleInput(data, this);
    }

    @FXML
    public void onMouseDragged(MouseEvent event) {
        Point2D worldPos = drawingPane.getMouseInWorld(event.getSceneX(), event.getSceneY());
        InteractionEventData data = new InteractionEventData(
            worldPos.getX(), worldPos.getY(),
            event.getSceneX(), event.getSceneY(),
            event.getClickCount(),
            event.isPrimaryButtonDown(),
            event.isSecondaryButtonDown(),
            event.isMiddleButtonDown(),
            event.isControlDown(),
            event.isShiftDown(),
            event.isAltDown(),
            Optional.empty()
        );
        currentState.handleInput(data, this);
    }

    @FXML
    public void onMouseReleased(MouseEvent event) {
        Point2D worldPos = drawingPane.getMouseInWorld(event.getSceneX(), event.getSceneY());
        InteractionEventData data = new InteractionEventData(
            worldPos.getX(), worldPos.getY(),
            event.getSceneX(), event.getSceneY(),
            event.getClickCount(),
            event.isPrimaryButtonDown(),
            event.isSecondaryButtonDown(),
            event.isMiddleButtonDown(),
            event.isControlDown(),
            event.isShiftDown(),
            event.isAltDown(),
            Optional.empty()
        );
        currentState.handleInput(data, this);
    }

    private UUID currentHoveredObjectId = null;
    private UUID currentHoveredConnectionId = null;

    @FXML
    public void onMouseMoved(MouseEvent event) {
        Point2D worldPos = drawingPane.getMouseInWorld(event.getSceneX(), event.getSceneY());
        handleHover(worldPos.getX(), worldPos.getY());
    }

    private void handleHover(double worldX, double worldY) {
        if (viewMapper == null || registry == null) return;

        // 1. Check objects first (exakt/exact collision)
        FmcObject hitObj = findObjectAt(worldX, worldY);
        UUID newHoveredObjId = (hitObj != null) ? hitObj.id() : null;

        UUID newHoveredConnId = null;
        // 2. If no object hovered, check connections nearby (within tolerance)
        if (newHoveredObjId == null) {
            // 8 to 10 pixels tolerance in world coordinates
            newHoveredConnId = findConnectionNear(worldX, worldY, 10.0);
        }

        // 3. Update view if hover state changed
        boolean objectChanged = (currentHoveredObjectId == null && newHoveredObjId != null) ||
                                (currentHoveredObjectId != null && !currentHoveredObjectId.equals(newHoveredObjId));
        boolean connectionChanged = (currentHoveredConnectionId == null && newHoveredConnId != null) ||
                                    (currentHoveredConnectionId != null && !currentHoveredConnectionId.equals(newHoveredConnId));

        if (objectChanged || connectionChanged) {
            currentHoveredObjectId = newHoveredObjId;
            currentHoveredConnectionId = newHoveredConnId;
            viewMapper.setHover(currentHoveredObjectId, currentHoveredConnectionId);
        }
    }

    @FXML
    public void onKeyPressed(KeyEvent event) {
        // Nur Tasten verarbeiten, wenn keine Texteingabe aktiv ist (z.B. kein TextField)
        // Hier nehmen wir alle Tasten an.
        // Point2D worldPos = drawingPane.getMouseInWorld(/* aktuelle Mausposition holen */);
        // Die Mausposition könntest du dir in Feldern merken, oder einfach 0/0 übergeben,
        // da sie für Tastenkürzel meist nicht gebraucht wird.
        InteractionEventData data = new InteractionEventData(
                0, 0,                     // worldX/Y – für ESC nicht relevant
                0, 0,                     // sceneX/Y – nicht relevant
                0,                        // clickCount – nicht relevant
                false, false, false,      // Mausbuttons – nicht gedrückt
                event.isControlDown(),
                event.isShiftDown(),
                event.isAltDown(),
                Optional.of(event.getCode())
        );
        currentState.handleInput(data, this);
    }

    @FXML
    public void handleScroll(ScrollEvent event) {
        drawingPane.handleZoom(event);
    }

    public GraphView getDrawingPane() {
        return drawingPane;
    }

    public UUID findConnectionAt(double sceneX, double sceneY) {
        for (Node node : drawingPane.getConnectionLayer().getChildren()) {
            if (node instanceof Group group) {
                for (Node child : group.getChildren()) {
                    if (child instanceof Path path) {
                        if (group.isVisible() && path.contains(path.sceneToLocal(sceneX, sceneY))) {
                            return (UUID) group.getProperties().get("UUID");
                        }
                    }
                }
            } else if (node instanceof Path path) {
                if (path.isVisible() && path.contains(path.sceneToLocal(sceneX, sceneY))) {
                    return (UUID) path.getProperties().get("UUID");
                }
            }
        }
        return null;
    }

    public UUID findConnectionNear(double worldX, double worldY, double tolerance) {
        for (var entry : registry.getConnections().entrySet()) {
            var conn = entry.getValue();
            var source = registry.getObject(conn.sourceId());
            var target = registry.getObject(conn.targetId());
            if (source == null || target == null) continue;

            // Check if connection group is visible (based on layer visibility of source and target)
            var srcLayer = registry.getLayers().get(source.layerId());
            var tgtLayer = registry.getLayers().get(target.layerId());
            if ((srcLayer != null && !srcLayer.visible()) || (tgtLayer != null && !tgtLayer.visible())) {
                continue;
            }

            java.util.List<FmcObject> points = new java.util.ArrayList<>();
            points.add(source);
            for (UUID wpId : conn.waypointIds()) {
                FmcObject wp = registry.getObject(wpId);
                if (wp != null) points.add(wp);
            }
            points.add(target);

            for (int i = 0; i < points.size() - 1; i++) {
                FmcObject p1 = points.get(i);
                FmcObject p2 = points.get(i + 1);
                double dist = de.fmc.editor.core.util.GeometryUtils.distanceToSegment(
                    worldX, worldY,
                    p1.x(), p1.y(),
                    p2.x(), p2.y()
                );
                if (dist < tolerance) {
                    return entry.getKey();
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
            .map(FmcObject::id)
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

                if (obj.type() == FmcType.KREIS || obj.type() == FmcType.WEGPUNKT) {

                    // Für Wegpunkte geben wir eine etwas größere Klick-Zone (10px statt 5px)
                    double radius = (obj.type() == FmcType.WEGPUNKT) ? 12.0 : (obj.width() / 2);
                    double dx = obj.x() - x;
                    double dy = obj.y() - y;
                    return (dx * dx + dy * dy) <= (radius * radius);
                } else if (obj.type() == FmcType.QUADRAT) {
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
