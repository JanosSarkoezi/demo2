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
        mainController.init(registry);

        // 5. JavaFX Bühne aufbauen
        Scene scene = new Scene(root, 1000, 700);
        mainController.setupShortcuts(scene);
        
        primaryStage.setTitle("FMC Editor - Prototype");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
