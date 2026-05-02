package org.sozotech.utils;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class Renderer {
    private final Stage stage;
    private Page currentPage;

    public Renderer(Stage stage) {
        this.stage = stage;
        this.stage.setFullScreen(true);
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