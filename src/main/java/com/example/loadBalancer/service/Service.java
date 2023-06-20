package com.example.loadBalancer.service;

import com.example.loadBalancer.entity.Call;
import com.example.loadBalancer.entity.CallFromControlLayer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.example.loadBalancer.entity.EventFromMediaLayer;
import com.example.loadBalancer.entity.MediaLayer;
import com.example.loadBalancer.repository.CallRepo;
import com.example.loadBalancer.repository.LoadRedis;
import com.example.loadBalancer.repository.MediaLayerRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


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
        long curTime = System.currentTimeMillis();
        for(MediaLayer mediaLayer : mediaLayerList) {
            mediaLayer.updateLastModified(curTime);
        }
        mediaLayerRepo.saveAll(mediaLayerList);

        if (mediaLayerNumber != -1) { //if this conversation already has an ongoing media layer assigned to it
            Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(mediaLayerNumber);
            destination = optionalMediaLayer.orElseThrow();
        } else {
            destination = Collections.min(mediaLayerList);
            mediaLayerNumber = destination.getLayerNumber();
            loadRedis.setMediaLayer(conversationId, String.valueOf(mediaLayerNumber));
        }

        callRepo.save(new Call(legId, conversationId, mediaLayerNumber, System.currentTimeMillis()));
        loadRedis.setConversationId(legId, conversationId);
        destination.incrLoad();
        mediaLayerRepo.save(destination);
        return "Send the call to media layer number : " + destination.getLayerNumber();
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

            mediaLayer.decreaseDuration(System.currentTimeMillis(),curCall.getTimeStamp());
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


}
