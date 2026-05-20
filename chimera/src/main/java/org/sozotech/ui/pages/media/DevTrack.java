package org.sozotech.ui.pages.media;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import org.sozotech.ml.preprocess.Matrix;
import org.sozotech.ml.preprocess.Normalizer;
import org.sozotech.stager.Stager;
import org.sozotech.system.WSClient;
import org.sozotech.utils.core.AppContext;
import org.sozotech.utils.core.Terminal;
import org.sozotech.utils.page.PageComponent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.sozotech.ui.pages.media.HandTrack.matToBufferedImage;

public class DevTrack extends PageComponent {
    private ImageView cameraView;
    private Canvas overlayCanvas;

    private ImageView previewImage;
    private Canvas previewCanvas;
    private Label previewLabel;

    private VideoCapture camera;
    private WSClient wsClient;

    private volatile boolean running = false;
    private volatile boolean paused  = false;
    private Mat currentFrame;

    @Override
    protected Parent createView() {

        cameraView = new ImageView();
        overlayCanvas = new Canvas(640, 480);
        StackPane livePane = new StackPane(cameraView, overlayCanvas);
        livePane.setPrefSize(640, 480);

        Label liveLabel = new Label("Live Feed");
        liveLabel.setStyle("-fx-text-fill:#aaa;-fx-font-size:12px;");
        VBox leftBox = new VBox(4, liveLabel, livePane);
        leftBox.setAlignment(Pos.TOP_CENTER);

        previewImage  = new ImageView();
        previewImage.setFitWidth(640);
        previewImage.setFitHeight(480);
        previewImage.setPreserveRatio(true);

        previewCanvas = new Canvas(640, 480);
        StackPane previewPane = new StackPane(previewImage, previewCanvas);
        previewPane.setPrefSize(640, 480);
        previewPane.setStyle("-fx-background-color:#111;");

        previewLabel = new Label("No capture yet");
        previewLabel.setStyle("-fx-text-fill:#aaa;-fx-font-size:12px;");

        VBox rightBox = new VBox(4, previewLabel, previewPane);
        rightBox.setAlignment(Pos.TOP_CENTER);

        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);

        HBox panels = new HBox(12, leftBox, sep, rightBox);
        panels.setPadding(new Insets(10));
        panels.setAlignment(Pos.CENTER);

        Button backButton = new Button("← Back");
        backButton.setStyle("""
            -fx-background-color:rgba(0,0,0,0.6);
            -fx-text-fill:white;
            -fx-font-size:14px;
            -fx-background-radius:8;
        """);
        backButton.setOnAction(e ->
                AppContext.router.navigate("/debug", Map.of("recent-page", "/home")));

        AnchorPane overlay = new AnchorPane(backButton);
        AnchorPane.setTopAnchor(backButton, 10.0);
        AnchorPane.setLeftAnchor(backButton, 10.0);

        StackPane root = getStackPane(panels, overlay);

        Platform.runLater(root::requestFocus);
        return root;
    }

    private StackPane getStackPane(HBox panels, AnchorPane overlay) {
        StackPane root = new StackPane(panels, overlay);
        root.setFocusTraversable(true);
        root.setOnKeyPressed(event -> {
            String key = event.getText();
            if (key == null || key.isBlank()) return;
            char ch = Character.toLowerCase(key.charAt(0));
            if (!Character.isLetter(ch)) return;
            if (currentFrame == null || currentFrame.empty()) return;
            if (paused) return;

            paused = true;
            Mat frozen = currentFrame.clone();
            Platform.runLater(() -> showCapturePopup(String.valueOf(ch), frozen));
        });
        return root;
    }

    @Override public void parameters(Map<String, Object> args) {}

    @Override
    public void onMount() {
        AppContext.router.getRenderer().lock = true;
        running = true;

        new Thread(() -> {
            boolean started = Stager.runMediapipe();

            if (!started) {
                System.err.println("[DevTrack] MediaPipe failed to start.");
                return;
            }

            try { Thread.sleep(500); } catch (Exception ignored) {}

            wsClient = new WSClient(overlayCanvas);
            startCamera();

        }).start();
    }

    @Override
    public void onUnmount() {
        running = false;
        if (camera   != null) camera.release();
        if (wsClient != null) wsClient.close();
        Stager.stopMediapipe();
    }

    private void showCapturePopup(String label, Mat frozen) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Capture Dataset");

        Label text = new Label("Capture frame for label: \"" + label + "\" ?");
        Button yes = new Button("Capture");
        Button no = new Button("Cancel");

        HBox buttons = new HBox(10, yes, no);
        buttons.setAlignment(Pos.CENTER);
        VBox root = new VBox(15, text, buttons);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding:20;-fx-background-color:#1e1e1e;");
        text.setStyle("-fx-text-fill:white;-fx-font-size:14px;");

        yes.setOnAction(e -> {
            popup.close();
            new Thread(() -> captureDataset(label, frozen)).start();
        });

        no.setOnAction(e -> {
            paused = false;
            popup.close();
        });

        popup.setScene(new Scene(root, 320, 140));
        popup.showAndWait();
    }

    private void captureDataset(String label, Mat frozen) {
        try {
            MatOfByte jpegBuf = new MatOfByte();
            Imgcodecs.imencode(".jpg", frozen, jpegBuf);
            byte[] jpegBytes = jpegBuf.toArray();

            Mat decoded = Imgcodecs.imdecode(new MatOfByte(jpegBytes), Imgcodecs.IMREAD_COLOR);
            int w = decoded.width(), h = decoded.height(), c = decoded.channels();
            byte[] rawPixels = new byte[w * h * c];
            decoded.get(0, 0, rawPixels);

            byte[] packet = buildPacket(w, h, c, rawPixels);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> responseRef = new AtomicReference<>();

            wsClient.sendOnce(packet, response -> {
                responseRef.set(response);
                latch.countDown();
            });

            boolean received = latch.await(3, TimeUnit.SECONDS);

            if (!received || responseRef.get() == null) {
                Terminal.error("[DATASET] No response from MediaPipe.");
                Platform.runLater(() -> paused = false);
                return;
            }

            String landmarkJson = responseRef.get();

            JSONParser parser = new JSONParser();
            JSONArray hands = (JSONArray) parser.parse(landmarkJson);

            if (hands == null || hands.isEmpty()) {
                Terminal.error("[DATASET] No hands detected in captured frame.");
                Platform.runLater(() -> paused = false);
                return;
            }

            JSONArray firstHand = (JSONArray) hands.getFirst();

            float[][] matrix     = Matrix.convert(firstHand);
            float[][] normalised = Normalizer.normalize(matrix);
            float[]   flat       = Normalizer.flattenLandmarks(normalised);

            saveCSV(label, flat);

            Image fxPreview = new Image(new ByteArrayInputStream(jpegBytes));
            Platform.runLater(() -> {
                updatePreviewPanel(fxPreview, firstHand, label, flat.length / 3);
                paused = false;
            });

        } catch (Exception ex) {
            Terminal.error("[DATASET] Capture failed: " + ex.getMessage());
            Platform.runLater(() -> paused = false);
        }
    }


    private void updatePreviewPanel(Image img, JSONArray hand, String label, int points) {
        previewImage.setImage(img);
        previewLabel.setText("Label: " + label.toUpperCase() + "   |   " + points + " landmarks");

        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, previewCanvas.getWidth(), previewCanvas.getHeight());
        gc.setFill(Color.LIME);

        double scaleX = previewCanvas.getWidth();
        double scaleY = previewCanvas.getHeight();

        for (Object obj : hand) {
            JSONObject lm = (JSONObject) obj;
            double x = ((Number) lm.get("x")).doubleValue() * scaleX;
            double y = ((Number) lm.get("y")).doubleValue() * scaleY;
            gc.fillOval(x - 4, y - 4, 8, 8);
        }
    }

    private static byte[] buildPacket(int w, int h, int c, byte[] pixels) {
        byte[] packet = new byte[12 + pixels.length];
        packet[0] = (byte)(w >> 24); packet[1] = (byte)(w >> 16);
        packet[2] = (byte)(w >>  8); packet[3] = (byte)(w);
        packet[4] = (byte)(h >> 24); packet[5] = (byte)(h >> 16);
        packet[6] = (byte)(h >>  8); packet[7] = (byte)(h);
        packet[8] = (byte)(c >> 24); packet[9] = (byte)(c >> 16);
        packet[10] = (byte)(c >>  8); packet[11]= (byte)(c);
        System.arraycopy(pixels, 0, packet, 12, pixels.length);
        return packet;
    }

    private void saveCSV(String label, float[] flat) throws IOException {
        File dir = new File("src/main/resources/model/datasets");
        if (!dir.exists()) dir.mkdirs();

        File csv = new File(dir, "app_dataset.csv");
        boolean writeHeader = !csv.exists();

        try (FileWriter w = new FileWriter(csv, true)) {
            if (writeHeader) {
                for (int i = 0; i < flat.length; i++) w.write("f" + i + ",");
                w.write("label\n");
            }
            for (float v : flat) w.write(v + ",");
            w.write(label + "\n");
        }

        System.out.println("[DATASET] Saved — label: " + label + "  pts: " + flat.length / 3);
    }

    private void startCamera() {
        camera = new VideoCapture();

        boolean opened = false;

        for (int attempt = 1; attempt <= 3; attempt++) {
            if (camera.open(0) && camera.isOpened()) {
                opened = true;
                break;
            }

            System.out.println("[Camera] Retry " + attempt + "/3 failed");
        }

        if (!opened) {
            System.out.println("Camera not accessible");
            return;
        }

        camera.set(3, 640);
        camera.set(4, 480);
        ScheduledExecutorService cameraExecutor = Executors.newSingleThreadScheduledExecutor();
        final long frameIntervalMs = 33;
        cameraExecutor.scheduleAtFixedRate(() -> {
            if (!running || paused) return;
            Mat frame = new Mat();
            camera.read(frame);

            if (!frame.empty()) {
                currentFrame = frame.clone();
                var img = matToBufferedImage(frame);
                WritableImage fx = javafx.embed.swing.SwingFXUtils.toFXImage(img, null);
                Platform.runLater(() -> cameraView.setImage(fx));
                wsClient.sendFrame(frame);
            }
        }, 0, frameIntervalMs, TimeUnit.MILLISECONDS);
    }
}