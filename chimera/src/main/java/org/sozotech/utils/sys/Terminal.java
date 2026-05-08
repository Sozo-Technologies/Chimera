package org.sozotech.utils.sys;
import org.sozotech.utils.style.Palette;

public class Terminal {
    public static void print(String string) {

    }

    public static void println(String string) {

    }

    public static void print_matrices(String matrices) {
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
}
