package com.PFE.backend.services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private final StringRedisTemplate redis;

    public RedisService(StringRedisTemplate redis){
        this.redis = redis;
    }

    public void setContainer(String containerId){
        redis.opsForHash().put("container:" + containerId, "lastSeen", String.valueOf(System.currentTimeMillis()));
    }

    public void updateHeartbeat(String containerId){
        redis.opsForHash().put("container:" + containerId, "lastSeen", String.valueOf(System.currentTimeMillis()));
    }

    public boolean containerExists(String containerId){
        return Boolean.TRUE.equals(redis.hasKey("container:" + containerId));
    }

    public void deleteContainer(String containerId){
        redis.delete("container:" + containerId);
    }
}
