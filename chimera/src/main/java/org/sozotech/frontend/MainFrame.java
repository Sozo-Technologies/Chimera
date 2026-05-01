package org.sozotech.frontend;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.opencv.core.Core;
import org.sozotech.controllers.MainController;

public class MainFrame {

    private VBox root;
    private Label titleLabel;

    private final MainController controller;

    public MainFrame() {
        initialize();
        controller = new MainController(this);
    }

    private void initialize() {
        root = new VBox(10);

        titleLabel = new Label("Welcome to SozoTech");

        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        String opencvVersion = Core.VERSION;

        Label title = new Label("🚀 Chimera System Info");
        Label javaLabel = new Label("Java Version: " + javaVersion);
        Label javafxLabel = new Label("JavaFX Version: " + javafxVersion);
        Label opencvLabel = new Label("OpenCV Version: " + opencvVersion);

        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(
                title,
                javaLabel,
                javafxLabel,
                opencvLabel
        );


        Button button = new Button("Click Me");
        button.setOnAction(e -> controller.handleClick());
        root.getChildren().addAll(titleLabel, button);
    }

    public VBox getRoot() {
        return root;
    }

    public void setTitle(String text) {
        titleLabel.setText(text);
    }
}
