package com.example.demo.controller;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.result.Result;
import com.example.demo.entity.IchInheritor;
import com.example.demo.entity.IchProject;
import com.example.demo.model.dto.IchProjectDTO;
import com.example.demo.model.vo.IchProjectVO;
import com.example.demo.service.IchInheritorService;
import com.example.demo.service.IchProjectService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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

    @Autowired
    private IchInheritorService inheritorService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/page")
    public Result<Page<IchProjectVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false) String protectLevel,
                                           @RequestParam(required = false) Integer auditStatus) {
        String queryKey = PROJECT_PAGE_CACHE_PREFIX + pageNum + "_" + pageSize + "_"
                + (StrUtil.isBlank(name) ? "all" : name) + "_"
                + (StrUtil.isBlank(protectLevel) ? "all" : protectLevel) + "_"
                + (auditStatus == null ? "all" : auditStatus);

        Page<IchProjectVO> cachedPage = getCachedPage(queryKey);
        if (cachedPage != null) {
            return Result.success(cachedPage);
        }

        QueryWrapper<IchProject> query = new QueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            query.like("p.name", name);
        }
        if (StrUtil.isNotBlank(protectLevel)) {
            query.eq("p.protect_level", protectLevel);
        }
        if (auditStatus != null) {
            query.eq("p.audit_status", auditStatus);
        }
        query.orderByDesc("p.id");

        Page<IchProject> pageResult = projectService.pageWithViewCount(new Page<>(pageNum, pageSize), query);
        Page<IchProjectVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());

        if (pageResult.getRecords() == null || pageResult.getRecords().isEmpty()) {
            cachePage(queryKey, voPage, 1);
            return Result.success(voPage);
        }

        List<IchProjectVO> voList = pageResult.getRecords().stream().map(project -> {
            IchProjectVO vo = new IchProjectVO();
            BeanUtils.copyProperties(project, vo);
            List<IchInheritor> inheritors = inheritorService.list(new QueryWrapper<IchInheritor>().eq("project_id", project.getId()));
            vo.setInheritorNames(inheritors.stream().map(IchInheritor::getName).collect(Collectors.joining(", ")));
            vo.setInheritorIds(inheritors.stream().map(IchInheritor::getId).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        cachePage(queryKey, voPage, 30 + RandomUtil.randomInt(1, 6));
        return Result.success(voPage);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        List<IchProject> projects = projectService.list(new QueryWrapper<IchProject>().orderByDesc("id"));
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Protect Level,Status,Category ID,Region ID\n");
        for (IchProject project : projects) {
            csv.append(safeCsv(project.getId()))
                    .append(',')
                    .append(safeCsv(project.getName()))
                    .append(',')
                    .append(safeCsv(project.getProtectLevel()))
                    .append(',')
                    .append(safeCsv(project.getStatus()))
                    .append(',')
                    .append(safeCsv(project.getCategoryId()))
                    .append(',')
                    .append(safeCsv(project.getRegionId()))
                    .append("\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=projects.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody IchProjectDTO project, HttpServletRequest request) {
        Long currentUserId = RequestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("Please log in first");
        }
        if (!RequestAuthUtil.isAdmin(request)) {
            project.setSubmitterId(currentUserId);
            project.setAuditStatus(0);
            project.setAuditBy(null);
            project.setAuditReason(null);
        } else if (project.getAuditStatus() == null) {
            project.setAuditStatus(0);
        }

        Result<Boolean> result = projectService.saveProjectWithInheritors(project);
        if (Boolean.TRUE.equals(result.getData())) {
            clearProjectCache();
        }
        return result;
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody IchProjectDTO project, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can update projects");
        }
        Result<Boolean> result = projectService.saveProjectWithInheritors(project);
        if (Boolean.TRUE.equals(result.getData())) {
            clearProjectCache();
        }
        return result;
    }

    @PutMapping("/audit")
    public Result<Boolean> audit(@RequestParam Long id,
                                 @RequestParam Integer auditStatus,
                                 @RequestParam(required = false) String auditReason,
                                 HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can audit projects");
        }
        if (auditStatus == 2 && (auditReason == null || auditReason.trim().isEmpty())) {
            return Result.error("Audit reason is required when rejecting a project");
        }

        IchProject project = projectService.getById(id);
        if (project == null) {
            return Result.error("Project not found");
        }

        project.setAuditStatus(auditStatus);
        if (auditStatus == 2) {
            project.setAuditReason(auditReason == null ? null : auditReason.trim());
        } else if (auditStatus == 1) {
            project.setAuditReason(null);
        }

        String username = RequestAuthUtil.getCurrentUsername(request);
        if (username != null) {
            project.setAuditBy(username);
        }

        boolean success = projectService.updateById(project);
        if (success) {
            clearProjectCache();
        }
        return Result.success(success);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can delete projects");
        }
        boolean success = projectService.removeById(id);
        if (success) {
            clearProjectCache();
        }
        return Result.success(success);
    }

    @DeleteMapping("/delete/batch")
    public Result<Boolean> deleteBatch(@RequestBody List<Integer> ids, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can batch delete projects");
        }
        boolean success = projectService.removeByIds(ids);
        if (success) {
            clearProjectCache();
        }
        return Result.success(success);
    }

    private Page<IchProjectVO> getCachedPage(String queryKey) {
        if (!redisAvailable()) {
            return null;
        }
        try {
            String cacheData = stringRedisTemplate.opsForValue().get(queryKey);
            if (StrUtil.isBlank(cacheData)) {
                return null;
            }
            return JSONUtil.toBean(cacheData, new TypeReference<Page<IchProjectVO>>() {}, false);
        } catch (Exception ex) {
            return null;
        }
    }

    private void cachePage(String queryKey, Page<IchProjectVO> voPage, long ttlMinutes) {
        if (!redisAvailable()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(queryKey, JSONUtil.toJsonStr(voPage), ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }
    }

    private void clearProjectCache() {
        if (!redisAvailable()) {
            return;
        }
        try {
            Set<String> keys = stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> cachedKeys = new HashSet<>();
                try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(
                        ScanOptions.scanOptions().match(PROJECT_PAGE_CACHE_PREFIX + "*").count(100).build())) {
                    while (cursor.hasNext()) {
                        cachedKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return cachedKeys;
            });
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
            stringRedisTemplate.delete(ECHARTS_DATA_KEY);
            stringRedisTemplate.convertAndSend("project_update_channel", "PROJECT_DATA_CHANGED");
        } catch (Exception ignored) {
        }
    }

    private String safeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private boolean redisAvailable() {
        if (stringRedisTemplate == null) {
            return false;
        }
        try {
            stringRedisTemplate.hasKey("redis:healthcheck");
            return true;
        } catch (RedisConnectionFailureException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}