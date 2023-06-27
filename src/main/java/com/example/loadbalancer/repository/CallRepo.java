package com.example.loadbalancer.repository;

import com.example.loadbalancer.entity.Call;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallRepo extends MongoRepository<Call, String> {
}
