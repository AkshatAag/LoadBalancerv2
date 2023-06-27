package com.example.loadbalancer.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LoadRedis {
    private final RedisTemplate<String, String> template;

    @Autowired
    public LoadRedis(RedisTemplate<String,String> template) {
        this.template = template;
    }

    public String getConversationId(String legId) {
        return template.opsForValue().get(legId);
    }

    public void setConversationId(String legId, String res) {
        template.opsForValue().set(legId, res);
    }

    public void remove(String key) {
        template.opsForValue().getOperations().delete(key);
    }
}
