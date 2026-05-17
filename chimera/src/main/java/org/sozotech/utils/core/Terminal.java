package org.sozotech.utils.core;
import org.sozotech.utils.style.Palette;

public class Terminal {
    public static void print(String string) {

    }

    public static void println(String string) {

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
            char  letter = (char) ('A' + i);
            float confidence = output[i] * 100f;
            int bars = (int) (confidence / 5);
            String bar = "█".repeat(bars) + "░".repeat(20 - bars);
            System.out.printf("  %s | %s | %6.2f%%%n", letter, bar, confidence);
        }
    }
}
