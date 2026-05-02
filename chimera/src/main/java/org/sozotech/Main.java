package org.sozotech;

import javafx.application.Application;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.sozotech.pages.Test.Test;
import org.sozotech.utils.Renderer;

public class Main extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chimera.");
        Renderer renderer = new Renderer(primaryStage);
        renderer.render(new Test());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}