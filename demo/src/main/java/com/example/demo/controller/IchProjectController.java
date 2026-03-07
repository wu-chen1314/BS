package com.example.demo.controller;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.IchProject;
import com.example.demo.service.IchProjectService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class IchProjectController {

    private static final String PROJECT_PAGE_CACHE_PREFIX = "ICH:PROJECT:PAGE:";
    private static final String ECHARTS_DATA_KEY = "ICH:STATISTICS:HOME_DATA";

    @Autowired
    private IchProjectService projectService;

    @Autowired(required = false)
    @Qualifier("stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RequestAuthUtil requestAuthUtil;

    @GetMapping("/page")
    public Result<Page<IchProject>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) String protectLevel) {

        String queryKey = PROJECT_PAGE_CACHE_PREFIX + pageNum + "_" + pageSize + "_" +
                (StrUtil.isBlank(name) ? "all" : name) + "_" +
                (StrUtil.isBlank(protectLevel) ? "all" : protectLevel);

        if (stringRedisTemplate != null) {
            try {
                String cacheData = stringRedisTemplate.opsForValue().get(queryKey);
                if (StrUtil.isNotBlank(cacheData)) {
                    Page<IchProject> cachedPage = JSONUtil.toBean(cacheData, new TypeReference<Page<IchProject>>() { }, false);
                    return Result.success(cachedPage);
                }
            } catch (Exception ignored) {
                // Fall back to DB when Redis is unavailable.
            }
        }

        QueryWrapper<IchProject> query = new QueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            query.like("name", name);
        }
        if (StrUtil.isNotBlank(protectLevel)) {
            query.eq("protect_level", protectLevel);
        }
        query.orderByDesc("id");

        Page<IchProject> pageResult = projectService.pageWithViewCount(new Page<>(pageNum, pageSize), query);
        if (pageResult.getRecords() == null || pageResult.getRecords().isEmpty()) {
            cachePage(queryKey, pageResult, 1, TimeUnit.MINUTES);
            return Result.success(pageResult);
        }

        projectService.populateProjectRelations(pageResult.getRecords());
        cachePage(queryKey, pageResult, 30 + RandomUtil.randomInt(1, 6), TimeUnit.MINUTES);
        return Result.success(pageResult);
    }

    @GetMapping("/list")
    public Result<List<IchProject>> list() {
        QueryWrapper<IchProject> query = new QueryWrapper<>();
        query.select("id", "name");
        query.orderByDesc("id");
        return Result.success(projectService.list(query));
    }

    @GetMapping("/{id}")
    public Result<IchProject> detail(@PathVariable Long id) {
        IchProject project = projectService.getProjectWithDetails(id);
        if (project == null) {
            return Result.error("Project not found");
        }
        return Result.success(project);
    }

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody IchProject project, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限新增项目");
        }
        Result<Boolean> result = projectService.saveProjectWithInheritors(project);
        if (Boolean.TRUE.equals(result.getData())) {
            clearProjectCache();
        }
        return result;
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody IchProject project, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限修改项目");
        }
        Result<Boolean> result = projectService.saveProjectWithInheritors(project);
        if (Boolean.TRUE.equals(result.getData())) {
            clearProjectCache();
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限删除项目");
        }
        boolean success = projectService.deleteProjectsWithRelations(Collections.singletonList(id));
        if (success) {
            clearProjectCache();
        }
        return Result.success(success);
    }

    @DeleteMapping("/delete/batch")
    public Result<Boolean> deleteBatch(@RequestBody List<Integer> ids, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限批量删除项目");
        }
        List<Long> longIds = ids == null ? Collections.emptyList()
                : ids.stream().map(Integer::longValue).collect(Collectors.toList());
        boolean success = projectService.deleteProjectsWithRelations(longIds);
        if (success) {
            clearProjectCache();
        }
        return Result.success(success);
    }

    private void cachePage(String queryKey, Page<IchProject> pageResult, long ttl, TimeUnit unit) {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(queryKey, JSONUtil.toJsonStr(pageResult), ttl, unit);
        } catch (Exception ignored) {
            // Ignore cache failures when Redis is unavailable.
        }
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
            stringRedisTemplate.convertAndSend("project_update_channel", "PROJECT_DATA_CHANGED");
        } catch (Exception ignored) {
            // Ignore cache failures when Redis is unavailable.
        }
    }
}
