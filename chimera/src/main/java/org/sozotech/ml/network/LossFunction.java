package org.sozotech.ml.network;

public interface LossFunction {
    // computes the total loss between prediction and target
    float compute(float[] predicted, float[] target);

    // computes the gradient (dLoss/dOutput) for backpropagation
    float[] gradient(float[] predicted, float[] target);
}