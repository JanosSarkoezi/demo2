package de.fmc.editor.view;

import de.fmc.editor.core.event.RegistryEvent;
import de.fmc.editor.core.event.RegistryListener;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ViewMapper implements RegistryListener {

    private final Pane canvas; // Das JavaFX Zeichenfenster
    private final de.fmc.editor.core.CoreRegistry registry;
    private final Map<UUID, Shape> visualNodes = new HashMap<>();

    // Hilfsvariablen für Drag & Drop
    private double mouseAnchorX;
    private double mouseAnchorY;

    public ViewMapper(Pane canvas, de.fmc.editor.core.CoreRegistry registry) {
        this.canvas = canvas;
        this.registry = registry;
    }

    @Override
    public void handleEvent(RegistryEvent event) {
        // Java 21 Pattern Matching für sealed-Interfaces:
        switch (event) {
            case RegistryEvent.ObjectAdded(var obj) -> handleObjectAdded(obj);
            case RegistryEvent.ObjectRemoved(var id) -> handleObjectRemoved(id);
            case RegistryEvent.ObjectMoved(var id, var x, var y) -> handleObjectMoved(id, x, y);
        }
    }

    private void handleObjectAdded(de.fmc.editor.core.model.FmcObject obj) {
        Shape shape = switch (obj.type()) {
            case KREIS -> new Circle(obj.x(), obj.y(), 20);
            case QUADRAT -> new javafx.scene.shape.Rectangle(obj.x() - 15, obj.y() - 15, 30, 30);
            case WEGPUNKT -> new Circle(obj.x(), obj.y(), 5);
        };

        // Wichtig aus den Richtlinien: Nur die ID in den Properties speichern
        shape.getProperties().put("UUID", obj.id());

        // Event-Handling für Drag & Drop
        shape.setOnMousePressed(event -> {
            mouseAnchorX = event.getSceneX();
            mouseAnchorY = event.getSceneY();
        });

        shape.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - mouseAnchorX;
            double deltaY = event.getSceneY() - mouseAnchorY;

            // Wir setzen die Anker neu für die nächste Bewegung
            mouseAnchorX = event.getSceneX();
            mouseAnchorY = event.getSceneY();

            UUID id = (UUID) shape.getProperties().get("UUID");
            // Aktuelle Position aus der Shape holen (da diese dem Modell entspricht)
            double currentX = 0;
            double currentY = 0;
            
            if (shape instanceof Circle c) {
                currentX = c.getCenterX();
                currentY = c.getCenterY();
            } else if (shape instanceof javafx.scene.shape.Rectangle r) {
                currentX = r.getX() + (r.getWidth() / 2);
                currentY = r.getY() + (r.getHeight() / 2);
            }

            registry.moveObject(id, currentX + deltaX, currentY + deltaY);
        });

        visualNodes.put(obj.id(), shape);
        canvas.getChildren().add(shape);
    }

    private void handleObjectMoved(UUID id, double x, double y) {
        Shape shape = visualNodes.get(id);
        if (shape instanceof Circle circle) {
            circle.setCenterX(x);
            circle.setCenterY(y);
        } else if (shape instanceof javafx.scene.shape.Rectangle rect) {
            // Wir müssen hier vorsichtig sein: Das obj.x() im switch oben ist der Ursprung.
            // In CoreRegistry wird das Objekt aber komplett neu erzeugt.
            rect.setX(x - (rect.getWidth() / 2));
            rect.setY(y - (rect.getHeight() / 2));
        }
    }

    private void handleObjectRemoved(UUID id) {
        Shape shape = visualNodes.remove(id);
        if (shape != null) {
            canvas.getChildren().remove(shape);
        }
    }
}
