package com.example.loadBalancer.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LoadRedis {
    @Autowired
    private RedisTemplate template;

    public String getMediaLayer(String conversationId) {
        return (String) template.opsForValue().get(conversationId);
    }

    public void setMediaLayer(String conversationId, String res) {
        template.opsForValue().set(conversationId, res);
    }

    public void setConversationId(String legId, String conversationId) {
        template.opsForValue().set(legId, conversationId);
    }

    public String getConversationId(String legId) {
        return (String) template.opsForValue().get(legId);
    }

    public void remove(String key) {
        template.opsForValue().getOperations().delete(key);
    }
}
