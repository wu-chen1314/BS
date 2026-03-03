package com.example.demo.controller;

import com.example.demo.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ✨ 新功能2：搜索历史记录
 * 利用 Redis List（LPUSH + LTRIM）实现最近 10 条搜索词记录
 * POST /api/search/record — 记录搜索词
 * GET /api/search/history?userId=1 — 获取最近搜索历史
 * DELETE /api/search/history?userId=1 — 清除搜索历史
 */
@RestController
@RequestMapping("/api/search")
public class SearchHistoryController {

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private static final String SEARCH_HISTORY_PREFIX = "ICH:SEARCH:HISTORY:";
    private static final int MAX_HISTORY_SIZE = 10;

    /**
     * 记录用户搜索词
     * POST /api/search/record
     * body: { "userId": 1, "keyword": "昆曲" }
     */
    @PostMapping("/record")
    public Result<Boolean> record(@RequestBody java.util.Map<String, Object> params) {
        Object userIdObj = params.get("userId");
        String keyword = (String) params.get("keyword");
        if (userIdObj == null || keyword == null || keyword.trim().isEmpty()) {
            return Result.error("参数不完整");
        }
        String key = SEARCH_HISTORY_PREFIX + userIdObj;
        // LPUSH 将新关键词压入列表头部（最新的在前）
        stringRedisTemplate.opsForList().leftPush(key, keyword.trim());
        // LTRIM 只保留最近 MAX_HISTORY_SIZE 条，自动淘汰旧记录
        stringRedisTemplate.opsForList().trim(key, 0, MAX_HISTORY_SIZE - 1);
        return Result.success(true);
    }

    /**
     * 获取用户最近的搜索历史（最多 10 条）
     * GET /api/search/history?userId=1
     */
    @GetMapping("/history")
    public Result<List<String>> history(@RequestParam Long userId) {
        String key = SEARCH_HISTORY_PREFIX + userId;
        List<String> list = stringRedisTemplate.opsForList().range(key, 0, MAX_HISTORY_SIZE - 1);
        return Result.success(list);
    }

    /**
     * 清除用户搜索历史
     * DELETE /api/search/history?userId=1
     */
    @DeleteMapping("/history")
    public Result<Boolean> clearHistory(@RequestParam Long userId) {
        String key = SEARCH_HISTORY_PREFIX + userId;
        stringRedisTemplate.delete(key);
        return Result.success(true);
    }
}
