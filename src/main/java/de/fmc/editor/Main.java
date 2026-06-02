package de.fmc.editor;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.view.ViewMapper;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Core Komponenten initialisieren
        CoreRegistry registry = new CoreRegistry();
        Pane canvas = new Pane();
        ViewMapper viewMapper = new ViewMapper(canvas, registry);

        // 2. ViewMapper als Listener registrieren
        registry.addListener(viewMapper);

        // 3. Test-Daten hinzufügen (Etappe 1 & 2 Demo)
        java.util.UUID defaultLayer = java.util.UUID.randomUUID();
        FmcObject circle = FmcFactory.createObject(FmcType.KREIS, 100, 100, defaultLayer);
        FmcObject square = FmcFactory.createObject(FmcType.QUADRAT, 300, 200, defaultLayer);
        FmcObject waypoint = FmcFactory.createObject(FmcType.WEGPUNKT, 200, 150, defaultLayer);

        registry.addObject(circle);
        registry.addObject(square);
        registry.addObject(waypoint);

        // 4. JavaFX Bühne aufbauen
        Scene scene = new Scene(canvas, 800, 600);
        primaryStage.setTitle("FMC Editor - Prototype");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
