package de.fmc.editor.view;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.core.model.Handle;
import de.fmc.editor.core.model.HandleType;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages the visual representation of selection handles.
 */
public class HandleViewMapper {
    private final Group handleGroup;
    private final List<Shape> activeHandles = new ArrayList<>();
    private UUID selectedObjectId = null;

    public HandleViewMapper(Group handleGroup) {
        this.handleGroup = handleGroup;
    }

    public void setSelectedObject(UUID id, List<Handle> handles) {
        this.selectedObjectId = id;
        refreshHandles(handles);
    }

    public void refreshHandles(List<Handle> handles) {
        handleGroup.getChildren().clear();
        activeHandles.clear();

        if (selectedObjectId != null && handles != null) {
            for (var h : handles) {
                var rect = new Rectangle(h.x() - 4, h.y() - 4, 8, 8);
                rect.setFill(Color.BLACK);
                rect.setStroke(Color.WHITE);
                rect.setStrokeWidth(1.0);
                handleGroup.getChildren().add(rect);
                activeHandles.add(rect);
            }
        }
    }

    public void clear() {
        handleGroup.getChildren().clear();
        activeHandles.clear();
        selectedObjectId = null;
    }

    public UUID getSelectedObjectId() {
        return selectedObjectId;
    }

    /**
     * Calculates handle positions for a given FmcObject.
     */
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
}
