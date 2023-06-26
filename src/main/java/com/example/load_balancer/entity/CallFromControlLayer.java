package com.example.load_balancer.entity;

import org.springframework.data.annotation.Id;

public class CallFromControlLayer {
    @Id
    private String legId;
    private String conversationId;

    public CallFromControlLayer(CallFromControlLayerDTO callFromControlLayerDTO) {
        legId = callFromControlLayerDTO.getLegId();
        conversationId = callFromControlLayerDTO.getConversationId();
    }

    public CallFromControlLayer() {
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

    public CallFromControlLayer(String legId, String conversationId) {
        this.legId = legId;
        this.conversationId = conversationId;
    }
}
