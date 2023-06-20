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

        if (mediaLayerNumber != -1) { //if this conversation already has an ongoing media layer assigned to it
            Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(mediaLayerNumber);
            destination = optionalMediaLayer.orElseThrow();
        } else {
            destination = getLeastLoaded();
            mediaLayerNumber = destination.getLayerNumber();
            loadRedis.setMediaLayer(conversationId, String.valueOf(mediaLayerNumber));
        }

        callRepo.save(new Call(legId, conversationId, mediaLayerNumber, LocalDateTime.now()));
        loadRedis.setConversationId(legId, conversationId);
        destination.updateLastModified(System.currentTimeMillis());
        destination.incrLoad();

        return "Send the call to media layer number : " + destination.getLayerNumber();
    }

    public int getMediaLayerNumber(CallFromControlLayer callFromControlLayer) {
        String conversationId = callFromControlLayer.getConversationId();
        return loadRedis.getMediaLayer(conversationId);
    }

    private MediaLayer getLeastLoaded() {
        List<MediaLayer> mediaLayerList = mediaLayerRepo.findAll();
        return Collections.min(mediaLayerList);
    }

    public String processEventFromMediaLayer(EventFromMediaLayer event) {
        if (event.getEventName().equals("CHANNEL_HANGUP")) {
            String conversationId = loadRedis.getConversationId(event.getCoreUUID());
            int mediaLayerNumber = loadRedis.getMediaLayer(conversationId);
            callRepo.deleteById(event.getCoreUUID());
            loadRedis.remove(event.getCoreUUID());
            Optional<MediaLayer> optionalMediaLayer = mediaLayerRepo.findById(mediaLayerNumber);
            MediaLayer mediaLayer = optionalMediaLayer.get();
            mediaLayer.updateLastModified(System.currentTimeMillis());
            mediaLayer.decrLoad();
        }
        return "EVENT FROM THE MEDIA LAYER WAS PROCESSED";
    }

    public String addNewMediaLayer(MediaLayer mediaLayer) {
        mediaLayerRepo.save(mediaLayer);
        return "NEW Media Layer was added to Mongo";
    }


}
