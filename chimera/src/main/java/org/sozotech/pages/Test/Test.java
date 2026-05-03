package org.sozotech.pages.Test;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.opencv.core.Core;
import org.sozotech.utils.PageComponent;
import org.sozotech.utils.AppContext;
import org.sozotech.utils.Router;

public class Test extends PageComponent {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void parameters(Object... args) {

    }

    @Override
    protected Parent createView() {
        VBox root = new VBox(10);
        Label titleLabel = new Label("SozoTech");

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

        Button button = new Button("Go to Next Page");
        button.setOnMouseClicked(event -> {
            AppContext.router.navigate("/home");
        });
        root.getChildren().addAll(titleLabel, button);

        return root;
    }

    @Override
    public void onMount() {

    }

    @Override
    public void onUnmount() {

    }
}
