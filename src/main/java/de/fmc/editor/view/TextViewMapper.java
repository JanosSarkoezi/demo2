package de.fmc.editor.view;

import de.fmc.editor.core.model.FmcText;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Collection;

public class TextViewMapper {

    private final GraphView graphView;
    private final Map<UUID, Text> visualTexts = new HashMap<>();
    private final Map<UUID, Rectangle> selectionBorders = new HashMap<>();
    private Collection<UUID> selectedIds = java.util.Collections.emptySet();

    public TextViewMapper(GraphView graphView) {
        this.graphView = graphView;
    }

    public void handleTextAdded(FmcText text) {
        Text node = createTextNode(text);
        visualTexts.put(text.id(), node);
        graphView.getTextLayer().getChildren().add(node);
        updateSelectionBorders();
    }

    public void handleTextUpdated(UUID id, FmcText text) {
        Text existing = visualTexts.get(id);
        if (existing != null) {
            updateTextNode(existing, text);
        } else {
            handleTextAdded(text);
        }
        updateSelectionBorders();
    }

    public void handleTextRemoved(UUID id) {
        Text removed = visualTexts.remove(id);
        if (removed != null) {
            graphView.getTextLayer().getChildren().remove(removed);
        }
        Rectangle border = selectionBorders.remove(id);
        if (border != null) {
            graphView.getTextLayer().getChildren().remove(border);
        }
    }

    public Text getTextNode(UUID id) {
        return visualTexts.get(id);
    }

    private Text createTextNode(FmcText text) {
        Text node = new Text(text.text());
        node.getProperties().put("UUID", text.id());
        node.setMouseTransparent(true); // matching ShapeViewMapper, click handling via CanvasController
        applyStyles(node, text);
        return node;
    }

    private void updateTextNode(Text node, FmcText text) {
        node.setText(text.text());
        applyStyles(node, text);
    }

    private void applyStyles(Text node, FmcText text) {
        // Font-Family, Weight & Posture
        FontWeight weight = switch (text.fontWeight().toLowerCase()) {
            case "bold" -> FontWeight.BOLD;
            default -> FontWeight.NORMAL;
        };
        FontPosture posture = switch (text.fontStyle().toLowerCase()) {
            case "italic" -> FontPosture.ITALIC;
            default -> FontPosture.REGULAR;
        };
        Font font = Font.font(text.fontFamily(), weight, posture, text.fontSize());
        node.setFont(font);

        // Farbe
        try {
            node.setFill(Color.web(text.textFill()));
        } catch (IllegalArgumentException e) {
            node.setFill(Color.BLACK);
        }

        // Text-Begrenzung (für Wrap / Zentrierung)
        node.setWrappingWidth(text.width() > 0 ? text.width() : 0);
        node.setTextAlignment(TextAlignment.CENTER);
        
        // Position: X is centered if width > 0, Y is centered or baseline. 
        // In JavaFX Text, X is the left edge of the wrapping width if wrappingWidth > 0.
        // If wrappingWidth is 0, X is the left of the text. Let's align it centered around (x, y):
        if (text.width() > 0) {
            node.setX(text.x() - text.width() / 2);
        } else {
            node.setX(text.x());
        }
        node.setY(text.y());
    }

    public void setSelectedTexts(Collection<UUID> selectedTextIds) {
        this.selectedIds = selectedTextIds;
        updateSelectionBorders();
    }

    private void updateSelectionBorders() {
        // Alte Rahmen entfernen
        selectionBorders.values().forEach(b -> graphView.getTextLayer().getChildren().remove(b));
        selectionBorders.clear();

        for (UUID id : selectedIds) {
            Text node = visualTexts.get(id);
            if (node != null) {
                // Bounds im Eltern-Koordinatensystem (textLayer) holen
                Bounds boundsInParent = node.getBoundsInParent();
                double x = boundsInParent.getMinX() - 2;
                double y = boundsInParent.getMinY() - 2;
                double w = boundsInParent.getWidth() + 4;
                double h = boundsInParent.getHeight() + 4;

                Rectangle rect = new Rectangle(x, y, w, h);
                rect.setFill(Color.TRANSPARENT);
                rect.setStroke(Color.web("#3498db"));
                rect.setStrokeWidth(1.0);
                rect.getStrokeDashArray().addAll(4.0, 4.0);
                rect.setMouseTransparent(true);

                selectionBorders.put(id, rect);
                graphView.getTextLayer().getChildren().add(rect);
            }
        }
    }

    public void clear() {
        visualTexts.values().forEach(node -> graphView.getTextLayer().getChildren().remove(node));
        visualTexts.clear();
        selectionBorders.values().forEach(b -> graphView.getTextLayer().getChildren().remove(b));
        selectionBorders.clear();
    }
}
