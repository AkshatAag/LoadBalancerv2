package com.example.loadbalancer.service;

import com.example.loadbalancer.entity.Call;
import com.example.loadbalancer.entity.MediaLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name = "instantiate-once", havingValue = "true")
public class IndexGenerator {
    private final MongoTemplate mongoTemplate;

    @Autowired
    public IndexGenerator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void generateIndexes() {
        Index indexCallConversationId = new Index().on("conversationId", Sort.Direction.ASC);
        Index indexMediaLayerRatioDuration = new Index().on("ratio", Sort.Direction.ASC).on("duration", Sort.Direction.ASC);

        mongoTemplate.indexOps(MediaLayer.class).ensureIndex(indexCallConversationId);
        mongoTemplate.indexOps(Call.class).ensureIndex(indexMediaLayerRatioDuration);

        System.out.println("INDEXES WERE GENERATED");
    }
}
