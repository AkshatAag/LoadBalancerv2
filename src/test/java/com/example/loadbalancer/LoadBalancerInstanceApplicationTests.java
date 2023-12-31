package com.example.loadbalancer;

import com.example.loadbalancer.dto.MediaLayerDTO;
import com.example.loadbalancer.entity.Call;
import com.example.loadbalancer.entity.CallFromControlLayer;
import com.example.loadbalancer.entity.EventFromMediaLayer;
import com.example.loadbalancer.entity.MediaLayer;
import com.example.loadbalancer.exceptions.CallCannotBeAddedAgainException;
import com.example.loadbalancer.exceptions.NoFreeMediaServerException;
import com.example.loadbalancer.exceptions.NoSuchObjectInDatabaseException;
import com.example.loadbalancer.service.Service;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest(classes = {LoadBalancerInstanceApplicationTests.class})
class LoadBalancerInstanceApplicationTests {

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
        int port = 27017;

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
    void newCallWhenNoExistingMediaLayerPresent() throws ExecutionException, InterruptedException {
        mongoTemplate.remove(new Query(), MediaLayer.class);
        mongoTemplate.remove(new Query(), Call.class);
        Exception e = assertThrows(ExecutionException.class, () -> mockService.processEventControlLayer(new CallFromControlLayer("8", "bvfd"), "1").get());
        assertEquals(NoFreeMediaServerException.class, e.getCause().getClass());
    }

    @Test
    void sameControlLayerCallMultipleTimes() throws ExecutionException, InterruptedException {
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "b"), "1").get();
        Exception e = assertThrows(ExecutionException.class, () -> mockService.processEventControlLayer(new CallFromControlLayer("1", "b"), "1").get());
        assertEquals(CallCannotBeAddedAgainException.class, e.getCause().getClass());
    }

    @Test
    void setServerStatusGreenToOrange() {
        mockService.addNewMediaLayer(new MediaLayer(new MediaLayerDTO("2")));
        mockService.setServerStatus("2", "orange");
        assertEquals(25, Objects.requireNonNull(mongoTemplate.findById("2", MediaLayer.class)).getMaxLoad());
    }

    @Test
    void setServerStatusRedToYellow() {
        mockService.addNewMediaLayer(new MediaLayer(false, "2", "red", 123432, 2143, 0, 2.5F, 0, 123));
        mockService.setServerStatus("2", "yellow");
        assertEquals(10, Objects.requireNonNull(mongoTemplate.findById("2", MediaLayer.class)).getMaxLoad());
    }

    @Test
    void setServerStatusMediaLayerDoesNotExist() {
        assertThrows(NoSuchObjectInDatabaseException.class, () -> mockService.setServerStatus("2", "yellow"));
    }

    @Test
    void setServerFaultyFalseToTrue() {
        mockService.addNewMediaLayer(new MediaLayer(false, "2", "red", 123432, 2143, 0, 2.5F, 0, 123));
        assertEquals(HttpStatus.OK.toString(), mockService.setFaultyStatus("2", true));
    }

    @Test
    void setServerFaultyMediaLayerDoesNotExist() {
        assertThrows(NoSuchObjectInDatabaseException.class, () -> mockService.setFaultyStatus("2", true));
    }


    @Test
    void processMediaEventHangupNoCall() {
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), "1");
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), "1");
        assertThrows(NoSuchObjectInDatabaseException.class,
                () -> mockService.processEventFromMediaLayer(new EventFromMediaLayer("1", "CHANNEL_HANGUP")));
    }

    @Test
    void processMediaEventMute() {
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), "1");
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), "1");
        assertEquals(HttpStatus.OK.toString(),
                mockService.processEventFromMediaLayer(new EventFromMediaLayer("1", "CHANNEL_MUTE")));
    }

    @Test
    void processMediaEventHangupNoCallExists() {
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), "1");
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), "1");
        assertThrows(NoSuchObjectInDatabaseException.class,
                () -> mockService.processEventFromMediaLayer(new EventFromMediaLayer("4", "CHANNEL_HANGUP")));
    }

    @Test
    void processMediaEventHangupNoConversationExists() {
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), "1");
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), "1");
        assertThrows(NoSuchObjectInDatabaseException.class,
                () -> mockService.processEventFromMediaLayer(new EventFromMediaLayer("1", "CHANNEL_HANGUP")));
    }

    @Test
    void processMediaEventHangupNoMediaLayerExists() {
        mongoTemplate.save(new MediaLayer(new MediaLayerDTO("1")));
        mockService.processEventControlLayer(new CallFromControlLayer("1", "abc"), "1");
        mockService.processEventControlLayer(new CallFromControlLayer("2", "abc"), "1");
        mongoTemplate.remove(new Query(), MediaLayer.class);
        assertThrows(NoSuchObjectInDatabaseException.class,
                () -> mockService.processEventFromMediaLayer(new EventFromMediaLayer("1", "CHANNEL_HANGUP")));
    }


}

