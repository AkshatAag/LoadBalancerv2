package com.example.loadBalancer.Utils;

public class Utils {
    public static final float RED = 0.1F;
    public static final float ORANGE = 0.3F;
    public static final float YELLOW = 0.7F;
    public static final float GREEN = 1F;

    public static float getNumberFromString(String inputString) {
        // Define your mappings here
        // Example:
        if (inputString.equalsIgnoreCase("red")) {
            return RED;
        } else if (inputString.equalsIgnoreCase("orange")) {
            return ORANGE;
        } else if (inputString.equalsIgnoreCase("yellow")) {
            return YELLOW;
        } else if (inputString.equalsIgnoreCase("green")) {
            return GREEN;
        }
        // Add more mappings as needed

        // If no matching mapping found, return a default value or throw an exception
        return 0;
    }
}
