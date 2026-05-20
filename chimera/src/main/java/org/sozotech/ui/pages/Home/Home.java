package org.sozotech.ui.pages.Home;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.sozotech.utils.core.AppContext;
import org.sozotech.utils.hooks.StateHook;
import org.sozotech.utils.page.PageComponent;
import org.sozotech.stager.Stager;

import java.util.Map;

public class Home extends PageComponent {
    private final StateHook<Boolean> darkMode = useState(false);
    private final StateHook<String> status = useState("Ready.");
    private final StateHook<Integer> countdown = useState(0);

    @Override
    protected Parent createView() {
        useEffect(
                () -> System.out.println("[Home] Status → " + status.get()),
                status.get()
        );

        useEffect(() -> {
            if (countdown.get() <= 0) return null;

            Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                int next = countdown.get() - 1;
                countdown.set(next);
                if (next <= 0) {
                    status.set("Launching...");
                    if (!Stager.runMediapipe()) status.set("Mediapipe failed.");
                    else AppContext.router.navigate("/media/handtrack");
                } else {
                    status.set("Starting in " + next + "...");
                }
            }));
            timer.setCycleCount(countdown.get());
            timer.play();

            return timer::stop;
        }, countdown.get());

        boolean dark = darkMode.get();
        String bg     = dark ? "#111111" : "#f5f5f5";
        String card   = dark ? "#1e1e1e" : "#ffffff";
        String fg     = dark ? "#f0f0f0" : "#111111";
        String muted  = dark ? "#aaaaaa" : "#666666";
        String accent = dark ? "#ffffff" : "#111111";
        String accentFg = dark ? "#111111" : "#ffffff";
        String border = dark ? "#2e2e2e" : "#e0e0e0";


        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + bg + ";");


        VBox card_box = new VBox(20);
        card_box.setAlignment(Pos.CENTER);
        card_box.setMaxWidth(360);
        card_box.setPadding(new Insets(40));
        card_box.setStyle(
                "-fx-background-color: " + card + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + border + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1;"
        );


        Label title = new Label("Chimera");
        title.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + fg + ";"
        );

        Label subtitle = new Label("Gesture Translation");
        subtitle.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: " + muted + ";"
        );

        VBox titleBlock = new VBox(4, title, subtitle);
        titleBlock.setAlignment(Pos.CENTER);


        Label statusLabel = new Label(status.get());
        statusLabel.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: " + muted + ";"
        );


        boolean counting = countdown.get() > 0;

        Button startButton = new Button(counting ? "Cancel  (" + countdown.get() + ")" : "Start");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setStyle(
                "-fx-background-color: " + accent + ";" +
                        "-fx-text-fill: " + accentFg + ";" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 12 0;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );
        startButton.setOnAction(e -> {
            if (countdown.get() > 0) {
                countdown.set(0);
                status.set("Ready.");
            } else {
                status.set("Starting in 3...");
                countdown.set(3);
            }
        });


        Region divider = new Region();
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: " + border + ";");

        Label themeLabel = new Label(dark ? "Dark mode" : "Light mode");
        themeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + muted + ";");

        Button themeToggle = new Button(dark ? "Switch to Light" : "Switch to Dark");
        themeToggle.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + muted + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-border-color: " + border + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-padding: 4 10;" +
                        "-fx-cursor: hand;"
        );
        themeToggle.setOnAction(e -> darkMode.set(!darkMode.get()));

        HBox themeRow = new HBox(themeLabel, themeToggle);
        themeRow.setAlignment(Pos.CENTER_LEFT);
        themeRow.setSpacing(0);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        themeRow.getChildren().add(1, spacer);

        card_box.getChildren().addAll(
                titleBlock,
                statusLabel,
                startButton,
                divider,
                themeRow
        );

        root.getChildren().add(card_box);
        return root;
    }

    @Override
    public void onMount() {
        super.onMount();
    }

    @Override
    public void onUnmount() {
        super.onUnmount();
    }

    @Override
    public void parameters(Map<String, Object> args) {}
}