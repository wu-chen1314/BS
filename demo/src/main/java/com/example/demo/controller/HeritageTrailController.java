package com.example.demo.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.entity.HeritageTrailAnalytics;
import com.example.demo.entity.HeritageTrailFavorite;
import com.example.demo.request.TrailAnalyticsRequest;
import com.example.demo.request.TrailFavoriteRequest;
import com.example.demo.service.HeritageTrailAnalyticsService;
import com.example.demo.service.HeritageTrailFavoriteService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/heritage-trails")
public class HeritageTrailController {

    private final HeritageTrailFavoriteService trailFavoriteService;
    private final HeritageTrailAnalyticsService trailAnalyticsService;
    private final RequestAuthUtil requestAuthUtil;

    public HeritageTrailController(HeritageTrailFavoriteService trailFavoriteService,
                                   HeritageTrailAnalyticsService trailAnalyticsService,
                                   RequestAuthUtil requestAuthUtil) {
        this.trailFavoriteService = trailFavoriteService;
        this.trailAnalyticsService = trailAnalyticsService;
        this.requestAuthUtil = requestAuthUtil;
    }

    @PostMapping("/favorites/toggle")
    public Result<Boolean> toggleFavorite(@RequestBody TrailFavoriteRequest requestBody, HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }
        if (!StringUtils.hasText(requestBody.getTrailId())) {
            return Result.error("trailId 不能为空");
        }

        String routeType = normalizeRouteType(requestBody.getRouteType());
        LambdaQueryWrapper<HeritageTrailFavorite> query = new LambdaQueryWrapper<HeritageTrailFavorite>()
                .eq(HeritageTrailFavorite::getUserId, userId)
                .eq(HeritageTrailFavorite::getTrailId, requestBody.getTrailId())
                .eq(HeritageTrailFavorite::getRouteType, routeType);

        HeritageTrailFavorite existing = trailFavoriteService.getOne(query);
        if (existing != null) {
            trailFavoriteService.removeById(existing.getId());
            return Result.success(false);
        }

        HeritageTrailFavorite favorite = new HeritageTrailFavorite();
        favorite.setUserId(userId);
        favorite.setTrailId(requestBody.getTrailId());
        favorite.setTrailTitle(requestBody.getTrailTitle());
        favorite.setRouteType(routeType);
        favorite.setCreatedAt(LocalDateTime.now());
        trailFavoriteService.save(favorite);
        return Result.success(true);
    }

    @GetMapping("/favorites/check")
    public Result<Boolean> checkFavorite(@RequestParam String trailId,
                                         @RequestParam(required = false) String routeType,
                                         HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }

        long count = trailFavoriteService.count(new LambdaQueryWrapper<HeritageTrailFavorite>()
                .eq(HeritageTrailFavorite::getUserId, userId)
                .eq(HeritageTrailFavorite::getTrailId, trailId)
                .eq(HeritageTrailFavorite::getRouteType, normalizeRouteType(routeType)));
        return Result.success(count > 0);
    }

    @GetMapping("/favorites/list")
    public Result<List<HeritageTrailFavorite>> getFavoriteList(HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }

        List<HeritageTrailFavorite> favorites = trailFavoriteService.list(new LambdaQueryWrapper<HeritageTrailFavorite>()
                .eq(HeritageTrailFavorite::getUserId, userId)
                .orderByDesc(HeritageTrailFavorite::getCreatedAt));
        return Result.success(favorites);
    }

    @PostMapping("/analytics/record")
    public Result<Boolean> recordAnalytics(@RequestBody TrailAnalyticsRequest requestBody, HttpServletRequest request) {
        if (!StringUtils.hasText(requestBody.getTrailId())) {
            return Result.error("trailId 不能为空");
        }
        if (!StringUtils.hasText(requestBody.getActionType())) {
            return Result.error("actionType 不能为空");
        }

        HeritageTrailAnalytics analytics = new HeritageTrailAnalytics();
        analytics.setUserId(requestAuthUtil.getCurrentUserId(request));
        analytics.setTrailId(requestBody.getTrailId());
        analytics.setTrailTitle(requestBody.getTrailTitle());
        analytics.setRouteType(normalizeRouteType(requestBody.getRouteType()));
        analytics.setActionType(requestBody.getActionType().trim());
        analytics.setTransportMode(blankToNull(requestBody.getTransportMode()));
        analytics.setBudgetLevel(blankToNull(requestBody.getBudgetLevel()));
        analytics.setDurationKey(blankToNull(requestBody.getDurationKey()));
        analytics.setInterestTags(joinTags(requestBody.getInterests()));
        analytics.setStopCount(requestBody.getStopCount() == null ? 0 : requestBody.getStopCount());
        analytics.setEstimatedHours(defaultDecimal(requestBody.getEstimatedHours()));
        analytics.setEstimatedCost(defaultDecimal(requestBody.getEstimatedCost()));
        analytics.setPayloadJson(requestBody.getPayload() == null ? null : JSONUtil.toJsonStr(requestBody.getPayload()));
        analytics.setCreatedAt(LocalDateTime.now());
        trailAnalyticsService.save(analytics);
        return Result.success(true);
    }

    @GetMapping("/analytics/summary")
    public Result<Map<String, Object>> getAnalyticsSummary(@RequestParam(defaultValue = "30") Integer days) {
        int safeDays = Math.max(1, Math.min(days == null ? 30 : days, 365));
        LocalDateTime startTime = LocalDateTime.now().minusDays(safeDays);

        List<HeritageTrailAnalytics> analyticsList = trailAnalyticsService.list(new LambdaQueryWrapper<HeritageTrailAnalytics>()
                .ge(HeritageTrailAnalytics::getCreatedAt, startTime)
                .orderByDesc(HeritageTrailAnalytics::getCreatedAt));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("favoriteCount", trailFavoriteService.count());
        result.put("viewCount", analyticsList.stream().filter(item -> "view".equals(item.getActionType())).count());
        result.put("customizationCount", analyticsList.stream().filter(item -> "customize".equals(item.getActionType())).count());
        result.put("shareCount", analyticsList.stream().filter(item -> "share".equals(item.getActionType())).count());
        result.put("exportCount", analyticsList.stream().filter(item -> "export".equals(item.getActionType())).count());
        result.put("topTrails", aggregateTrailRanking(analyticsList));
        result.put("transportPreferences", aggregateByField(analyticsList, HeritageTrailAnalytics::getTransportMode));
        result.put("budgetPreferences", aggregateByField(analyticsList, HeritageTrailAnalytics::getBudgetLevel));
        result.put("durationPreferences", aggregateByField(analyticsList, HeritageTrailAnalytics::getDurationKey));
        result.put("interestPreferences", aggregateTags(analyticsList));
        result.put("actionSummary", aggregateByField(analyticsList, HeritageTrailAnalytics::getActionType));
        return Result.success(result);
    }

    private String normalizeRouteType(String routeType) {
        return StringUtils.hasText(routeType) ? routeType.trim() : "template";
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

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? null : value;
    }

    private List<Map<String, Object>> aggregateTrailRanking(List<HeritageTrailAnalytics> analyticsList) {
        return analyticsList.stream()
                .filter(item -> "view".equals(item.getActionType()) || "customize".equals(item.getActionType()))
                .collect(Collectors.groupingBy(
                        item -> String.join("|",
                                blankToPlaceholder(item.getTrailId()),
                                blankToPlaceholder(item.getTrailTitle()),
                                blankToPlaceholder(item.getRouteType())),
                        LinkedHashMap::new,
                        Collectors.counting()))
                .entrySet()
                .stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(8)
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", -1);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("trailId", restoreNull(parts[0]));
                    item.put("trailTitle", restoreNull(parts[1]));
                    item.put("routeType", restoreNull(parts[2]));
                    item.put("value", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> aggregateByField(List<HeritageTrailAnalytics> analyticsList,
                                                       java.util.function.Function<HeritageTrailAnalytics, String> extractor) {
        Map<String, Long> counter = analyticsList.stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()));

        return counter.entrySet().stream()
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

    private List<Map<String, Object>> aggregateTags(List<HeritageTrailAnalytics> analyticsList) {
        Map<String, Long> counter = new LinkedHashMap<>();
        for (HeritageTrailAnalytics item : analyticsList) {
            if (!StringUtils.hasText(item.getInterestTags())) {
                continue;
            }
            List<String> tags = Arrays.stream(item.getInterestTags().split(","))
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
