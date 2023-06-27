package com.example.loadbalancer.controller;

import com.example.loadbalancer.dto.CallFromControlLayerDTO;
import com.example.loadbalancer.dto.EventFromMediaLayerDTO;
import com.example.loadbalancer.dto.MediaLayerDTO;
import com.example.loadbalancer.entity.*;
import com.example.loadbalancer.service.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("controller")
public class RestController {
    private final Service service;

    @Autowired
    public RestController(Service service) {
        this.service = service;
    }


    @PostMapping("/control_layer")
    public String processEventFromControlLayer(@RequestBody CallFromControlLayerDTO callFromControlLayerDTO) {
        CallFromControlLayer callFromControlLayer = new CallFromControlLayer(callFromControlLayerDTO);
        return service.processEventControlLayer(callFromControlLayer);
    }

    @PostMapping("/new_event")
    public String processEventFromMediaLayer(@RequestBody EventFromMediaLayerDTO eventDTO) {
        EventFromMediaLayer event = new EventFromMediaLayer(eventDTO);
        return service.processEventFromMediaLayer(event);
    }

    @PostMapping("/add_new_layer")
    public String addNewMediaLayer(@RequestBody MediaLayerDTO mediaLayerDTO) {
        MediaLayer mediaLayer = new MediaLayer(mediaLayerDTO);
        return service.addNewMediaLayer(mediaLayer);
    }

    @GetMapping("/change_status/{layerNumber}/{color}")
    public String changeServerStatus(@PathVariable String layerNumber, @PathVariable String color) {
        return service.setServerStatus(layerNumber, color);
    }

    @GetMapping("/set_faulty_status/{layerNumber}/{faulty}")
    public String changeServerStatus(@PathVariable String layerNumber, @PathVariable boolean faulty) {
        return service.setFaultyStatus(layerNumber, faulty);
    }
}
