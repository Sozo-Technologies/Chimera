package org.sozotech.ml.preprocess;

public record HandData(float[][] landmarks, float[][] world, float[] visibility) {

    public static HandData empty() {
        float[][] lm = new float[21][3];
        float[][] wl = new float[21][3];
        float[] vis  = new float[21];

        for (int i = 0; i < 21; i++) {
            lm[i][0] = -1f; lm[i][1] = -1f; lm[i][2] = -1f;
            wl[i][0] = -1f; wl[i][1] = -1f; wl[i][2] = -1f;
        }

        return new HandData(lm, wl, vis);
    }

    public boolean isPresent() {
        return landmarks[0][0] != -1f && !Float.isNaN(landmarks[0][0]);
    }

    public boolean isLandmarkVisible(int id, float threshold) {
        return visibility[id] >= threshold;
    }
}