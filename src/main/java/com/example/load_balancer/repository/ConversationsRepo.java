package com.example.load_balancer.repository;

import com.example.load_balancer.entity.ConversationDetails;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConversationsRepo extends MongoRepository<ConversationDetails, String> {
}
