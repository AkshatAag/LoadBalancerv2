package com.example.loadBalancer.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.LocalDateTime;

@RedisHash("Event")
public class EventFromMediaLayer implements Serializable {
    @JsonProperty("Event-Name")
    private String eventName;
    @Id
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
    public EventFromMediaLayer() {
    }

    public EventFromMediaLayer(String eventName, String coreUUID, String freeSwitchHostname, String freeSwitchSwitchname, String freeSwitchIPv4, LocalDateTime eventDateLocal, long relativeTime) {
        this.eventName = eventName;
        this.coreUUID = coreUUID;
        this.freeSwitchHostname = freeSwitchHostname;
        this.freeSwitchSwitchname = freeSwitchSwitchname;
        this.freeSwitchIPv4 = freeSwitchIPv4;
        this.eventDateLocal = eventDateLocal;
        this.relativeTime = relativeTime;
    }

    public String getEventName() {
        return eventName;
    }

    public String getCoreUUID() {
        return coreUUID;
    }
}
