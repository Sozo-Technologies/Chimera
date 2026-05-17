package org.sozotech.ui.pages.media;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.AnchorPane;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import org.sozotech.stager.Stager;
import org.sozotech.system.WSClient;
import org.sozotech.utils.core.Terminal;
import org.sozotech.utils.page.PageComponent;

import org.sozotech.utils.core.AppContext;

import java.awt.image.BufferedImage;
import java.util.Map;

public class HandTrack extends PageComponent {

    private ImageView cameraView;
    private Canvas canvas;

    private VideoCapture camera;
    private WSClient wsClient;

    private volatile boolean running = false;

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

        backButton.setOnAction(e -> {
            AppContext.router.navigate("/home");
        });

        AnchorPane overlay = new AnchorPane(backButton);
        AnchorPane.setTopAnchor(backButton, 10.0);
        AnchorPane.setLeftAnchor(backButton, 10.0);

        return new StackPane(cameraView, canvas, overlay);
    }

    @Override
    public void parameters(Map<String, Object> args) {}

    @Override
    public void onMount() {
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

    public static BufferedImage matToBufferedImage(Mat mat) {
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
                camera.read(frame);
                if (!frame.empty()) {
                    BufferedImage img = matToBufferedImage(frame);
                    WritableImage fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(img, null);
                    Platform.runLater(() -> {
                        cameraView.setImage(fxImage);
                    });

                    wsClient.sendFrame(frame);
                }

                try {
                    Thread.sleep(10);
                } catch (Exception ignored) { Terminal.error("[DATASET] Sleep interrupted"); }
            }
        }).start();
    }
}