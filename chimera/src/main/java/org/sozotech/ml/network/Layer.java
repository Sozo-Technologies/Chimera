package org.sozotech.ml.network;

public class Layer {
    public Neuron[] neurons;
    public ActivationFunction activation;
    public float[][] weights;

    public Layer(int size, ActivationFunction activation) {
        this.neurons = new Neuron[size];
        this.activation = activation;
        for (int i = 0; i < size; i++) {
            this.neurons[i] = new Neuron(0f);
        }
    }

    public int size() {
        return neurons.length;
    }

    public float[] getValues() {
        float[] out = new float[neurons.length];
        for (int i = 0; i < neurons.length; i++) out[i] = neurons[i].value;
        return out;
    }
}