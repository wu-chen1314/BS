package com.example.demo.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.entity.LearningPlanAnalytics;
import com.example.demo.entity.LearningPlanFavorite;
import com.example.demo.request.LearningPlanAnalyticsRequest;
import com.example.demo.request.LearningPlanFavoriteRequest;
import com.example.demo.service.LearningPlanAnalyticsService;
import com.example.demo.service.LearningPlanFavoriteService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/learning-studio")
public class LearningStudioController {

    private final LearningPlanFavoriteService learningPlanFavoriteService;
    private final LearningPlanAnalyticsService learningPlanAnalyticsService;
    private final RequestAuthUtil requestAuthUtil;

    public LearningStudioController(LearningPlanFavoriteService learningPlanFavoriteService,
                                    LearningPlanAnalyticsService learningPlanAnalyticsService,
                                    RequestAuthUtil requestAuthUtil) {
        this.learningPlanFavoriteService = learningPlanFavoriteService;
        this.learningPlanAnalyticsService = learningPlanAnalyticsService;
        this.requestAuthUtil = requestAuthUtil;
    }

    @PostMapping("/favorites/toggle")
    public Result<Boolean> toggleFavorite(@RequestBody LearningPlanFavoriteRequest requestBody, HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }
        if (!StringUtils.hasText(requestBody.getPlanId())) {
            return Result.error("planId 不能为空");
        }

        LambdaQueryWrapper<LearningPlanFavorite> query = new LambdaQueryWrapper<LearningPlanFavorite>()
                .eq(LearningPlanFavorite::getUserId, userId)
                .eq(LearningPlanFavorite::getPlanId, requestBody.getPlanId());

        LearningPlanFavorite existing = learningPlanFavoriteService.getOne(query);
        if (existing != null) {
            learningPlanFavoriteService.removeById(existing.getId());
            return Result.success(false);
        }

        LearningPlanFavorite favorite = new LearningPlanFavorite();
        favorite.setUserId(userId);
        favorite.setPlanId(requestBody.getPlanId());
        favorite.setPlanTitle(requestBody.getPlanTitle());
        favorite.setTrackId(blankToNull(requestBody.getTrackId()));
        favorite.setCreatedAt(LocalDateTime.now());
        learningPlanFavoriteService.save(favorite);
        return Result.success(true);
    }

    @GetMapping("/favorites/check")
    public Result<Boolean> checkFavorite(@RequestParam String planId, HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }

        long count = learningPlanFavoriteService.count(new LambdaQueryWrapper<LearningPlanFavorite>()
                .eq(LearningPlanFavorite::getUserId, userId)
                .eq(LearningPlanFavorite::getPlanId, planId));
        return Result.success(count > 0);
    }

    @GetMapping("/favorites/list")
    public Result<List<LearningPlanFavorite>> getFavoriteList(HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }

        return Result.success(learningPlanFavoriteService.list(new LambdaQueryWrapper<LearningPlanFavorite>()
                .eq(LearningPlanFavorite::getUserId, userId)
                .orderByDesc(LearningPlanFavorite::getCreatedAt)));
    }

    @PostMapping("/analytics/record")
    public Result<Boolean> recordAnalytics(@RequestBody LearningPlanAnalyticsRequest requestBody, HttpServletRequest request) {
        if (!StringUtils.hasText(requestBody.getPlanId())) {
            return Result.error("planId 不能为空");
        }
        if (!StringUtils.hasText(requestBody.getActionType())) {
            return Result.error("actionType 不能为空");
        }

        LearningPlanAnalytics analytics = new LearningPlanAnalytics();
        analytics.setUserId(requestAuthUtil.getCurrentUserId(request));
        analytics.setPlanId(requestBody.getPlanId());
        analytics.setPlanTitle(requestBody.getPlanTitle());
        analytics.setTrackId(blankToNull(requestBody.getTrackId()));
        analytics.setActionType(requestBody.getActionType().trim());
        analytics.setAudienceTag(blankToNull(requestBody.getAudienceTag()));
        analytics.setDurationLabel(blankToNull(requestBody.getDurationLabel()));
        analytics.setLinkedThemeId(blankToNull(requestBody.getLinkedThemeId()));
        analytics.setRegionKeyword(blankToNull(requestBody.getRegionKeyword()));
        analytics.setProjectCount(requestBody.getProjectCount() == null ? 0 : requestBody.getProjectCount());
        analytics.setKeywordTags(joinTags(requestBody.getKeywords()));
        analytics.setPayloadJson(requestBody.getPayload() == null ? null : JSONUtil.toJsonStr(requestBody.getPayload()));
        analytics.setCreatedAt(LocalDateTime.now());
        learningPlanAnalyticsService.save(analytics);
        return Result.success(true);
    }

    @GetMapping("/analytics/summary")
    public Result<Map<String, Object>> getAnalyticsSummary(@RequestParam(defaultValue = "30") Integer days) {
        int safeDays = Math.max(1, Math.min(days == null ? 30 : days, 365));
        LocalDateTime startTime = LocalDateTime.now().minusDays(safeDays);

        List<LearningPlanAnalytics> analyticsList = learningPlanAnalyticsService.list(new LambdaQueryWrapper<LearningPlanAnalytics>()
                .ge(LearningPlanAnalytics::getCreatedAt, startTime)
                .orderByDesc(LearningPlanAnalytics::getCreatedAt));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("favoriteCount", learningPlanFavoriteService.count());
        result.put("viewCount", analyticsList.stream().filter(item -> "view".equals(item.getActionType())).count());
        result.put("routeCarryCount", analyticsList.stream().filter(item -> "routeCarry".equals(item.getActionType())).count());
        result.put("exportCount", analyticsList.stream().filter(item -> "export".equals(item.getActionType())).count());
        result.put("sheetExportCount", analyticsList.stream().filter(item -> "teacherSheet".equals(item.getActionType())).count());
        result.put("topPlans", aggregatePlanRanking(analyticsList));
        result.put("trackPreferences", aggregateByField(analyticsList, LearningPlanAnalytics::getTrackId));
        result.put("audiencePreferences", aggregateByField(analyticsList, LearningPlanAnalytics::getAudienceTag));
        result.put("themePreferences", aggregateByField(analyticsList, LearningPlanAnalytics::getLinkedThemeId));
        result.put("keywordPreferences", aggregateTags(analyticsList));
        result.put("actionSummary", aggregateByField(analyticsList, LearningPlanAnalytics::getActionType));
        return Result.success(result);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private List<Map<String, Object>> aggregatePlanRanking(List<LearningPlanAnalytics> analyticsList) {
        return analyticsList.stream()
                .filter(item -> "view".equals(item.getActionType()) || "routeCarry".equals(item.getActionType()))
                .collect(Collectors.groupingBy(
                        item -> String.join("|",
                                blankToPlaceholder(item.getPlanId()),
                                blankToPlaceholder(item.getPlanTitle()),
                                blankToPlaceholder(item.getTrackId())),
                        LinkedHashMap::new,
                        Collectors.counting()))
                .entrySet()
                .stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(8)
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", -1);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("planId", restoreNull(parts[0]));
                    item.put("planTitle", restoreNull(parts[1]));
                    item.put("trackId", restoreNull(parts[2]));
                    item.put("value", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> aggregateByField(List<LearningPlanAnalytics> analyticsList,
                                                       java.util.function.Function<LearningPlanAnalytics, String> extractor) {
        return analyticsList.stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(8)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> aggregateTags(List<LearningPlanAnalytics> analyticsList) {
        Map<String, Long> counter = new LinkedHashMap<>();
        for (LearningPlanAnalytics item : analyticsList) {
            if (!StringUtils.hasText(item.getKeywordTags())) {
                continue;
            }
            List<String> tags = Arrays.stream(item.getKeywordTags().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            for (String tag : tags) {
                counter.merge(tag, 1L, Long::sum);
            }
        }

        return counter.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(10)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private String blankToPlaceholder(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String restoreNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
