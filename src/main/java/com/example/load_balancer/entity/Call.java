package com.example.load_balancer.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Calls")

public class Call {
    @Id
    private String callId;
    private String conversationId;
    private String mediaLayerNumber;
    private long timeStamp;

    public Call() {
    }

    public Call(String callId, String conversationId, String mediaLayerNumber, long timeStamp) {
        this.callId = callId;
        this.conversationId = conversationId;
        this.mediaLayerNumber = mediaLayerNumber;
        this.timeStamp = timeStamp;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMediaLayerNumber() {
        return mediaLayerNumber;
    }

    public void setMediaLayerNumber(String mediaLayerNumber) {
        this.mediaLayerNumber = mediaLayerNumber;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
