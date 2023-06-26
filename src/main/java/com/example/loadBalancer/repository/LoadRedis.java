package com.example.loadBalancer.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LoadRedis {
    @Autowired
    private RedisTemplate template;

    public String getConversationId(String legId) {
        return (String) template.opsForValue().get(legId);
    }

    public void setConversationId(String legId, String res) {
        template.opsForValue().set(legId, res);
    }

    public void remove(String key) {
        template.opsForValue().getOperations().delete(key);
    }
}
