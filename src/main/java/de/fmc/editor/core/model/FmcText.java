package de.fmc.editor.core.model;

import de.fmc.editor.core.CoreRegistry;
import java.util.Objects;
import java.util.UUID;

public record FmcText(
    UUID id,
    String text,
    double x,
    double y,
    double width,      // Für Text-Box (Wrap)
    String fontFamily,
    double fontSize,
    String fontWeight, // "normal", "bold"
    String fontStyle,  // "normal", "italic"
    String textFill,   // Farbe als Hex oder Farbname
    UUID parentObjectId, // Assoziation zu einem FmcObject (kann null sein)
    UUID layerId
) {
    public FmcText {
        Objects.requireNonNull(id, "ID darf nicht null sein");
        if (text == null) text = "";
        if (fontFamily == null) fontFamily = "System";
        if (fontSize <= 0) fontSize = 14;
        if (fontWeight == null) fontWeight = "normal";
        if (fontStyle == null) fontStyle = "normal";
        if (textFill == null) textFill = "#000000";
        if (layerId == null) layerId = CoreRegistry.DEFAULT_LAYER_ID;
    }
}
