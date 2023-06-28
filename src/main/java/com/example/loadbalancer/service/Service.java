package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.*;
import com.example.loadbalancer.repository.LoadRedis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.loadbalancer.utils.Utils.LEAST_CONNECTIONS;
import static com.example.loadbalancer.utils.Utils.ROUND_ROBIN;

@org.springframework.stereotype.Service
public class Service {


    private final LoadRedis loadRedis;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public Service(LoadRedis loadRedis, MongoTemplate mongoTemplate) {
        this.loadRedis = loadRedis;
        this.mongoTemplate = mongoTemplate;
    }
    public void deleteById(String id,Class<?> entityClass) {
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(id)), entityClass);
    }

    @Scheduled(fixedDelay = 5, initialDelay = 0, timeUnit = TimeUnit.SECONDS)
    public void refreshDatabaseMongo() {
        List<MediaLayer> mediaLayerList = mongoTemplate.findAll(MediaLayer.class);
        for (MediaLayer mediaLayer : mediaLayerList) {
            long curTime = System.currentTimeMillis();
            long duration = mediaLayer.getDuration() + (curTime - mediaLayer.getLastModified()) * mediaLayer.getNumberOfCalls();
            mediaLayer.setDuration(duration);
            mediaLayer.setLastModified(curTime);
            mediaLayer.setRatio();
            mongoTemplate.save(mediaLayer);
        }
    }

    public String processEventControlLayer(CallFromControlLayer callFromControlLayer, int alg) {

        String legId = callFromControlLayer.getLegId();
        String conversationId = callFromControlLayer.getConversationId();
        ConversationDetails conversationDetails = mongoTemplate.findById(conversationId, ConversationDetails.class);
        MediaLayer destinationMediaLayer;
        String mediaLayerNumber;

        if (conversationDetails != null) { //if this conversation already has an ongoing media layer assigned to it
            destinationMediaLayer = mongoTemplate.findById(conversationDetails.getMediaLayerNumber(), MediaLayer.class);

            if (destinationMediaLayer == null) {
                return "Conversation is going on but unable to find corresponding media layer";
            }

            mediaLayerNumber = conversationDetails.getMediaLayerNumber();
            conversationDetails.incrementLegCount();

            new Thread(() -> mongoTemplate.save(conversationDetails)).start();

            System.out.println("Call was added to ongoing conversation");
        } else {
            destinationMediaLayer = getLeastLoaded(alg);
            assert destinationMediaLayer != null;
            mediaLayerNumber = destinationMediaLayer.getLayerNumber();

            new Thread(() -> mongoTemplate.save(new ConversationDetails(1, mediaLayerNumber, conversationId))).start();
            System.out.println("Call was added to the least loaded server");
        }

        destinationMediaLayer.updateDetails();
        new Thread(() -> {
            mongoTemplate.save(new Call(legId, conversationId, mediaLayerNumber, System.currentTimeMillis()));
            loadRedis.setConversationId(legId, conversationId);
            mongoTemplate.save(destinationMediaLayer);
        }).start();

        return "Send the call to media layer number : " + destinationMediaLayer.getLayerNumber();
    }

    private MediaLayer getLeastLoaded(int alg) {
        switch (alg) {
            case LEAST_CONNECTIONS:
                Query queryLeastConnections = new Query(Criteria.where("faulty").is(false))
                        .with(Sort.by(Sort.Direction.ASC, "ratio").and(Sort.by(Sort.Direction.ASC, "duration")))
                        .limit(1);
                return mongoTemplate.findOne(queryLeastConnections, MediaLayer.class);

            case ROUND_ROBIN:
                Query queryRoundRobin = new Query(Criteria.where("faulty").is(false))
                        .with(Sort.by(Sort.Direction.ASC, "latestCallTimeStamp"))
                        .limit(1);
                return mongoTemplate.findOne(queryRoundRobin, MediaLayer.class);
            default:
                return null;
        }
    }


    public String processEventFromMediaLayer(EventFromMediaLayer event) {
        boolean flag = false;
        if (event.getEventName().equals("CHANNEL_HANGUP")) {
            flag = handleHangupEvent(event);
        }
        if (flag) return "EVENT FROM THE MEDIA LAYER WAS PROCESSED";
        else return "UNABLE TO PROCESS EVENT";
    }

    private boolean handleHangupEvent(EventFromMediaLayer event) {

        Call currentCall = mongoTemplate.findById(event.getCoreUUID(), Call.class);
        if (currentCall == null) {
            return false;
        }
        String conversationId = currentCall.getConversationId();
        String legId = currentCall.getCallId();
        ConversationDetails conversationDetails = mongoTemplate.findById(conversationId, ConversationDetails.class);

        if (conversationDetails != null) {

            new Thread(() -> {
                deleteById(legId,Call.class);
                loadRedis.remove(legId);
            }).start();
            conversationDetails.decrementLegCount();

            new Thread(() -> {
                if (conversationDetails.getLegCount() == 0) {
                    deleteById(conversationId,ConversationDetails.class);
                } else {
                    mongoTemplate.save(conversationDetails);
                }
            }).start();

            return updateMediaLayerDatabaseHangupEvent(conversationDetails, currentCall);

        } else {
            System.out.println("FALSE POSITIVE HANGUP EVENT");
            return false;
        }
    }

    private Boolean updateMediaLayerDatabaseHangupEvent(ConversationDetails conversationDetails, Call currentCall) {
        MediaLayer mediaLayer = mongoTemplate.findById(conversationDetails.getMediaLayerNumber(), MediaLayer.class);
        if (mediaLayer!=null) {
            new Thread(() -> {
                mediaLayer.decreaseDuration(System.currentTimeMillis(), currentCall.getTimeStamp());
                mediaLayer.decrementNumberOfCalls();
                mongoTemplate.save(mediaLayer);
            }).start();
            return true;
        } else {
            return false;
        }
    }

    public String addNewMediaLayer(MediaLayer mediaLayer) {
        mongoTemplate.save(mediaLayer);
        return "NEW Media Layer was added to Mongo";
    }


    public String setServerStatus(String layerNumber, String color) {
        MediaLayer mediaLayer = mongoTemplate.findById(layerNumber, MediaLayer.class);
        if (mediaLayer!=null) {
            mediaLayer.setStatus(color);
            mongoTemplate.save(mediaLayer);
        }
        return "Server number " + layerNumber + " status was changed to " + color;
    }

    public String setFaultyStatus(String layerNumber, boolean status) {
        MediaLayer mediaLayer = mongoTemplate.findById(layerNumber, MediaLayer.class);
        if (mediaLayer!=null) {
            mediaLayer.setFaulty(status);
            mongoTemplate.save(mediaLayer);
        }
        return "Server number " + layerNumber + " faulty status was changed to " + status;
    }

}
