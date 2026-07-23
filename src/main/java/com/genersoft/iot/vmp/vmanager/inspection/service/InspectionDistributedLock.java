package com.genersoft.iot.vmp.vmanager.inspection.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Component
public class InspectionDistributedLock {
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",
                    Long.class);
    private final StringRedisTemplate redis;

    public InspectionDistributedLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String acquire(String key, long seconds) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redis.opsForValue().setIfAbsent(
                key, token, Duration.ofSeconds(seconds));
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    public void release(String key, String token) {
        if (token != null) {
            redis.execute(RELEASE_SCRIPT, Collections.singletonList(key), token);
        }
    }
}
