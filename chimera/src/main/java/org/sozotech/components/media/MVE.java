package org.sozotech.components.media;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public class MVE extends ImageView {

    private WebSocket ws;
    private volatile boolean connected = false;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public MVE() {
        connectToPython();
    }

    private void connectToPython() {

        HttpClient client = HttpClient.newHttpClient();

        ws = client.newWebSocketBuilder()
                .buildAsync(
                        URI.create("ws://localhost:8765/viewer"),
                        new WebSocket.Listener() {

                            @Override
                            public void onOpen(WebSocket webSocket) {
                                System.out.println("🟢 Connected to Python Viewer Stream");
                                connected = true;
                                WebSocket.Listener.super.onOpen(webSocket);
                            }

                            @Override
                            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {

                                byte[] chunk = new byte[data.remaining()];
                                data.get(chunk);

                                try {
                                    buffer.write(chunk);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }

                                if (last) {
                                    byte[] fullImage = buffer.toByteArray();
                                    buffer.reset();

                                    renderFrame(fullImage);
                                }

                                return WebSocket.Listener.super.onBinary(webSocket, data, last);
                            }

                            @Override
                            public void onError(WebSocket webSocket, Throwable error) {
                                System.out.println("❌ MVE WebSocket Error");
                            }

                            @Override
                            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                                System.out.println("🔴 Disconnected from Python: " + reason);
                                connected = false;
                                return null;
                            }
                        }
                ).join();
    }

    private void renderFrame(byte[] imageBytes) {
        try {
            Image img = new Image(new ByteArrayInputStream(imageBytes));

            Platform.runLater(() -> setImage(img));

        } catch (Exception e) {
            System.out.println("Frame render error:");
        }
    }

    public void send(byte[] data) {
        if (ws != null && connected) {
            ws.sendBinary(ByteBuffer.wrap(data), true);
        }
    }

    public void stop() {
        try {
            if (ws != null) ws.abort();
            connected = false;
        } catch (Exception _) {
        }
    }
}