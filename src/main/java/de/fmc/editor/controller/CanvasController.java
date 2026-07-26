package de.fmc.editor.controller;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.command.CommandHistory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.SelectionModel;
import de.fmc.editor.state.CreateConnectionState;
import de.fmc.editor.state.CreateObjectState;
import de.fmc.editor.state.CreateTextState;
import de.fmc.editor.state.EditorState;
import de.fmc.editor.state.IdleState;
import de.fmc.editor.state.InteractionEventData;
import de.fmc.editor.state.InteractionMap;
import de.fmc.editor.state.ResizeState;
import de.fmc.editor.view.GraphView;
import de.fmc.editor.view.ViewMapper;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import de.fmc.editor.core.event.EventBus;
import de.fmc.editor.core.event.EditorActionEvent;
import de.fmc.editor.state.EditorReadContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

public class CanvasController implements EditorReadContext {

    @FXML
    private GraphView drawingPane;

    private Tool activeTool;
    private CoreRegistry registry;
    private ToolbarController toolbarController;
    private ViewMapper viewMapper;
    private final EventBus eventBus = new EventBus();
    private EditorState currentState = new IdleState(eventBus);
    private final SelectionModel selectionModel = new SelectionModel();

    public CanvasController() {
        registerEventSubscriptions();
    }

    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

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

        selectionModel.setOnChangeListener(this::updateSelectionInView);
    }

    private void applyStateForTool(Tool tool) {
        switch (tool) {
            case CIRCLE_CREATE -> setCurrentState(new CreateObjectState(FmcType.CIRCLE, eventBus));
            case RECTANGLE_CREATE -> setCurrentState(new CreateObjectState(FmcType.RECTANGLE, eventBus));
            case CONNECTION_CREATE -> setCurrentState(new CreateConnectionState(eventBus));
            case TEXT_CREATE -> setCurrentState(new CreateTextState(eventBus));
            case SELECT -> setCurrentState(new IdleState(eventBus));
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

    /**
     * Setzt den Editor zurück in den Idle-Zustand (Auswahlmodus) und löscht alle Selektionen.
     * Wird z.B. bei ESC oder nach Beendigung von Werkzeugen aufgerufen.
     */
    public void resetToIdleState() {
        setCurrentState(new IdleState(eventBus));
        setActiveTool(Tool.SELECT);

        if (toolbarController != null) {
            toolbarController.clearSelection();
        }

        selectionModel.clearAll();
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

    public void updateSelectionInView() {
        if (viewMapper != null) {
            viewMapper.setSelectedObjects(selectionModel.getSelectedObjectIds());
            viewMapper.setSelectedTexts(selectionModel.getSelectedTextIds());
        }
    }

    @FXML
    public void onMousePressed(MouseEvent event) {
        InteractionEventData data = InteractionEventData.from(event, drawingPane::getMouseInWorld);
        InteractionMap map = currentState.getInteractionMap();
        if (map != null) {
            map.handlePress(data);
        } else {
            currentState.handleInput(data, this);
        }
    }

    @FXML
    public void onMouseDragged(MouseEvent event) {
        InteractionEventData data = InteractionEventData.from(event, drawingPane::getMouseInWorld);
        InteractionMap map = currentState.getInteractionMap();
        if (map != null) {
            map.handleDrag(data);
        } else {
            currentState.handleInput(data, this);
        }
    }

    @FXML
    public void onMouseReleased(MouseEvent event) {
        InteractionEventData data = InteractionEventData.from(event, drawingPane::getMouseInWorld);
        InteractionMap map = currentState.getInteractionMap();
        if (map != null) {
            map.handleRelease(data);
        } else {
            currentState.handleInput(data, this);
        }
    }

    private UUID currentHoveredObjectId = null;
    private UUID currentHoveredConnectionId = null;

    @FXML
    public void onMouseMoved(MouseEvent event) {
        Point2D worldPos = drawingPane.getMouseInWorld(event.getSceneX(), event.getSceneY());
        handleHover(worldPos.getX(), worldPos.getY());
    }

    @FXML
    public void onKeyPressed(KeyEvent event) {
        InteractionEventData data = InteractionEventData.from(event);
        InteractionMap map = currentState.getInteractionMap();
        if (map != null) {
            map.handlePress(data);
        } else {
            currentState.handleInput(data, this);
        }
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
    public void handleScroll(ScrollEvent event) {
        drawingPane.handleZoom(event);
    }

    public GraphView getDrawingPane() {
        return drawingPane;
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

                if (obj.type() == FmcType.CIRCLE || obj.type() == FmcType.WAYPOINT) {

                    // Für Wegpunkte geben wir eine etwas größere Klick-Zone (10px statt 5px)
                    double radius = (obj.type() == FmcType.WAYPOINT) ? 12.0 : (obj.width() / 2);
                    double dx = obj.x() - x;
                    double dy = obj.y() - y;
                    return (dx * dx + dy * dy) <= (radius * radius);
                } else if (obj.type() == FmcType.RECTANGLE) {
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

    public de.fmc.editor.core.model.FmcText findTextAt(double x, double y) {
        return registry.getTexts().stream()
            .filter(t -> {
                var layer = registry.getLayers().get(t.layerId());
                if (layer != null && !layer.visible()) {
                    return false;
                }
                double w = t.width() > 0 ? t.width() : 100;
                double h = 20;
                double left = t.x() - w / 2;
                double right = t.x() + w / 2;
                double top = t.y() - h;
                double bottom = t.y() + 5;
                return x >= left && x <= right && y >= top && y <= bottom;
            })
            .findFirst()
            .orElse(null);
    }

    @Override
    public boolean isSnapToGrid() {
        return toolbarController != null && toolbarController.isSnapToGrid();
    }

    @Override
    public boolean isWaypointsVisible() {
        return toolbarController != null && toolbarController.isWaypointsVisible();
    }

    @Override
    public boolean isSticky() {
        return toolbarController != null && toolbarController.isSticky();
    }

    @Override
    public Set<UUID> getSelectedObjectIds() {
        return selectionModel.getSelectedObjectIds();
    }

    @Override
    public Set<UUID> getSelectedTextIds() {
        return selectionModel.getSelectedTextIds();
    }

    private void registerEventSubscriptions() {
        eventBus.subscribe(EditorActionEvent.SelectObject.class, e -> {
            if (!e.isControlDown()) {
                selectionModel.clearTextSelection();
            }
            if (e.isControlDown()) {
                selectionModel.toggleObjectSelection(e.id());
            } else {
                if (!selectionModel.isObjectSelected(e.id())) {
                    selectionModel.selectObject(e.id());
                }
            }
            FmcObject hit = registry.getObject(e.id());
            if (hit != null && hit.type() != FmcType.WAYPOINT && !isWaypointsVisible()) {
                registry.setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, false);
            }
        });

        eventBus.subscribe(EditorActionEvent.SelectText.class, e -> {
            if (!e.isControlDown()) {
                selectionModel.clearObjectSelection();
            }
            if (e.isControlDown()) {
                selectionModel.toggleTextSelection(e.id());
            } else {
                if (!selectionModel.isTextSelected(e.id())) {
                    selectionModel.selectText(e.id());
                }
            }
        });

        eventBus.subscribe(EditorActionEvent.ClearSelection.class, e -> {
            selectionModel.clearAll();
        });

        eventBus.subscribe(EditorActionEvent.ChangeState.class, e -> {
            setCurrentState(e.newState());
        });

        eventBus.subscribe(EditorActionEvent.ReactivateTool.class, e -> {
            reactivateCurrentTool();
        });

        eventBus.subscribe(EditorActionEvent.SetLayerVisibility.class, e -> {
            if (e.layerId().equals(CoreRegistry.WAYPOINT_LAYER_ID)) {
                if (!e.visible() && isWaypointsVisible()) {
                    return;
                }
            }
            registry.setLayerVisibility(e.layerId(), e.visible());
        });

        eventBus.subscribe(EditorActionEvent.AddWaypoint.class, e -> {
            var waypoint = de.fmc.editor.core.factory.FmcFactory.createObject(
                    FmcType.WAYPOINT,
                    e.x(), e.y(),
                    CoreRegistry.WAYPOINT_LAYER_ID
            );

            var conn = registry.getConnections().get(e.connectionId());
            int index = 0;
            if (conn != null) {
                var source = registry.getObject(conn.sourceId());
                var target = registry.getObject(conn.targetId());
                List<FmcObject> currentWps = new ArrayList<>();
                for (UUID id : conn.waypointIds()) {
                    FmcObject wp = registry.getObject(id);
                    if (wp != null) currentWps.add(wp);
                }
                index = de.fmc.editor.core.util.GeometryUtils.calculateInsertionIndex(e.x(), e.y(), source, target, currentWps);
            }

            var cmd = new de.fmc.editor.core.command.AddWaypointCommand(registry, e.connectionId(), waypoint, index);
            commandHistory.executeCommand(cmd);

            selectionModel.clearObjectSelection();
            selectionModel.addObjectToSelection(waypoint.id());

            registry.setLayerVisibility(CoreRegistry.WAYPOINT_LAYER_ID, true);
        });

        eventBus.subscribe(EditorActionEvent.ResetToIdle.class, e -> {
            resetToIdleState();
        });

        eventBus.subscribe(EditorActionEvent.CreateObject.class, e -> {
            UUID layerId = CoreRegistry.DEFAULT_LAYER_ID;
            var obj = de.fmc.editor.core.factory.FmcFactory.createObject(e.type(), e.x(), e.y(), layerId);
            var cmd = new de.fmc.editor.core.command.CreateObjectCommand(registry, obj);
            commandHistory.executeCommand(cmd);

            if (!isSticky()) {
                reactivateCurrentTool();
            }
        });

        eventBus.subscribe(EditorActionEvent.CreateWaypoint.class, e -> {
            var wegpunkt = new FmcObject(e.id(), FmcType.WAYPOINT, e.x(), e.y(), 10, 10, CoreRegistry.WAYPOINT_LAYER_ID);
            var cmd = new de.fmc.editor.core.command.CreateObjectCommand(registry, wegpunkt);
            commandHistory.executeCommand(cmd);
        });

        eventBus.subscribe(EditorActionEvent.CreateConnection.class, e -> {
            var cmd = new de.fmc.editor.core.command.CreateConnectionCommand(registry, e.sourceId(), e.targetId(), e.waypointIds());
            cmd.execute();

            if (cmd.isSuccess()) {
                commandHistory.addExecutedCommand(cmd);
                selectionModel.clearObjectSelection();
                reactivateCurrentTool();
            }
        });

        eventBus.subscribe(EditorActionEvent.CreateText.class, e -> {
            var cmd = new de.fmc.editor.core.command.CreateTextCommand(registry, e.text());
            commandHistory.executeCommand(cmd);
        });

        eventBus.subscribe(EditorActionEvent.ResizeObject.class, e -> {
            registry.resizeObject(e.id(), e.newW(), e.newH());
        });

        eventBus.subscribe(EditorActionEvent.CommitResize.class, e -> {
            var cmd = new de.fmc.editor.core.command.ResizeObjectCommand(
                registry, e.id(), e.startW(), e.startH(), e.endW(), e.endH()
            );
            commandHistory.executeCommand(cmd);
        });
    }
}
