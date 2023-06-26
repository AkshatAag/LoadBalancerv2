package com.example.loadBalancer.controller;

import com.example.loadBalancer.entity.*;
import com.example.loadBalancer.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("controller")
public class MyController {
    @Autowired
    private Service service;

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
