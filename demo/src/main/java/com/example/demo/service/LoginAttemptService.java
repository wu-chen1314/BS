package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 30;
    private static final String ATTEMPT_KEY_PREFIX = "login:attempt:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private final Map<String, Integer> localAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> localLocks = new ConcurrentHashMap<>();

    public void incrementLoginAttempts(String username) {
        String key = ATTEMPT_KEY_PREFIX + username;
        if (useRedis()) {
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
            return;
        }
        localAttempts.merge(username, 1, Integer::sum);
    }

    public void resetLoginAttempts(String username) {
        String key = ATTEMPT_KEY_PREFIX + username;
        if (useRedis()) {
            redisTemplate.delete(key);
            return;
        }
        localAttempts.remove(username);
    }

    public int getLoginAttempts(String username) {
        String key = ATTEMPT_KEY_PREFIX + username;
        if (useRedis()) {
            Object value = redisTemplate.opsForValue().get(key);
            return value == null ? 0 : Integer.parseInt(value.toString());
        }
        return localAttempts.getOrDefault(username, 0);
    }

    public void lockAccount(String username) {
        String key = LOCK_KEY_PREFIX + username;
        if (useRedis()) {
            redisTemplate.opsForValue().set(key, "locked", LOCK_TIME_MINUTES, TimeUnit.MINUTES);
            return;
        }
        localLocks.put(username, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(LOCK_TIME_MINUTES));
    }

    public boolean isAccountLocked(String username) {
        String key = LOCK_KEY_PREFIX + username;
        if (useRedis()) {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        }
        Long expiresAt = localLocks.get(username);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= System.currentTimeMillis()) {
            localLocks.remove(username);
            return false;
        }
        return true;
    }

    public long getLockRemainingTime(String username) {
        String key = LOCK_KEY_PREFIX + username;
        if (useRedis()) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
            return ttl == null || ttl < 0 ? 0 : ttl;
        }
        Long expiresAt = localLocks.get(username);
        if (expiresAt == null) {
            return 0;
        }
        long remainingMillis = expiresAt - System.currentTimeMillis();
        return remainingMillis <= 0 ? 0 : TimeUnit.MILLISECONDS.toMinutes(remainingMillis);
    }

    public String checkLoginAttempts(String username) {
        if (isAccountLocked(username)) {
            long remainingTime = getLockRemainingTime(username);
            return String.format("Account is locked. Try again in %d minutes", remainingTime);
        }

        int attempts = getLoginAttempts(username);
        if (attempts >= MAX_ATTEMPTS) {
            lockAccount(username);
            return String.format("Too many failed attempts. Account locked for %d minutes", LOCK_TIME_MINUTES);
        }

        return null;
    }

    public int getRemainingAttempts(String username) {
        int attempts = getLoginAttempts(username);
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    private boolean useRedis() {
        if (redisTemplate == null) {
            return false;
        }
        try {
            redisTemplate.hasKey("redis:healthcheck");
            return true;
        } catch (RedisConnectionFailureException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}
