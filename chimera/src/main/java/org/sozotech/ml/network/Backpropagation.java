package org.sozotech.ml.network;

import org.sozotech.ml.network.loss.CrossEntropyLoss;

public class Backpropagation {

    private final Network network;
    private final CrossEntropyLoss lossFunction;
    private final float learningRate;

    public Backpropagation(Network network, float learningRate) {
        this.network = network;
        this.lossFunction = new CrossEntropyLoss();
        this.learningRate = learningRate;
    }

    public float backward(float[] predicted, float[] target) {
        Layer[] layers = network.getLayers();
        float loss = lossFunction.compute(predicted, target);

        Layer outputLayer = layers[layers.length - 1];
        float[] outputGradient = lossFunction.gradient(predicted, target);

        for (int j = 0; j < outputLayer.size(); j++) outputLayer.neurons[j].delta = outputGradient[j];

        for (int l = layers.length - 2; l >= 1; l--) {
            Layer current = layers[l];
            Layer next = layers[l + 1];

            for (int i = 0; i < current.size(); i++) {
                float errorSum = 0f;

                for (int j = 0; j < next.size(); j++)
                    errorSum += layers[l].weights[i][j] * next.neurons[j].delta;

                current.neurons[i].delta = errorSum * current.activation.derivative(current.neurons[i].raw);
            }
        }

        for (int l = 0; l < layers.length - 1; l++) {
            Layer current = layers[l];
            Layer next = layers[l + 1];

            for (int i = 0; i < current.size(); i++)
                for (int j = 0; j < next.size(); j++)
                    current.weights[i][j] -= learningRate * current.neurons[i].value * next.neurons[j].delta;

            for (int j = 0; j < next.size(); j++)
                next.neurons[j].bias -= learningRate * next.neurons[j].delta;
        }

        return loss;
    }
}