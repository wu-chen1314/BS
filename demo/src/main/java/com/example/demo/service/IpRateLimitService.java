package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Simple rate limiting service for login / register / verification-code APIs.
 * Falls back to local in-memory state when Redis is unavailable.
 */
@Service
public class IpRateLimitService {

    private static final String KEY_PREFIX = "ip:rate:";

    private final Map<String, WindowRecord> localWindows = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        String redisKey = KEY_PREFIX + key;
        if (useRedis()) {
            try {
                Long count = redisTemplate.opsForValue().increment(redisKey);
                if (count != null && count == 1L) {
                    redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
                }
                return count != null && count <= maxRequests;
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to local state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to local state when Redis is unavailable.
            }
        }

        long now = System.currentTimeMillis();
        long expiresAt = now + windowSeconds * 1000;
        WindowRecord record = localWindows.compute(redisKey, (ignoredKey, existing) -> {
            if (existing == null || existing.expired(now)) {
                return new WindowRecord(1, expiresAt);
            }
            existing.increment();
            return existing;
        });
        return record.count <= maxRequests;
    }

    public long getRemainingSeconds(String key) {
        String redisKey = KEY_PREFIX + key;
        if (useRedis()) {
            try {
                Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
                return ttl == null ? -1 : ttl;
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to local state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to local state when Redis is unavailable.
            }
        }

        WindowRecord record = localWindows.get(redisKey);
        if (record == null) {
            return -1;
        }
        long remainingMillis = record.expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            localWindows.remove(redisKey);
            return -1;
        }
        return Math.max(1, (remainingMillis + 999) / 1000);
    }

    private boolean useRedis() {
        return redisTemplate != null;
    }

    private static final class WindowRecord {
        private int count;
        private final long expiresAt;

        private WindowRecord(int count, long expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }

        private void increment() {
            this.count += 1;
        }

        private boolean expired(long now) {
            return now > expiresAt;
        }
    }
}
