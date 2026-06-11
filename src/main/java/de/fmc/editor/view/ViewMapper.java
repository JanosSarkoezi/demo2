package de.fmc.editor.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.event.RegistryEvent;
import de.fmc.editor.core.event.RegistryListener;
import de.fmc.editor.core.model.Handle;
import javafx.scene.shape.Shape;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ViewMapper implements RegistryListener {

    private final CoreRegistry registry;
    private final ShapeViewMapper shapeMapper;
    private final ConnectionViewMapper connectionMapper;
    private final SelectionViewManager selectionManager;

    public ViewMapper(GraphView graphView, CoreRegistry registry) {
        this.registry = registry;
        this.shapeMapper = new ShapeViewMapper(graphView);
        this.connectionMapper = new ConnectionViewMapper(graphView, registry);
        this.selectionManager = new SelectionViewManager(graphView, shapeMapper);
    }

    public void setSelectedObjects(Collection<UUID> objectIds) {
        selectionManager.setSelectedObjects(objectIds);
    }

    public void setRoutingStrategy(RoutingStrategy strategy) {
        connectionMapper.setRoutingStrategy(strategy);
    }

    public static List<Handle> getHandles(de.fmc.editor.core.model.FmcObject obj) {
        return SelectionViewManager.getHandles(obj);
    }

    @Override
    public void handleEvent(RegistryEvent event) {
        switch (event) {
            case RegistryEvent.ObjectAdded(var obj) -> {
                shapeMapper.handleObjectAdded(obj);
                if (selectionManager.getSelectedObjectIds().contains(obj.id())) {
                    Shape s = shapeMapper.getShape(obj.id());
                    if (s != null) {
                        // This is a bit tricky since SelectionViewManager handles effects.
                        // We might want to re-apply selection after adding.
                        selectionManager.setSelectedObjects(selectionManager.getSelectedObjectIds());
                    }
                }
                connectionMapper.updateConnectionsForObject(obj.id());
            }
            case RegistryEvent.ObjectRemoved(var id) -> {
                shapeMapper.handleObjectRemoved(id);
                connectionMapper.updateConnectionsForObject(id);
            }
            case RegistryEvent.ObjectMoved(var id, var x, var y) -> {
                shapeMapper.handleObjectMoved(id, x, y);
                connectionMapper.updateConnectionsForObject(id);
            }
            case RegistryEvent.ObjectResized(var id, var w, var h) -> {
                // ShapeViewMapper needs x, y too for Rectangle
                registry.getObjects().stream()
                        .filter(o -> o.id().equals(id))
                        .findFirst()
                        .ifPresent(obj -> shapeMapper.handleObjectResized(id, obj.x(), obj.y(), w, h));
                connectionMapper.updateConnectionsForObject(id);
            }
            case RegistryEvent.ConnectionAdded(var id, var conn) -> connectionMapper.handleConnectionAdded(id, conn);
            case RegistryEvent.ConnectionRemoved(var id) -> connectionMapper.handleConnectionRemoved(id);
            case RegistryEvent.ConnectionUpdated(var id, var conn) -> connectionMapper.handleConnectionUpdated(id, conn);
            case RegistryEvent.LayerAdded(var layer) -> {}
            case RegistryEvent.LayerRemoved(var id) -> {}
            case RegistryEvent.LayerVisibilityChanged(var id, var visible) -> handleLayerVisibilityChanged(id, visible);
            case RegistryEvent.RegistryReset() -> handleRegistryReset();
        }

        // Handles refreshen, falls das selektierte Objekt betroffen ist
        UUID singleId = selectionManager.getSingleSelectedObjectId();
        if (singleId != null) {
            UUID affectedId = switch (event) {
                case RegistryEvent.ObjectMoved(var id, var x, var y) -> id;
                case RegistryEvent.ObjectResized(var id, var w, var h) -> id;
                default -> null;
            };

            if (singleId.equals(affectedId)) {
                registry.getObjects().stream()
                        .filter(o -> o.id().equals(singleId))
                        .findFirst()
                        .ifPresent(obj -> selectionManager.refreshHandles(getHandles(obj)));
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(event.getClass().getSimpleName() + ": " + gson.toJson(event));
    }

    public void setSelectedObject(UUID id, List<Handle> handles) {
        selectionManager.setSingleSelectedObject(id, handles);
    }

    private void handleRegistryReset() {
        shapeMapper.clear();
        connectionMapper.clear();
        selectionManager.clear();

        registry.getObjects().forEach(shapeMapper::handleObjectAdded);
        registry.getConnections().forEach(connectionMapper::handleConnectionAdded);
        registry.getLayers().forEach((id, layer) -> handleLayerVisibilityChanged(id, layer.visible()));
    }

    private void handleLayerVisibilityChanged(UUID layerId, boolean visible) {
        registry.getObjects().stream()
                .filter(obj -> obj.layerId().equals(layerId))
                .forEach(obj -> {
                    Shape node = shapeMapper.getShape(obj.id());
                    if (node != null) node.setVisible(visible);

                    connectionMapper.getVisualConnections().forEach((connId, path) -> {
                        var conn = registry.getConnections().get(connId);
                        if (conn != null && (conn.sourceId().equals(obj.id()) || conn.targetId().equals(obj.id()))) {
                            path.setVisible(visible);
                        }
                    });
                });
    }
}
