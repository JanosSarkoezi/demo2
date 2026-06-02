package de.fmc.editor.core.factory;

import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import java.util.UUID;

public class FmcFactory {
    public static FmcObject createObject(FmcType type, double x, double y, UUID layerId) {
        return new FmcObject(UUID.randomUUID(), type, x, y, layerId);
    }

    public static FmcObject moveObject(FmcObject original, double newX, double newY) {
        return new FmcObject(original.id(), original.type(), newX, newY, original.layerId());
    }
}
