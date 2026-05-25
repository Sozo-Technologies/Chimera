package org.sozotech.ml.translation;

public class FrameBuffer {

    private int capacity;
    private final float[][][] frames;
    private int head;
    private int size;

    public FrameBuffer(int capacity) {
        this.capacity = capacity;
        this.frames = new float[capacity][21][3];
        this.head = 0;
        this.size = 0;
    }

    public void push(float[][] landmarks) {
        for (int i = 0; i < 21; i++) {
            frames[head][i][0] = landmarks[i][0];
            frames[head][i][1] = landmarks[i][1];
            frames[head][i][2] = landmarks[i][2];
        }

        head = (head + 1) % capacity;
        if (size < capacity) size++;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public void clear() {
        head = 0;
        size = 0;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        clear();
    }

    public float[][] resolve() {
        float[][] result = new float[21][3];

        for (int lm = 0; lm < 21; lm++) {
            double sumX = 0, sumY = 0, sumZ = 0;
            int valid = 0;

            for (int f = 0; f < size; f++) {
                float x = frames[f][lm][0];
                float y = frames[f][lm][1];
                float z = frames[f][lm][2];

                if (Float.isNaN(x) || x == -1f) continue;

                sumX += x;
                sumY += y;
                sumZ += z;
                valid++;
            }

            if (valid == 0) {
                result[lm][0] = -1f;
                result[lm][1] = -1f;
                result[lm][2] = -1f;
            } else {
                result[lm][0] = (float) (sumX / valid);
                result[lm][1] = (float) (sumY / valid);
                result[lm][2] = (float) (sumZ / valid);
            }
        }

        return result;
    }

    public DebugSnapshot debug() {
        return new DebugSnapshot(size, capacity, head);
    }

    public record DebugSnapshot(int filled, int capacity, int headIndex) {
        @Override
        public String toString() {
            return String.format("[FrameBuffer] %d/%d frames (head=%d)", filled, capacity, headIndex);
        }
    }
}