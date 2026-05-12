package org.sozotech.ml.network.loss;

import org.sozotech.ml.network.LossFunction;

public class CrossEntropyLoss implements LossFunction {
    private static final float EPSILON = 1e-7f;

    @Override
    public float compute(float[] predicted, float[] target) {
        if (predicted.length != target.length) {
            throw new IllegalArgumentException(
                    "Predicted length " + predicted.length +
                            " does not match target length " + target.length
            );
        }

        float loss = 0f;
        for (int i = 0; i < predicted.length; i++) {
            float p = Math.max(EPSILON, Math.min(1f - EPSILON, predicted[i]));
            loss += target[i] * (float) Math.log(p);
        }

        return -loss;
    }

    @Override
    public float[] gradient(float[] predicted, float[] target) {
        float[] grad = new float[predicted.length];
        for (int i = 0; i < predicted.length; i++) {
            grad[i] = predicted[i] - target[i];
        }
        return grad;
    }
}