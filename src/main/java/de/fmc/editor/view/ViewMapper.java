package de.fmc.editor.view;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.event.RegistryEvent;
import de.fmc.editor.core.event.RegistryListener;
import de.fmc.editor.core.model.Handle;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Central dispatcher for mapping registry events to visual representations.
 * Delegates work to specialized sub-mappers.
 */
public class ViewMapper implements RegistryListener {

    private final CoreRegistry registry;
    private final ObjectViewMapper objectMapper;
    private final ConnectionViewMapper connectionMapper;
    private final HandleViewMapper handleMapper;

    public ViewMapper(GraphView graphView, CoreRegistry registry) {
        this.registry = registry;
        this.objectMapper = new ObjectViewMapper(graphView.getShapeLayer(), registry);
        this.connectionMapper = new ConnectionViewMapper(graphView.getConnectionLayer(), registry);
        this.handleMapper = new HandleViewMapper(graphView.getUiLayer());
    }

    public void setSelectedObjects(java.util.Collection<UUID> objectIds) {
        objectMapper.setSelectedObjects(objectIds);
    }

    public void setRoutingStrategy(RoutingStrategy strategy) {
        connectionMapper.setRoutingStrategy(strategy);
    }

    @Override
    public void handleEvent(RegistryEvent event) {
        // Dispatch to specialized sub-mappers
        switch (event) {
            case RegistryEvent.ObjectAdded(var obj) -> {
                objectMapper.handleAdd(obj);
                connectionMapper.updateConnectionsForObject(obj.id());
            }
            case RegistryEvent.ObjectRemoved(var id) -> {
                objectMapper.handleRemove(id);
                connectionMapper.updateConnectionsForObject(id);
            }
            case RegistryEvent.ObjectMoved(var id, var x, var y) -> {
                objectMapper.handleMove(id, x, y);
                connectionMapper.updateConnectionsForObject(id);
            }
            case RegistryEvent.ObjectResized(var id, var w, var h) -> {
                objectMapper.handleResize(id, w, h);
                connectionMapper.updateConnectionsForObject(id);
            }
            case RegistryEvent.ObjectTextUpdated(var id, var text) -> {
                objectMapper.handleTextUpdate(id, text);
            }
            case RegistryEvent.ConnectionAdded(var id, var conn) -> {
                connectionMapper.handleAdd(id, conn);
            }
            case RegistryEvent.ConnectionRemoved(var id) -> {
                connectionMapper.handleRemove(id);
            }
            case RegistryEvent.ConnectionUpdated(var id, var conn) -> {
                connectionMapper.handleUpdate(id, conn);
            }
            case RegistryEvent.LayerAdded(var layer) -> {}
            case RegistryEvent.LayerRemoved(var id) -> {}
            case RegistryEvent.LayerVisibilityChanged(var id, var visible) -> {
                handleLayerVisibilityChanged(id, visible);
            }
            case RegistryEvent.RegistryReset() -> handleRegistryReset();
        }

        // Refresh handles if the selected object was affected by the event
        refreshHandlesIfNeeded(event);
    }

    private void refreshHandlesIfNeeded(RegistryEvent event) {
        UUID affectedId = switch (event) {
            case RegistryEvent.ObjectMoved(var id, var x, var y) -> id;
            case RegistryEvent.ObjectResized(var id, var w, var h) -> id;
            default -> null;
        };

        if (affectedId != null && affectedId.equals(handleMapper.getSelectedObjectId())) {
            var obj = registry.getObject(affectedId);
            if (obj != null) {
                handleMapper.refreshHandles(HandleViewMapper.getHandles(obj));
            }
        }
    }

    /**
     * Shows handles for a specific object (usually the one being currently resized or individually selected).
     */
    public void setSelectedObject(UUID id, List<Handle> handles) {
        handleMapper.setSelectedObject(id, handles);
    }

    private void handleRegistryReset() {
        objectMapper.clear();
        connectionMapper.clear();
        handleMapper.clear();

        registry.getObjects().forEach(objectMapper::handleAdd);
        registry.getConnections().forEach((id, conn) -> connectionMapper.handleAdd(id, conn));
        registry.getLayers().forEach((id, layer) -> handleLayerVisibilityChanged(id, layer.visible()));
    }

    private void handleLayerVisibilityChanged(UUID layerId, boolean visible) {
        registry.getObjects().stream()
                .filter(obj -> obj.layerId().equals(layerId))
                .forEach(obj -> {
                    objectMapper.setVisible(obj.id(), visible);

                    // Connection visibility: hide if one of the endpoints belongs to this layer
                    connectionMapper.getVisualConnections().forEach((connId, path) -> {
                        var conn = registry.getConnections().get(connId);
                        if (conn != null && (conn.sourceId().equals(obj.id()) || conn.targetId().equals(obj.id()))) {
                            path.setVisible(visible);
                        }
                    });
                });
    }

    public void setOnTextUpdateRequested(BiConsumer<UUID, String> callback) {
        objectMapper.setOnTextUpdateRequested(callback);
    }
}
