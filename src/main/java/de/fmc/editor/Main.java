package de.fmc.editor;

import de.fmc.editor.core.CoreRegistry;
import de.fmc.editor.core.factory.FmcFactory;
import de.fmc.editor.core.model.FmcObject;
import de.fmc.editor.core.model.FmcType;
import de.fmc.editor.controller.MainController;
import de.fmc.editor.view.ViewMapper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. FXML laden
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/graph/main-view.fxml"));
        Parent root = loader.load();
        MainController mainController = loader.getController();

        // 2. Core Komponenten initialisieren
        CoreRegistry registry = new CoreRegistry();
        
        // Wir brauchen Zugriff auf den CanvasController, um den ViewMapper zu binden
        // In diesem einfachen Beispiel gehen wir davon aus, dass MainController Zugriff bietet
        // oder wir suchen die Node manuell.
        
        // Da wir das FXML und die Controller kontrollieren, können wir den Canvas finden:
        javafx.scene.layout.Pane canvas = (javafx.scene.layout.Pane) root.lookup("#drawingPane");
        
        ViewMapper viewMapper = new ViewMapper(canvas, registry);

        // 3. ViewMapper als Listener registrieren
        registry.addListener(viewMapper);

        // 4. Test-Daten hinzufügen (Etappe 1 & 2 Demo)
        java.util.UUID defaultLayer = java.util.UUID.randomUUID();
        FmcObject circle = FmcFactory.createObject(FmcType.KREIS, 100, 100, defaultLayer);
        FmcObject square = FmcFactory.createObject(FmcType.QUADRAT, 300, 200, defaultLayer);
        FmcObject waypoint = FmcFactory.createObject(FmcType.WEGPUNKT, 200, 150, defaultLayer);

        registry.addObject(circle);
        registry.addObject(square);
        registry.addObject(waypoint);

        // 5. JavaFX Bühne aufbauen
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setTitle("FMC Editor - Prototype");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
