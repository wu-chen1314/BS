package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {

    private static final String KEY_PREFIX = "captcha:";
    private static final long EXPIRE_SECONDS = 120;

    private final Map<String, CaptchaRecord> localCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public String saveAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            throw new IllegalArgumentException("Captcha answer must not be empty");
        }

        String id = UUID.randomUUID().toString().replace("-", "");
        String key = KEY_PREFIX + id;
        if (useRedis()) {
            try {
                redisTemplate.opsForValue().set(key, answer.trim(), EXPIRE_SECONDS, TimeUnit.SECONDS);
                return id;
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            }
        }

        localCache.put(key, new CaptchaRecord(answer.trim(), System.currentTimeMillis() + EXPIRE_SECONDS * 1000));
        return id;
    }

    public boolean validate(String id, String userAnswer, boolean consumeAfterSuccess) {
        if (!StringUtils.hasText(id) || !StringUtils.hasText(userAnswer)) {
            return false;
        }

        String key = KEY_PREFIX + id;
        if (useRedis()) {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                if (value == null) {
                    return false;
                }
                boolean matched = userAnswer.trim().equalsIgnoreCase(value.toString().trim());
                if (matched && consumeAfterSuccess) {
                    redisTemplate.delete(key);
                }
                return matched;
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            }
        }

        CaptchaRecord record = localCache.get(key);
        if (record == null || record.expired()) {
            localCache.remove(key);
            return false;
        }
        boolean matched = userAnswer.trim().equalsIgnoreCase(record.answer);
        if (matched && consumeAfterSuccess) {
            localCache.remove(key);
        }
        return matched;
    }

    private boolean useRedis() {
        return redisTemplate != null;
    }

    private static final class CaptchaRecord {
        private final String answer;
        private final long expiresAt;

        private CaptchaRecord(String answer, long expiresAt) {
            this.answer = answer;
            this.expiresAt = expiresAt;
        }

        private boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
