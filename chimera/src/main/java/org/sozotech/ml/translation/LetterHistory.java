package org.sozotech.ml.translation;

public class LetterHistory {

    private final int maxSize;
    private final char[] buffer;
    private int size;

    public LetterHistory(int maxSize) {
        this.maxSize = maxSize;
        this.buffer = new char[maxSize];
        this.size = 0;
    }

    public void push(char letter) {
        if (letter == '?' || letter == 0) return;

        if (size < maxSize) {
            buffer[size++] = letter;
            return;
        }

        System.arraycopy(buffer, 1, buffer, 0, maxSize - 1);
        buffer[maxSize - 1] = letter;
    }

    public char last() {
        if (size == 0) return 0;
        return buffer[size - 1];
    }

    public void reset() {
        size = 0;
    }

    public String word() {
        return new String(buffer, 0, size);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public DebugSnapshot debug() {
        return new DebugSnapshot(word(), last(), size, maxSize);
    }

    public record DebugSnapshot(String currentWord, char lastLetter, int size, int maxSize) {
        @Override
        public String toString() {
            return String.format("[LetterHistory] word='%s' last='%s' (%d/%d)", currentWord, lastLetter == 0 ? "START" : String.valueOf(lastLetter), size, maxSize);
        }
    }
}