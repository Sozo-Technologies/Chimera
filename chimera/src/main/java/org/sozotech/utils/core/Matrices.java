package org.sozotech.utils.core;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Matrices {

    private static final String RESET = "\u001B[0m";

    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";

    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";

    public static void print(JSONArray json) {
        if (json == null || json.isEmpty()) {

            System.out.println(
                    YELLOW + "[" + RESET +
                            RED + "x:null" + RESET + ", " +
                            GREEN + "y:null" + RESET + ", " +
                            BLUE + "z:null" + RESET +
                            YELLOW + "]" + RESET
            );

            return;
        }

        for (int i = 0; i < json.size(); i++) {

            JSONObject obj = (JSONObject) json.get(i);

            if (obj == null) {
                continue;
            }

            Object x = obj.containsKey("x") ? obj.get("x") : "null";
            Object y = obj.containsKey("y") ? obj.get("y") : "null";
            Object z = obj.containsKey("z") ? obj.get("z") : "null";

            System.out.println(
                    YELLOW + "[" + i + "] " + RESET +
                            "{ " +
                            RED + "x: " + x + RESET + ", " +
                            GREEN + "y: " + y + RESET + ", " +
                            BLUE + "z: " + z + RESET +
                            " }"
            );
        }
    }
}