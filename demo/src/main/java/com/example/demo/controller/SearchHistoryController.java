package com.example.demo.controller;

import com.example.demo.common.result.Result;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/search")
public class SearchHistoryController {

    private static final String SEARCH_HISTORY_PREFIX = "ICH:SEARCH:HISTORY:";
    private static final int MAX_HISTORY_SIZE = 10;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private final Map<Long, LinkedList<String>> localHistory = new ConcurrentHashMap<>();

    @PostMapping("/record")
    public Result<Boolean> record(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Object userIdObj = params.get("userId");
        String keyword = (String) params.get("keyword");
        if (userIdObj == null || keyword == null || keyword.trim().isEmpty()) {
            return Result.error("Missing required parameters");
        }
        Long userId = Long.valueOf(String.valueOf(userIdObj));
        if (!RequestAuthUtil.isSelfOrAdmin(request, userId)) {
            return Result.error("Cannot record search history for another user");
        }

        String normalizedKeyword = keyword.trim();
        if (redisAvailable()) {
            String key = SEARCH_HISTORY_PREFIX + userIdObj;
            stringRedisTemplate.opsForList().leftPush(key, normalizedKeyword);
            stringRedisTemplate.opsForList().trim(key, 0, MAX_HISTORY_SIZE - 1);
            return Result.success(true);
        }

        LinkedList<String> history = localHistory.computeIfAbsent(userId, k -> new LinkedList<>());
        history.remove(normalizedKeyword);
        history.addFirst(normalizedKeyword);
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeLast();
        }
        return Result.success(true);
    }

    @GetMapping("/history")
    public Result<List<String>> history(@RequestParam Long userId, HttpServletRequest request) {
        if (!RequestAuthUtil.isSelfOrAdmin(request, userId)) {
            return Result.error("Cannot view another user's search history");
        }
        if (redisAvailable()) {
            String key = SEARCH_HISTORY_PREFIX + userId;
            List<String> list = stringRedisTemplate.opsForList().range(key, 0, MAX_HISTORY_SIZE - 1);
            return Result.success(list == null ? Collections.emptyList() : list);
        }
        LinkedList<String> history = localHistory.get(userId);
        if (history == null) {
            return Result.success(Collections.emptyList());
        }
        return Result.success(new ArrayList<>(history));
    }

    @DeleteMapping("/history")
    public Result<Boolean> clearHistory(@RequestParam Long userId, HttpServletRequest request) {
        if (!RequestAuthUtil.isSelfOrAdmin(request, userId)) {
            return Result.error("Cannot clear another user's search history");
        }
        if (redisAvailable()) {
            String key = SEARCH_HISTORY_PREFIX + userId;
            stringRedisTemplate.delete(key);
            return Result.success(true);
        }
        localHistory.remove(userId);
        return Result.success(true);
    }

    private boolean redisAvailable() {
        if (stringRedisTemplate == null) {
            return false;
        }
        try {
            stringRedisTemplate.hasKey("redis:healthcheck");
            return true;
        } catch (RedisConnectionFailureException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}