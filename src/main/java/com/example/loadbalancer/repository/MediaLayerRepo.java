package com.example.loadbalancer.repository;

import com.example.loadbalancer.entity.MediaLayer;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaLayerRepo extends MongoRepository<MediaLayer, String> {

    @Aggregation(pipeline = {
            "{ $match: { faulty: { $ne: true } } }",
            "{ $sort: { ratio: 1, duration: 1 } }",
            "{ $limit: 1 }"
    })
    MediaLayer findDestinationLeastConnections();
    @Aggregation(pipeline = {
            "{ $match: { faulty: { $ne: true } } }",
            "{ $sort: { latestCallTimeStamp: 1} }",
            "{ $limit: 1 }"
    })
    MediaLayer findDestinationRoundRobin();
}
