package com.example.loadbalancer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventFromMediaLayerDTO {
    @JsonProperty("Event-Name")
    private String eventName;
    @JsonProperty("Core-UUID")
    private String coreUUID;
    @JsonProperty("FreeSWITCH-HostName")
    private String freeSwitchHostname;
    @JsonProperty("FreeSWITCH-SwitchName")
    private String freeSwitchSwitchname;
    @JsonProperty("FreeSWITCH-IPv4")
    private String freeSwitchIPv4;
    @JsonProperty("Event-Date-Local")
    private LocalDateTime eventDateLocal;
    @JsonProperty("Relative-Time")
    private long relativeTime;

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getCoreUUID() {
        return coreUUID;
    }

    public void setCoreUUID(String coreUUID) {
        this.coreUUID = coreUUID;
    }

    public String getFreeSwitchHostname() {
        return freeSwitchHostname;
    }

    public void setFreeSwitchHostname(String freeSwitchHostname) {
        this.freeSwitchHostname = freeSwitchHostname;
    }

    public String getFreeSwitchSwitchname() {
        return freeSwitchSwitchname;
    }

    public void setFreeSwitchSwitchname(String freeSwitchSwitchname) {
        this.freeSwitchSwitchname = freeSwitchSwitchname;
    }

    public String getFreeSwitchIPv4() {
        return freeSwitchIPv4;
    }

    public void setFreeSwitchIPv4(String freeSwitchIPv4) {
        this.freeSwitchIPv4 = freeSwitchIPv4;
    }

    public LocalDateTime getEventDateLocal() {
        return eventDateLocal;
    }

    public void setEventDateLocal(LocalDateTime eventDateLocal) {
        this.eventDateLocal = eventDateLocal;
    }

    public long getRelativeTime() {
        return relativeTime;
    }

    public void setRelativeTime(long relativeTime) {
        this.relativeTime = relativeTime;
    }
}
