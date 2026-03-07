package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 简单计算题验证码服务：
 * - 生成验证码题目并存入 Redis
 * - 支持一次性校验与自动过期
 */
@Service
public class CaptchaService {

    private static final String KEY_PREFIX = "captcha:";
    private static final long EXPIRE_SECONDS = 120; // 验证码有效期 2 分钟

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成一个新的验证码，返回对应的 ID
     */
    public String saveAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            throw new IllegalArgumentException("Captcha answer must not be empty");
        }
        String id = UUID.randomUUID().toString().replace("-", "");
        String key = KEY_PREFIX + id;
        redisTemplate.opsForValue().set(key, answer.trim(), EXPIRE_SECONDS, TimeUnit.SECONDS);
        return id;
    }

    /**
     * 校验验证码是否正确（可选一次性消费）
     */
    public boolean validate(String id, String userAnswer, boolean consumeAfterSuccess) {
        if (!StringUtils.hasText(id) || !StringUtils.hasText(userAnswer)) {
            return false;
        }
        String key = KEY_PREFIX + id;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return false;
        }
        boolean matched = userAnswer.trim().equalsIgnoreCase(value.toString().trim());
        if (matched && consumeAfterSuccess) {
            redisTemplate.delete(key);
        }
        return matched;
    }
}

