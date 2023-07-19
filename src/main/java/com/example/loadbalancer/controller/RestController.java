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

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.concurrent.ExecutionException;

@Validated
@org.springframework.web.bind.annotation.RestController
@RequestMapping("controller")
public class RestController {
    private final Service service;

    @Autowired
    public RestController(Service service) {
        this.service = service;
    }

    @GetMapping("/init")
    public String init() {
        return service.initialize();
    }

    @GetMapping("/server_status")
    public String getServerStatus(@RequestParam(required = false, defaultValue = "all") String serverAddress){
        return service.getServerStatus(serverAddress);
    }

    @PostMapping("/control_layer/{alg}")
    public String processEventFromControlLayer(@RequestBody @Valid CallFromControlLayerDTO callFromControlLayerDTO,
                                               @PathVariable
                                               @Min(value = 1, message = "Select an algorithm between 1-2")
                                               @Max(value = 2, message = "Select an algorithm between 1-2") String alg) throws ExecutionException, InterruptedException {
        //return the destination media layer server's number
        if (alg == null) alg = "1";
        CallFromControlLayer callFromControlLayer = new CallFromControlLayer(callFromControlLayerDTO);
        return service.processEventControlLayer(callFromControlLayer, alg).get();
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
    public String changeServerStatus(@PathVariable @NotBlank(message = "Layer number cannot be blank") String layerNumber,
                                     @PathVariable
                                     @NotBlank(message = "Enter a valid color")
                                     @Pattern(regexp = "^(red|green|orange|yellow)$", message = "Color can only be red, green, orange or yellow.")
                                     String color) {
        //used to change the max load of a server. The max load depends on the color category of the server
        return service.setServerStatus(layerNumber, color);
    }

    @GetMapping("/set_faulty_status/{layerNumber}/{faulty}")
    public String changeServerStatus(@PathVariable @NotBlank(message = "Layer number cannot be blank") String layerNumber,
                                     @PathVariable @NotNull boolean faulty) {
        //used to change the current condition of server to faulty. Calls are not routed to faulty server.
        return service.setFaultyStatus(layerNumber, faulty);
    }
}
