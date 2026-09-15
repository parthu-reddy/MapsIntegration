package com.fooddelivery.mapsintegration.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class RedisIdempotencyService {

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Claims an event for one processor without prematurely declaring it successful.
     * The short lease allows recovery if a JVM dies while processing.
     */
    public String beginProcessing(String key) {
        String claimToken = "PROCESSING:" + java.util.UUID.randomUUID();
        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(key, claimToken, Duration.ofMinutes(5));
        return Boolean.TRUE.equals(claimed) ? claimToken : null;
    }

    public void markProcessed(String key, String claimToken) {
        String script = "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                + "redis.call('SET', KEYS[1], 'PROCESSED', 'PX', ARGV[2]); return 1 else return 0 end";
        Long completed = redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                java.util.List.of(key), claimToken, Long.toString(Duration.ofDays(7).toMillis()));
        if (!Long.valueOf(1L).equals(completed)) {
            throw new IllegalStateException("Dispatch idempotency lease was lost before completion");
        }
    }

    /** Releases only this processor's in-progress marker; a completed marker is immutable. */
    public void releaseProcessing(String key, String claimToken) {
        String script = "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                + "return redis.call('DEL', KEYS[1]) else return 0 end";
        redisTemplate.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                java.util.List.of(key), claimToken);
    }
}
