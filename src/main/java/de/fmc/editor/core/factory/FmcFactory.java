package de.fmc.editor.core.factory;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import java.util.UUID;

public class FmcFactory {
    public static FmcObject createObject(FmcType type, double x, double y, UUID layerId) {
        UUID id = UUID.randomUUID();
        return switch (type) {
            case KREIS -> new FmcObject(id, type, x, y, 40, 40, layerId, "");
            case QUADRAT -> new FmcObject(id, type, x, y, 30, 30, layerId, "");
            case WEGPUNKT -> new FmcObject(id, type, x, y, 10, 10, layerId, "");
            case TEXT_BOX -> new FmcObject(id, type, x, y, 140, 50, layerId, "Freitext hier...");
        };
    }

    public static FmcObject moveObject(FmcObject original, double newX, double newY) {
        return new FmcObject(original.id(), original.type(), newX, newY, original.width(), original.height(), original.layerId(), original.text());
    }

    public static FmcObject resizeObject(FmcObject original, double newW, double newH) {
        return new FmcObject(original.id(), original.type(), original.x(), original.y(), newW, newH, original.layerId(), original.text());
    }

    public static FmcObject updateText(FmcObject original, String newText) {
        return new FmcObject(original.id(), original.type(), original.x(), original.y(), original.width(), original.height(), original.layerId(), newText);
    }
}
