package org.sozotech.ml.preprocess;

public class Normalizer {

    public static float[][] normalize(float[][] input) {
        float[][] output = new float[21][3];

        if (input == null || input.length == 0) return output;

        float ox = input[0][0];
        float oy = input[0][1];
        float oz = input[0][2];

        if (ox == -1f || oy == -1f || oz == -1f) return output;

        float[][] translated = new float[21][3];

        for (int i = 0; i < 21; i++) {
            float x = input[i][0];
            float y = input[i][1];
            float z = input[i][2];

            if (x == -1f || y == -1f || z == -1f) {
                translated[i][0] = Float.NaN;
                translated[i][1] = Float.NaN;
                translated[i][2] = Float.NaN;
                continue;
            }

            translated[i][0] = x - ox;
            translated[i][1] = y - oy;
            translated[i][2] = z - oz;
        }

        float mx = translated[9][0];
        float my = translated[9][1];

        if (Float.isNaN(mx) || Float.isNaN(my)) return translated;

        float angle = (float) Math.atan2(mx, -my);
        float cos = (float) Math.cos(-angle);
        float sin = (float) Math.sin(-angle);

        float scale = (float) Math.sqrt(mx * mx + my * my);
        if (scale < 1e-6f) scale = 1f;

        for (int i = 0; i < 21; i++) {
            float rx = translated[i][0];
            float ry = translated[i][1];
            float rz = translated[i][2];

            if (Float.isNaN(rx)) {
                output[i][0] = Float.NaN;
                output[i][1] = Float.NaN;
                output[i][2] = Float.NaN;
                continue;
            }

            output[i][0] = (cos * rx - sin * ry) / scale;
            output[i][1] = (sin * rx + cos * ry) / scale;
            output[i][2] = rz / scale;
        }

        return output;
    }

    public static float[] flattenLandmarks(float[][] matrix) {
        int count = 0;

        for (int i = 0; i < 21; i++) {
            if (!Float.isNaN(matrix[i][0])) count++;
        }

        float[] flat = new float[count * 3];
        int idx = 0;

        for (int i = 0; i < 21; i++) {
            if (Float.isNaN(matrix[i][0])) continue;
            flat[idx++] = matrix[i][0];
            flat[idx++] = matrix[i][1];
            flat[idx++] = matrix[i][2];
        }

        return flat;
    }
}