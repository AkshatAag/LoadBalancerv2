package com.example.loadBalancer.Utils;

public class Utils {
    public static final int RED = 4;
    public static final int ORANGE = 3;
    public static final int YELLOW = 2;
    public static final int GREEN = 1;

    public static int getNumberFromString(String inputString) {
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
