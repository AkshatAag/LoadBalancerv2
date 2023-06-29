package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.loadbalancer.utils.Utils.*;

@org.springframework.stereotype.Service
public class Service {
    Logger logger = LoggerFactory.getLogger(Service.class);
    private final MongoTemplate mongoTemplate;

    @Autowired
    public Service(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void deleteById(String id, Class<?> entityClass) {
        mongoTemplate.remove(Query.query(Criteria.where(FIELD_ID).is(id)), entityClass);
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

    public String processEventControlLayer(CallFromControlLayer callFromControlLayer, int alg) {

        String legId = callFromControlLayer.getLegId();
        String conversationId = callFromControlLayer.getConversationId();
        Call call = mongoTemplate.findById(legId, Call.class);
        if (call != null) return call.getMediaLayerNumber();
        ConversationDetails conversationDetails = mongoTemplate.findById(conversationId, ConversationDetails.class);

        MediaLayer destinationMediaLayer;
        String mediaLayerNumber;

        if (conversationDetails != null) {
            //if this conversation already has an ongoing media layer assigned to it
            destinationMediaLayer = mongoTemplate.findById(conversationDetails.getMediaLayerNumber(), MediaLayer.class);
            if (destinationMediaLayer == null) {
                logger.error("Conversation is going on but unable to find corresponding media layer");
                return "-1";
            }
            mediaLayerNumber = conversationDetails.getMediaLayerNumber();
            conversationDetails.incrementLegCount();

            new Thread(() -> mongoTemplate.save(conversationDetails)).start();
            logger.info("Call was added to ongoing conversation");
        } else {
            destinationMediaLayer = getLeastLoaded(alg);
            assert destinationMediaLayer != null;
            mediaLayerNumber = assignLayerToNewConversation(conversationId, destinationMediaLayer);
        }

        new Thread(() -> {
            long currentTime = System.currentTimeMillis();
            mongoTemplate.save(new Call(legId, conversationId, mediaLayerNumber, currentTime));
            updateMediaLayerNewCall(destinationMediaLayer, currentTime);
            mongoTemplate.save(destinationMediaLayer);
        }).start();

        return destinationMediaLayer.getLayerNumber();
    }

    private String assignLayerToNewConversation(String conversationId, MediaLayer destinationMediaLayer) {
        //creates a new conversation and assigns it to a media layer
        String mediaLayerNumber;
        mediaLayerNumber = destinationMediaLayer.getLayerNumber();

        mongoTemplate.save(new ConversationDetails(1, mediaLayerNumber, conversationId));
        logger.info("Call was added to the least loaded server");
        return mediaLayerNumber;
    }

    private void updateMediaLayerNewCall(MediaLayer destinationMediaLayer, long currentTime) {
        destinationMediaLayer.incrementNumberOfCalls();
        destinationMediaLayer.setRatio();
        destinationMediaLayer.setLatestCallTimeStamp(currentTime);
    }

    private MediaLayer getLeastLoaded(int alg) {
        //RETURNS THE LEAST LOADED MEDIA LAYER SERVER BASED ON THE ALGORITHM.
        switch (alg) {
            case LEAST_CONNECTIONS:
                Query queryLeastConnections = new Query(Criteria.where(FIELD_FAULTY).is(false)).with(Sort.by(Sort.Direction.ASC, FIELD_RATIO).and(Sort.by(Sort.Direction.ASC, FIELD_DURATION))).limit(1);
                return mongoTemplate.findOne(queryLeastConnections, MediaLayer.class);

            case ROUND_ROBIN:
                Query queryRoundRobin = new Query(Criteria.where(FIELD_FAULTY).is(false)).with(Sort.by(Sort.Direction.ASC, FIELD_LATEST_CALL_TIME_STAMP)).limit(1);
                return mongoTemplate.findOne(queryRoundRobin, MediaLayer.class);
            default:
                return null;
        }
    }


    public String processEventFromMediaLayer(EventFromMediaLayer event) {
        //PROCESSES THE EVENT FROM THE MEDIA LAYER
        boolean flag = true;
        if (event.getEventName().equals(CHANNEL_HANGUP)) {
            flag = handleEventHangup(event);
        }
        if (flag) {
            logger.info("Media Layer event was processed");
            return HttpStatus.OK.toString();
        } else {
            logger.error("Unable to process media layer event");
            return HttpStatus.BAD_REQUEST.toString();
        }
    }

    private boolean handleEventHangup(EventFromMediaLayer event) {
        //handles the hangup events

        Call currentCall = mongoTemplate.findById(event.getCoreUUID(), Call.class);
        if (currentCall == null) {
            //there is no ongoing call with that call id
            logger.error("FALSE POSITIVE HANGUP EVENT");
            return true;
        }
        String conversationId = currentCall.getConversationId();
        String legId = currentCall.getCallId();
        ConversationDetails conversationDetails = mongoTemplate.findById(conversationId, ConversationDetails.class);

        if (conversationDetails != null) {

            new Thread(() -> deleteById(legId, Call.class)).start();
            new Thread(() -> updateDatabaseDecrementLegCount(conversationId, conversationDetails)).start();

            return updateMediaLayerDatabaseHangupEvent(conversationDetails, currentCall);

        } else {
            //there exists a call, but it has no corresponding conversation going on, so we delete that call.
            deleteById(currentCall.getCallId(), Call.class);
            logger.error("NO ONGOING CONVERSATION FOR THIS CALL ID");
            return true;
        }
    }

    private void updateDatabaseDecrementLegCount(String conversationId, ConversationDetails conversationDetails) {
        conversationDetails.decrementLegCount();
        if (conversationDetails.getLegCount() == 0) {
            deleteById(conversationId, ConversationDetails.class);
        } else {
            mongoTemplate.save(conversationDetails);
        }
    }

    private Boolean updateMediaLayerDatabaseHangupEvent(ConversationDetails conversationDetails, Call currentCall) {
        //makes necessary updates to the database when there is a hangup.
        MediaLayer mediaLayer = mongoTemplate.findById(conversationDetails.getMediaLayerNumber(), MediaLayer.class);
        if (mediaLayer != null) {
            new Thread(() -> {
                mediaLayer.decreaseDuration(System.currentTimeMillis(), currentCall.getTimeStamp());
                mediaLayer.decrementNumberOfCalls();
                mongoTemplate.save(mediaLayer);
            }).start();
            return true;
        } else {
            logger.error("Media server for this conversation does not exist");
            return false;
        }
    }

    public String addNewMediaLayer(MediaLayer mediaLayer) {
        //adds a new media layer
        mongoTemplate.save(mediaLayer);
        logger.info("New Media Layer was added.");
        return mediaLayer.toString();
    }


    public String setServerStatus(String layerNumber, String color) {
        //sets the load category of the media server
        MediaLayer mediaLayer = mongoTemplate.findById(layerNumber, MediaLayer.class);
        if (mediaLayer != null) {
            mediaLayer.setStatus(color);
            mongoTemplate.save(mediaLayer);
        } else {
            logger.error("No currently running Media Server with this layer number exists");
            return HttpStatus.BAD_REQUEST.toString();
        }
        logger.info("Faulty status was changed");
        return HttpStatus.OK.toString();
    }

    public String setFaultyStatus(String layerNumber, boolean status) {
        //sets the faulty status of the media layer
        MediaLayer mediaLayer = mongoTemplate.findById(layerNumber, MediaLayer.class);
        if (mediaLayer != null) {
            mediaLayer.setFaulty(status);
            mongoTemplate.save(mediaLayer);
        } else {
            logger.error("No currently running Media Server with this layer number exists");
            return HttpStatus.BAD_REQUEST.toString();
        }
        logger.info("Faulty status was changed");
        return HttpStatus.OK.toString();
    }

}
