package com.example.loadBalancer.repository;

import com.example.loadBalancer.entity.FreeswitchMediaLayerLoad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LoadRedis {
    @Autowired
    private RedisTemplate template;
    public int findMediaLayer(String conversationId) {
        String s= (String) template.opsForValue().get(conversationId);
        if(s!=null) return Integer.parseInt(s);
        return -1;
    }
    public List<FreeswitchMediaLayerLoad> findAllLoads() {
        return  template.opsForHash().values("FreeswitchMediaLayerLoad");
    }
    public FreeswitchMediaLayerLoad setLoad(FreeswitchMediaLayerLoad freeswitchMediaLayerLoad) {
        template.opsForHash().put("FreeswitchMediaLayerLoad", freeswitchMediaLayerLoad.getLayerNumber(), freeswitchMediaLayerLoad);
        return freeswitchMediaLayerLoad;
    }

    public void setMediaLayer(String conversationId, String res) {
        template.opsForValue().set(conversationId,res);
    }

    public void setConversationId(String legId, String conversationId) {
        template.opsForValue().set(legId,conversationId);
    }

    public String getConversationId(String legId) {
        return (String) template.opsForValue().get(legId);
    }

    public int getLoad(int mediaLayerNumber) {
        return (int) template.opsForValue().get(mediaLayerNumber);
    }
}
