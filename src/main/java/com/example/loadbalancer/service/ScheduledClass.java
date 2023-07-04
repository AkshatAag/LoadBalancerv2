package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.Call;
import com.example.loadbalancer.entity.EventFromMediaLayer;
import com.example.loadbalancer.entity.MediaLayer;
import com.example.loadbalancer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.loadbalancer.utils.Utils.CHANNEL_HANGUP;
import static com.example.loadbalancer.utils.Utils.FIXED_DELAY;

@Component
public class ScheduledClass {
    private final MongoTemplate mongoTemplate;
    private final Service service;

    Logger logger = LoggerFactory.getLogger(ScheduledClass.class);

    @Autowired
    public ScheduledClass(MongoTemplate mongoTemplate, Service service) {
        this.mongoTemplate = mongoTemplate;
        this.service = service;
    }

    private static void refreshMediaLayerAttributes(MediaLayer mediaLayer) {
        //updates the media layer attributes as per real time.
        long curTime = System.currentTimeMillis();
        long duration = mediaLayer.getDuration() + (curTime - mediaLayer.getLastModified()) * mediaLayer.getNumberOfCalls();
        mediaLayer.setDuration(duration);
        mediaLayer.setLastModified(curTime);
        mediaLayer.calculateAndSetRatio();
    }

    @Scheduled(fixedDelay = FIXED_DELAY, initialDelay = 0, timeUnit = TimeUnit.SECONDS)
    public void refreshDatabaseMongo() {
        //updates the duration and lastModified fields of the database every few seconds
        List<MediaLayer> mediaLayerList = mongoTemplate.findAll(MediaLayer.class);
        for (MediaLayer mediaLayer : mediaLayerList) {
            refreshMediaLayerAttributes(mediaLayer);
            mongoTemplate.save(mediaLayer);
        }
        logger.info("Media Layers refreshed");
    }

    @Scheduled(fixedDelay = Utils.GENERATE_AUTOHANGUP, timeUnit = TimeUnit.MINUTES)
    public void hangupCalls() {
        long cutoff = System.currentTimeMillis() - 2 * 60 * 60 * 1000;

        Query query = new Query(Criteria.where("fieldName").gt(cutoff));
        List<Call> callList = mongoTemplate.find(query, Call.class);
        for (Call call : callList) {
            service.handleEventHangup(new EventFromMediaLayer(call.getCallId(), CHANNEL_HANGUP));
            logger.info("automatic hangup event generated for callID {}", call.getCallId());
        }
    }
}
