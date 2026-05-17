package org.sozotech.ml.trainer;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class DatasetReader {

    private static final String DATASETS_PATH = "datasets/";
    private static final String APP_DATASET   = "app_dataset.csv";
    private static final String USER_DATASET  = "dataset.csv";

    public static Map<Character, List<float[]>> load() {
        Map<Character, List<float[]>> dataset = new HashMap<>();

        Set<String> seen = new HashSet<>(); // for deduplication

        readFile(DATASETS_PATH + APP_DATASET, dataset, seen);
        readFile(DATASETS_PATH + USER_DATASET, dataset, seen);

        for (List<float[]> samples : dataset.values()) {
            Collections.shuffle(samples);
        }

        printSummary(dataset);
        return dataset;
    }

    private static void readFile(
            String resourcePath,
            Map<Character, List<float[]>> dataset,
            Set<String> seen
    ) {
        URL url = DatasetReader.class
                .getClassLoader()
                .getResource(resourcePath);

        if (url == null) {
            System.out.println("[DatasetReader] Not found, skipping: " + resourcePath);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(url.toURI().getPath()))) {
            String line;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");

                if (parts.length != 64) {
                    System.out.printf("[DatasetReader] Skipping malformed row %d in %s (columns: %d)%n",
                            lineNum, resourcePath, parts.length);
                    continue;
                }

                String labelStr = parts[0].trim().toLowerCase();
                if (labelStr.length() != 1 || !Character.isLetter(labelStr.charAt(0))) {
                    System.out.printf("[DatasetReader] Skipping invalid label '%s' at row %d%n",
                            labelStr, lineNum);
                    continue;
                }

                char label = labelStr.charAt(0);

                if (!seen.add(line)) {
                    continue;
                }

                float[] sample = new float[63];
                boolean valid = true;

                for (int i = 0; i < 63; i++) {
                    try {
                        sample[i] = Float.parseFloat(parts[i + 1].trim());
                    } catch (NumberFormatException e) {
                        System.out.printf("[DatasetReader] Invalid float at row %d col %d in %s%n",
                                lineNum, i + 1, resourcePath);
                        valid = false;
                        break;
                    }
                }

                if (!valid) continue;

                dataset
                        .computeIfAbsent(label, k -> new ArrayList<>())
                        .add(sample);
            }

            System.out.println("[DatasetReader] Loaded: " + resourcePath);

        } catch (IOException | URISyntaxException e) {
            System.out.println("[DatasetReader] Error reading " + resourcePath + ": " + e.getMessage());
        }
    }

    private static void printSummary(Map<Character, List<float[]>> dataset) {
        System.out.println("\n[DatasetReader] ── Dataset Summary ──────────────────");
        int total = 0;
        for (char c = 'a'; c <= 'z'; c++) {
            List<float[]> samples = dataset.get(c);
            int count = samples != null ? samples.size() : 0;
            total += count;
            System.out.printf("  %s : %d samples%n", Character.toUpperCase(c), count);
        }
        System.out.println("  TOTAL : " + total + " samples");
        System.out.println("[DatasetReader] ────────────────────────────────────\n");
    }
}