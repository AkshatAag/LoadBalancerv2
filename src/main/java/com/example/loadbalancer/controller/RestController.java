package com.example.loadbalancer.controller;

import com.example.loadbalancer.dto.CallFromControlLayerDTO;
import com.example.loadbalancer.dto.EventFromMediaLayerDTO;
import com.example.loadbalancer.dto.MediaLayerDTO;
import com.example.loadbalancer.entity.CallFromControlLayer;
import com.example.loadbalancer.entity.EventFromMediaLayer;
import com.example.loadbalancer.entity.MediaLayer;
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


    @PostMapping("/control_layer/{alg}")
    public String processEventFromControlLayer(@RequestBody CallFromControlLayerDTO callFromControlLayerDTO,@PathVariable int alg) {
        //return the destination media layer server's number
        CallFromControlLayer callFromControlLayer = new CallFromControlLayer(callFromControlLayerDTO);
        return service.processEventControlLayer(callFromControlLayer,alg);
    }

    @PostMapping("/new_event")
    public String processEventFromMediaLayer(@RequestBody EventFromMediaLayerDTO eventDTO) {
        //makes changes to current state of database as per calls received from media layer
        EventFromMediaLayer event = new EventFromMediaLayer(eventDTO);
        return service.processEventFromMediaLayer(event);
    }

    @PostMapping("/add_new_layer")
    public String addNewMediaLayer(@RequestBody MediaLayerDTO mediaLayerDTO) {
        //adds a new layer to the load balancer
        MediaLayer mediaLayer = new MediaLayer(mediaLayerDTO);
        return service.addNewMediaLayer(mediaLayer);
    }

    @GetMapping("/change_status/{layerNumber}/{color}")
    public String changeServerStatus(@PathVariable String layerNumber, @PathVariable String color) {
        //used to change the max load of a server. The max load depends on the color category of the server
        return service.setServerStatus(layerNumber, color);
    }

    @GetMapping("/set_faulty_status/{layerNumber}/{faulty}")
    public String changeServerStatus(@PathVariable String layerNumber, @PathVariable boolean faulty) {
        //used to change the current condition of server to faulty. Calls are not routed to faulty server.
        return service.setFaultyStatus(layerNumber, faulty);
    }
}
