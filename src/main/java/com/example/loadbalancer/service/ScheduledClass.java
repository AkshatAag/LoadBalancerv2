package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.MediaLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.loadbalancer.utils.Utils.FIXED_DELAY;
@Component
public class ScheduledClass {
    private final MongoTemplate mongoTemplate;
    @Autowired
    public ScheduledClass(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Scheduled(fixedDelay = FIXED_DELAY, initialDelay = 0, timeUnit = TimeUnit.SECONDS)
    public void refreshDatabaseMongo() {
        //updates the duration and lastModified fields of the database every few seconds
        List<MediaLayer> mediaLayerList = mongoTemplate.findAll(MediaLayer.class);
        for (MediaLayer mediaLayer : mediaLayerList) {
            refreshMediaLayerAttributes(mediaLayer);
            mongoTemplate.save(mediaLayer);
        }
    }

    private static void refreshMediaLayerAttributes(MediaLayer mediaLayer) {
        //updates the media layer attributes as per real time.
        long curTime = System.currentTimeMillis();
        long duration = mediaLayer.getDuration() + (curTime - mediaLayer.getLastModified()) * mediaLayer.getNumberOfCalls();
        mediaLayer.setDuration(duration);
        mediaLayer.setLastModified(curTime);
        mediaLayer.setRatio();
    }
}
