package com.example.loadbalancer.service;

import com.example.loadbalancer.dto.MediaLayerDTO;
import com.example.loadbalancer.entity.Call;
import com.example.loadbalancer.entity.CallFromControlLayer;
import com.example.loadbalancer.entity.EventFromMediaLayer;
import com.example.loadbalancer.entity.MediaLayer;
import com.example.loadbalancer.exceptions.CallCannotBeAddedAgainException;
import com.example.loadbalancer.exceptions.NoFreeMediaServerException;
import com.example.loadbalancer.exceptions.NoSuchObjectInDatabaseException;
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
import java.util.concurrent.Future;

import static com.example.loadbalancer.utils.Utils.*;

@org.springframework.stereotype.Service
public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);
    private final MongoTemplate mongoTemplate;

    @Autowired
    public Service(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    private static Update createUpdateMediaLayerNewCall(MediaLayer mediaLayer) {
        return new Update()
                .set(FIELD_NUMBER_OF_CALLS, mediaLayer.getNumberOfCalls())
                .set(FIELD_RATIO, mediaLayer.getRatio())
                .set(FIELD_LATEST_CALL_TIME_STAMP, mediaLayer.getLatestCallTimeStamp())
                .set(FIELD_DURATION, mediaLayer.getDuration())
                .set(FIELD_LAST_MODIFIED, mediaLayer.getLastModified())
                .set(FIELD_STATUS, mediaLayer.getStatus());
    }

    private static void updateMediaLayerPropertiesNewCall(long currentTime, MediaLayer mediaLayer, long lastModifiedTimeStamp) {
        long duration = mediaLayer.getDuration() + (currentTime - lastModifiedTimeStamp) * mediaLayer.getNumberOfCalls();

        mediaLayer.incrementNumberOfCalls();
        mediaLayer.calculateAndSetStatus();
        mediaLayer.setLatestCallTimeStamp(currentTime);
        mediaLayer.setDuration(duration);
        mediaLayer.setLastModified(currentTime);
        mediaLayer.calculateAndSetStatus();
    }

    public DeleteResult deleteById(String id, Class<?> entityClass) {
        Query query = Query.query(Criteria.where(FIELD_ID).is(id));
        DeleteResult result = mongoTemplate.remove(query, entityClass);
        logger.info("Deleted document with ID: {}", id);
        return result;
    }

    public Future<String> processEventControlLayer(CallFromControlLayer callFromControlLayer, String alg) {
        MediaServerWorker<String> task = new MediaServerWorker<>() {
            @Override
            public String doCall() {
                String legId = callFromControlLayer.getLegId();
                String conversationId = callFromControlLayer.getConversationId();
                String mediaLayerNumber;

                Query query = Query.query(Criteria.where(FIELD_CONVERSATION_ID).is(conversationId));
                List<Call> callsWithSameConversationId = mongoTemplate.find(query, Call.class);


                for (Call call : callsWithSameConversationId) {
                    if (call.getCallId().equals(legId)) {
                        throw new CallCannotBeAddedAgainException(legId, conversationId);
                    }
                }

                Call callWithSameConversationId = !callsWithSameConversationId.isEmpty() ? callsWithSameConversationId.get(0) : null;

                mediaLayerNumber = getMediaLayer(alg, callWithSameConversationId);

                long currentTime = System.currentTimeMillis();
                while (!updateAndSaveMediaLayerOnNewCall(mediaLayerNumber, currentTime)) {
                    currentTime = System.currentTimeMillis();
                }
                logger.info("NEW CALL WAS SAVED TO MONGO DATABASE MEDIA_LAYERS: TO MEDIA LAYER NUMBER : {}", mediaLayerNumber);
                mongoTemplate.save(new Call(legId, conversationId, mediaLayerNumber, currentTime));
                logger.info("NEW CALL WAS SAVED TO MONGO DATABASE CAll : TO MEDIA LAYER NUMBER : {}", mediaLayerNumber);
                return mediaLayerNumber;
            }
        };
        return LoadBalancerExecutorService.getInstance().submit(task);
    }

    private String getMediaLayer(String alg, Call callWithSameConversationId) {
        if (callWithSameConversationId != null) {
            return callWithSameConversationId.getMediaLayerNumber();
        } else {
            return assignNewMediaLayer(alg);
        }
    }

    private String assignNewMediaLayer(String alg) {
        MediaLayer destinationMediaLayer = getLeastLoaded(alg);
        if (destinationMediaLayer == null) {
            logger.error("Could not find least loaded media server.");
            throw new NoFreeMediaServerException();
        }
        return destinationMediaLayer.getLayerNumber();
    }

    private boolean updateAndSaveMediaLayerOnNewCall(String mediaLayerNumber, long currentTime) {
        MediaLayer mediaLayer = mongoTemplate.findById(mediaLayerNumber, MediaLayer.class);
        if (null == mediaLayer)
            throw new NoSuchObjectInDatabaseException(MediaLayer.class, mediaLayerNumber, HttpStatus.INTERNAL_SERVER_ERROR);

        long lastModifiedTimeStamp = mediaLayer.getLastModified();
        updateMediaLayerPropertiesNewCall(currentTime, mediaLayer, lastModifiedTimeStamp);
        Query query = new Query(Criteria.where(FIELD_LAST_MODIFIED).is(lastModifiedTimeStamp).and(FIELD_ID).is(mediaLayer.getLayerNumber()));
        Update update = createUpdateMediaLayerNewCall(mediaLayer);

        return 0 != mongoTemplate.updateFirst(query, update, MediaLayer.class).getModifiedCount();

    }

    private MediaLayer getLeastLoaded(String alg) {
        //RETURNS THE LEAST LOADED MEDIA LAYER SERVER BASED ON THE ALGORITHM.
        switch (Integer.parseInt(alg)) {
            case LEAST_CONNECTIONS:
                Query queryLeastConnections = Query.query(Criteria.where(FIELD_FAULTY).is(false).and(FIELD_STATUS).ne("red")).with(Sort.by(Sort.Direction.ASC, FIELD_RATIO).and(Sort.by(Sort.Direction.ASC, FIELD_DURATION))).limit(1);
                return mongoTemplate.findOne(queryLeastConnections, MediaLayer.class);

            case ROUND_ROBIN:
                Query queryRoundRobin = Query.query(Criteria.where(FIELD_FAULTY).is(false).and(FIELD_STATUS).ne("red")).with(Sort.by(Sort.Direction.ASC, FIELD_LATEST_CALL_TIME_STAMP)).limit(1);
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
            throw new NoSuchObjectInDatabaseException(Call.class, callId, HttpStatus.OK);
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
            mediaLayer.calculateAndSetStatus();

            Query query = Query.query(Criteria.where(FIELD_LAST_MODIFIED).is(lastModifiedTimeStamp).and(ID).is(mediaLayer.getLayerNumber()));
            Update update = new Update()
                    .set(FIELD_NUMBER_OF_CALLS, mediaLayer.getNumberOfCalls())
                    .set(FIELD_DURATION, mediaLayer.getDuration())
                    .set(FIELD_LAST_MODIFIED, mediaLayer.getLastModified())
                    .set(FIELD_STATUS, mediaLayer.getStatus());
            return mongoTemplate.updateFirst(query, update, MediaLayer.class).getModifiedCount() != 0;
        } else {
            logger.error("Media server for this conversation does not exist. Media layer number: {}", currentCall.getMediaLayerNumber());
            throw new NoSuchObjectInDatabaseException(MediaLayer.class, currentCall.getMediaLayerNumber(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String addNewMediaLayer(MediaLayer mediaLayer) {
        mongoTemplate.save(mediaLayer);
        logger.info("New Media Layer was added.");
        return mediaLayer.toString();
    }


    public String setServerStatus(String layerNumber, String color) {
        MediaLayer mediaLayer = mongoTemplate.findById(layerNumber, MediaLayer.class);
        if (mediaLayer != null) {
            mediaLayer.setStatusAndMaxLoad(color);
            logger.info("Color status was changed");
            mongoTemplate.save(mediaLayer);
            return HttpStatus.OK.toString();
        } else {
            logger.error("No currently running Media Server with this layer number exists");
            throw new NoSuchObjectInDatabaseException(MediaLayer.class, layerNumber, HttpStatus.BAD_REQUEST);
        }
    }

    public String setFaultyStatus(String layerNumber, boolean status) {
        MediaLayer mediaLayer = mongoTemplate.findById(layerNumber, MediaLayer.class);
        if (null != mediaLayer) {
            mediaLayer.setFaulty(status);
            mongoTemplate.save(mediaLayer);
            logger.info("Faulty status was changed");
            return HttpStatus.OK.toString();
        } else {
            logger.error("No currently running Media Server with this layer number exists");
            throw new NoSuchObjectInDatabaseException(MediaLayer.class, layerNumber, HttpStatus.BAD_REQUEST);
        }
    }

    public String initialize() {
        Query query = new Query();
        mongoTemplate.remove(query, Call.class);
        addNewMediaLayer(new MediaLayer(new MediaLayerDTO("1")));
        addNewMediaLayer(new MediaLayer(new MediaLayerDTO("2")));
        addNewMediaLayer(new MediaLayer(new MediaLayerDTO("3")));
        return "databases cleared";
    }
}
