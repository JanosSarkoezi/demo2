package de.fmc.editor.view;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.model.FmcObject;
import javafx.scene.Group;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Handles the visual mapping of FmcObjects to FmcObjectNodes.
 */
public class ObjectViewMapper {
    private final Group shapeLayer;
    private final CoreRegistry registry;
    private final Map<UUID, FmcObjectNode> visualNodes = new HashMap<>();
    private BiConsumer<UUID, String> onTextUpdateRequested;
    private final java.util.Set<UUID> selectedObjectIds = new java.util.HashSet<>();

    public ObjectViewMapper(Group shapeLayer, CoreRegistry registry) {
        this.shapeLayer = shapeLayer;
        this.registry = registry;
    }

    public void setOnTextUpdateRequested(BiConsumer<UUID, String> callback) {
        this.onTextUpdateRequested = callback;
        visualNodes.values().forEach(node -> node.setOnTextUpdateRequested(callback));
    }

    public void handleAdd(FmcObject obj) {
        removeNode(obj.id());

        FmcObjectNode node = new FmcObjectNode(obj);
        node.setOnTextUpdateRequested(onTextUpdateRequested);
        node.setSelectionEffect(selectedObjectIds.contains(obj.id()));

        visualNodes.put(obj.id(), node);
        shapeLayer.getChildren().add(node);
    }

    public void handleRemove(UUID id) {
        removeNode(id);
    }

    public void handleMove(UUID id, double x, double y) {
        FmcObjectNode node = visualNodes.get(id);
        if (node != null) {
            FmcObject obj = registry.getObject(id);
            if (obj != null) {
                node.updatePosition(x, y, obj.width(), obj.height());
            }
        }
    }

    public void handleResize(UUID id, double w, double h) {
        FmcObjectNode node = visualNodes.get(id);
        if (node != null) {
            FmcObject obj = registry.getObject(id);
            if (obj != null) {
                node.updateSize(w, h, obj.x(), obj.y());
            }
        }
    }

    public void handleTextUpdate(UUID id, String text) {
        FmcObjectNode node = visualNodes.get(id);
        if (node != null) {
            node.updateText(text);
        }
    }

    public void setVisible(UUID id, boolean visible) {
        FmcObjectNode node = visualNodes.get(id);
        if (node != null) {
            node.setVisible(visible);
        }
    }

    public void clear() {
        visualNodes.values().forEach(shapeLayer.getChildren()::remove);
        visualNodes.clear();
    }

    public void setSelectedObjects(java.util.Collection<UUID> objectIds) {
        // Clear old selection effects
        selectedObjectIds.forEach(id -> {
            FmcObjectNode node = visualNodes.get(id);
            if (node != null) node.setSelectionEffect(false);
        });

        selectedObjectIds.clear();
        if (objectIds != null) {
            selectedObjectIds.addAll(objectIds);
        }

        // Apply new selection effects
        selectedObjectIds.forEach(id -> {
            FmcObjectNode node = visualNodes.get(id);
            if (node != null) node.setSelectionEffect(true);
        });
    }

    private void removeNode(UUID id) {
        FmcObjectNode node = visualNodes.remove(id);
        if (node != null) {
            shapeLayer.getChildren().remove(node);
        }
    }

    public FmcObjectNode getNode(UUID id) {
        return visualNodes.get(id);
    }
}
