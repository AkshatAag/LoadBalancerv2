package com.example.loadbalancer.controller;

import com.example.loadbalancer.dto.CallFromControlLayerDTO;
import com.example.loadbalancer.dto.EventFromMediaLayerDTO;
import com.example.loadbalancer.dto.MediaLayerDTO;
import com.example.loadbalancer.entity.CallFromControlLayer;
import com.example.loadbalancer.entity.EventFromMediaLayer;
import com.example.loadbalancer.entity.MediaLayer;
import com.example.loadbalancer.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PreDestroy;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Validated
@org.springframework.web.bind.annotation.RestController
@RequestMapping("controller")
public class RestController {
    private final Service service;
    @Autowired
    public RestController(Service service) {
        this.service = service;
    }

    @GetMapping("/hi")
    public String hello() {
        return service.getTimeStamps().toString();
    }


    @PostMapping("/control_layer/{alg}")
    public String processEventFromControlLayer(@RequestBody @Valid CallFromControlLayerDTO callFromControlLayerDTO,
                                               @PathVariable(required = false)
                                               @Min(value = 1, message = "Select an algorithm between 1-2")
                                               @Max(value = 2, message = "Select an algorithm between 1-2") String alg) {
        //return the destination media layer server's number
        if (alg == null) alg = "1";
        CallFromControlLayer callFromControlLayer = new CallFromControlLayer(callFromControlLayerDTO);
        return service.processEventControlLayer(callFromControlLayer, alg);
    }

    @PostMapping("/new_event")
    public String processEventFromMediaLayer(@RequestBody @Valid EventFromMediaLayerDTO eventDTO) {
        //makes changes to current state of database as per calls received from media layer
        EventFromMediaLayer event = new EventFromMediaLayer(eventDTO);
        return service.processEventFromMediaLayer(event);
    }

    @PostMapping("/add_new_layer")
    public String addNewMediaLayer(@RequestBody @Valid MediaLayerDTO mediaLayerDTO) {
        //adds a new layer to the load balancer
        MediaLayer mediaLayer = new MediaLayer(mediaLayerDTO);
        return service.addNewMediaLayer(mediaLayer);
    }

    @GetMapping("/change_status/{layerNumber}/{color}")
    public String changeServerStatus(@PathVariable @NotBlank(message = "Layer number cannot be blank") String layerNumber, @PathVariable @NotBlank(message = "Enter a valid color") String color) {
        //used to change the max load of a server. The max load depends on the color category of the server
        return service.setServerStatus(layerNumber, color);
    }

    @GetMapping("/set_faulty_status/{layerNumber}/{faulty}")
    public String changeServerStatus(@PathVariable @NotBlank(message = "Layer number cannot be blank") String layerNumber, @PathVariable @NotNull boolean faulty) {
        //used to change the current condition of server to faulty. Calls are not routed to faulty server.
        return service.setFaultyStatus(layerNumber, faulty);
    }
}
