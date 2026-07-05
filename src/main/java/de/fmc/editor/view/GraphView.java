package de.fmc.editor.view;

import javafx.scene.Group;
import javafx.geometry.Point2D;
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

    // Hilfsmethode für die Koordinaten-Umrechnung (für die States)
    public Point2D getMouseInWorld(double sceneX, double sceneY) {
        return world.sceneToLocal(sceneX, sceneY);
    }

    public void handleZoom(javafx.scene.input.ScrollEvent event) {
        double delta = event.getDeltaY();
        if (delta == 0.0 || !event.isControlDown()) return;

        double zoomFactor = (delta > 0) ? 1.1 : 0.9;

        // Aktuellen Scale abrufen
        double oldScale = zoomTransform.getX();
        double newScale = oldScale * zoomFactor;

        if (newScale < MIN_SCALE || newScale > MAX_SCALE) return;

        double mouseX = event.getSceneX();
        double mouseY = event.getSceneY();

        Point2D mouseInWorldBefore = world.sceneToLocal(mouseX, mouseY);

        zoomTransform.setX(newScale);
        zoomTransform.setY(newScale);

        Point2D mouseInWorldAfter = world.sceneToLocal(mouseX, mouseY);

        double deltaX = mouseInWorldAfter.getX() - mouseInWorldBefore.getX();
        double deltaY = mouseInWorldAfter.getY() - mouseInWorldBefore.getY();

        world.setTranslateX(world.getTranslateX() + deltaX * newScale);
        world.setTranslateY(world.getTranslateY() + deltaY * newScale);

        event.consume();
    }
}
