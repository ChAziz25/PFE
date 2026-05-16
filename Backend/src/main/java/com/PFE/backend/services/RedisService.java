package com.PFE.backend.services;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setContainerTTL(String containerId){
        redisTemplate.opsForValue().set(
                "container:" + containerId,
                "active",
                Duration.ofMinutes(1)
        );
    }
}
