package org.sozotech;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.opencv.core.Core;

public class Main extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage primaryStage) {

        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        String opencvVersion = Core.VERSION;

        Label title = new Label("🚀 Chimera System Info");
        Label javaLabel = new Label("Java Version: " + javaVersion);
        Label javafxLabel = new Label("JavaFX Version: " + javafxVersion);
        Label opencvLabel = new Label("OpenCV Version: " + opencvVersion);

        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(
                title,
                javaLabel,
                javafxLabel,
                opencvLabel
        );

        Scene scene = new Scene(root, 400, 200);

        primaryStage.setTitle("Chimera Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {

        launch();
    }
}