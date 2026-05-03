package org.sozotech.utils;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Renderer {
    private final Stage stage;
    private Page currentPage;
    private boolean isTransitioning = false;

    public Renderer(Stage stage) {
        this.stage = stage;
        this.stage.setFullScreen(true);
    }

    public void render(Page page, Transition transition, String hexColor) {
        if (isTransitioning) return;
        isTransitioning = true;

        if (currentPage != null) {
            currentPage.onUnmount();
        }

        Scene scene = stage.getScene();

        // 🎨 Background container
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: " + hexColor + ";");

        Parent newRoot = page.getView();

        if (scene == null || transition == Transition.NONE) {
            container.getChildren().add(newRoot);

            if (scene == null) {
                scene = new Scene(container);
                stage.setScene(scene);
            } else {
                scene.setRoot(container);
            }

            currentPage = page;
            page.onMount();
            isTransitioning = false;
            return;
        }

        // old root must also be wrapped
        Parent oldRoot = scene.getRoot();

        container.getChildren().addAll(oldRoot, newRoot);
        scene.setRoot(container);

        switch (transition) {

            case FADE -> {
                newRoot.setOpacity(0);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), oldRoot);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);

                currentPage = page;
                page.onMount();

                fadeIn.setOnFinished(e -> {
                    container.getChildren().setAll(newRoot);
                    isTransitioning = false;
                });

                fadeOut.play();
                fadeIn.play();
            }

            case SLIDE_LEFT, SLIDE_RIGHT -> {
                double width = stage.getWidth();

                double start = transition == Transition.SLIDE_LEFT ? width : -width;
                double endOld = transition == Transition.SLIDE_LEFT ? -width : width;

                newRoot.setTranslateX(start);

                TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), newRoot);
                slideIn.setFromX(start);
                slideIn.setToX(0);

                TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), oldRoot);
                slideOut.setFromX(0);
                slideOut.setToX(endOld);

                currentPage = page;
                page.onMount();

                slideIn.setOnFinished(e -> {
                    container.getChildren().setAll(newRoot);
                    isTransitioning = false;
                });

                slideOut.play();
                slideIn.play();
            }

            default -> {
                container.getChildren().setAll(newRoot);
                currentPage = page;
                page.onMount();
                isTransitioning = false;
            }
        }
    }
}