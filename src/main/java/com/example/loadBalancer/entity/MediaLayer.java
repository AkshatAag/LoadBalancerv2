package com.example.loadBalancer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "MediaLayers")
public class MediaLayer  {
    @Id
    private int layerNumber;//a number which tells which layer number it is
    private String status;  // red<orange<yellow<green
    private long duration; // total sum of durations of all the simultaneous calls going on in the media layer
    private long lastModified; // it shows the last time a new call was originated/hung-up
    private int numberOfCalls; //it tells us the current total number of calls in the media Layer
    private int maxLoad; //defines the maximum load of a mediaLayer

    public void updateLastModified(long curTime) {
        // last modified fields and ....  to update the value of duration
        duration += (curTime - lastModified) * numberOfCalls;
        lastModified = curTime;
    }

    public void decrLoad() {
        numberOfCalls = numberOfCalls - 1;
    }

    public void incrLoad() {
        numberOfCalls = numberOfCalls + 1;
    }

    public void decreaseDuration(long curTime, long startTime) {
        duration = duration + numberOfCalls * (curTime - lastModified) - (curTime - startTime);
    }
}
