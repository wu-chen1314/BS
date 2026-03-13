package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired(required=false)
    private StringRedisTemplate redisTemplate;

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
    
    @GetMapping("/clear-cache")
    public String clearCache() {
        if(redisTemplate != null) {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            return "Cache cleared";
        }
        return "Redis not found";
    }
}
