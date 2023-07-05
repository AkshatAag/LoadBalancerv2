package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.Call;
import com.example.loadbalancer.entity.MediaLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class IndexGenerator {
    private final MongoTemplate mongoTemplate;
    private final Environment environment;
    @Autowired
    public IndexGenerator(MongoTemplate mongoTemplate, Environment environment) {
        this.mongoTemplate = mongoTemplate;
        this.environment = environment;
    }

    @PostConstruct
    public void generateIndexes() {
        if (!"8080".equals(environment.getProperty("server.port"))) return;
        Index indexCallConversationId = new Index().on("conversationId", Sort.Direction.ASC);
        Index indexMediaLayerRatioDuration = new Index().on("ratio", Sort.Direction.ASC).on("duration", Sort.Direction.ASC);

        mongoTemplate.indexOps(MediaLayer.class).ensureIndex(indexCallConversationId);
        mongoTemplate.indexOps(Call.class).ensureIndex(indexMediaLayerRatioDuration);

        System.out.println("INDEXES WERE GENERATED");
    }
}
