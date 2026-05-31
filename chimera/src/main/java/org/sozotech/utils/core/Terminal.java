package org.sozotech.utils.core;
import org.sozotech.utils.style.Palette;
import org.sozotech.ml.preprocess.HandData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Terminal {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] LANDMARK_NAMES = {
            "WRIST     ", "THUMB_CMC ", "THUMB_MCP ", "THUMB_IP  ", "THUMB_TIP ",
            "INDEX_MCP ", "INDEX_PIP ", "INDEX_DIP ", "INDEX_TIP ",
            "MID_MCP   ", "MID_PIP   ", "MID_DIP   ", "MID_TIP   ",
            "RING_MCP  ", "RING_PIP  ", "RING_DIP  ", "RING_TIP  ",
            "PINKY_MCP ", "PINKY_PIP ", "PINKY_DIP ", "PINKY_TIP "
    };

    private static String time() {
        return LocalDateTime.now().format(FORMAT);
    }

    public static void info(String message) {
        System.out.println("[INFO][" + time() + "] " + message);
    }

    public static void warn(String message) {
        System.out.println("[WARN][" + time() + "] " + message);
    }

    public static void error(String message) {
        System.err.println("[ERROR][" + time() + "] " + message);
    }

    public static void error(String message, Throwable throwable) {
        System.err.println("[ERROR][" + time() + "] " + message);

        if (throwable != null) {
            System.err.println("Cause: " + throwable.getMessage());
            for (StackTraceElement element : throwable.getStackTrace())
                System.err.println("\tat " + element);
        }
    }

    public static void debug(String message) {
        System.out.println("[DEBUG][" + time() + "] " + message);
    }

    public static void print_raw_matrices(String matrices) {
        if (matrices == null || matrices.equals("[]") || matrices.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < 21; i++) {
                sb.append("{x:-1, y:-1, z:-1}");
                if (i != 20) sb.append(",");
            }
            sb.append("]");
            System.out.printf("[%sLANDMARK%s]: %s\n", Palette.colors.CYAN, Palette.RESET, sb);
            return;
        }
        System.out.printf("[%sLANDMARK%s]: %s\n", Palette.colors.CYAN, Palette.RESET, matrices);
    }

    public static void print_matrices(float[][] matrices) {
        System.out.printf("[%sLANDMARK%s]: \n", Palette.colors.CYAN, Palette.RESET);
        for (int i = 0; i < matrices.length; i++) {
            float[] row = matrices[i];
            System.out.print("[" + i + "] ");
            for (int j = 0; j < row.length; j++) {
                System.out.print(row[j]);
                if (j < row.length - 1) System.out.print(", ");
            }
            System.out.println();
        }
    }

    public static void printConfidence(float[][] landmarks) {
        float[][] normalized = org.sozotech.ml.preprocess.Normalizer.normalize(landmarks);
        float[] flat = org.sozotech.ml.preprocess.Normalizer.flattenLandmarks(normalized);

        if (flat.length != 63) {
            System.out.printf("[%sCONFIDENCE%s]: Invalid landmarks (expected 63, got %d)\n", Palette.colors.CYAN, Palette.RESET, flat.length);
            return;
        }

        float[] output = org.sozotech.ml.core.NeuralNetwork.getInstance().getNetwork().forward(flat);
        System.out.printf("[%sCONFIDENCE%s]:\n", Palette.colors.CYAN, Palette.RESET);

        for (int i = 0; i < output.length; i++) {
            char letter = (char) ('A' + i);
            float confidence = output[i] * 100f;
            int bars = (int) (confidence / 5);
            String bar = "█".repeat(bars) + "░".repeat(20 - bars);
            System.out.printf("  %s | %s | %6.2f%%%n", letter, bar, confidence);
        }
    }

    public static void printHandData(HandData hand) {
        String C  = Palette.colors.CYAN;
        String R  = Palette.RESET;
        String DIM = "\u001B[2m";

        System.out.printf("%n[%sHAND DATA%s] ─────────────────────────────────────────────────────────%n", C, R);

        if (hand == null || !hand.isPresent()) {
            System.out.printf("  %s(no hand detected)%s%n", DIM, R);
            System.out.printf("[%sHAND DATA%s] ─────────────────────────────────────────────────────────%n%n", C, R);
            return;
        }

        float[][] lm  = hand.landmarks();
        float[][] wl  = hand.world();
        float[] vis   = hand.visibility();

        System.out.printf("  %-12s  %-28s  %-32s  %s%n", "LANDMARK", "IMAGE (x, y, z)", "WORLD  (x, y, z) m", "VIS");
        System.out.printf("  %s%n", "─".repeat(90));

        for (int i = 0; i < 21; i++) {
            float vScore = vis[i];
            String visBar = visBar(vScore);
            String visColor = vScore >= 0.8f ? "\u001B[32m" : vScore >= 0.5f ? "\u001B[33m" : "\u001B[31m";

            String imgCoords = String.format("(%7.4f, %7.4f, %7.4f)", lm[i][0], lm[i][1], lm[i][2]);
            String wldCoords = String.format("(%8.5f, %8.5f, %8.5f)", wl[i][0], wl[i][1], wl[i][2]);

            System.out.printf("  [%s%02d%s] %s  %s  %s  %s%s%s %.2f%n",
                    C, i, R,
                    LANDMARK_NAMES[i],
                    imgCoords,
                    wldCoords,
                    visColor, visBar, R,
                    vScore
            );
        }

        System.out.printf("  %s%n", "─".repeat(90));
        System.out.printf("  %simage%s   x/y: normalised 0–1   z: depth relative to wrist%n", DIM, R);
        System.out.printf("  %sworld%s   x/y/z: metric (meters), perspective-corrected%n", DIM, R);
        System.out.printf("[%sHAND DATA%s] ─────────────────────────────────────────────────────────%n%n", C, R);
    }

    private static String visBar(float score) {
        int filled = Math.round(score * 8);
        filled = Math.max(0, Math.min(8, filled));
        return "█".repeat(filled) + "░".repeat(8 - filled);
    }
}