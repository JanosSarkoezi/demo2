package de.fmc.editor.view;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

public class GraphView extends Pane {
    private final Group world = new Group();
    private final Group connectionLayer = new Group();
    private final Group shapeLayer = new Group();
    private final Group textLayer = new Group();
    private final Group uiLayer = new Group();

    private static final double MIN_SCALE = 0.2;
    private static final double MAX_SCALE = 10.0;

    private final Scale zoomTransform = new Scale(1, 1, 0, 0);

    public GraphView() {
        getStyleClass().add("graph-grid");
        setFocusTraversable(true);
        setupLayers();
        
        // Clipping aktivieren, damit Zeichnungen nicht über den Rand ragen
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(this.widthProperty());
        clip.heightProperty().bind(this.heightProperty());
        this.setClip(clip);
    }

    private void setupLayers() {
        // Die Reihenfolge entscheidet über die Z-Ebene (was liegt vorne?)
        world.getChildren().addAll(connectionLayer, shapeLayer, textLayer, uiLayer);
        world.getTransforms().add(zoomTransform);
        this.getChildren().add(world);
    }

    public Group getShapeLayer() {
        return shapeLayer;
    }

    public Group getConnectionLayer() {
        return connectionLayer;
    }

    public Group getTextLayer() {
        return textLayer;
    }

    public Group getUiLayer() {
        return uiLayer;
    }

    public Group getWorld() {
        return world;
    }
}
