package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.entity.IchProject;
import com.example.demo.entity.IchRegion;
import com.example.demo.mapper.IchRegionMapper;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")

public class StatisticsController {

    @Autowired
    private IchProjectService projectService;

    @Autowired
    private IchRegionMapper regionMapper;

    // 1. 饼图数据：统计“保护级别” (原有接口)
    @GetMapping("/level")
    @Cacheable(value = "statistics", key = "'level'")
    public Result<List<Map<String, Object>>> getLevelStatistics() {
        return getGroupCount("protect_level");
    }

    // ✨ 2. 柱状图数据：统计“存续状态” (新增接口)
    // 目标：返回 [ {name: "在传承", value: 20}, {name: "濒危", value: 5} ]
    @GetMapping("/status")
    @Cacheable(value = "statistics", key = "'status'")
    public Result<List<Map<String, Object>>> getStatusStatistics() {
        return getGroupCount("status");
    }

    @GetMapping("/map")
    @Cacheable(value = "statistics", key = "'map'")
    public Result<List<Map<String, Object>>> getRegionStatisticsForMap() {
        QueryWrapper<IchProject> wrapper = new QueryWrapper<>();
        wrapper.select("region_id", "count(*) as count").groupBy("region_id");

        List<Map<String, Object>> list = projectService.listMaps(wrapper);
        Set<Long> regionIds = list.stream()
                .map(item -> item.get("region_id"))
                .filter(Objects::nonNull)
                .map(this::toLong)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> regionNameMap = regionIds.isEmpty()
                ? Collections.emptyMap()
                : regionMapper.selectBatchIds(regionIds).stream()
                .collect(Collectors.toMap(IchRegion::getId, IchRegion::getName, (left, right) -> left));

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, Object> map : list) {
            Long regionId = toLong(map.get("region_id"));
            if (regionId == null) {
                continue;
            }
            Map<String, Object> newMap = new HashMap<>();
            newMap.put("name", regionNameMap.getOrDefault(regionId, "地区待补充"));
            newMap.put("value", toLong(map.get("count")));
            resultList.add(newMap);
        }
        resultList.sort((left, right) -> Long.compare(toLong(right.get("value"), 0L), toLong(left.get("value"), 0L)));
        return Result.success(resultList);
    }

    // 通用的私有统计方法 (避免代码重复)
    private Result<List<Map<String, Object>>> getGroupCount(String columnName) {
        QueryWrapper<IchProject> wrapper = new QueryWrapper<>();
        // SQL: SELECT column, COUNT(*) as count FROM table GROUP BY column
        wrapper.select(columnName, "count(*) as count")
                .groupBy(columnName);

        List<Map<String, Object>> list = projectService.listMaps(wrapper);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, Object> map : list) {
            Map<String, Object> newMap = new HashMap<>();
            // 注意：MyBatis-Plus 返回的 Map key 可能是 null，要做空指针保护
            Object key = map.get(columnName);
            newMap.put("name", key == null ? "未知" : key);
            newMap.put("value", map.get("count"));
            resultList.add(newMap);
        }
        return Result.success(resultList);
    }

    private Long toLong(Object value) {
        return toLong(value, null);
    }

    private Long toLong(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
