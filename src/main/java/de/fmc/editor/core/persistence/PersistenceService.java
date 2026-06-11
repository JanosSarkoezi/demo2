package de.fmc.editor.core.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;

/**
 * Service zum Serialisieren und Deserialisieren von Diagrammdaten mit Gson.
 */
public class PersistenceService {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static void saveDiagram(DiagramData data, Writer writer) {
        GSON.toJson(data, writer);
    }

    public static DiagramData loadDiagram(Reader reader) {
        return GSON.fromJson(reader, DiagramData.class);
    }
}
