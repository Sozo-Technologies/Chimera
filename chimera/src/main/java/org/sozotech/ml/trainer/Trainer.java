package org.sozotech.ml.trainer;

import org.sozotech.ml.core.NeuralNetwork;
import org.sozotech.ml.network.Backpropagation;
import org.sozotech.ml.network.Network;
import org.sozotech.ml.network.loss.OneHot;

import java.util.*;

public class Trainer {

    private static final float LEARNING_RATE = 0.01f;
    private static final int LOG_EVERY = 1;

    private final Network network;
    private final Backpropagation backprop;
    private Thread trainingThread;
    private volatile boolean running = false;

    private volatile int   currentEpoch    = 0;
    private volatile float currentLoss     = 0f;
    private volatile float currentAccuracy = 0f;
    private volatile long  startTimeMs     = 0L;
    private volatile long  lastEpochMs     = 0L;

    public Trainer() {
        this.network  = new Network();
        this.backprop = new Backpropagation(network, LEARNING_RATE);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Trainer] Shutdown detected. Saving model...");
            stop();
            ModelIO.save(network);
            System.out.println("[Trainer] Model saved. Goodbye.");
        }));
    }

    public void start() {
        System.out.println("[Trainer] Starting...");

        if (ModelIO.exists()) {
            System.out.println("[Trainer] Saved model found. Loading weights...");
            ModelIO.load(network);
        } else {
            System.out.println("[Trainer] No saved model. Starting fresh.");
        }

        Map<Character, List<float[]>> dataset = DatasetReader.load();

        if (dataset.isEmpty()) {
            System.out.println("[Trainer] Dataset is empty. Training skipped.");
            return;
        }

        List<float[]>   inputs = new ArrayList<>();
        List<Character> labels = new ArrayList<>();

        for (Map.Entry<Character, List<float[]>> entry : dataset.entrySet()) {
            for (float[] sample : entry.getValue()) {
                inputs.add(sample);
                labels.add(entry.getKey());
            }
        }

        running = true;
        trainingThread = new Thread(() -> trainLoop(inputs, labels), "chimera-trainer");
        trainingThread.setDaemon(true);
        trainingThread.start();

        System.out.println("[Trainer] Training started in background.");
    }

    public void stop() {
        running = false;
        if (trainingThread != null) {
            trainingThread.interrupt();
            try {
                trainingThread.join(2000);
            } catch (InterruptedException ignored) {}
        }
    }

    private void trainLoop(List<float[]> inputs, List<Character> labels) {
        startTimeMs = System.currentTimeMillis();
        int epoch = 0;

        while (running && !Thread.currentThread().isInterrupted()) {
            epoch++;
            long epochStart = System.currentTimeMillis();

            shufflePairs(inputs, labels);

            float totalLoss = 0f;
            int   correct   = 0;

            for (int i = 0; i < inputs.size(); i++) {
                if (!running) break;

                float[] input     = inputs.get(i);
                char    label     = labels.get(i);
                float[] target    = OneHot.encode(label);
                float[] predicted = network.forward(input);

                float loss = backprop.backward(predicted, target);
                totalLoss += loss;

                if (network.predict(input) == Character.toUpperCase(label)) correct++;
            }

            currentEpoch    = epoch;
            currentLoss     = totalLoss / inputs.size();
            currentAccuracy = (correct / (float) inputs.size()) * 100f;
            lastEpochMs     = System.currentTimeMillis() - epochStart;

            System.out.printf(
                    "[Trainer] Epoch %4d | Loss: %.5f | Accuracy: %.2f%%%n",
                    currentEpoch, currentLoss, currentAccuracy
            );
        }

        System.out.println("[Trainer] Training loop exited.");
    }

    private void shufflePairs(List<float[]> inputs, List<Character> labels) {
        Random rand = new Random();
        for (int i = inputs.size() - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);

            float[] tmpInput = inputs.get(i);
            inputs.set(i, inputs.get(j));
            inputs.set(j, tmpInput);

            char tmpLabel = labels.get(i);
            labels.set(i, labels.get(j));
            labels.set(j, tmpLabel);
        }
    }

    public Network getNetwork() {
        return network;
    }

    public NeuralNetwork.EstimateResult getEstimate() {
        long elapsed = startTimeMs == 0 ? 0 : System.currentTimeMillis() - startTimeMs;
        return new NeuralNetwork.EstimateResult(
                currentEpoch,
                currentLoss,
                currentAccuracy,
                elapsed,
                lastEpochMs
        );
    }
}