package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.*;
import com.example.loadbalancer.repository.CallRepo;
import com.example.loadbalancer.repository.ConversationsRepo;
import com.example.loadbalancer.repository.LoadRedis;
import com.example.loadbalancer.repository.MediaLayerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@org.springframework.stereotype.Service
public class Service {

    private static final int LEAST_CONNECTIONS = 1;
    private static final int ROUND_ROBIN = 2;
    private final LoadRedis loadRedis;
    private final CallRepo callRepo;
    private final MediaLayerRepo mediaLayerRepo;
    private final ConversationsRepo conversationsRepo;

    @Autowired
    public Service(LoadRedis loadRedis, CallRepo callRepo, MediaLayerRepo mediaLayerRepo, ConversationsRepo conversationsRepo) {
        this.loadRedis = loadRedis;
        this.callRepo = callRepo;
        this.mediaLayerRepo = mediaLayerRepo;
        this.conversationsRepo = conversationsRepo;
    }

    @Scheduled(fixedDelay = 5, initialDelay = 0, timeUnit = TimeUnit.SECONDS)
    public void updateMongoDuration() {
        List<MediaLayer> mediaLayerList = mediaLayerRepo.findAll();
        for (MediaLayer mediaLayer : mediaLayerList) {
            long curTime = System.currentTimeMillis();
            long duration = mediaLayer.getDuration() + (curTime - mediaLayer.getLastModified()) * mediaLayer.getNumberOfCalls();
            mediaLayer.setDuration(duration);
            mediaLayer.setLastModified(curTime);
            mediaLayer.setRatio();
        }
        mediaLayerRepo.saveAll(mediaLayerList);
    }

    public String processEventControlLayer(CallFromControlLayer callFromControlLayer, int alg) {

        String legId = callFromControlLayer.getLegId();
        String conversationId = callFromControlLayer.getConversationId();
        Optional<ConversationDetails> optionalConversationDetails = conversationsRepo.findById(conversationId);

        MediaLayer destinationMediaLayer;
        String mediaLayerNumber;

        if (optionalConversationDetails.isPresent()) { //if this conversation already has an ongoing media layer assigned to it
            ConversationDetails conversationDetails = optionalConversationDetails.get();
            Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(conversationDetails.getMediaLayerNumber());

            if (optionalMediaLayer.isPresent()) {
                destinationMediaLayer = optionalMediaLayer.get();
            } else {
                return "Conversation is going on but unable to find corresponding media layer";
            }

            mediaLayerNumber = conversationDetails.getMediaLayerNumber();
            conversationDetails.incrementLegCount();

            new Thread(() -> conversationsRepo.save(conversationDetails)).start();

            System.out.println("Call was added to ongoing conversation");
        } else {
            destinationMediaLayer = getLeastLoaded(alg);
            mediaLayerNumber = destinationMediaLayer.getLayerNumber();

            new Thread(() -> conversationsRepo.save(new ConversationDetails(1, mediaLayerNumber, conversationId))).start();
            System.out.println("Call was added to the least loaded server");
        }

        destinationMediaLayer.updateDetails();
        new Thread(() -> {
            callRepo.save(new Call(legId, conversationId, mediaLayerNumber, System.currentTimeMillis()));
            loadRedis.setConversationId(legId, conversationId);
            mediaLayerRepo.save(destinationMediaLayer);
        }).start();

        return "Send the call to media layer number : " + destinationMediaLayer.getLayerNumber();
    }

    private MediaLayer getLeastLoaded(int alg) {
        switch (alg) {
            case LEAST_CONNECTIONS:
                return mediaLayerRepo.findDestinationLeastConnections();
            case ROUND_ROBIN:
                return mediaLayerRepo.findDestinationRoundRobin();
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

        Optional<Call> optionalCall = callRepo.findById(event.getCoreUUID());
        Call currentCall;
        if (optionalCall.isPresent()) {
            currentCall = optionalCall.get();
        } else {
            return false;
        }
        String conversationId = currentCall.getConversationId();
        String legId = currentCall.getCallId();
        Optional<ConversationDetails> optionalConversationDetails = conversationsRepo.findById(conversationId);

        if (optionalConversationDetails.isPresent()) {
            ConversationDetails conversationDetails = optionalConversationDetails.get();

            new Thread(() -> {
                callRepo.deleteById(legId);
                loadRedis.remove(legId);
            }).start();
            conversationDetails.decrementLegCount();

            new Thread(() -> {
                if (conversationDetails.getLegCount() == 0) {
                    conversationsRepo.deleteById(conversationId);
                } else {
                    conversationsRepo.save(conversationDetails);
                }
            }).start();

            return updateMediaLayerDatabaseHangupEvent(conversationDetails, currentCall);

        } else {
            System.out.println("FALSE POSITIVE HANGUP EVENT");
            return false;
        }
    }

    private Boolean updateMediaLayerDatabaseHangupEvent(ConversationDetails conversationDetails, Call currentCall) {
        Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(conversationDetails.getMediaLayerNumber());
        if (optionalMediaLayer.isPresent()) {
            new Thread(() -> {
                MediaLayer mediaLayer = optionalMediaLayer.get();


                mediaLayer.decreaseDuration(System.currentTimeMillis(), currentCall.getTimeStamp());
                mediaLayer.decrementNumberOfCalls();

                mediaLayerRepo.save(mediaLayer);
            }).start();
            return true;
        } else {
            return false;
        }
    }

    public String addNewMediaLayer(MediaLayer mediaLayer) {
        mediaLayerRepo.save(mediaLayer);
        return "NEW Media Layer was added to Mongo";
    }


    public String setServerStatus(String layerNumber, String color) {
        Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(layerNumber);
        if (optionalMediaLayer.isPresent()) {
            MediaLayer mediaLayer = optionalMediaLayer.get();

            mediaLayer.setStatus(color);
            mediaLayerRepo.save(mediaLayer);
        }
        return "Server number " + layerNumber + " status was changed to " + color;
    }

    public String setFaultyStatus(String layerNumber, boolean status) {
        Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(layerNumber);
        if (optionalMediaLayer.isPresent()) {
            MediaLayer mediaLayer = optionalMediaLayer.get();

            mediaLayer.setFaulty(status);
            mediaLayerRepo.save(mediaLayer);
        }
        return "Server number " + layerNumber + " faulty status was changed to " + status;
    }

}
