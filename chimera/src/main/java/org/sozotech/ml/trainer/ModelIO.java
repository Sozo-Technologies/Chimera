package org.sozotech.ml.trainer;

import org.sozotech.ml.network.Layer;
import org.sozotech.ml.network.Network;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;

public class ModelIO {

    private static final String MODEL_RESOURCE = "model/model.csv";

    public static boolean exists() {
        URL url = ModelIO.class.getClassLoader().getResource(MODEL_RESOURCE);
        return url != null;
    }

    public static void load(Network network) {
        URL url = ModelIO.class.getClassLoader().getResource(MODEL_RESOURCE);
        if (url == null) {
            System.out.println("[ModelIO] No saved model found.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(url.toURI().getPath()))) {
            Layer[] layers = network.getLayers();
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",");

                if (parts[0].equals("W")) {
                    int l = Integer.parseInt(parts[1].trim());
                    int i = Integer.parseInt(parts[2].trim());
                    int j = Integer.parseInt(parts[3].trim());
                    float v = Float.parseFloat(parts[4].trim());
                    layers[l].weights[i][j] = v;

                } else if (parts[0].equals("B")) {
                    int l = Integer.parseInt(parts[1].trim());
                    int n = Integer.parseInt(parts[2].trim());
                    layers[l].neurons[n].bias = Float.parseFloat(parts[3].trim());
                }
            }

            System.out.println("[ModelIO] Model loaded from: " + MODEL_RESOURCE);

        } catch (IOException | URISyntaxException e) {
            System.out.println("[ModelIO] Failed to load model: " + e.getMessage());
        }
    }

    public static void save(Network network) {
        try {
            Path path = resolveWritablePath();
            Files.createDirectories(path.getParent());

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(path.toFile()))) {
                Layer[] layers = network.getLayers();

                bw.write("# Chimera Neural Network - Saved Model");
                bw.newLine();
                bw.write("# W, layer, from, to, weight");
                bw.newLine();
                bw.write("# B, layer, neuron, bias");
                bw.newLine();

                for (int l = 0; l < layers.length - 1; l++) {
                    Layer layer = layers[l];
                    for (int i = 0; i < layer.size(); i++) {
                        for (int j = 0; j < layers[l + 1].size(); j++) {
                            bw.write(String.format("W,%d,%d,%d,%.8f", l, i, j, layer.weights[i][j]));
                            bw.newLine();
                        }
                    }
                }

                for (int l = 1; l < layers.length; l++) {
                    Layer layer = layers[l];
                    for (int n = 0; n < layer.size(); n++) {
                        bw.write(String.format("B,%d,%d,%.8f", l, n, layer.neurons[n].bias));
                        bw.newLine();
                    }
                }
            }

            System.out.println("[ModelIO] Model saved to: " + path);

        } catch (IOException e) {
            System.out.println("[ModelIO] Failed to save model: " + e.getMessage());
        }
    }

    private static Path resolveWritablePath() {
        return Paths.get("src/main/resources/model/model.csv")
                .toAbsolutePath();
    }
}