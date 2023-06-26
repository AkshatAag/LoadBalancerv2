package com.example.loadBalancer.repository;

import com.example.loadBalancer.entity.ConversationDetails;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConversationsRepo extends MongoRepository<ConversationDetails, String> {
}
