package org.sozotech.pages.Home;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.sozotech.utils.core.AppContext;
import org.sozotech.utils.page.PageComponent;

import java.util.Map;

public class Home extends PageComponent {

    @Override
    public void parameters(Map<String, Object> args) {

    }

    @Override
    protected Parent createView() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #ffffff;");

        Label titleLabel = new Label("Sozo Tech");
        Button startButton = new Button("Start");
        startButton.setOnAction(e -> {
            AppContext.router.navigate("/media/external-camera");
        });
        root.getChildren().addAll(titleLabel, startButton);
        return root;
    }

    @Override
    public void onMount() {

    }

    @Override
    public void onUnmount() {

    }
}
