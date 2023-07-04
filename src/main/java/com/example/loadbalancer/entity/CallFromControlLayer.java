package com.example.loadbalancer.entity;

import com.example.loadbalancer.dto.CallFromControlLayerDTO;

public class CallFromControlLayer {
    private String legId;
    private String conversationId;

    public CallFromControlLayer(CallFromControlLayerDTO callFromControlLayerDTO) {
        legId = callFromControlLayerDTO.getLegId();
        conversationId = callFromControlLayerDTO.getConversationId();
    }

    public CallFromControlLayer() {
    }

    public CallFromControlLayer(String legId, String conversationId) {
        this.legId = legId;
        this.conversationId = conversationId;
    }

    @Override
    public String toString() {
        return "CallFromControlLayer{" +
                "legId='" + legId + '\'' +
                ", conversationId='" + conversationId + '\'' +
                '}';
    }

    public String getLegId() {
        return legId;
    }

    public void setLegId(String legId) {
        this.legId = legId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
