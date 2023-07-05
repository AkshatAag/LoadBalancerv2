package com.example.loadbalancer.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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

    @Override
    public String toString() {
        return "Call{" +
                "callId='" + callId + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", mediaLayerNumber='" + mediaLayerNumber + '\'' +
                ", timeStamp=" + timeStamp +
                '}';
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
