package com.example.loadBalancer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "Calls")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Call {
    @Id
    private String callId;
    private String conversationId;
    private int mediaLayerNumber;
    private LocalDateTime timeStamp;
}
