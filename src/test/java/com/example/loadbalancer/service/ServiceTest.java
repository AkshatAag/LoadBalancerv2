package com.example.loadbalancer.service;

import com.example.loadbalancer.dto.MediaLayerDTO;
import com.example.loadbalancer.entity.*;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {ServiceTest.class})
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
    static void setup() throws IOException {
        String ip = "localhost";
        int port = 27021;

        ImmutableMongodConfig mongodConfig = MongodConfig.builder().version(Version.Main.V5_0).net(new Net(ip, port, Network.localhostIsIPv6())).build();

        MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(mongodConfig);
        mongodExecutable.start();
        mongoTemplate = new MongoTemplate(MongoClients.create(String.format(CONNECTION_STRING, ip, port)), "test");
        mockService = new Service(mongoTemplate);
    }

    @AfterEach
    void clearDatabase() {
        mongoTemplate.remove(new Query(), MediaLayer.class);
        mongoTemplate.remove(new Query(), Call.class);
        mongoTemplate.remove(new Query(), ConversationDetails.class);
    }

    @Test
    void addSameMediaLayerMultipleTimes() {
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("3")));
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("3")));
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("3")));
        System.out.println(mongoTemplate.findAll(MediaLayer.class).toString());
        assertEquals(1, mongoTemplate.findAll(MediaLayer.class).size());
    }

    @Test
    void newCallWhenNoExistingMediaLayerPresent() {
        mongoTemplate.remove(new Query(), MediaLayer.class);
        assertEquals("-1", mockService.processEventControlLayer(new CallFromControlLayer("3", "b"), 1));
    }

    @Test
    void sameControlLayerCallMultipleTimes() {
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "b"), 1);
        mockService.processEventControlLayer(new CallFromControlLayer("1", "b"), 1);
        assertEquals(1, mongoTemplate.findAll(Call.class).size());
        assertEquals(1, mongoTemplate.findAll(ConversationDetails.class).size());
        assertEquals(Objects.requireNonNull(mongoTemplate.findById("1", MediaLayer.class)).getDuration(), (long) 0);
    }

    @Test
    void setServerStatusGreenToOrange() {
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("2")));
        mockService.setServerStatus("2", "orange");
        assertEquals(2.5, Objects.requireNonNull(mongoTemplate.findById("2", MediaLayer.class)).getMaxLoad());
    }

    @Test
    void setServerStatusRedToYellow() {
        mockService.addNewMediaLayer(new MediaLayer(false, "2", "red", 123432, 2143, 0, 2.5F, 0, 123));
        mockService.setServerStatus("2", "yellow");
        assertEquals(10, Objects.requireNonNull(mongoTemplate.findById("2", MediaLayer.class)).getMaxLoad());
    }

    @Test
    void setServerStatusWrongColor() {
        mockService.addNewMediaLayer(new MediaLayer(false, "2", "red", 123432, 2143, 0, 2.5F, 0, 123));
        assertEquals(HttpStatus.BAD_REQUEST.toString(), mockService.setServerStatus("2", "black"));
    }

    @Test
    void setServerStatusMediaLayerDoesNotExist() {
        assertEquals(HttpStatus.BAD_REQUEST.toString(), mockService.setServerStatus("2", "yellow"));
    }

    @Test
    void setServerFaultyFalseToTrue() {
        mockService.addNewMediaLayer(new MediaLayer(false, "2", "red", 123432, 2143, 0, 2.5F, 0, 123));
        assertEquals(HttpStatus.OK.toString(), mockService.setFaultyStatus("2", true));
    }

    @Test
    void setServerFaultyMediaLayerDoesNotExist() {
        assertEquals(HttpStatus.BAD_REQUEST.toString(), mockService.setFaultyStatus("2", true));
    }


    @Test
    void processMediaEventHangup() {
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), 1);
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), 1);
        assertEquals(HttpStatus.OK.toString(),
                mockService.processEventFromMediaLayer(new EventFromMediaLayer("1", "CHANNEL_HANGUP")));
    }

    @Test
    void processMediaEventMute() {
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), 1);
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), 1);
        assertEquals(HttpStatus.OK.toString(),
                mockService.processEventFromMediaLayer(new EventFromMediaLayer("1", "CHANNEL_MUTE")));
    }

    @Test
    void processMediaEventMuteWrongID(){
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), 1);
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), 1);
        assertEquals(HttpStatus.OK.toString(),
                mockService.processEventFromMediaLayer(new EventFromMediaLayer("1", "CHANNEL_MUTE")));
    }
    @Test
    void processMediaEventHangupNoCallExists(){
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), 1);
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), 1);
        assertEquals(HttpStatus.BAD_REQUEST.toString(),
                mockService.processEventFromMediaLayer(new EventFromMediaLayer("4", "CHANNEL_HANGUP")));
    }
    @Test
    void processMediaEventHangupNoConversationExists(){
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), 1);
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), 1);
        mongoTemplate.remove(new Query(),ConversationDetails.class);
        assertEquals(HttpStatus.BAD_REQUEST.toString(),
                mockService.processEventFromMediaLayer(new EventFromMediaLayer("1", "CHANNEL_HANGUP")));
    }
    @Test
    void processMediaEventHangupNoMediaLayerExists(){
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), 1);
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), 1);
        mongoTemplate.remove(new Query(),MediaLayer.class);
        assertEquals(HttpStatus.BAD_REQUEST.toString(),
                mockService.processEventFromMediaLayer(new EventFromMediaLayer("1", "CHANNEL_HANGUP")));
    }

    @Test
    @Disabled
    void processEventControlLayer() {

    }
}
