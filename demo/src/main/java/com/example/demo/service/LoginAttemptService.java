package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_ATTEMPTS = 5; // 最大尝试次数
    private static final int LOCK_TIME_MINUTES = 30; // 锁定时间（分钟）
    private static final String ATTEMPT_KEY_PREFIX = "login:attempt:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";

    /**
     * 增加登录失败次数
     */
    public void incrementLoginAttempts(String username) {
        String key = ATTEMPT_KEY_PREFIX + username;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 24, TimeUnit.HOURS); // 24 小时后重置
    }

    /**
     * 重置登录失败次数
     */
    public void resetLoginAttempts(String username) {
        String key = ATTEMPT_KEY_PREFIX + username;
        redisTemplate.delete(key);
    }

    /**
     * 获取登录失败次数
     */
    public int getLoginAttempts(String username) {
        String key = ATTEMPT_KEY_PREFIX + username;
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    /**
     * 锁定账户
     */
    public void lockAccount(String username) {
        String key = LOCK_KEY_PREFIX + username;
        redisTemplate.opsForValue().set(key, "locked", LOCK_TIME_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 检查账户是否被锁定
     */
    public boolean isAccountLocked(String username) {
        String key = LOCK_KEY_PREFIX + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 获取账户锁定剩余时间（分钟）
     */
    public long getLockRemainingTime(String username) {
        String key = LOCK_KEY_PREFIX + username;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
        return ttl == null || ttl < 0 ? 0 : ttl;
    }

    /**
     * 检查并处理登录失败
     * @return 返回错误信息，如果返回 null 表示可以正常登录
     */
    public String checkLoginAttempts(String username) {
        // 检查是否已被锁定
        if (isAccountLocked(username)) {
            long remainingTime = getLockRemainingTime(username);
            return String.format("账户已被锁定，请 %.0f 分钟后再试", remainingTime);
        }

        // 检查失败次数
        int attempts = getLoginAttempts(username);
        if (attempts >= MAX_ATTEMPTS) {
            lockAccount(username);
            return String.format("密码错误超过 %d 次，账户已被锁定 %d 分钟", MAX_ATTEMPTS, LOCK_TIME_MINUTES);
        }

        return null;
    }

    /**
     * 获取剩余尝试次数
     */
    public int getRemainingAttempts(String username) {
        int attempts = getLoginAttempts(username);
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }
}
