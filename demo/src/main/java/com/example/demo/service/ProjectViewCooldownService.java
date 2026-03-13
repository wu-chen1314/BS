package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ProjectViewCooldownService {

    private static final String KEY_PREFIX = "project:view:cooldown:";

    private final Map<String, Long> localCooldowns = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public boolean shouldCountView(Long projectId, String viewerKey, long cooldownSeconds) {
        if (projectId == null || !StringUtils.hasText(viewerKey) || cooldownSeconds <= 0) {
            return true;
        }

        String key = KEY_PREFIX + projectId + ":" + viewerKey.trim();
        if (stringRedisTemplate != null) {
            try {
                Boolean success = stringRedisTemplate.opsForValue()
                        .setIfAbsent(key, String.valueOf(System.currentTimeMillis()), cooldownSeconds, TimeUnit.SECONDS);
                return Boolean.TRUE.equals(success);
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to local state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to local state when Redis is unavailable.
            }
        }

        long now = System.currentTimeMillis();
        long expiresAt = now + cooldownSeconds * 1000;
        AtomicBoolean allowed = new AtomicBoolean(false);
        localCooldowns.compute(key, (ignoredKey, existingExpiresAt) -> {
            if (existingExpiresAt == null || existingExpiresAt <= now) {
                allowed.set(true);
                return expiresAt;
            }
            allowed.set(false);
            return existingExpiresAt;
        });
        return allowed.get();
    }
}
