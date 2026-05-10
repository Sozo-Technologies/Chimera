package org.sozotech.utils.sys;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.opencv.core.Mat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.sozotech.ml.preprocess.Matrix;
import org.sozotech.ml.preprocess.Normalizer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public class WSClient implements WebSocket.Listener {

    private WebSocket socket;
    private final Canvas canvas;

    public WSClient(Canvas canvas) {

        this.canvas = canvas;

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(
                        URI.create("ws://localhost:8765"),
                        this
                )
                .thenAccept(ws -> {
                    this.socket = ws;
                    System.out.println("WebSocket connected");
                });
    }

    public void sendFrame(Mat frame) {

        if (socket == null) return;

        int w = frame.width();
        int h = frame.height();
        int c = frame.channels();

        byte[] data = new byte[w * h * c];
        frame.get(0, 0, data);

        byte[] packet = new byte[12 + data.length];

        packet[0] = (byte)(w >> 24);
        packet[1] = (byte)(w >> 16);
        packet[2] = (byte)(w >> 8);
        packet[3] = (byte)(w);

        packet[4] = (byte)(h >> 24);
        packet[5] = (byte)(h >> 16);
        packet[6] = (byte)(h >> 8);
        packet[7] = (byte)(h);

        packet[8]  = (byte)(c >> 24);
        packet[9]  = (byte)(c >> 16);
        packet[10] = (byte)(c >> 8);
        packet[11] = (byte)(c);

        System.arraycopy(data, 0, packet, 12, data.length);

        socket.sendBinary(ByteBuffer.wrap(packet), true);
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {

        try {

            String json = data.toString();

            if (json.isBlank()) {
                ws.request(1);
                return null;
            }


            JSONParser parser = new JSONParser();
            Object obj = parser.parse(json);
            JSONArray hands = (JSONArray) obj;

            Platform.runLater(() -> renderHands(hands));

        } catch (Exception _) {

        }

        ws.request(1);
        return null;
    }

    private void renderHands(JSONArray hands) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.RED);

        for (Object hObj : hands) {
            JSONArray hand = (JSONArray) hObj;

            for (Object lmObj : hand) {
                JSONObject lm = (JSONObject) lmObj;

                double x = ((Number) lm.get("x")).doubleValue() * canvas.getWidth();
                double y = ((Number) lm.get("y")).doubleValue() * canvas.getHeight();

                gc.fillOval(x, y, 8, 8);
            }
        }
    }

    private void renderHands(float[][] hands) {

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.RED);

        if (hands == null) {
            return;
        }

        for (float[] lm : hands) {
            if (lm == null || lm.length < 2) {
                continue;
            }

            double x = lm[0] * canvas.getWidth();
            double y = lm[1] * canvas.getHeight();

            gc.fillOval(x, y, 8, 8);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("WS Open");
        webSocket.request(1);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        error.printStackTrace();
    }

    public void close() {
        if (socket != null) {
            socket.abort();
        }
    }
}