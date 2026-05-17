package org.sozotech.ui.pages.introduction;

import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Screen;

import org.sozotech.utils.core.AppContext;
import org.sozotech.utils.page.PageComponent;
import org.sozotech.utils.style.Transition;

import java.util.Map;
import java.util.Objects;

public class IntroPage extends PageComponent {

    private MediaPlayer mediaPlayer;

    @Override
    protected Parent createView() {
        String path = Objects.requireNonNull(getClass().getResource("/assets/intro/Introduction.mp4")).toExternalForm();

        Media media = new Media(path);
        mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        mediaView.setFitWidth(screen.getWidth());
        mediaView.setFitHeight(screen.getHeight());
        mediaView.setPreserveRatio(true);

        StackPane root = new StackPane(mediaView);
        root.setStyle("-fx-background-color: black;");
        root.setPrefSize(screen.getWidth(), screen.getHeight());
        return root;
    }

    @Override
    public void parameters(Map<String, Object> args) {

    }

    @Override
    public void onMount() {
        mediaPlayer.play();
        mediaPlayer.setOnEndOfMedia(this::onVideoEnd);
    }

    @Override
    public void onUnmount() {
        mediaPlayer.stop();
        mediaPlayer.dispose();
    }

    private void onVideoEnd() {
        AppContext.router.navigate("/home", Transition.FADE, "#00000");
    }
}