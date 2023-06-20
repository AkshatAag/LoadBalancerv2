package com.example.loadBalancer.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventFromMediaLayer {
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
}
