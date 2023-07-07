package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.Call;
import com.example.loadbalancer.entity.CallFromControlLayer;
import com.example.loadbalancer.entity.EventFromMediaLayer;
import com.example.loadbalancer.entity.MediaLayer;
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

import java.util.List;

import static com.example.loadbalancer.utils.Utils.*;

@org.springframework.stereotype.Service
public class Service {
    private final MongoTemplate mongoTemplate;
    //    private final ExecutorService executorService = Executors.newFixedThreadPool(100);
    Logger logger = LoggerFactory.getLogger(Service.class);

    @Autowired
    public Service(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public DeleteResult deleteById(String id, Class<?> entityClass) {
        return mongoTemplate.remove(Query.query(Criteria.where(FIELD_ID).is(id)), entityClass);
    }


    public String processEventControlLayer(CallFromControlLayer callFromControlLayer, String alg) {

        String legId = callFromControlLayer.getLegId();
        String conversationId = callFromControlLayer.getConversationId();
        String mediaLayerNumber;
        Call callWithSameConversationId = null;
        Query query = new Query(Criteria.where(FIELD_CONVERSATION_ID).is(conversationId));
        List<Call> callsWithSameConversationId = mongoTemplate.find(query, Call.class); //find a call with same conversation ID
        for (Call call : callsWithSameConversationId) {
            if (call.getCallId().equals(legId)) {
                return call.getMediaLayerNumber();
            }
        }
        if (!callsWithSameConversationId.isEmpty()) {
            callWithSameConversationId = callsWithSameConversationId.get(0);
        }
        if ((mediaLayerNumber = getMediaLayer(alg, callWithSameConversationId)) == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR.toString();
        }

        long currentTime;

        while (!updateMediaLayerNewCall(mediaLayerNumber, currentTime=System.currentTimeMillis(), mongoTemplate)) ;
        logger.info("NEW CALL WAS SAVED TO MONGO database MEDIA LAYERSS: TO MEDIA LAYER NUMBER : {}", mediaLayerNumber);
        mongoTemplate.save(new Call(legId, conversationId, mediaLayerNumber, currentTime));
        logger.info("NEW CALL WAS SAVED TO MONGO database CAlls : TO MEDIA LAYER NUMBER : {}", mediaLayerNumber);
        return mediaLayerNumber;
    }

    private String getMediaLayer(String alg, Call callWithSameConversationId) {
        MediaLayer destinationMediaLayer = null;
        if (callWithSameConversationId != null) { //ongoing call with this conversation ID.
            return callWithSameConversationId.getMediaLayerNumber();
//            if (destinationMediaLayer == null) {
//                logger.info("Ongoing call with conversation ID, but no media layer... Conversation ID : {}", callWithSameConversationId.getConversationId());
//            }
        } else {
            destinationMediaLayer = getLeastLoaded(alg);
            if (destinationMediaLayer == null) {
                logger.error("Conversation is going on but unable to find corresponding media layer... conversationID:{}", callWithSameConversationId.getConversationId());
            }
        }
        return destinationMediaLayer.getLayerNumber();
    }

    private boolean updateMediaLayerNewCall(String mediaLayerNumber, long currentTime, MongoTemplate mongoTemplate) {
        MediaLayer mediaLayer = mongoTemplate.findById(mediaLayerNumber, MediaLayer.class);
        assert mediaLayer != null;
        long lastModifiedTimeStamp = mediaLayer.getLastModified();
        long duration = mediaLayer.getDuration() + (currentTime - lastModifiedTimeStamp) * mediaLayer.getNumberOfCalls();
        mediaLayer.incrementNumberOfCalls();
        mediaLayer.calculateAndSetRatio();
        mediaLayer.setLatestCallTimeStamp(currentTime);
        mediaLayer.setDuration(duration);
        mediaLayer.setLastModified(currentTime);
        Query query = new Query(Criteria.where("lastModified").is(lastModifiedTimeStamp).and("_id").is(mediaLayer.getLayerNumber()));
        Update update = new Update();
        update.set("numberOfCalls", mediaLayer.getNumberOfCalls());
        update.set("ratio", mediaLayer.getRatio());
        update.set("latestCallTimeStamp", mediaLayer.getLatestCallTimeStamp());
        update.set("duration", mediaLayer.getDuration());
        update.set("lastModified", mediaLayer.getLastModified());
        return null != mongoTemplate.findAndModify(query, update, MediaLayer.class);
    }

    private MediaLayer getLeastLoaded(String alg) {
        //RETURNS THE LEAST LOADED MEDIA LAYER SERVER BASED ON THE ALGORITHM.
        switch (Integer.parseInt(alg)) {
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
        if (event.getEventName().equals(CHANNEL_HANGUP)) {
            handleEventHangup(event);
        }
        logger.info("Media Layer event was processed");
        return HttpStatus.OK.toString();
    }

    public void handleEventHangup(EventFromMediaLayer event) {
        //handles the hangup events

        Call currentCall = mongoTemplate.findById(event.getCoreUUID(), Call.class);
        if (currentCall == null) {
            //there is no ongoing call with that call id
            logger.info("no call with this call ID :{}", currentCall.getCallId());
            return;
        }
        String legId = currentCall.getCallId();
        if (deleteById(legId, Call.class).getDeletedCount() > 0) {
            logger.info("call deleted from call repo: call id {}",currentCall.getCallId());
            while(!updateMediaLayerDatabaseHangupEvent(currentCall));
            logger.info("call deleted from medialayer repo : call id {}",currentCall.getCallId());
        }
    }

    private boolean updateMediaLayerDatabaseHangupEvent(Call currentCall) {
        //makes necessary updates to the database when there is a hangup.
        MediaLayer mediaLayer = mongoTemplate.findById(currentCall.getMediaLayerNumber(), MediaLayer.class);
        if (mediaLayer != null) {
            long currentTime = System.currentTimeMillis();
            long lastModifiedTimeStamp = mediaLayer.getLastModified();
            mediaLayer.decreaseDuration(currentTime, currentCall.getTimeStamp());
            mediaLayer.setLastModified(currentTime);
            mediaLayer.decrementNumberOfCalls();
            Query query = new Query(Criteria.where("lastModified").is(lastModifiedTimeStamp).and("_id").is(mediaLayer.getLayerNumber()));
            Update update = new Update();
            update.set("numberOfCalls", mediaLayer.getNumberOfCalls());
            update.set("duration", mediaLayer.getDuration());
            update.set("lastModified", mediaLayer.getLastModified());
            return null!=mongoTemplate.findAndModify(query,update, MediaLayer.class);
        } else {
            logger.error("Media server for this conversation does not exist  media layer number:{}", currentCall.getMediaLayerNumber());
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
        //sets the load category of the media server
        MediaLayer mediaLayer = mongoTemplate.findById(layerNumber, MediaLayer.class);
        if (mediaLayer != null) {
            if (mediaLayer.setStatusAndMaxLoad(color)) {
                logger.info("Color status was changed");
                mongoTemplate.save(mediaLayer);
            } else {
                logger.info("status was not changed");
                return HttpStatus.BAD_REQUEST.toString();
            }
        } else {
            logger.error("No currently running Media Server with this layer number exists");
            return HttpStatus.BAD_REQUEST.toString();
        }
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
