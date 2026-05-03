package org.sozotech.pages.LoadingScreen;

import javafx.animation.*;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.util.Duration;


import org.sozotech.utils.core.AppContext;
import org.sozotech.utils.page.PageComponent;
import org.sozotech.utils.Transition;

import java.util.Map;
import java.util.Objects;

public class LoadingScreen extends PageComponent {

    private VBox root;

    @Override
    public void parameters(Map<String, Object> args) {}

    @Override
    protected Parent createView() {
        root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #333446;");
        root.setSpacing(0);
        return root;
    }

    @Override
    public void onMount() {

        ImageView logo = new ImageView(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo2.png")))
        );
        logo.setFitWidth(450);
        logo.setPreserveRatio(true);

        Label bottomText = new Label("Chimera.");
        bottomText.setStyle("-fx-text-fill: white; -fx-font-size: 45px;");

        logo.setOpacity(0);
        bottomText.setOpacity(0);
        VBox.setMargin(logo, new javafx.geometry.Insets(0, 0, 10, 0));
        VBox.setMargin(bottomText, new javafx.geometry.Insets(0, 0, 10, 0));

        root.getChildren().addAll(logo, bottomText);

        SequentialTransition sequence = new SequentialTransition(
                pause(5),
                fadeIn(logo, 1),
                pause(0.5),
                fadeIn(bottomText, 1.5),
                pause(2)
        );

        sequence.setOnFinished(event -> {
            AppContext.router.navigate("/home", Transition.FADE, "#333446");
        });

        sequence.play();
    }

    @Override
    public void onUnmount() {}

    private FadeTransition fadeIn(javafx.scene.Node node, double seconds) {
        FadeTransition ft = new FadeTransition(Duration.seconds(seconds), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_BOTH);
        return ft;
    }

    private FadeTransition fadeOut(javafx.scene.Node node, double seconds) {
        FadeTransition ft = new FadeTransition(Duration.seconds(seconds), node);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setInterpolator(Interpolator.EASE_BOTH);
        return ft;
    }

    private PauseTransition pause(double seconds) {
        return new PauseTransition(Duration.seconds(seconds));
    }
}