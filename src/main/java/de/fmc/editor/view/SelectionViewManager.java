package de.fmc.editor.view;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.Handle;
import de.fmc.editor.core.model.HandleType;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.effect.Effect;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SelectionViewManager {

    private final GraphView graphView;
    private final ShapeViewMapper shapeMapper;
    private final Group handleGroup = new Group();
    private final Set<UUID> selectedObjectIds = new HashSet<>();
    private UUID singleSelectedObjectId = null;

    private UUID currentHoveredObjectId = null;
    private UUID currentHoveredConnectionId = null;
    private final ConnectionViewMapper connectionMapper;

    public SelectionViewManager(GraphView graphView, ShapeViewMapper shapeMapper) {
        this.graphView = graphView;
        this.shapeMapper = shapeMapper;
        this.connectionMapper = null; // compatibility constructor or will add the second constructor
        this.graphView.getUiLayer().getChildren().add(handleGroup);
    }

    public SelectionViewManager(GraphView graphView, ShapeViewMapper shapeMapper, ConnectionViewMapper connectionMapper) {
        this.graphView = graphView;
        this.shapeMapper = shapeMapper;
        this.connectionMapper = connectionMapper;
        this.graphView.getUiLayer().getChildren().add(handleGroup);
    }

    public void setHover(UUID hoveredObjectId, UUID hoveredConnectionId) {
        // 1. Reset old hovered object style if it changed
        if (this.currentHoveredObjectId != null && !this.currentHoveredObjectId.equals(hoveredObjectId)) {
            Shape oldShape = shapeMapper.getShape(this.currentHoveredObjectId);
            if (oldShape != null) {
                // If it is selected, keep the selection effect, otherwise remove effect
                if (selectedObjectIds.contains(this.currentHoveredObjectId)) {
                    oldShape.setEffect(createGlowEffect());
                    oldShape.setStroke(Color.BLACK);
                    oldShape.setStrokeWidth(1.5);
                } else {
                    oldShape.setEffect(null);
                    oldShape.setStroke(Color.BLACK);
                    oldShape.setStrokeWidth(1.5);
                }
            }
        }

        // 2. Reset old hovered connection style if it changed
        if (this.currentHoveredConnectionId != null && !this.currentHoveredConnectionId.equals(hoveredConnectionId)) {
            if (connectionMapper != null) {
                Group group = connectionMapper.getVisualConnections().get(this.currentHoveredConnectionId);
                if (group != null && !group.getChildren().isEmpty() && group.getChildren().get(0) instanceof javafx.scene.shape.Path path) {
                    path.setStroke(Color.BLACK);
                    path.setStrokeWidth(1.0);
                }
            }
        }

        this.currentHoveredObjectId = hoveredObjectId;
        this.currentHoveredConnectionId = hoveredConnectionId;

        // 3. Apply new hovered object style (gray stroke and gray glow) only if NOT selected
        if (this.currentHoveredObjectId != null) {
            if (!selectedObjectIds.contains(this.currentHoveredObjectId)) {
                Shape newShape = shapeMapper.getShape(this.currentHoveredObjectId);
                if (newShape != null) {
                    newShape.setStroke(Color.web("#808080"));
                    newShape.setStrokeWidth(2.5);
                    // Subtle light gray glow for hover
                    DropShadow hoverGlow = new DropShadow();
                    hoverGlow.setColor(Color.web("#808080"));
                    hoverGlow.setRadius(10.0);
                    hoverGlow.setSpread(0.2);
                    newShape.setEffect(hoverGlow);
                }
            }
        }

        // 4. Apply new hovered connection style (gray border and width) only if NOT selected
        // (Note: Selection for connections is currently not stored in selectedObjectIds, 
        // but we apply gray to hovered connections as requested)
        if (this.currentHoveredConnectionId != null) {
            if (connectionMapper != null) {
                Group group = connectionMapper.getVisualConnections().get(this.currentHoveredConnectionId);
                if (group != null && !group.getChildren().isEmpty() && group.getChildren().get(0) instanceof javafx.scene.shape.Path path) {
                    path.setStroke(Color.web("#808080"));
                    path.setStrokeWidth(2.5);
                }
            }
        }
    }

    public void setSelectedObjects(Collection<UUID> objectIds) {
        // 1. Alten Effekt entfernen
        for (UUID id : this.selectedObjectIds) {
            Shape node = shapeMapper.getShape(id);
            if (node != null) {
                node.setEffect(null);
                node.setStroke(Color.BLACK);
                node.setStrokeWidth(1.5);
            }
        }

        // 2. Neue IDs übernehmen
        this.selectedObjectIds.clear();
        if (objectIds != null) {
            this.selectedObjectIds.addAll(objectIds);
        }

        // 3. Neuen Effekt anwenden (Selected objects do NOT get hover effect applied here)
        for (UUID id : this.selectedObjectIds) {
            Shape node = shapeMapper.getShape(id);
            if (node != null) {
                node.setStroke(Color.BLACK);
                node.setStrokeWidth(1.5);
                node.setEffect(createGlowEffect());
            }
        }
    }

    public void setSingleSelectedObject(UUID id, List<Handle> handles) {
        this.singleSelectedObjectId = id;
        refreshHandles(handles);
    }

    public void refreshHandles(List<Handle> handles) {
        handleGroup.getChildren().clear();
        if (singleSelectedObjectId != null && handles != null) {
            for (var h : handles) {
                var rect = new Rectangle(h.x() - 4, h.y() - 4, 8, 8);
                rect.setFill(Color.BLACK);
                rect.setStroke(Color.WHITE);
                rect.setStrokeWidth(1.0);
                handleGroup.getChildren().add(rect);
            }
        }
    }

    private Effect createGlowEffect() {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#0078D7"));
        glow.setRadius(15.0);
        glow.setSpread(0.5);
        return glow;
    }

    public static List<Handle> getHandles(FmcObject obj) {
        List<Handle> handles = new ArrayList<>();
        double hw = obj.width() / 2;
        double hh = obj.height() / 2;

        if (obj.type() == FmcType.QUADRAT) {
            handles.add(new Handle(HandleType.NW, obj.x() - hw, obj.y() - hh));
            handles.add(new Handle(HandleType.N,  obj.x(),      obj.y() - hh));
            handles.add(new Handle(HandleType.NE, obj.x() + hw, obj.y() - hh));
            handles.add(new Handle(HandleType.E,  obj.x() + hw, obj.y()));
            handles.add(new Handle(HandleType.SE, obj.x() + hw, obj.y() + hh));
            handles.add(new Handle(HandleType.S,  obj.x(),      obj.y() + hh));
            handles.add(new Handle(HandleType.SW, obj.x() - hw, obj.y() + hh));
            handles.add(new Handle(HandleType.W,  obj.x() - hw, obj.y()));
        } else if (obj.type() == FmcType.KREIS) {
            handles.add(new Handle(HandleType.N, obj.x(),      obj.y() - hh));
            handles.add(new Handle(HandleType.E, obj.x() + hw, obj.y()));
            handles.add(new Handle(HandleType.S, obj.x(),      obj.y() + hh));
            handles.add(new Handle(HandleType.W, obj.x() - hw, obj.y()));
        }
        return handles;
    }

    public void clear() {
        handleGroup.getChildren().clear();
        selectedObjectIds.clear();
        singleSelectedObjectId = null;
        currentHoveredObjectId = null;
        currentHoveredConnectionId = null;
    }

    public Set<UUID> getSelectedObjectIds() {
        return selectedObjectIds;
    }

    public UUID getSingleSelectedObjectId() {
        return singleSelectedObjectId;
    }
}
