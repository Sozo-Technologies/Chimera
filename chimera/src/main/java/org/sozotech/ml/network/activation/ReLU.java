package org.sozotech.ml.network.activation;

import org.sozotech.ml.network.ActivationFunction;

public class ReLU implements ActivationFunction {

    @Override
    public float activate(float x) {
        return Math.max(0f, x);
    }

    @Override
    public float[] activateLayer(float[] values) {
        float[] out = new float[values.length];
        for (int i = 0; i < values.length; i++) out[i] = activate(values[i]);
        return out;
    }

    @Override
    public float derivative(float z) {
        return z > 0f ? 1f : 0f;
    }
}