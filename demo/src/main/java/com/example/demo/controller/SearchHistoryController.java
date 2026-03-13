package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 搜索历史接口。
 * 使用 Redis List（LPUSH + LTRIM）保存最近 10 条搜索记录。
 */
@RestController
@RequestMapping("/api/search")
public class SearchHistoryController {

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RequestAuthUtil requestAuthUtil;

    private static final String SEARCH_HISTORY_PREFIX = "ICH:SEARCH:HISTORY:";
    private static final int MAX_HISTORY_SIZE = 10;

    /**
     * 记录当前登录用户的搜索词。
     * POST /api/search/record
     * body: { "keyword": "昆曲" }
     */
    @PostMapping("/record")
    public Result<Boolean> record(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        String keyword = params.get("keyword") == null ? null : params.get("keyword").toString();
        if (currentUserId == null) {
            return Result.error("用户未登录或令牌无效");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("参数不完整");
        }

        String key = SEARCH_HISTORY_PREFIX + currentUserId;
        if (stringRedisTemplate == null) {
            return Result.success(true);
        }

        try {
            stringRedisTemplate.opsForList().leftPush(key, keyword.trim());
            stringRedisTemplate.opsForList().trim(key, 0, MAX_HISTORY_SIZE - 1);
            return Result.success(true);
        } catch (Exception ignored) {
            return Result.success(true);
        }
    }

    /**
     * 获取当前登录用户最近的搜索历史。
     * GET /api/search/history
     */
    @GetMapping("/history")
    public Result<List<String>> history(HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("用户未登录或令牌无效");
        }

        String key = SEARCH_HISTORY_PREFIX + currentUserId;
        if (stringRedisTemplate == null) {
            return Result.success(Collections.emptyList());
        }

        try {
            List<String> list = stringRedisTemplate.opsForList().range(key, 0, MAX_HISTORY_SIZE - 1);
            return Result.success(list);
        } catch (Exception ignored) {
            return Result.success(Collections.emptyList());
        }
    }

    /**
     * 清除当前登录用户的搜索历史。
     * DELETE /api/search/history
     */
    @DeleteMapping("/history")
    public Result<Boolean> clearHistory(HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("用户未登录或令牌无效");
        }

        String key = SEARCH_HISTORY_PREFIX + currentUserId;
        if (stringRedisTemplate == null) {
            return Result.success(true);
        }

        try {
            stringRedisTemplate.delete(key);
            return Result.success(true);
        } catch (Exception ignored) {
            return Result.success(true);
        }
    }
}
