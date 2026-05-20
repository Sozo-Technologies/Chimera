package org.sozotech.utils.core;

import org.opencv.core.Core;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AppContext {
    public static Router router;
    private static boolean opencv_loaded = false;

    public static void loadOpenCV() {
        if (opencv_loaded) return;
        try {
            Path dllPath = Paths.get("lib", "opencv", "x64", "opencv_java4120.dll").toAbsolutePath();
            System.out.println("[OpenCV] Loading DLL: " + dllPath);

            System.load(dllPath.toString());
            opencv_loaded = true;

            System.out.println("[OpenCV] Loaded Successfully!");
            System.out.println("[OpenCV] Version: " + Core.VERSION);
        } catch (Exception e) { Terminal.error("[OpenCV] Failed to load OpenCV DLL"); }
    }
}