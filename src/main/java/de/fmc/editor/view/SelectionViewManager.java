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

    public SelectionViewManager(GraphView graphView, ShapeViewMapper shapeMapper) {
        this.graphView = graphView;
        this.shapeMapper = shapeMapper;
        this.graphView.getUiLayer().getChildren().add(handleGroup);
    }

    public void setSelectedObjects(Collection<UUID> objectIds) {
        // 1. Alten Effekt entfernen
        for (UUID id : this.selectedObjectIds) {
            Shape node = shapeMapper.getShape(id);
            if (node != null) {
                node.setEffect(null);
            }
        }

        // 2. Neue IDs übernehmen
        this.selectedObjectIds.clear();
        if (objectIds != null) {
            this.selectedObjectIds.addAll(objectIds);
        }

        // 3. Neuen Effekt anwenden
        for (UUID id : this.selectedObjectIds) {
            Shape node = shapeMapper.getShape(id);
            if (node != null) {
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
    }

    public Set<UUID> getSelectedObjectIds() {
        return selectedObjectIds;
    }

    public UUID getSingleSelectedObjectId() {
        return singleSelectedObjectId;
    }
}
