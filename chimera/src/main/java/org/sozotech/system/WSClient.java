package org.sozotech.system;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.opencv.core.Mat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.sozotech.ml.preprocess.HandData;
import org.sozotech.ml.preprocess.Matrix;
import org.sozotech.ml.translation.TranslationUnit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WSClient implements WebSocket.Listener {

    private WebSocket socket;
    private final Canvas canvas;
    private final TranslationUnit translator;

    private volatile String latestJson;
    private volatile Consumer<String> oneShotCallback = null;
    private final Object callbackLock = new Object();

    private final CountDownLatch readyLatch = new CountDownLatch(1);

    public WSClient(Canvas canvas, TranslationUnit translator) {
        this.canvas = canvas;
        this.translator = translator;
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:8765"), this)
                .thenAccept(ws -> {
                    this.socket = ws;
                    readyLatch.countDown();
                    System.out.println("WebSocket connected");
                })
                .exceptionally(ex -> {
                    System.err.println("[WSClient] Connection failed: " + ex.getMessage());
                    readyLatch.countDown();
                    return null;
                });
    }

    private boolean awaitReady() {
        try {
            return !readyLatch.await(5, TimeUnit.SECONDS) || socket == null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    public void sendFrame(Mat frame) {
        if (awaitReady()) return;
        int w = frame.width(), h = frame.height(), c = frame.channels();
        byte[] data = new byte[w * h * c];
        frame.get(0, 0, data);
        socket.sendBinary(ByteBuffer.wrap(buildPacket(w, h, c, data)), true);
    }

    public void sendOnce(byte[] packet, Consumer<String> callback) {
        if (awaitReady()) {
            System.err.println("[WSClient] sendOnce: socket not ready");
            callback.accept(null);
            return;
        }
        synchronized (callbackLock) {
            oneShotCallback = callback;
        }
        socket.sendBinary(ByteBuffer.wrap(packet), true);
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        try {
            String json = data.toString();
            if (json.isBlank()) { ws.request(1); return null; }

            latestJson = json;

            Consumer<String> cb;
            synchronized (callbackLock) {
                cb = oneShotCallback;
                oneShotCallback = null;
            }

            if (cb != null) {
                cb.accept(json);
                ws.request(1);
                return null;
            }

            HandData hand = Matrix.parse(json);
            translator.feedFrame(hand);

            if (hand.isPresent()) {
                JSONArray lmArray = extractLandmarkArray(json);
                if (lmArray != null) Platform.runLater(() -> renderHands(lmArray));
            } else {
                Platform.runLater(() -> clearCanvas());
            }

        } catch (Exception ignored) {}

        ws.request(1);
        return null;
    }

    private JSONArray extractLandmarkArray(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            return (JSONArray) root.get("landmarks");
        } catch (Exception e) {
            return null;
        }
    }

    private void clearCanvas() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void renderHands(JSONArray landmarks) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.RED);
        for (Object lmObj : landmarks) {
            JSONObject lm = (JSONObject) lmObj;
            double x = ((Number) lm.get("x")).doubleValue() * canvas.getWidth();
            double y = ((Number) lm.get("y")).doubleValue() * canvas.getHeight();
            gc.fillOval(x, y, 8, 8);
        }
    }

    public String getLatestJson() {
        return latestJson;
    }

    private static byte[] buildPacket(int w, int h, int c, byte[] pixels) {
        byte[] packet = new byte[12 + pixels.length];
        packet[0]  = (byte)(w >> 24); packet[1]  = (byte)(w >> 16);
        packet[2]  = (byte)(w >>  8); packet[3]  = (byte) w;
        packet[4]  = (byte)(h >> 24); packet[5]  = (byte)(h >> 16);
        packet[6]  = (byte)(h >>  8); packet[7]  = (byte) h;
        packet[8]  = (byte)(c >> 24); packet[9]  = (byte)(c >> 16);
        packet[10] = (byte)(c >>  8); packet[11] = (byte) c;
        System.arraycopy(pixels, 0, packet, 12, pixels.length);
        return packet;
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
        if (socket != null) socket.abort();
    }
}