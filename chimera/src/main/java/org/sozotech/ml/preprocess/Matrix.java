package org.sozotech.ml.preprocess;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Matrix {
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

    public static float[][] convert(String data) {

        float[][] buffer = new float[21][3];

        for (int i = 0; i < 21; i++) {
            buffer[i][0] = -1f;
            buffer[i][1] = -1f;
            buffer[i][2] = -1f;
        }

        if (data == null || data.length() < 10) {
            return buffer;
        }

        int index = -1;
        int len = data.length();

        float x = -1f, y = -1f, z = -1f;

        StringBuilder num = new StringBuilder(16);
        char key = 0;

        for (int i = 0; i < len; i++) {

            char c = data.charAt(i);

            if (c == '{') {
                index++;
                x = y = z = -1f;
            }

            if (c == 'x') key = 1;
            else if (c == 'y') key = 2;
            else if (c == 'z') key = 3;

            if ((c >= '0' && c <= '9') || c == '.' || c == '-') {
                num.append(c);
            }

            if ((c == ',' || c == '}' ) && !num.isEmpty()) {

                float value = Float.parseFloat(num.toString());
                num.setLength(0);

                if (key == 1) x = value;
                else if (key == 2) y = value;
                else if (key == 3) z = value;

                key = 0;
            }

            if (c == '}' && index >= 0 && index < 21) {
                buffer[index][0] = x;
                buffer[index][1] = y;
                buffer[index][2] = z;
            }
        }

        return buffer;
    }
}