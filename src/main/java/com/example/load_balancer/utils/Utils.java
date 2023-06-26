package com.example.loadBalancer.utils;

public class Utils {

    private Utils() {
        throw new IllegalStateException("Utility class");
    }
    public static final int RED = 1;
    public static final int ORANGE = 2;
    public static final int YELLOW = 3;
    public static final int GREEN = 4;

    public static int getNumberFromString(String inputString) {
        if (inputString.equalsIgnoreCase("red")) {
            return RED;
        } else if (inputString.equalsIgnoreCase("orange")) {
            return ORANGE;
        } else if (inputString.equalsIgnoreCase("yellow")) {
            return YELLOW;
        } else if (inputString.equalsIgnoreCase("green")) {
            return GREEN;
        }
        return 0;
    }
}
