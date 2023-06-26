package com.example.loadBalancer.entity;

import com.example.loadBalancer.utils.Utils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "MediaLayers")
public class MediaLayer {
    private boolean faulty;
    @Id
    private String layerNumber;//a number which tells which layer number it is
    private String status;  // red<orange<yellow<green
    private long duration; // total sum of durations of all the simultaneous calls going on in the media layer
    private long lastModified; // it shows the last time a new call was originated/hung-up
    private int numberOfCalls; //it tells us the current total number of calls in the media Layer
    private float maxLoad; //defines the maximum load of a mediaLayer

    public MediaLayer() {
    }

    public MediaLayer(String layerNumber, String status, long duration, long lastModified, int numberOfCalls, float maxLoad, boolean faulty) {
        this.layerNumber = layerNumber;
        this.status = status;
        this.duration = duration;
        this.lastModified = lastModified;
        this.numberOfCalls = numberOfCalls;
        this.maxLoad = maxLoad;
        this.faulty = faulty;
    }

    public MediaLayer(MediaLayerDTO mediaLayerDTO) {
        this.layerNumber = mediaLayerDTO.getLayerNumber();
        this.status = mediaLayerDTO.getStatus();
        this.duration = mediaLayerDTO.getDuration();
        this.lastModified = mediaLayerDTO.getLastModified();
        this.numberOfCalls = mediaLayerDTO.getNumberOfCalls();
        this.maxLoad = mediaLayerDTO.getMaxLoad();
        this.faulty = mediaLayerDTO.isFaulty();
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
        String prevStatus = this.status;
        this.status = status;
        this.maxLoad = (maxLoad * calculateMultiplier(prevStatus, status));
    }

    private float calculateMultiplier(String prevStatus, String newStatus) {
        int diff = Utils.getNumberFromString(newStatus) - Utils.getNumberFromString(prevStatus);
        float res;
        switch (diff) {
            case -3:
                res = 0.125F;
                break;
            case -2:
                res = 0.25F;
                break;
            case -1:
                res = 0.5F;
                break;
            case 1:
                res = 2F;
                break;
            case 2:
                res = 4F;
                break;
            case 3:
                res = 8F;
                break;
            default:
                res = 1F;
        }
        return res;
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

    public float getMaxLoad() {
        return maxLoad;
    }

    public void setMaxLoad(float maxLoad) {
        this.maxLoad = maxLoad;
    }

    public void updateLastModified(long curTime) {
        // last modified fields and ....  to update the value of duration
        duration += (curTime - lastModified) * numberOfCalls;
        lastModified = curTime;
    }

    public void decrNumberOfCalls() {
        numberOfCalls = numberOfCalls - 1;
    }

    public void incrNumberOfCalls() {
        numberOfCalls = numberOfCalls + 1;
    }

    public void decreaseDuration(long curTime, long startTime) {
        duration = duration + numberOfCalls * (curTime - lastModified) - (curTime - startTime);
        lastModified = curTime;
    }
}
