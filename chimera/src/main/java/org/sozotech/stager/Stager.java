package org.sozotech.stager;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class Stager {
    private static final int REQUIRED_MAJOR = 3;
    private static final int REQUIRED_MINOR = 13;
    private static Process mediapipeProcess;
    private static final String PYTHON_INSTALLER_URL = "https://www.python.org/ftp/python/3.13.3/python-3.13.3-amd64.exe";
    private static final String INSTALLER_NAME = "python-installer.exe";

    public static void Install() {
        try {
            if (isPythonInstalled()) {
                System.out.println("[Stager] Compatible Python installation found.");
                return;
            }

            System.out.println("[Stager] Python 3.13+ not found.");
            System.out.println("[Stager] Downloading installer...");
            Path installerPath = Paths.get(System.getProperty("java.io.tmpdir"), INSTALLER_NAME);
            downloadFile(installerPath);
            System.out.println("[Stager] Running silent installer...");
            installPythonSilently(installerPath);
            System.out.println("[Stager] Python installation completed.");

        } catch (Exception _) {
            System.out.println("[Stager] Installation failed.");
        }
    }

    private static boolean isPythonInstalled() {
        try {
            Process process = new ProcessBuilder("python", "--version").redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            if (output == null) return false;
            System.out.println("[Stager] Detected: " + output);
            Pattern pattern = Pattern.compile("Python (\\d+)\\.(\\d+)");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                return major > REQUIRED_MAJOR || (major == REQUIRED_MAJOR && minor >= REQUIRED_MINOR);
            }

        } catch (Exception ignored) {

        }

        return false;
    }

    private static void downloadFile(Path destination) throws IOException {
        try (InputStream in = URI.create(Stager.PYTHON_INSTALLER_URL).toURL().openStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void installPythonSilently(Path installerPath) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(installerPath.toString(), "/quiet", "InstallAllUsers=1", "PrependPath=1", "Include_test=0");
        builder.inheritIO();
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new RuntimeException("Python installer exited with code: " + exitCode);
    }





    public static boolean runMediapipe() {

        try {

            Install();

            Path dir = Paths.get(".chimera");
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Path scriptPath = dir.resolve("mediapipe_server.py");
            Files.write(scriptPath, MediapipeServer.SERVER_SCRIPT);

            Path modelPath = dir.resolve("hand_landmarker.task");

            try (InputStream in = Objects.requireNonNull(
                    Stager.class.getResourceAsStream("/hand_landmarker.task")
            )) {

                Files.copy(
                        in,
                        modelPath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            ProcessBuilder builder = new ProcessBuilder(
                    "python",
                    scriptPath.toAbsolutePath().toString(),
                    modelPath.toAbsolutePath().toString()
            );

            builder.redirectErrorStream(true);

            mediapipeProcess = builder.start();

            AtomicBoolean ready = new AtomicBoolean(false);

            Executors.newSingleThreadExecutor().submit(() -> {

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(mediapipeProcess.getInputStream())
                )) {

                    String line;

                    while ((line = reader.readLine()) != null) {

                        System.out.println("[MediaPipe] " + line);

                        if (line.contains("READY")) {
                            ready.set(true);
                        }
                    }

                } catch (Exception ignored) {}

            });

            long start = System.currentTimeMillis();

            while (!ready.get()) {

                if (System.currentTimeMillis() - start > 10_000) {
                    mediapipeProcess.destroy();
                    System.out.println("[Stager] MediaPipe timeout.");
                    return false;
                }

                Thread.sleep(100);
            }

            System.out.println("[Stager] MediaPipe server launched.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void stopMediapipe() {

        if (mediapipeProcess != null) {
            mediapipeProcess.destroy();
            System.out.println("[Stager] MediaPipe server stopped.");
        }

    }
}