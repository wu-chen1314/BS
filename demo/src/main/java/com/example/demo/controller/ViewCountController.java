package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.entity.IchProject;
import com.example.demo.entity.IchProjectView;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.demo.service.IchProjectViewService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = { "/api/statistics", "/api/view" })
public class ViewCountController {

    @Autowired
    private IchProjectService projectService;

    @Autowired
    private IchProjectViewService viewService;

    @GetMapping("/count")
    public Result<Long> increaseViewCount(@RequestParam Long projectId) {
        // Record and return new view count
        Long count = viewService.increaseViewCount(projectId);
        return Result.success(count);
    }

    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> getHotRanking(@RequestParam(defaultValue = "5") int limit) {
        List<IchProjectView> hotViews = viewService.getHotRanking(limit);
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        for (IchProjectView view : hotViews) {
            IchProject project = projectService.getById(view.getProjectId());
            if (project != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", project.getId());
                map.put("name", project.getName());
                map.put("viewCount", view.getViewCount());
                map.put("coverUrl", project.getCoverUrl());
                result.add(map);
            }
        }
        return Result.success(result);
    }

    @GetMapping("/project-stats")
    public Result<Map<String, Object>> getProjectStats() {
        Map<String, Object> result = new HashMap<>();

        // 获取所有项目
        List<IchProject> projects = projectService.list();

        // 统计总数
        result.put("total", projects.size());

        // 按类别统计
        Map<Long, Long> categoryCount = new HashMap<>();
        for (IchProject project : projects) {
            Long categoryId = project.getCategoryId();
            if (categoryId != null) {
                categoryCount.put(categoryId, categoryCount.getOrDefault(categoryId, 0L) + 1);
            }
        }
        result.put("byCategory", categoryCount);

        // 按地区统计
        Map<Long, Long> regionCount = new HashMap<>();
        for (IchProject project : projects) {
            Long regionId = project.getRegionId();
            if (regionId != null) {
                regionCount.put(regionId, regionCount.getOrDefault(regionId, 0L) + 1);
            }
        }
        result.put("byRegion", regionCount);

        return Result.success(result);
    }
}
