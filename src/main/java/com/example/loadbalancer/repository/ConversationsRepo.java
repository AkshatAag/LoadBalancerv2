package com.example.loadbalancer.repository;

import com.example.loadbalancer.entity.ConversationDetails;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConversationsRepo extends MongoRepository<ConversationDetails, String> {
}
