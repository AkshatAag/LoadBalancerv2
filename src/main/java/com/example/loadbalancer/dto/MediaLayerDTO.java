package com.example.loadbalancer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaLayerDTO {

    private String layerNumber;//a number which tells which layer number it is
    private String status = "green";  // red<orange<yellow<green
    private long duration = 0; // total sum of durations of all the simultaneous calls going on in the media layer
    private long lastModified = 0; // it shows the last time a new call was originated/hung-up
    private int numberOfCalls = 0; //it tells us the current total number of calls in the media Layer
    private int maxLoad = 10; //defines the maximum load of a mediaLayer
    private boolean faulty = false;
    private int ratio = 0;
    private long latestCallTimeStamp = 0;

    public MediaLayerDTO(String layerNumber) {
        this.layerNumber = layerNumber;
    }

    public MediaLayerDTO() {
    }

    public long getLatestCallTimeStamp() {
        return latestCallTimeStamp;
    }

    public void setLatestCallTimeStamp(long latestCallTimeStamp) {
        this.latestCallTimeStamp = latestCallTimeStamp;
    }

    public int getRatio() {
        return ratio;
    }

    public void setRatio(int ratio) {
        this.ratio = ratio;
    }

    public boolean isFaulty() {
        return faulty;
    }

    public void setFaulty(boolean faulty) {
        this.faulty = faulty;
    }

    public String getLayerNumber() {
        return layerNumber;
    }

    public void setLayerNumber(String layerNumber) {
        this.layerNumber = layerNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public int getNumberOfCalls() {
        return numberOfCalls;
    }

    public void setNumberOfCalls(int numberOfCalls) {
        this.numberOfCalls = numberOfCalls;
    }

    public int getMaxLoad() {
        return maxLoad;
    }

    public void setMaxLoad(int maxLoad) {
        this.maxLoad = maxLoad;
    }
}
