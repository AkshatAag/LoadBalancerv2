package com.example.loadBalancer.repository;

import com.example.loadBalancer.entity.MediaLayer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaLayerRepo extends MongoRepository<MediaLayer, String> {

    List<MediaLayer> findByFaulty(boolean faulty);
}
