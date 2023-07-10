package com.example.loadbalancer.utils;

public class Utils {


    public static final String CHANNEL_HANGUP = "CHANNEL_HANGUP";
    public static final String FIELD_FAULTY = "faulty";
    public static final String FIELD_RATIO = "ratio";
    public static final String FIELD_LATEST_CALL_TIME_STAMP = "latestCallTimeStamp";
    public static final String FIELD_DURATION = "duration";
    public static final int GENERATE_AUTOHANGUP = 10;
    public static final String FIELD_CONVERSATION_ID = "conversationId";
    public static final int RED = 1;
    public static final int ORANGE = 2;
    public static final int YELLOW = 3;
    public static final int GREEN = 4;
    public static final int LEAST_CONNECTIONS = 1;
    public static final int ROUND_ROBIN = 2;
    public static final int FIXED_DELAY = 10;
    public static final String FIELD_ID = "_id";
    public static final int TWO_HOURS_IN_MILLIS = 2 * 60 * 60 * 1000;
    public static final String FIELD_LAST_MODIFIED = "lastModified";
    public static final String FIELD_NUMBER_OF_CALLS = "numberOfCalls";
    public static final String ID = "_id";

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

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
