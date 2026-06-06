package de.fmc.editor.core.factory;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import java.util.UUID;

public class FmcFactory {
    public static FmcObject createObject(FmcType type, double x, double y, UUID layerId) {
        double w = (type == FmcType.KREIS) ? 40 : 30;
        double h = (type == FmcType.KREIS) ? 40 : 30;
        return new FmcObject(UUID.randomUUID(), type, x, y, w, h, layerId);
    }

    public static FmcObject moveObject(FmcObject original, double newX, double newY) {
        return new FmcObject(original.id(), original.type(), newX, newY, original.width(), original.height(), original.layerId());
    }

    public static FmcObject resizeObject(FmcObject original, double newW, double newH) {
        return new FmcObject(original.id(), original.type(), original.x(), original.y(), newW, newH, original.layerId());
    }
}
