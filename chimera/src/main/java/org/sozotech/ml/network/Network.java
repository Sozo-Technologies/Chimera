package org.sozotech.ml.network;

import org.sozotech.ml.network.activation.ReLU;
import org.sozotech.ml.network.activation.Softmax;
import org.sozotech.ml.network.loss.OneHot;

import java.util.Random;

public class Network {

    private final Layer[] layers;

    public Network() {
        layers = new Layer[4];
        layers[0] = new Layer(63, null);
        layers[1] = new Layer(128, new ReLU());
        layers[2] = new Layer(64, new ReLU());
        layers[3] = new Layer(26, new Softmax());

        initWeights();
    }

    private void initWeights() {
        Random rand = new Random();
        for (int l = 0; l < layers.length - 1; l++) {
            int current = layers[l].size();
            int next = layers[l + 1].size();
            layers[l].weights = new float[current][next];
            float scale = (float) Math.sqrt(2.0 / current);
            for (int i = 0; i < current; i++)
                for (int j = 0; j < next; j++) layers[l].weights[i][j] = (float) rand.nextGaussian() * scale;
        }
    }

    public float[] forward(float[] input) {
        if (input.length != 63) throw new IllegalArgumentException("Expected 63 inputs, got " + input.length);
        for (int i = 0; i < 63; i++) layers[0].neurons[i].value = input[i];
        for (int l = 0; l < layers.length - 1; l++) {
            Layer current = layers[l];
            Layer next = layers[l + 1];
            float[] rawValues = new float[next.size()];

            for (int j = 0; j < next.size(); j++) {
                float sum = next.neurons[j].bias;
                for (int i = 0; i < current.size(); i++) sum += current.neurons[i].value * current.weights[i][j];
                next.neurons[j].raw = sum;
                rawValues[j] = sum;
            }

            float[] activated = next.activation.activateLayer(rawValues);
            for (int j = 0; j < next.size(); j++) next.neurons[j].value = activated[j];
        }

        return layers[layers.length - 1].getValues();
    }

    public char predict(float[] input) {
        float[] output = forward(input);
        return OneHot.decode(output);
    }

    public Layer[] getLayers() {
        return layers;
    }
}