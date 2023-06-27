package com.example.load_balancer.repository;

import com.example.load_balancer.entity.Call;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallRepo extends MongoRepository<Call, String> {
}
