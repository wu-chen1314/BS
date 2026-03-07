package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 简单的基于 Redis 的 IP 限流服务
 * 用于登录 / 注册 / 发送验证码等接口的防刷控制
 */
@Service
public class IpRateLimitService {

    private static final String KEY_PREFIX = "ip:rate:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 在给定时间窗口内检查是否允许继续请求
     *
     * @param key           业务 key（例如 login:ip 或 register:ip）
     * @param maxRequests   窗口内允许的最大请求次数
     * @param windowSeconds 时间窗口，单位秒
     * @return true 表示允许，false 表示超过限制
     */
    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        String redisKey = KEY_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            // 第一次访问时设置过期时间
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }
        return count != null && count <= maxRequests;
    }

    /**
     * 获取当前 key 剩余的过期时间（秒）
     */
    public long getRemainingSeconds(String key) {
        String redisKey = KEY_PREFIX + key;
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        return ttl == null ? -1 : ttl;
    }
}

