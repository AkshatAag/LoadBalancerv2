package com.example.loadbalancer.exceptions;

public class CallCannotBeAddedAgainException extends RuntimeException {
    private final String callId;
    private final String conversationId;

    public CallCannotBeAddedAgainException(String callId, String conversationId) {
        this.callId = callId;
        this.conversationId = conversationId;
    }

    public String getCallId() {
        return callId;
    }

    public String getConversationId() {
        return conversationId;
    }

    @Override
    public String toString() {
        return "Call Cannot Be Added Again Exception{\n" +
                "\t\tcallId='" + callId + "',\n" +
                "\t\tconversationId='" + conversationId + "'\n" +
                '}';
    }
}
