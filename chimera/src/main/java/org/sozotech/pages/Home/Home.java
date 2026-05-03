package org.sozotech.pages.Home;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.sozotech.utils.PageComponent;

public class Home extends PageComponent {

    @Override
    public void parameters(Object... args) {

    }

    @Override
    protected Parent createView() {
        VBox root = new VBox(10);
        Label titleLabel = new Label("SozoTech");
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(titleLabel);

        return root;
    }

    @Override
    public void onMount() {

    }

    @Override
    public void onUnmount() {

    }
}
