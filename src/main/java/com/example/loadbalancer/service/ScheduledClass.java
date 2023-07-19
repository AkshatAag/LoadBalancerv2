package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.Call;
import com.example.loadbalancer.entity.EventFromMediaLayer;
import com.example.loadbalancer.entity.MediaLayer;
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
    Logger logger = LoggerFactory.getLogger(ScheduledClass.class);

    @Autowired
    public ScheduledClass(MongoTemplate mongoTemplate, Service service) {
        this.mongoTemplate = mongoTemplate;
        this.service = service;
    }

    private static Update refreshMediaLayerAttributes(MediaLayer mediaLayer) {
        //updates the media layer attributes as per real time.
        long curTime = System.currentTimeMillis();
        long duration = mediaLayer.getDuration() + (curTime - mediaLayer.getLastModified()) * mediaLayer.getNumberOfCalls();
        mediaLayer.setDuration(duration);
        mediaLayer.setLastModified(curTime);
        mediaLayer.calculateAndSetStatus();
        mediaLayer.calculateAndSetRatio();
        Update update=new Update();
        update.set(FIELD_DURATION, mediaLayer.getDuration());
        update.set(FIELD_LAST_MODIFIED, mediaLayer.getLastModified());
        update.set(FIELD_STATUS, mediaLayer.getStatus());
        update.set(FIELD_MAX_LOAD, mediaLayer.getMaxLoad());
        update.set(FIELD_RATIO, mediaLayer.getRatio());
        return update;
    }

    @Scheduled(fixedDelay = FIXED_DELAY, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void refreshDatabaseMongo() {
        List<MediaLayer> mediaLayerList = mongoTemplate.findAll(MediaLayer.class);
        List<String> mediaLayerIdList = mediaLayerList.stream().map(MediaLayer::getLayerNumber).collect(Collectors.toList());

        for (String id : mediaLayerIdList) {
            MediaLayer mediaLayer = mongoTemplate.findById(id, MediaLayer.class);
            assert mediaLayer != null;
            long timeStamp = mediaLayer.getLastModified();
            Query query = new Query(Criteria.where(FIELD_LAST_MODIFIED).is(timeStamp).and(ID).is(mediaLayer.getLayerNumber()));
            Update update = refreshMediaLayerAttributes(mediaLayer);
            mongoTemplate.updateFirst(query, update, MediaLayer.class);
        }
        logger.info("Media Layers refreshed");
    }

    @Scheduled(fixedDelay = GENERATE_AUTOHANGUP, timeUnit = TimeUnit.MINUTES)
    public void hangupCalls() {
        long cutoff = System.currentTimeMillis() - TWO_HOURS_IN_MILLIS;

        Query query = new Query(Criteria.where(FIELD_DURATION).gt(cutoff));
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
        exit(0);
    }
}
