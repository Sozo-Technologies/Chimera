package org.sozotech.ml.preprocess;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Matrix {

    public static HandData parse(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) return HandData.empty();

        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);

            JSONArray lmArr  = (JSONArray) root.get("landmarks");
            JSONArray wlArr  = (JSONArray) root.get("world");
            JSONArray visArr = (JSONArray) root.get("visibility");

            if (lmArr == null || wlArr == null || visArr == null) return HandData.empty();

            float[][] landmarks  = extractMatrix(lmArr);
            float[][] world      = extractMatrix(wlArr);
            float[] visibility   = extractVisibility(visArr);

            return new HandData(landmarks, world, visibility);

        } catch (Exception e) {
            return HandData.empty();
        }
    }

    public static float[][] convert(String json) {
        return parse(json).landmarks();
    }

    public static float[][] convert(JSONArray landmarks) {
        float[][] buffer = new float[21][3];

        for (int i = 0; i < 21; i++) {
            buffer[i][0] = -1f;
            buffer[i][1] = -1f;
            buffer[i][2] = -1f;
        }

        if (landmarks == null || landmarks.isEmpty()) return buffer;

        int size = Math.min(landmarks.size(), 21);

        for (int i = 0; i < size; i++) {
            Object obj = landmarks.get(i);
            if (!(obj instanceof JSONObject point)) continue;

            Object xObj = point.get("x");
            Object yObj = point.get("y");
            Object zObj = point.get("z");

            if (xObj instanceof Number) buffer[i][0] = ((Number) xObj).floatValue();
            if (yObj instanceof Number) buffer[i][1] = ((Number) yObj).floatValue();
            if (zObj instanceof Number) buffer[i][2] = ((Number) zObj).floatValue();
        }

        return buffer;
    }

    private static float[][] extractMatrix(JSONArray arr) {
        float[][] buffer = new float[21][3];

        for (int i = 0; i < 21; i++) {
            buffer[i][0] = -1f;
            buffer[i][1] = -1f;
            buffer[i][2] = -1f;
        }

        int size = Math.min(arr.size(), 21);

        for (int i = 0; i < size; i++) {
            Object obj = arr.get(i);
            if (!(obj instanceof JSONObject point)) continue;

            Object xObj = point.get("x");
            Object yObj = point.get("y");
            Object zObj = point.get("z");

            if (xObj instanceof Number) buffer[i][0] = ((Number) xObj).floatValue();
            if (yObj instanceof Number) buffer[i][1] = ((Number) yObj).floatValue();
            if (zObj instanceof Number) buffer[i][2] = ((Number) zObj).floatValue();
        }

        return buffer;
    }

    private static float[] extractVisibility(JSONArray arr) {
        float[] buffer = new float[21];
        int size = Math.min(arr.size(), 21);

        for (int i = 0; i < size; i++) {
            Object v = arr.get(i);
            if (v instanceof Number) buffer[i] = ((Number) v).floatValue();
        }

        return buffer;
    }
}