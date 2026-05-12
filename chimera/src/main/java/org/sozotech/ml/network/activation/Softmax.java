package org.sozotech.ml.network.activation;

import org.sozotech.ml.network.ActivationFunction;

public class Softmax implements ActivationFunction {

    @Override
    public float activate(float x) {
        throw new UnsupportedOperationException("Softmax requires full layer.");
    }

    @Override
    public float[] activateLayer(float[] values) {
        float max = values[0];
        for (float v : values) if (v > max) max = v;

        float sum = 0f;
        float[] exps = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            exps[i] = (float) Math.exp(values[i] - max);
            sum += exps[i];
        }

        float[] out = new float[values.length];
        for (int i = 0; i < values.length; i++) out[i] = exps[i] / sum;
        return out;
    }

    @Override
    public float derivative(float z) {
        return 1f;
    }
}