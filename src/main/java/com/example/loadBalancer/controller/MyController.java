package com.example.loadBalancer.controller;

import com.example.loadBalancer.entity.CallFromControlLayer;
import com.example.loadBalancer.entity.EventFromMediaLayer;
import com.example.loadBalancer.entity.FreeswitchMediaLayerLoad;
import com.example.loadBalancer.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("controller")
public class MyController {
    @Autowired
    private Service service;
    @PostMapping("/controlLayer")
    public String getMediaLayer(@RequestBody CallFromControlLayer callFromControlLayer){
        return service.getMediaLayerNumber(callFromControlLayer);
    }
    @PostMapping("/mediaLayer")
    public String processEventFromMediaLayer(@RequestBody EventFromMediaLayer event){
        return service.processEventFromMediaLayer(event);
    }
    @PostMapping("/add_new_layer")
    public String addNewMediaLayer(@RequestBody FreeswitchMediaLayerLoad freeswitchMediaLayerLoad){
        return service.addNewMediaLayer(freeswitchMediaLayerLoad);
    }
}
