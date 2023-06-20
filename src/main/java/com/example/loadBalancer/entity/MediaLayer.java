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
public class MediaLayer implements Comparable<MediaLayer> {
    @Id
    private int layerNumber;//a number which tells which layer number it is
    private String status;  // red<orange<yellow<green
    private long duration; // total sum of durations of all the simultaneous calls going on in the media layer
    private long lastModified; // it shows the last time a new call was originated/hung-up
    private int numberOfCalls; //it tells us the current total number of calls in the media Layer
    public void updateLastModified(long curTime) {
        // last modified fields and ....  to update the value of duration
        duration += (curTime - lastModified) * numberOfCalls;
        lastModified = curTime;
    }

    public void decrLoad() {
        numberOfCalls=numberOfCalls-1;
    }

    public void incrLoad() {
        numberOfCalls=numberOfCalls+1;
    }
    @Override
    public int compareTo(MediaLayer other) {
        int numberOfCallsComparison = Integer.compare(this.numberOfCalls, other.numberOfCalls);

        if (numberOfCallsComparison != 0) {
            return numberOfCallsComparison;
        } else {
            return Long.compare(this.duration, other.duration);
        }
    }

    public void decreaseDuration(long curTime, long startTime) {
        duration = duration - (curTime-startTime);
    }
}
