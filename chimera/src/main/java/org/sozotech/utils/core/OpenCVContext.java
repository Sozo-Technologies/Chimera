package org.sozotech.utils.core;

import org.opencv.core.Core;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenCVContext {

    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;

        try {

            Path dllPath = Paths.get(
                    "lib",
                    "opencv",
                    "x64",
                    "opencv_java4120.dll"
            ).toAbsolutePath();

            System.out.println("[OpenCV] Loading DLL: " + dllPath);

            System.load(dllPath.toString());

            loaded = true;

            System.out.println("[OpenCV] Loaded Successfully!");
            System.out.println("[OpenCV] Version: " + Core.VERSION);

        } catch (Exception e) {
            System.err.println("[OpenCV] Failed to load OpenCV DLL");
            e.printStackTrace();
        }
    }
}