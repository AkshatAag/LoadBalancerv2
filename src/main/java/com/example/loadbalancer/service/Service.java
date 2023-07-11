package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.*;
import com.mongodb.client.result.DeleteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static com.example.loadbalancer.utils.Utils.*;

@org.springframework.stereotype.Service
public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);
    private final MongoTemplate mongoTemplate;
    private List<Long> timeStamps = new ArrayList<>();
    long localtime;

    @Autowired
    public Service(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public DeleteResult deleteById(String id, Class<?> entityClass) {
        Query query = Query.query(Criteria.where(FIELD_ID).is(id));
        DeleteResult result = mongoTemplate.remove(query, entityClass);
        logger.info("Deleted document with ID: {}", id);
        return result;
    }


    public String processEventControlLayer(CallFromControlLayer callFromControlLayer, String alg) {

        String legId = callFromControlLayer.getLegId();
        String conversationId = callFromControlLayer.getConversationId();
        String mediaLayerNumber;

        Query query = Query.query(Criteria.where(FIELD_CONVERSATION_ID).is(conversationId));
        localtime = System.nanoTime();
        List<Call> callsWithSameConversationId = mongoTemplate.find(query, Call.class);
        timeStamps.add(System.nanoTime()-localtime);

        for (Call call : callsWithSameConversationId) {
            if (call.getCallId().equals(legId)) {
                return call.getMediaLayerNumber();
            }
        }

        Call callWithSameConversationId = !callsWithSameConversationId.isEmpty() ? callsWithSameConversationId.get(0) : null;

        mediaLayerNumber = getMediaLayer(alg, callWithSameConversationId);
        if (mediaLayerNumber == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR.toString();
        }

        long currentTime = System.currentTimeMillis();
        while (!updateMediaLayerNewCall(mediaLayerNumber, currentTime, mongoTemplate)) {
            currentTime = System.currentTimeMillis();
        }
        timeStamps.add(System.nanoTime()-localtime);
        logger.info("NEW CALL WAS SAVED TO MONGO DATABASE MEDIA_LAYERS: TO MEDIA LAYER NUMBER : {}", mediaLayerNumber);
        localtime = System.nanoTime();
        mongoTemplate.save(new Call(legId, conversationId, mediaLayerNumber, currentTime));
        timeStamps.add(System.nanoTime()-localtime);
        logger.info("NEW CALL WAS SAVED TO MONGO DATABASE CAll : TO MEDIA LAYER NUMBER : {}", mediaLayerNumber);

        return mediaLayerNumber;
    }

    private String getMediaLayer(String alg, Call callWithSameConversationId) {
        if (callWithSameConversationId != null) {
            // Ongoing call with the same conversation ID
            return callWithSameConversationId.getMediaLayerNumber();
        } else {
            //Initiate new conversation for this call
            MediaLayer destinationMediaLayer = getLeastLoaded(alg);
            if (destinationMediaLayer == null) {
                logger.error("Could not find least loaded media server.");
            }
            return destinationMediaLayer != null ? destinationMediaLayer.getLayerNumber() : null;
        }
    }

    private boolean updateMediaLayerNewCall(String mediaLayerNumber, long currentTime, MongoTemplate mongoTemplate) {
        localtime = System.nanoTime();
        MediaLayer mediaLayer = mongoTemplate.findById(mediaLayerNumber, MediaLayer.class);
        timeStamps.add(System.nanoTime()-localtime);
        assert mediaLayer != null;
        long lastModifiedTimeStamp = mediaLayer.getLastModified();
        long duration = mediaLayer.getDuration() + (currentTime - lastModifiedTimeStamp) * mediaLayer.getNumberOfCalls();

        mediaLayer.incrementNumberOfCalls();
        mediaLayer.calculateAndSetRatio();
        mediaLayer.setLatestCallTimeStamp(currentTime);
        mediaLayer.setDuration(duration);
        mediaLayer.setLastModified(currentTime);

        Query query = new Query(Criteria.where(FIELD_LAST_MODIFIED).is(lastModifiedTimeStamp).and(FIELD_ID).is(mediaLayer.getLayerNumber()));
        Update update = new Update()
                .set(FIELD_NUMBER_OF_CALLS, mediaLayer.getNumberOfCalls())
                .set(FIELD_RATIO, mediaLayer.getRatio())
                .set(FIELD_LATEST_CALL_TIME_STAMP, mediaLayer.getLatestCallTimeStamp())
                .set(FIELD_DURATION, mediaLayer.getDuration())
                .set(FIELD_LAST_MODIFIED, mediaLayer.getLastModified());
        localtime = System.nanoTime();
        return 0 != mongoTemplate.updateFirst(query, update, MediaLayer.class).getModifiedCount();

    }

    private MediaLayer getLeastLoaded(String alg) {
        //RETURNS THE LEAST LOADED MEDIA LAYER SERVER BASED ON THE ALGORITHM.
        switch (Integer.parseInt(alg)) {
            case LEAST_CONNECTIONS:
                Query queryLeastConnections = Query.query(Criteria.where(FIELD_FAULTY).is(false)).with(Sort.by(Sort.Direction.ASC, FIELD_RATIO).and(Sort.by(Sort.Direction.ASC, FIELD_DURATION))).limit(1);
                return mongoTemplate.findOne(queryLeastConnections, MediaLayer.class);

            case ROUND_ROBIN:
                Query queryRoundRobin = Query.query(Criteria.where(FIELD_FAULTY).is(false)).with(Sort.by(Sort.Direction.ASC, FIELD_LATEST_CALL_TIME_STAMP)).limit(1);
                return mongoTemplate.findOne(queryRoundRobin, MediaLayer.class);

            default:
                return null;
        }
    }

    public String processEventFromMediaLayer(EventFromMediaLayer event) {
        if (event.getEventName().equals(CHANNEL_HANGUP)) {
            handleEventHangup(event);
        }
        logger.info("Media Layer event was processed");
        return HttpStatus.OK.toString();
    }

    public void handleEventHangup(EventFromMediaLayer event) {
        String callId = event.getCoreUUID();

        Call currentCall = mongoTemplate.findById(callId, Call.class);
        if (currentCall == null) {
            logger.info("No ongoing call with this call ID: {}", event.getCoreUUID());
            return;
        }
        if (deleteById(callId, Call.class).getDeletedCount() > 0) {
            logger.info("Call deleted from call repository: Call ID: {}", currentCall.getCallId());
            while (!updateMediaLayerDatabaseHangupEvent(currentCall)) {
                // Retry until successful update
            }
            logger.info("Call deleted from media layer repository: Call ID: {}", currentCall.getCallId());
        }
    }

    private boolean updateMediaLayerDatabaseHangupEvent(Call currentCall) {
        MediaLayer mediaLayer = mongoTemplate.findById(currentCall.getMediaLayerNumber(), MediaLayer.class);
        if (mediaLayer != null) {
            long currentTime = System.currentTimeMillis();
            long lastModifiedTimeStamp = mediaLayer.getLastModified();

            mediaLayer.decreaseDuration(currentTime, currentCall.getTimeStamp());
            mediaLayer.setLastModified(currentTime);
            mediaLayer.decrementNumberOfCalls();

            Query query = Query.query(Criteria.where(FIELD_LAST_MODIFIED).is(lastModifiedTimeStamp).and(ID).is(mediaLayer.getLayerNumber()));
            Update update = new Update()
                    .set(FIELD_NUMBER_OF_CALLS, mediaLayer.getNumberOfCalls())
                    .set(FIELD_DURATION, mediaLayer.getDuration())
                    .set(FIELD_LAST_MODIFIED, mediaLayer.getLastModified());
            return mongoTemplate.updateFirst(query, update, MediaLayer.class).getModifiedCount() != 0;
        } else {
            logger.error("Media server for this conversation does not exist. Media layer number: {}", currentCall.getMediaLayerNumber());
            return true;
        }
    }

    public String addNewMediaLayer(MediaLayer mediaLayer) {
        //adds a new media layer
        mongoTemplate.save(mediaLayer);
        logger.info("New Media Layer was added.");
        return mediaLayer.toString();
    }


    public String setServerStatus(String layerNumber, String color) {
        MediaLayer mediaLayer = mongoTemplate.findById(layerNumber, MediaLayer.class);
        if (mediaLayer != null) {
            if (mediaLayer.setStatusAndMaxLoad(color)) {
                logger.info("Color status was changed");
                mongoTemplate.save(mediaLayer);
            } else {
                logger.info("Status was not changed");
                return HttpStatus.BAD_REQUEST.toString();
            }
        } else {
            logger.error("No currently running Media Server with this layer number exists");
            return HttpStatus.BAD_REQUEST.toString();
        }
        return HttpStatus.OK.toString();
    }

    public String setFaultyStatus(String layerNumber, boolean status) {
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

    public List<Long> getTimeStamps() {
        return timeStamps;
    }
}
