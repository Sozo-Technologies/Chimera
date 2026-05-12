package org.sozotech.ml.preprocess;

import java.util.ArrayList;
import java.util.List;

public class Normalizer {

    public static float[][] normalize(float[][] input) {

        float[][] output = new float[21][3];

        if (input == null || input.length == 0) {
            return output;
        }

        float originX = input[0][0];
        float originY = input[0][1];
        float originZ = input[0][2];

        if (originX == -1f || originY == -1f || originZ == -1f) {
            return output;
        }

        for (int i = 0; i < 21; i++) {

            float x = input[i][0];
            float y = input[i][1];
            float z = input[i][2];

            if (x == -1f || y == -1f || z == -1f) {
                output[i][0] = Float.NaN;
                output[i][1] = Float.NaN;
                output[i][2] = Float.NaN;
                continue;
            }

            output[i][0] = x - originX;
            output[i][1] = y - originY;
            output[i][2] = z - originZ;
        }

        return output;
    }

    public static float[] flattenLandmarks(float[][] matrix) {
        List<Float> values = new ArrayList<>();

        for (int i = 0; i < 21; i++) {
            float x = matrix[i][0];
            float y = matrix[i][1];
            float z = matrix[i][2];

            if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) continue;

            values.add(x);
            values.add(y);
            values.add(z);
        }

        float[] flat = new float[values.size()];
        for (int i = 0; i < flat.length; i++) flat[i] = values.get(i);
        return flat;
    }
}