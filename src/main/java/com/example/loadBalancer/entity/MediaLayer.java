package com.example.loadBalancer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "MediaLayers")
public class MediaLayer {
    @Id
    private int LayerNumber;
    private String status;
    private long duration;
    private LocalDateTime lastModified;
    private int numberOfCalls;

}
