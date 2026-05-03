package org.sozotech.pages.dev;

import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import org.opencv.core.Core;
import org.sozotech.utils.core.AppContext;
import org.sozotech.utils.page.PageComponent;

public class DebugPage extends PageComponent {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private TextArea output;
    private TextField input;

    @Override
    public void parameters(Map<String, Object> args) {}

    @Override
    protected Parent createView() {
        VBox root = new VBox(5);
        root.setPadding(new Insets(100));
        root.setStyle("-fx-background-color: #333446");

        output = new TextArea();
        output.setEditable(false);
        output.setWrapText(true);
        output.setFont(Font.font("Consolas", 14));
        output.setStyle("-fx-control-inner-background: #0E2148; -fx-text-fill: #7965C1;");

        ScrollPane scroll = new ScrollPane(output);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        input = new TextField();
        input.setFont(Font.font("Consolas", 14));
        input.setStyle("-fx-control-inner-background: #0E2148; -fx-text-fill: #7965C1;");
        input.setPromptText("Enter command...");

        input.setOnAction(e -> {
            String command = input.getText();
            append("> " + command);

            handleCommand(command);

            input.clear();
        });

        root.getChildren().addAll(scroll, input);

        return root;
    }

    @Override
    public void onMount() {
        append("=== Chimera Debug Terminal ===");
        append("Java Version: " + System.getProperty("java.version"));
        append("JavaFX Version: " + System.getProperty("javafx.version"));
        append("OpenCV Version: " + Core.VERSION);
        append("Type 'help' for commands\n");
    }

    @Override
    public void onUnmount() {
        append("Exiting Debug Page...");
    }


    private void append(String text) {
        output.appendText(text + "\n");
    }

    private void handleCommand(String cmd) {
        switch (cmd.toLowerCase()) {
            case "help":
                append("Available commands:");
                append("help - show commands");
                append("clear - clear terminal");
                append("opencv - show OpenCV version");
                break;

            case "clear":
                output.clear();
                break;

            case "opencv":
                append("OpenCV Version: " + Core.VERSION);
                break;
            case "exit":
                append("Exiting Debug Page...");
                AppContext.router.navigate("/home");
                break;
            default:
                append("Unknown command: " + cmd);
        }
    }
}