package com.example.loadbalancer.service;

import com.example.loadbalancer.dto.MediaLayerDTO;
import com.example.loadbalancer.entity.Call;
import com.example.loadbalancer.entity.CallFromControlLayer;
import com.example.loadbalancer.entity.ConversationDetails;
import com.example.loadbalancer.entity.MediaLayer;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTest {
    private static final String CONNECTION_STRING = "mongodb://%s:%d";
    private static MongodExecutable mongodExecutable;
    private static Service mockService;
    private static MongoTemplate mongoTemplate;

    @AfterAll
    static void clean() {
        mongodExecutable.stop();
    }

    @BeforeAll
    static void setup() throws Exception {
        String ip = "localhost";
        int port = 27020;

        ImmutableMongodConfig mongodConfig = MongodConfig
                .builder()
                .version(Version.Main.V5_0)
                .net(new Net(ip, port, Network.localhostIsIPv6()))
                .build();

        MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(mongodConfig);
        mongodExecutable.start();
        mongoTemplate = new MongoTemplate(MongoClients.create(String.format(CONNECTION_STRING, ip, port)), "test");

        mockService = new Service(mongoTemplate);
    }

    @Test
    void addSameMediaLayerMultipleTimes(){
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("2")));
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("2")));
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("2")));
        assertEquals(1,mongoTemplate.findAll(MediaLayer.class).size());
    }
    @Test
    void newCallWhenNoExistingMediaLayerPresent() {
        assertEquals("-1", mockService.processEventControlLayer(new CallFromControlLayer("3", "b"), 1));
    }

    @Test
    void sameControlLayerCallMultipleTimes() throws InterruptedException {
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "b"), 1);
        Thread.sleep(5000);
        assertEquals(Objects.requireNonNull(mongoTemplate.findById("1", MediaLayer.class)).getDuration(),(long) 0);
//        assertEquals(1, mongoTemplate.findAll(Call.class).size());
//        assertEquals(1, mongoTemplate.findAll(ConversationDetails.class).size());
    }


    @Test
    void setServerStatus() {
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("1")));
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("2")));
        mockService.setServerStatus("2","green");
        assertEquals(10, Objects.requireNonNull(mongoTemplate.findById("2", MediaLayer.class)).getMaxLoad());
    }
}
