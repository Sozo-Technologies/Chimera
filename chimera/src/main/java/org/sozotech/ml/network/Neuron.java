package org.sozotech.ml.network;

public class Neuron {
    public float value;
    public float raw;
    public float bias;
    public float delta;

    public Neuron(float bias) {
        this.bias = bias;
        this.value = 0f;
        this.raw = 0f;
        this.delta = 0f;
    }
}