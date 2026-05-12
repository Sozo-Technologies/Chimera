package org.sozotech.ml.network.activation;

import org.sozotech.ml.network.ActivationFunction;

public class Sigmoid implements ActivationFunction {

    @Override
    public float activate(float x) {
        return 1f / (1f + (float) Math.exp(-x));
    }

    @Override
    public float[] activateLayer(float[] values) {
        float[] out = new float[values.length];
        for (int i = 0; i < values.length; i++) out[i] = activate(values[i]);
        return out;
    }

    @Override
    public float derivative(float z) {
        float s = activate(z);
        return s * (1f - s);
    }
}