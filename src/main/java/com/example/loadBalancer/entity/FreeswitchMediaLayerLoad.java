package com.example.loadBalancer.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@RedisHash("FreeswitchMediaLayerLoad")
public class FreeswitchMediaLayerLoad implements Serializable {
    @Id
    private int layerNumber;
    private int currentLoad;

    public FreeswitchMediaLayerLoad() {
    }

    public FreeswitchMediaLayerLoad(int layerNumber, int currentLoad) {
        this.layerNumber = layerNumber;
        this.currentLoad = currentLoad;
    }

    public int getLayerNumber() {
        return layerNumber;
    }

    public void setLayerNumber(int layerNumber) {
        this.layerNumber = layerNumber;
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }
}
