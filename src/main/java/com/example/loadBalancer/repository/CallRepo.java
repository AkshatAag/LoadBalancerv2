package com.example.loadBalancer.repository;

import com.example.loadBalancer.entity.Call;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallRepo extends MongoRepository<Call,String> {
}
