package com.example.loadBalancer.service;

import com.example.loadBalancer.Utils.Utils;
import com.example.loadBalancer.entity.Call;
import com.example.loadBalancer.entity.CallFromControlLayer;
import com.example.loadBalancer.entity.EventFromMediaLayer;
import com.example.loadBalancer.entity.MediaLayer;
import com.example.loadBalancer.repository.CallRepo;
import com.example.loadBalancer.repository.LoadRedis;
import com.example.loadBalancer.repository.MediaLayerRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;


@org.springframework.stereotype.Service
public class Service {
    @Autowired
    private LoadRedis loadRedis;
    @Autowired
    private CallRepo callRepo;
    @Autowired
    private MediaLayerRepo mediaLayerRepo;
    static Logger logger = LogManager.getLogger(Service.class);

    public String processEventControlLayer(CallFromControlLayer callFromControlLayer) {
        int mediaLayerNumber = getMediaLayerNumber(callFromControlLayer);
        String legId = callFromControlLayer.getLegId();
        String conversationId = callFromControlLayer.getConversationId();
        MediaLayer destination = null;

        List<MediaLayer> mediaLayerList = mediaLayerRepo.findAll();
        updateDurationOfCalls(mediaLayerList);

        if (mediaLayerNumber != -1) { //if this conversation already has an ongoing media layer assigned to it
            Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(mediaLayerNumber);
            destination = optionalMediaLayer.orElseThrow();
        } else {
            destination = getMin(mediaLayerList);
            mediaLayerNumber = destination.getLayerNumber();
            loadRedis.setMediaLayer(conversationId, String.valueOf(mediaLayerNumber));
        }

        callRepo.save(new Call(legId, conversationId, mediaLayerNumber, System.currentTimeMillis()));
        loadRedis.setConversationId(legId, conversationId);
        destination.incrLoad();
        mediaLayerRepo.save(destination);
        return "Send the call to media layer number : " + destination.getLayerNumber();
    }

    private void updateDurationOfCalls(List<MediaLayer> mediaLayerList) {
        long curTime = System.currentTimeMillis();
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
            int maxLoad = (int) (curMediaLayer.getMaxLoad() * Utils.getNumberFromString(curMediaLayer.getStatus()));
            float curRatio = (float) numCalls / maxLoad;
            long curDuration = curMediaLayer.getDuration();
            if (curRatio < ratio || (curRatio == ratio && curDuration < minDuration)) {
                minLayerIdx = idx;
                minDuration = curDuration;
                ratio=curRatio;
            }
        }
        return mediaLayerList.get(minLayerIdx);
    }

    public int getMediaLayerNumber(CallFromControlLayer callFromControlLayer) {
        String conversationId = callFromControlLayer.getConversationId();
        return loadRedis.getMediaLayer(conversationId);
    }

    public String processEventFromMediaLayer(EventFromMediaLayer event) {
        if (event.getEventName().equals("CHANNEL_HANGUP")) {
            String conversationId = loadRedis.getConversationId(event.getCoreUUID());
            int mediaLayerNumber = loadRedis.getMediaLayer(conversationId);
            Optional<Call> optionalCurCall = callRepo.findById(event.getCoreUUID());
            Call curCall = optionalCurCall.get();
            callRepo.deleteById(event.getCoreUUID());

            loadRedis.remove(event.getCoreUUID());
            Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(mediaLayerNumber);
            MediaLayer mediaLayer = optionalMediaLayer.get();

            mediaLayer.decreaseDuration(System.currentTimeMillis(), curCall.getTimeStamp());
            mediaLayer.decrLoad();

            mediaLayerRepo.save(mediaLayer);

        }
        return "EVENT FROM THE MEDIA LAYER WAS PROCESSED";
    }

    public String addNewMediaLayer(MediaLayer mediaLayer) {
        mediaLayer.setLastModified(System.currentTimeMillis());
        mediaLayerRepo.save(mediaLayer);
        return "NEW Media Layer was added to Mongo";
    }


    public String setServerStatus(int layerNumber, String color) {
        Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(layerNumber);
        MediaLayer mediaLayer = optionalMediaLayer.get();
        mediaLayer.setStatus(color);
        mediaLayerRepo.save(mediaLayer);
        return "Server number " + layerNumber + " status was changed to " + color;
    }
}
