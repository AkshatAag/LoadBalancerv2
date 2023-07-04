package com.example.loadbalancer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallFromControlLayerDTO {
    @NotBlank(message = "Leg ID cannot be blank")
    private String legId;
    @NotBlank(message = "Conversation ID cannot be blank")
    private String conversationId;

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
