package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 30;
    private static final String ATTEMPT_KEY_PREFIX = "login:attempt:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";

    private final Map<String, Integer> localAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> localLocks = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public void incrementLoginAttempts(String username) {
        if (useRedis()) {
            try {
                String key = ATTEMPT_KEY_PREFIX + username;
                Long count = redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, 24, TimeUnit.HOURS);
                if (count != null && count >= MAX_ATTEMPTS) {
                    redisTemplate.opsForValue().set(LOCK_KEY_PREFIX + username, "locked", LOCK_TIME_MINUTES, TimeUnit.MINUTES);
                }
                return;
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            }
        }

        int attempts = localAttempts.merge(username, 1, Integer::sum);
        if (attempts >= MAX_ATTEMPTS) {
            lockAccount(username);
        }
    }

    public void resetLoginAttempts(String username) {
        if (useRedis()) {
            try {
                redisTemplate.delete(ATTEMPT_KEY_PREFIX + username);
                return;
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            }
        }

        localAttempts.remove(username);
        localLocks.remove(username);
    }

    public int getLoginAttempts(String username) {
        if (useRedis()) {
            try {
                Object value = redisTemplate.opsForValue().get(ATTEMPT_KEY_PREFIX + username);
                return value == null ? 0 : Integer.parseInt(value.toString());
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            }
        }

        return localAttempts.getOrDefault(username, 0);
    }

    public void lockAccount(String username) {
        if (useRedis()) {
            try {
                redisTemplate.opsForValue().set(LOCK_KEY_PREFIX + username, "locked", LOCK_TIME_MINUTES, TimeUnit.MINUTES);
                return;
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            }
        }

        localLocks.put(username, Instant.now().plusSeconds(LOCK_TIME_MINUTES * 60L).toEpochMilli());
    }

    public boolean isAccountLocked(String username) {
        if (useRedis()) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY_PREFIX + username));
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            }
        }

        return getLocalLockRemainingMillis(username) > 0;
    }

    public long getLockRemainingTime(String username) {
        if (useRedis()) {
            try {
                Long ttl = redisTemplate.getExpire(LOCK_KEY_PREFIX + username, TimeUnit.MINUTES);
                return ttl == null || ttl < 0 ? 0 : ttl;
            } catch (RedisConnectionFailureException ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            } catch (Exception ignored) {
                // Fall back to in-memory state when Redis is unavailable.
            }
        }

        long remainingMillis = getLocalLockRemainingMillis(username);
        if (remainingMillis <= 0) {
            return 0;
        }
        return (long) Math.ceil(remainingMillis / 60000.0);
    }

    public String checkLoginAttempts(String username) {
        if (isAccountLocked(username)) {
            long remainingTime = getLockRemainingTime(username);
            return String.format("账户已被锁定，请 %.0f 分钟后再试", (double) remainingTime);
        }

        int attempts = getLoginAttempts(username);
        if (attempts >= MAX_ATTEMPTS) {
            lockAccount(username);
            return String.format("密码错误超过 %d 次，账户已被锁定 %d 分钟", MAX_ATTEMPTS, LOCK_TIME_MINUTES);
        }

        return null;
    }

    public int getRemainingAttempts(String username) {
        int attempts = getLoginAttempts(username);
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    private boolean useRedis() {
        return redisTemplate != null;
    }

    private long getLocalLockRemainingMillis(String username) {
        Long lockedUntil = localLocks.get(username);
        if (lockedUntil == null) {
            return 0;
        }

        long remainingMillis = lockedUntil - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            localLocks.remove(username);
            return 0;
        }
        return remainingMillis;
    }
}
