package org.sozotech;

import java.util.Objects;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import org.sozotech.ml.core.NeuralNetwork;
import org.sozotech.utils.core.OpenCVContext;
import org.sozotech.utils.core.Renderer;
import org.sozotech.utils.core.Router;
import org.sozotech.utils.core.AppContext;

import org.sozotech.ui.PageRegistry;

import org.sozotech.stager.Stager;

public class Main extends Application {
    static { OpenCVContext.load(); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Chimera.");
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png"))));
        Renderer renderer = new Renderer(stage);
        AppContext.router = new Router(renderer);
        PageRegistry.loadRegisteredPages();

        NeuralNetwork.getInstance().start();
        stage.setOnCloseRequest(event -> NeuralNetwork.getInstance().shutdown());
        
        AppContext.router.navigate("/intropage");
        stage.show();
    }

    public static void main(String[] args) {
        Stager.Install();
        launch();
    }
}