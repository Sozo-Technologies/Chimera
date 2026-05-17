package org.sozotech.ui.pages.media;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.jetbrains.annotations.NotNull;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import org.sozotech.ml.preprocess.Matrix;
import org.sozotech.ml.preprocess.Normalizer;

import org.sozotech.stager.Stager;
import org.sozotech.system.WSClient;
import org.sozotech.utils.page.PageComponent;
import org.sozotech.utils.core.AppContext;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Files;

import java.util.Map;

public class DevTrack extends PageComponent {

    private ImageView cameraView;
    private Canvas canvas;

    private VideoCapture camera;
    private WSClient wsClient;

    private volatile boolean running = false;
    private volatile boolean paused = false;

    private Mat currentFrame;

    @Override
    protected Parent createView() {
        cameraView = new ImageView();
        canvas = new Canvas(640, 480);
        Button backButton = new Button("← Back");
        backButton.setStyle("""
            -fx-background-color: rgba(0,0,0,0.6);
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-background-radius: 8;
        """);

        backButton.setOnAction(e -> AppContext.router.navigate("/debug", Map.of("recent-page", "/home")));

        StackPane root = new StackPane(cameraView, canvas);
        AnchorPane overlay = new AnchorPane(backButton);

        AnchorPane.setTopAnchor(backButton, 10.0);
        AnchorPane.setLeftAnchor(backButton, 10.0);

        StackPane container = getStackPane(root, overlay);
        Platform.runLater(container::requestFocus);

        return container;
    }

    @NotNull
    private StackPane getStackPane(StackPane root, AnchorPane overlay) {
        StackPane container = new StackPane(root, overlay);
        container.setFocusTraversable(true);
        container.setOnKeyPressed(event -> {
            String key = event.getText();
            if (key == null || key.isBlank()) return;
            char ch = Character.toLowerCase(key.charAt(0));
            if (!Character.isLetter(ch)) return;
            if (currentFrame == null || currentFrame.empty()) return;
            paused = true;
            Platform.runLater(() -> showCapturePopup(String.valueOf(ch)));
        });

        return container;
    }

    @Override
    public void parameters(Map<String, Object> args) {}

    @Override
    public void onMount() {
        AppContext.router.getRenderer().lock = true;
        running = true;
        wsClient = new WSClient(canvas);
        startCamera();
    }

    @Override
    public void onUnmount() {
        running = false;
        if (camera != null) camera.release();
        if (wsClient != null) wsClient.close();
        Stager.stopMediapipe();
    }

    private void showCapturePopup(String label) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Capture Dataset");

        Label text = new Label("Capture this frame for label: \"" + label + "\" ?");
        Button yes = new Button("Capture");
        Button no = new Button("Cancel");
        HBox buttons = new HBox(10, yes, no);
        buttons.setAlignment(Pos.CENTER);
        VBox root = new VBox(15, text, buttons);
        root.setAlignment(Pos.CENTER);

        root.setStyle("""
            -fx-padding: 20;
            -fx-background-color: #1e1e1e;
        """);

        text.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 14px;
        """);

        yes.setOnAction(e -> {
            captureDataset(label);
            paused = false;
            popup.close();
        });

        no.setOnAction(e -> {
            paused = false;
            popup.close();
        });

        popup.setScene(new Scene(root, 320, 140));
        popup.showAndWait();
    }

    private void captureDataset(String label) {
        try {
            File datasetDir = new File("src/main/resources/datasets");
            if (!datasetDir.exists()) datasetDir.mkdirs();

            File tempImage = Files.createTempFile("chimera_capture_", ".png").toFile();
            Imgcodecs.imwrite(tempImage.getAbsolutePath(), currentFrame);

            String landmarkData = wsClient.getLatestLandmarks();

            if (landmarkData == null || landmarkData.isBlank()) {

                System.out.println("[DATASET] No landmarks detected.");

                tempImage.delete();

                return;
            }

            float[][] matrix = Matrix.convert(landmarkData);
            float[][] normalized = Normalizer.normalize(matrix);
            float[] flat = Normalizer.flattenLandmarks(normalized);

            File csvFile = new File(datasetDir,"app-dataset.csv");
            boolean createHeader = !csvFile.exists();

            try (FileWriter writer = new FileWriter(csvFile, true)) {
                if (createHeader) {
                    for (int i = 0; i < flat.length; i++) writer.append("f").append(String.valueOf(i)).append(",");
                    writer.append("label\n");
                }

                for (float value : flat) {
                    writer.append(String.valueOf(value));
                    writer.append(",");
                }

                writer.append(label);
                writer.append("\n");
            }

            tempImage.delete();
            System.out.println("[DATASET] Saved label: " + label);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;
        byte[] data = new byte[mat.rows() * mat.cols() * (int) mat.elemSize()];
        mat.get(0, 0, data);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
        return image;
    }

    private void startCamera() {
        camera = new VideoCapture(0);
        camera.set(3, 640);
        camera.set(4, 480);

        if (!camera.isOpened()) {
            System.out.println("Camera not accessible");
            return;
        }

        new Thread(() -> {
            Mat frame = new Mat();
            while (running) {
                if (paused) {
                    try {
                        Thread.sleep(50);
                    } catch (Exception ignored) {}
                    continue;
                }

                camera.read(frame);
                if (!frame.empty()) {
                    currentFrame = frame.clone();
                    BufferedImage img = matToBufferedImage(frame);
                    WritableImage fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(img, null);
                    Platform.runLater(() -> cameraView.setImage(fxImage));
                    wsClient.sendFrame(frame);
                }

                try {
                    Thread.sleep(10);
                } catch (Exception ignored) {}
            }

        }).start();
    }
}