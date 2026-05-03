package org.sozotech;

import javafx.application.Application;
import javafx.stage.Stage;
import org.opencv.core.Core;

import org.sozotech.utils.Renderer;
import org.sozotech.utils.Router;
import org.sozotech.utils.AppContext;

import org.sozotech.pages.Home.Home;
import org.sozotech.pages.Test.Test;

public class Main extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Chimera.");
        
        Renderer renderer = new Renderer(stage);
        AppContext.router = new Router(renderer);

        AppContext.router.register("/test", Test::new);
        AppContext.router.register("/home", Home::new);

        AppContext.router.navigate("/test");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}