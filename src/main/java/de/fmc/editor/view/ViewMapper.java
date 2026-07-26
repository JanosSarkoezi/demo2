package de.fmc.editor.view;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.event.RegistryEvent;
import de.fmc.editor.core.event.RegistryListener;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.Handle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ViewMapper implements RegistryListener {

    private final CoreRegistry registry;
    private final ShapeViewMapper shapeMapper;
    private final ConnectionViewMapper connectionMapper;
    private final SelectionViewManager selectionManager;
    private final TextViewMapper textMapper;

    public ViewMapper(GraphView graphView, CoreRegistry registry) {
        this.registry = registry;
        this.shapeMapper = new ShapeViewMapper(graphView);
        this.connectionMapper = new ConnectionViewMapper(graphView, registry);
        this.selectionManager = new SelectionViewManager(graphView, shapeMapper, connectionMapper);
        this.textMapper = new TextViewMapper(graphView);
    }

    // --------------------- Öffentliche API ---------------------
    public void setHover(UUID hoveredObjectId, UUID hoveredConnectionId) {
        selectionManager.setHover(hoveredObjectId, hoveredConnectionId);
    }

    public void setSelectedObjects(Collection<UUID> objectIds) {
        selectionManager.setSelectedObjects(objectIds);
    }

    public void setSelectedTexts(Collection<UUID> textIds) {
        textMapper.setSelectedTexts(textIds);
    }

    public void setRoutingStrategy(RoutingStrategy strategy) {
        connectionMapper.setRoutingStrategy(strategy);
    }

    public static List<Handle> getHandles(FmcObject obj) {
        return SelectionViewManager.getHandles(obj);
    }

    public TextViewMapper getTextMapper() {
        return textMapper;
    }

    // --------------------- RegistryListener ---------------------
    @Override
    public void handleEvent(RegistryEvent event) {
        // 1. Event-spezifische Verarbeitung
        switch (event) {
            case RegistryEvent.ObjectAdded e -> handleObjectAdded(e);
            case RegistryEvent.ObjectRemoved e -> handleObjectRemoved(e);
            case RegistryEvent.ObjectMoved e -> handleObjectMoved(e);
            case RegistryEvent.ObjectResized e -> handleObjectResized(e);
            case RegistryEvent.ConnectionAdded e -> handleConnectionAdded(e);
            case RegistryEvent.ConnectionRemoved e -> handleConnectionRemoved(e);
            case RegistryEvent.ConnectionUpdated e -> handleConnectionUpdated(e);
            case RegistryEvent.LayerAdded e -> handleLayerAdded(e);
            case RegistryEvent.LayerRemoved e -> handleLayerRemoved(e);
            case RegistryEvent.LayerVisibilityChanged e -> handleLayerVisibilityChanged(e);
            case RegistryEvent.TextAdded e -> handleTextAdded(e);
            case RegistryEvent.TextRemoved e -> handleTextRemoved(e);
            case RegistryEvent.TextUpdated e -> handleTextUpdated(e);
            case RegistryEvent.RegistryReset e -> handleRegistryReset();
        }

        // 2. Handles aktualisieren, falls das selektierte Objekt betroffen ist
        refreshHandlesIfNeeded(event);
    }

    // --------------------- Event-Handler (privat) ---------------------
    private void handleObjectAdded(RegistryEvent.ObjectAdded e) {
        FmcObject obj = e.object();
        shapeMapper.handleObjectAdded(obj);
        if (selectionManager.getSelectedObjectIds().contains(obj.id())) {
            Shape s = shapeMapper.getShape(obj.id());
            if (s != null) {
                selectionManager.setSelectedObjects(selectionManager.getSelectedObjectIds());
            }
        }
        connectionMapper.updateConnectionsForObject(obj.id());
    }

    private void handleObjectRemoved(RegistryEvent.ObjectRemoved e) {
        UUID id = e.id();
        shapeMapper.handleObjectRemoved(id);
        connectionMapper.updateConnectionsForObject(id);
    }

    private void handleObjectMoved(RegistryEvent.ObjectMoved e) {
        UUID id = e.id();
        shapeMapper.handleObjectMoved(id, e.newX(), e.newY());
        connectionMapper.updateConnectionsForObject(id);
    }

    private void handleObjectResized(RegistryEvent.ObjectResized e) {
        UUID id = e.id();
        FmcObject obj = registry.getObject(id);
        if (obj != null) {
            shapeMapper.handleObjectResized(id, obj.x(), obj.y(), e.newW(), e.newH());
        }
        connectionMapper.updateConnectionsForObject(id);
    }

    private void handleConnectionAdded(RegistryEvent.ConnectionAdded e) {
        connectionMapper.handleConnectionAdded(e.id(), e.connection());
    }

    private void handleConnectionRemoved(RegistryEvent.ConnectionRemoved e) {
        connectionMapper.handleConnectionRemoved(e.id());
    }

    private void handleConnectionUpdated(RegistryEvent.ConnectionUpdated e) {
        connectionMapper.handleConnectionUpdated(e.id(), e.connection());
    }

    private void handleLayerAdded(RegistryEvent.LayerAdded e) {
        // nichts zu tun
    }

    private void handleLayerRemoved(RegistryEvent.LayerRemoved e) {
        // nichts zu tun
    }

    private void handleLayerVisibilityChanged(RegistryEvent.LayerVisibilityChanged e) {
        UUID layerId = e.id();
        boolean visible = e.visible();
        // Shapes
        registry.getObjects().forEach(obj -> {
            if (obj.layerId().equals(layerId)) {
                Shape node = shapeMapper.getShape(obj.id());
                if (node != null) node.setVisible(visible);
            }
        });
        // Connections
        connectionMapper.getVisualConnections().forEach((connId, group) -> {
            var conn = registry.getConnections().get(connId);
            if (conn != null) {
                FmcObject src = registry.getObject(conn.sourceId());
                FmcObject tgt = registry.getObject(conn.targetId());
                if ((src != null && src.layerId().equals(layerId)) ||
                        (tgt != null && tgt.layerId().equals(layerId))) {
                    group.setVisible(visible);
                }
            }
        });
        // Texts
        registry.getTexts().forEach(txt -> {
            if (txt.layerId().equals(layerId)) {
                Text node = textMapper.getTextNode(txt.id());
                if (node != null) node.setVisible(visible);
            }
        });
    }

    private void handleTextAdded(RegistryEvent.TextAdded e) {
        textMapper.handleTextAdded(e.text());
    }

    private void handleTextRemoved(RegistryEvent.TextRemoved e) {
        textMapper.handleTextRemoved(e.id());
    }

    private void handleTextUpdated(RegistryEvent.TextUpdated e) {
        textMapper.handleTextUpdated(e.id(), e.text());
    }

    private void handleRegistryReset() {
        shapeMapper.clear();
        connectionMapper.clear();
        selectionManager.clear();
        textMapper.clear();

        registry.getObjects().forEach(shapeMapper::handleObjectAdded);
        registry.getConnections().forEach(connectionMapper::handleConnectionAdded);
        registry.getTexts().forEach(textMapper::handleTextAdded);
        registry.getLayers().forEach((id, layer) -> handleLayerVisibilityChanged(
                new RegistryEvent.LayerVisibilityChanged(id, layer.visible())
        ));
    }

    // --------------------- Hilfsmethoden ---------------------
    private void refreshHandlesIfNeeded(RegistryEvent event) {
        UUID singleId = selectionManager.getSingleSelectedObjectId();
        if (singleId == null) return;

        UUID affectedId = switch (event) {
            case RegistryEvent.ObjectMoved(var id, var x, var y) -> id;
            case RegistryEvent.ObjectResized(var id, var w, var h) -> id;
            default -> null;
        };

        if (singleId.equals(affectedId)) {
            FmcObject obj = registry.getObject(singleId);
            if (obj != null) {
                selectionManager.refreshHandles(getHandles(obj));
            }
        }
    }

    // --------------------- Sonstige ---------------------
    public void setSelectedObject(UUID id, List<Handle> handles) {
        selectionManager.setSingleSelectedObject(id, handles);
    }
}