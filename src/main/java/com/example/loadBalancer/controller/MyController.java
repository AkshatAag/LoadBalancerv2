package com.example.loadBalancer.controller;

import com.example.loadBalancer.entity.CallFromControlLayer;
import com.example.loadBalancer.entity.EventFromMediaLayer;
import com.example.loadBalancer.entity.MediaLayer;
import com.example.loadBalancer.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("controller")
public class MyController {
    @Autowired
    private Service service;
    @PostMapping("/control_layer")
    public String processEventFromControlLayer(@RequestBody CallFromControlLayer callFromControlLayer){
        return service.processEventControlLayer(callFromControlLayer);
    }
    @PostMapping("/new_event")
    public String processEventFromMediaLayer(@RequestBody EventFromMediaLayer event){
        return service.processEventFromMediaLayer(event);
    }
    @PostMapping("/add_new_layer")
    public String addNewMediaLayer(@RequestBody MediaLayer mediaLayer){
        return service.addNewMediaLayer(mediaLayer);
    }
}
