package org.sozotech.ml.network;

public interface LossFunction {
    float compute(float[] predicted, float[] target);
    float[] gradient(float[] predicted, float[] target);
}