package com.example.loadBalancer.entity;

import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;


public class EventFromMediaLayer {
    private String eventName;
    @Id
    private String coreUUID;
    private String freeSwitchHostname;
    private String freeSwitchSwitchname;
    private String freeSwitchIPv4;
    private LocalDateTime eventDateLocal;
    private long relativeTime;

    public EventFromMediaLayer(String eventName, String coreUUID, String freeSwitchHostname, String freeSwitchSwitchname, String freeSwitchIPv4, LocalDateTime eventDateLocal, long relativeTime) {
        this.eventName = eventName;
        this.coreUUID = coreUUID;
        this.freeSwitchHostname = freeSwitchHostname;
        this.freeSwitchSwitchname = freeSwitchSwitchname;
        this.freeSwitchIPv4 = freeSwitchIPv4;
        this.eventDateLocal = eventDateLocal;
        this.relativeTime = relativeTime;
    }

    public EventFromMediaLayer(EventFromMediaLayerDTO eventDTO) {
        this.coreUUID = eventDTO.getCoreUUID();
        this.eventName = eventDTO.getEventName();
        this.freeSwitchHostname = eventDTO.getFreeSwitchHostname();
        this.freeSwitchSwitchname = eventDTO.getFreeSwitchSwitchname();
        this.freeSwitchIPv4 = eventDTO.getFreeSwitchIPv4();
        this.eventDateLocal = eventDTO.getEventDateLocal();
        this.relativeTime = eventDTO.getRelativeTime();
    }

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

    public EventFromMediaLayer() {
    }


}
