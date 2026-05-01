package org.sozotech;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.sozotech.frontend.MainFrame;

public class Main extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage primaryStage) {
        MainFrame frame = new MainFrame();


        Scene scene = new Scene(frame.getRoot(), 400, 200);

        primaryStage.setTitle("Chimera Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {

        launch();
    }
}