package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.entity.IchProject;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")

public class StatisticsController {

    @Autowired
    private IchProjectService projectService;

    // 1. 饼图数据：统计“保护级别” (原有接口)
    @GetMapping("/level")
    public Result<List<Map<String, Object>>> getLevelStatistics() {
        return getGroupCount("protect_level");
    }

    // ✨ 2. 柱状图数据：统计“存续状态” (新增接口)
    // 目标：返回 [ {name: "在传承", value: 20}, {name: "濒危", value: 5} ]
    @GetMapping("/status")
    public Result<List<Map<String, Object>>> getStatusStatistics() {
        return getGroupCount("status");
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
}