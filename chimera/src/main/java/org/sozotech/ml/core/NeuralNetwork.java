package org.sozotech.ml.core;

import org.sozotech.ml.network.Network;
import org.sozotech.ml.preprocess.Normalizer;
import org.sozotech.ml.trainer.Trainer;

public class NeuralNetwork {

    private static NeuralNetwork instance;

    private final Trainer trainer;
    private final Network network;

    private NeuralNetwork() {
        this.trainer = new Trainer();
        this.network = trainer.getNetwork();
    }

    public static NeuralNetwork getInstance() {
        if (instance == null) {
            instance = new NeuralNetwork();
        }
        return instance;
    }

    public void start() {
        trainer.start();
    }

    public void shutdown() {
        trainer.stop();
    }

    public char predict(float[][] landmarks) {
        if (isComplete(landmarks)) return '?';

        float[][] normalized = Normalizer.normalize(landmarks);
        float[]   flat       = Normalizer.flattenLandmarks(normalized);

        if (flat.length != 63) return '?';

        return network.predict(flat);
    }

    public PredictionResult predictWithConfidence(float[][] landmarks) {
        if (isComplete(landmarks)) return new PredictionResult('?', 0f);

        float[][] normalized = Normalizer.normalize(landmarks);
        float[]   flat       = Normalizer.flattenLandmarks(normalized);

        if (flat.length != 63) return new PredictionResult('?', 0f);

        float[] output = network.forward(flat);

        int best = 0;
        for (int i = 1; i < output.length; i++) {
            if (output[i] > output[best]) best = i;
        }

        char  letter     = (char) ('A' + best);
        float confidence = output[best];

        return new PredictionResult(letter, confidence);
    }

    public boolean isComplete(float[][] landmarks) {
        if (landmarks == null || landmarks.length != 21) return true;

        for (int i = 0; i < 21; i++) {
            if (landmarks[i] == null || landmarks[i].length != 3) return true;

            float x = landmarks[i][0];
            float y = landmarks[i][1];
            float z = landmarks[i][2];

            if (x == -1f || y == -1f || z == -1f) return true;
            if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) return true;
        }

        return false;
    }

    public boolean isReady() {
        return network != null;
    }

    public Network getNetwork() {
        return network;
    }

    public record PredictionResult(char letter, float confidence) {

        @Override
            public String toString() {
                return String.format("%s (%.1f%%)", letter, confidence * 100f);
            }
        }

    public EstimateResult estimateModel() {
        return trainer.getEstimate();
    }

    public record EstimateResult(int currentEpoch, float currentLoss, float currentAccuracy, long elapsedMs, long estimatedMsPerEpoch) {

        public String elapsedFormatted() {
                return formatDuration(elapsedMs);
            }

            public String msPerEpochFormatted() {
                return estimatedMsPerEpoch + "ms/epoch";
            }

            private static String formatDuration(long ms) {
                long seconds = ms / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                return String.format("%02dh %02dm %02ds", hours, minutes % 60, seconds % 60);
            }

            @Override
            public String toString() {
                return String.format(
                        "Epoch: %d | Loss: %.5f | Accuracy: %.2f%% | Elapsed: %s | Speed: %s",
                        currentEpoch, currentLoss, currentAccuracy,
                        elapsedFormatted(), msPerEpochFormatted()
                );
            }
        }
}