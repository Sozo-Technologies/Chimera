package org.sozotech;

import javafx.application.Application;
import javafx.stage.Stage;
import org.opencv.core.Core;
import javafx.scene.image.Image;

import org.sozotech.pages.media.ExternalCameraPage;
import org.sozotech.pages.media.HandTrack;
import org.sozotech.utils.core.Renderer;
import org.sozotech.utils.core.Router;
import org.sozotech.utils.core.AppContext;

import org.sozotech.pages.LoadingScreen.LoadingScreen;
import org.sozotech.pages.Home.Home;
import org.sozotech.pages.dev.DebugPage;

import java.util.Objects;
import org.sozotech.utils.stager.Stager;

public class Main extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Chimera.");
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png"))));

        Renderer renderer = new Renderer(stage);
        AppContext.router = new Router(renderer);

        AppContext.router.register("/loading_screen", LoadingScreen::new);
        AppContext.router.register("/home", Home::new);
        AppContext.router.register("/debug", DebugPage::new);
        AppContext.router.register("/media/external-camera", ExternalCameraPage::new);
        AppContext.router.register("/media/handtrack", HandTrack::new);

        AppContext.router.navigate("/home");
        stage.show();
    }

    public static void main(String[] args) {
        Stager.Install();
        launch();
    }
}