package com.example.load_balancer.service;

import com.example.load_balancer.entity.*;
import com.example.load_balancer.repository.CallRepo;
import com.example.load_balancer.repository.ConversationsRepo;
import com.example.load_balancer.repository.LoadRedis;
import com.example.load_balancer.repository.MediaLayerRepo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;


@org.springframework.stereotype.Service
public class Service {

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

    public String processEventControlLayer(CallFromControlLayer callFromControlLayer) {

        String legId = callFromControlLayer.getLegId();
        String conversationId = callFromControlLayer.getConversationId();
        Optional<ConversationDetails> optionalConversationDetails = conversationsRepo.findById(conversationId);

        MediaLayer destinationMediaLayer;
        String mediaLayerNumber;
        long curTime = System.currentTimeMillis();
        List<MediaLayer> mediaLayerList = mediaLayerRepo.findByFaulty(false);
        updateDurationOfCalls(mediaLayerList, curTime);

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

            conversationsRepo.save(conversationDetails);

            System.out.println("Call was added to ongoing conversation");
        } else {
            destinationMediaLayer = getMin(mediaLayerList);
            mediaLayerNumber = destinationMediaLayer.getLayerNumber();

            conversationsRepo.save(new ConversationDetails(1, mediaLayerNumber, conversationId));
            System.out.println("Call was added to the least loaded server");
        }
        destinationMediaLayer.incrementNumberOfCalls();

        loadRedis.setConversationId(legId, conversationId);
        callRepo.save(new Call(legId, conversationId, mediaLayerNumber, curTime));
        mediaLayerRepo.save(destinationMediaLayer);

        return "Send the call to media layer number : " + destinationMediaLayer.getLayerNumber();
    }

    private void updateDurationOfCalls(List<MediaLayer> mediaLayerList, long curTime) {
        for (MediaLayer mediaLayer : mediaLayerList) {
            mediaLayer.updateLastModified(curTime);
        }
        mediaLayerRepo.saveAll(mediaLayerList);
    }

    private MediaLayer getMin(List<MediaLayer> mediaLayerList) {
        int minLayerIdx = -1;
        long minDuration = Long.MAX_VALUE;
        float ratio = 1;

        for (int idx = 0; idx < mediaLayerList.size(); idx++) {
            MediaLayer curMediaLayer = mediaLayerList.get(idx);
            int numCalls = curMediaLayer.getNumberOfCalls();
            float maxLoad = curMediaLayer.getMaxLoad();
            float curRatio = numCalls / maxLoad;
            long curDuration = curMediaLayer.getDuration();
            if (curRatio < ratio || (curRatio == ratio && curDuration < minDuration)) {
                minLayerIdx = idx;
                minDuration = curDuration;
                ratio = curRatio;
            }
        }
        return mediaLayerList.get(minLayerIdx);
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

        String conversationId = loadRedis.getConversationId(event.getCoreUUID());
        String legId = event.getCoreUUID();
        Optional<ConversationDetails> optionalConversationDetails = conversationsRepo.findById(conversationId);
        Optional<Call> optionalCurrentCall = callRepo.findById(legId);

        if (optionalConversationDetails.isPresent() && optionalCurrentCall.isPresent()) {
            ConversationDetails conversationDetails = optionalConversationDetails.get();
            Call currentCall = optionalCurrentCall.get();

            callRepo.deleteById(legId);
            loadRedis.remove(legId);
            conversationDetails.decrementLegCount();

            if (conversationDetails.getLegCount() == 0) {
                conversationsRepo.deleteById(conversationId);
            } else {
                conversationsRepo.save(conversationDetails);
            }

            return updateMediaLayerDatabaseHangupEvent(conversationDetails, currentCall);

        } else {
            System.out.println("FALSE POSITIVE HANGUP EVENT");
            return false;
        }
    }

    private Boolean updateMediaLayerDatabaseHangupEvent(ConversationDetails conversationDetails, Call currentCall) {
        Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(conversationDetails.getMediaLayerNumber());
        if (optionalMediaLayer.isPresent()) {
            MediaLayer mediaLayer = optionalMediaLayer.get();


            mediaLayer.decreaseDuration(System.currentTimeMillis(), currentCall.getTimeStamp());
            mediaLayer.decrementNumberOfCalls();

            mediaLayerRepo.save(mediaLayer);
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
