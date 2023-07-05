package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.Call;
import com.example.loadbalancer.entity.EventFromMediaLayer;
import com.example.loadbalancer.entity.MediaLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.loadbalancer.utils.Utils.*;

@Component
public class ScheduledClass {
    private final MongoTemplate mongoTemplate;
    private final Service service;

    private final Environment environment;
    Logger logger = LoggerFactory.getLogger(ScheduledClass.class);

    @Autowired
    public ScheduledClass(MongoTemplate mongoTemplate, Service service, Environment environment) {
        this.mongoTemplate = mongoTemplate;
        this.service = service;
        this.environment = environment;
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
        if (!"8080".equals(environment.getProperty("server.port"))) {
            return;
        }
        //updates the duration and lastModified fields of the database every few seconds
        List<MediaLayer> mediaLayerList = mongoTemplate.findAll(MediaLayer.class);
        for (MediaLayer mediaLayer : mediaLayerList) {
            refreshMediaLayerAttributes(mediaLayer);
            mongoTemplate.save(mediaLayer);
        }
        logger.info("Media Layers refreshed");
    }

    @Scheduled(fixedDelay = GENERATE_AUTOHANGUP, timeUnit = TimeUnit.MINUTES)
    public void hangupCalls() {
        if (!"8080".equals(environment.getProperty("server.port"))) {
            return;
        }
        long cutoff = System.currentTimeMillis() - TWO_HOURS_IN_MILLIS;

        Query query = new Query(Criteria.where("fieldName").gt(cutoff));
        List<Call> callList = mongoTemplate.find(query, Call.class);
        for (Call call : callList) {
            service.handleEventHangup(new EventFromMediaLayer(call.getCallId(), CHANNEL_HANGUP));
            logger.info("automatic hangup event generated for callID {}", call.getCallId());
        }
    }
}
