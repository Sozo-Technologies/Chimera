package org.sozotech.utils;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class Renderer {
    private final Stage stage;
    private Page currentPage;

    public Renderer(Stage stage) {
        this.stage = stage;
        this.stage.setFullScreen(true);
        this.stage.setWidth(1000);
        this.stage.setHeight(600);
        this.stage.setResizable(false);
        this.stage.fullScreenExitHintProperty().setValue(null);
    }

    public void render(Page page) {
        if (currentPage != null) currentPage.onUnmount();
        this.currentPage = page;
        Scene scene = this.stage.getScene();

        if (scene == null) {
            scene = new Scene(page.getView());
            this.stage.setScene(scene);
        } else scene.setRoot(page.getView());

        this.stage.setFullScreen(true);
        page.onMount();
    }
}