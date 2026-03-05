package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.common.result.Result;
import com.example.demo.entity.IchProject;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")

public class StatisticsController {

    @Autowired
    private IchProjectService projectService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ✨ 3. 地图分布数据：统计各省市的非遗项目数量
    @GetMapping("/map")
    public Result<List<Map<String, Object>>> getMapStatistics() {
        String sql = "SELECT " +
                "  CASE " +
                "    WHEN r1.level = 1 THEN r1.name " +
                "    WHEN r2.level = 1 THEN r2.name " +
                "    WHEN r3.level = 1 THEN r3.name " +
                "    ELSE r1.name " +
                "  END as name, " +
                "  COUNT(p.id) as value " +
                "FROM ich_project p " +
                "LEFT JOIN ich_region r1 ON p.region_id = r1.id " +
                "LEFT JOIN ich_region r2 ON r1.parent_id = r2.id " +
                "LEFT JOIN ich_region r3 ON r2.parent_id = r3.id " +
                "GROUP BY name " +
                "HAVING name IS NOT NULL";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        // Echarts 地图匹配的省份名称通常需要去掉"省"、"市"、"自治区"等后缀，所以在这里处理一下名称
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> map : list) {
            String name = (String) map.get("name");
            if (name != null) {
                name = name.replace("省", "")
                        .replace("市", "")
                        .replace("自治区", "")
                        .replace("回族", "")
                        .replace("维吾尔", "")
                        .replace("壮族", "")
                        .replace("特别行政区", "");
            }
            Map<String, Object> newMap = new HashMap<>();
            newMap.put("name", name);
            newMap.put("value", map.get("value"));
            result.add(newMap);
        }
        return Result.success(result);
    }

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