package org.sozotech.ml.network;

public interface ActivationFunction {
    float activate(float x);
    float[] activateLayer(float[] values);
    float derivative(float z);
}
