package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.IchProject;
import com.example.demo.entity.IchProjectView;
import com.example.demo.service.IchProjectService;
import com.example.demo.service.IchProjectViewService;
import com.example.demo.service.ProjectViewCooldownService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(value = { "/api/statistics", "/api/view" })
public class ViewCountController {

    private static final long VIEW_COOLDOWN_SECONDS = 300;
    private static final String PROJECT_PAGE_CACHE_PREFIX = "ICH:PROJECT:PAGE:";
    private static final String ECHARTS_DATA_KEY = "ICH:STATISTICS:HOME_DATA";

    @Autowired
    private IchProjectService projectService;

    @Autowired
    private IchProjectViewService viewService;

    @Autowired
    private ProjectViewCooldownService projectViewCooldownService;

    @Autowired
    private RequestAuthUtil requestAuthUtil;

    @Autowired(required = false)
    @Qualifier("stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/count")
    public Result<Long> increaseViewCount(@RequestParam Long projectId, HttpServletRequest request) {
        if (projectId == null) {
            return Result.error("项目编号不能为空");
        }

        IchProject project = projectService.getById(projectId);
        if (project == null) {
            return Result.error("项目不存在");
        }

        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            String viewerKey = buildViewerKey(request);
            if (!projectViewCooldownService.shouldCountView(projectId, viewerKey, VIEW_COOLDOWN_SECONDS)) {
                return Result.success(viewService.getCurrentViewCount(projectId));
            }
        }

        Long count = viewService.increaseViewCount(projectId);
        clearProjectCache();
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
        List<IchProject> projects = projectService.list();

        result.put("total", projects.size());

        Map<Long, Long> categoryCount = new HashMap<>();
        for (IchProject project : projects) {
            Long categoryId = project.getCategoryId();
            if (categoryId != null) {
                categoryCount.put(categoryId, categoryCount.getOrDefault(categoryId, 0L) + 1);
            }
        }
        result.put("byCategory", categoryCount);

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

    private String buildViewerKey(HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId != null) {
            return "user:" + userId;
        }

        String forwardedFor = request == null ? null : request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String[] parts = forwardedFor.split(",");
            if (parts.length > 0 && StringUtils.hasText(parts[0])) {
                return "ip:" + parts[0].trim();
            }
        }

        String remoteAddr = request == null ? null : request.getRemoteAddr();
        return "ip:" + (StringUtils.hasText(remoteAddr) ? remoteAddr.trim() : "unknown");
    }

    private void clearProjectCache() {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            Set<String> keys = stringRedisTemplate.keys(PROJECT_PAGE_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
            stringRedisTemplate.delete(ECHARTS_DATA_KEY);
            stringRedisTemplate.convertAndSend("project_update_channel", "PROJECT_VIEW_CHANGED");
        } catch (Exception ignored) {
            // Ignore cache failures when Redis is unavailable.
        }
    }
}
