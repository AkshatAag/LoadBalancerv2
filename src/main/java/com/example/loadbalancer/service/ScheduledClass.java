package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.Call;
import com.example.loadbalancer.entity.EventFromMediaLayer;
import com.example.loadbalancer.entity.MediaLayer;
import com.mongodb.client.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.loadbalancer.utils.Utils.*;
import static java.lang.System.exit;

@Component
@ConditionalOnProperty(name = "instantiate-once", havingValue = "true")
public class ScheduledClass {
    private final MongoTemplate mongoTemplate;
    private final Service service;
    private final MongoClient mongoClient;
    Logger logger = LoggerFactory.getLogger(ScheduledClass.class);

    @Autowired
    public ScheduledClass(MongoTemplate mongoTemplate, Service service, MongoClient mongoClient) {
        this.mongoTemplate = mongoTemplate;
        this.service = service;
        this.mongoClient = mongoClient;
    }

    private static void refreshMediaLayerAttributes(MediaLayer mediaLayer, Update update) {
        //updates the media layer attributes as per real time.
        long curTime = System.currentTimeMillis();
        long duration = mediaLayer.getDuration() + (curTime - mediaLayer.getLastModified()) * mediaLayer.getNumberOfCalls();
        mediaLayer.setDuration(duration);
        mediaLayer.setLastModified(curTime);
        mediaLayer.calculateAndSetStatus();
        mediaLayer.calculateAndSetRatio();
        update.set("duration", mediaLayer.getDuration());
        update.set("lastModified", mediaLayer.getLastModified());
        update.set("status", mediaLayer.getStatus());
        update.set("maxLoad", mediaLayer.getMaxLoad());
        update.set("ratio", mediaLayer.getRatio());
    }

    @Scheduled(fixedDelay = FIXED_DELAY, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void refreshDatabaseMongo() {
        List<MediaLayer> mediaLayerList = mongoTemplate.findAll(MediaLayer.class);
        List<String> mediaLayerIdList = mediaLayerList.stream().map(MediaLayer::getLayerNumber).collect(Collectors.toList());

        for (String id : mediaLayerIdList) {
            MediaLayer mediaLayer = mongoTemplate.findById(id, MediaLayer.class);
            assert mediaLayer != null;
            long timeStamp = mediaLayer.getLastModified();
            Update update = new Update();
            Query query = new Query(Criteria.where("lastModified").is(timeStamp).and("_id").is(mediaLayer.getLayerNumber()));
            refreshMediaLayerAttributes(mediaLayer, update);
            mongoTemplate.updateFirst(query, update, MediaLayer.class);
        }
        logger.info("Media Layers refreshed");
    }

    @Scheduled(fixedDelay = GENERATE_AUTOHANGUP, timeUnit = TimeUnit.MINUTES)
    public void hangupCalls() {
        long cutoff = System.currentTimeMillis() - TWO_HOURS_IN_MILLIS;

        Query query = new Query(Criteria.where("fieldName").gt(cutoff));
        List<Call> callList = mongoTemplate.find(query, Call.class);
        for (Call call : callList) {
            service.handleEventHangup(new EventFromMediaLayer(call.getCallId(), CHANNEL_HANGUP));
            logger.info("automatic hangup event generated for callID {}", call.getCallId());
        }
    }

    @PreDestroy
    public void onShutDown() {
        mongoTemplate.remove(Call.class);
        mongoTemplate.remove(MediaLayer.class);
        System.out.println("DATABASES WERE CLEARED");
        mongoClient.close();
        System.out.println("CONNECTION WAS CLOSED");
        exit(0);
    }
}
