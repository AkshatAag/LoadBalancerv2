package com.example.loadBalancer.repository;

import com.example.loadBalancer.entity.MediaLayer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MediaLayerRepo extends MongoRepository<MediaLayer,Integer> {

}
