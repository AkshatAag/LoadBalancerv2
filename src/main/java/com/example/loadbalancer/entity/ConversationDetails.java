package com.example.loadbalancer.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "conversations")
public class ConversationDetails {
    @Id
    private String conversationID;
    private int legCount;
    private String mediaLayerNumber;

    public String getConversationID() {
        return conversationID;
    }

    public void setConversationID(String conversationID) {
        this.conversationID = conversationID;
    }

    public int getLegCount() {
        return legCount;
    }

    public void setLegCount(int legCount) {
        this.legCount = legCount;
    }

    public String getMediaLayerNumber() {
        return mediaLayerNumber;
    }

    public void setMediaLayerNumber(String mediaLayerNumber) {
        this.mediaLayerNumber = mediaLayerNumber;
    }

    public ConversationDetails() {
    }

    public ConversationDetails(int legCount, String mediaLayerNumber, String conversationID) {
        this.legCount = legCount;
        this.conversationID = conversationID;
        this.mediaLayerNumber = mediaLayerNumber;
    }

    public void incrementLegCount() {
        legCount++;
    }

    public void decrementLegCount() {
        legCount--;
    }

}
