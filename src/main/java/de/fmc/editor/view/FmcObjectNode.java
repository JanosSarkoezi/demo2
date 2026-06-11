package de.fmc.editor.view;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Visual representation of an FmcObject.
 * Encapsulates the JavaFX nodes and basic interaction logic like text editing.
 */
public class FmcObjectNode extends StackPane {
    private final UUID objectId;
    private final Shape backgroundShape;
    private final Label label;
    private BiConsumer<UUID, String> onTextUpdateRequested;

    public FmcObjectNode(FmcObject obj) {
        this.objectId = obj.id();
        this.setPrefSize(obj.width(), obj.height());

        // Background Shape
        this.backgroundShape = createBackgroundShape(obj);

        // Label
        this.label = new Label(obj.text());
        this.label.setWrapText(true);
        this.label.setAlignment(Pos.CENTER);
        if (obj.type() == FmcType.TEXT_BOX || obj.type() == FmcType.QUADRAT) {
            this.label.setPadding(new Insets(5));
        }
        this.label.prefWidthProperty().bind(this.prefWidthProperty());
        this.label.prefHeightProperty().bind(this.prefHeightProperty());
        this.label.setMouseTransparent(true);

        this.getChildren().addAll(backgroundShape, label);

        // Position
        updatePosition(obj.x(), obj.y(), obj.width(), obj.height());

        // ID in Properties for identification in the scene graph
        this.getProperties().put("UUID", obj.id());

        // Interaction
        setupInteractions();
    }

    private Shape createBackgroundShape(FmcObject obj) {
        Shape shape = switch (obj.type()) {
            case KREIS -> new Circle(obj.width() / 2);
            case QUADRAT -> new Rectangle(obj.width(), obj.height());
            case WEGPUNKT -> {
                Circle c = new Circle(obj.width());
                c.setFill(Color.YELLOW);
                c.setStroke(Color.BLACK);
                c.setStrokeWidth(1.5);
                yield c;
            }
            case TEXT_BOX -> {
                Rectangle textFrame = new Rectangle(obj.width(), obj.height());
                textFrame.setFill(Color.TRANSPARENT);
                textFrame.setStroke(Color.GRAY);
                textFrame.getStrokeDashArray().addAll(4.0, 4.0);
                yield textFrame;
            }
        };

        if (obj.type() != FmcType.WEGPUNKT) {
            shape.setFill(obj.type() == FmcType.TEXT_BOX ? Color.TRANSPARENT : Color.WHITE);
            shape.setStroke(obj.type() == FmcType.TEXT_BOX ? Color.GRAY : Color.BLACK);
            shape.setStrokeWidth(1.5);
            shape.setMouseTransparent(true);
        } else {
            shape.setMouseTransparent(false);
        }

        if (shape instanceof Rectangle rect) {
            rect.widthProperty().bind(this.prefWidthProperty());
            rect.heightProperty().bind(this.prefHeightProperty());
        }

        return shape;
    }

    private void setupInteractions() {
        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                startEditing();
            }
        });
    }

    private void startEditing() {
        TextField textField = new TextField(label.getText());
        textField.prefWidthProperty().bind(this.prefWidthProperty());
        textField.prefHeightProperty().bind(this.prefHeightProperty());
        textField.setMaxWidth(Double.MAX_VALUE);
        this.getChildren().add(textField);
        textField.requestFocus();

        textField.setOnAction(e -> {
            if (onTextUpdateRequested != null) {
                onTextUpdateRequested.accept(objectId, textField.getText());
            }
            this.getChildren().remove(textField);
        });

        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                this.getChildren().remove(textField);
            }
        });
    }

    public void updatePosition(double x, double y, double w, double h) {
        this.setLayoutX(x - (w / 2));
        this.setLayoutY(y - (h / 2));
    }

    public void updateSize(double w, double h, double x, double y) {
        this.setPrefSize(w, h);
        updatePosition(x, y, w, h);
    }

    public void updateText(String text) {
        label.setText(text);
    }

    public void setSelectionEffect(boolean selected) {
        if (selected) {
            backgroundShape.setEffect(createGlowEffect());
        } else {
            backgroundShape.setEffect(null);
        }
    }

    private DropShadow createGlowEffect() {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#0078D7"));
        glow.setRadius(15.0);
        glow.setSpread(0.5);
        return glow;
    }

    public void setOnTextUpdateRequested(BiConsumer<UUID, String> callback) {
        this.onTextUpdateRequested = callback;
    }
}
