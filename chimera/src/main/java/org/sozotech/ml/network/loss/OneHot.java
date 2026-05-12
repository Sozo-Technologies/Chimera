package org.sozotech.ml.network.loss;

public class OneHot {

    public static float[] encode(char letter) {
        float[] target = new float[26];
        int index = Character.toUpperCase(letter) - 'A';

        if (index < 0 || index > 25) {
            throw new IllegalArgumentException("Letter must be A-Z, got: " + letter);
        }

        target[index] = 1f;
        return target;
    }

    public static char decode(float[] oneHot) {
        int best = 0;
        for (int i = 1; i < oneHot.length; i++) {
            if (oneHot[i] > oneHot[best]) best = i;
        }
        return (char) ('A' + best);
    }
}