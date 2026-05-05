package org.sozotech.pages.media;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Pos;

import java.util.Map;

import org.sozotech.utils.core.AppContext;
import org.sozotech.utils.page.PageComponent;
import org.sozotech.components.media.MVE;

public class ExternalCameraPage extends PageComponent {

    private MVE mediaView;

    private Label statusLabel;

    @Override
    public void parameters(Map<String, Object> args) {
        // optional configs
    }

    @Override
    public Parent createView() {

        BorderPane root = new BorderPane();

        // =========================
        // HEADER
        // =========================
        Label title = new Label("External Camera MediaView");
        title.setStyle("-fx-font-size: 18px; -fx-padding: 10;");

        statusLabel = new Label("Status: Initializing...");
        statusLabel.setStyle("-fx-text-fill: gray;");

        BorderPane top = new BorderPane();
        top.setTop(title);
        top.setBottom(statusLabel);
        BorderPane.setAlignment(title, Pos.CENTER);

        // =========================
        // MEDIA VIEW
        // =========================
        mediaView = new MVE();
        mediaView.setPreserveRatio(true);
        mediaView.setFitWidth(800);

        // =========================
        // CONTROLS
        // =========================
        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> {
            AppContext.router.navigate("/home");
        });

        // =========================
        // LAYOUT
        // =========================
        root.setTop(top);
        root.setCenter(mediaView);
        root.setBottom(backBtn);

        return root;
    }

    @Override
    public void onMount() {

        System.out.println("ExternalCameraPage Mounted");

        statusLabel.setText("Status: Waiting for camera connection...");

        // Let MVE initialize pipeline
        // (important: this ensures websocket + server starts)
    }

    @Override
    public void onUnmount() {

        System.out.println("ExternalCameraPage Unmounted");

        statusLabel.setText("Status: Stopped");

        if (mediaView != null) {
            mediaView.stop();
        }
    }
}