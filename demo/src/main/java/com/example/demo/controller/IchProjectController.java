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
import com.example.demo.service.IchInheritorService;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class IchProjectController {

    @Autowired
    private IchProjectService projectService;

    @Autowired
    private IchInheritorService inheritorService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String PROJECT_PAGE_CACHE_PREFIX = "ICH:PROJECT:PAGE:";
    private static final String ECHARTS_DATA_KEY = "ICH:STATISTICS:HOME_DATA";

    // ================== 查询操作 (高级缓存架构) ==================

    @GetMapping("/page")
    public Result<Page<com.example.demo.model.vo.IchProjectVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String protectLevel,
            @RequestParam(required = false) Integer auditStatus) {

        String queryKey = PROJECT_PAGE_CACHE_PREFIX + pageNum + "_" + pageSize + "_" +
                (StrUtil.isBlank(name) ? "all" : name) + "_" +
                (StrUtil.isBlank(protectLevel) ? "all" : protectLevel) + "_" +
                (auditStatus == null ? "all" : auditStatus);

        String cacheData = stringRedisTemplate.opsForValue().get(queryKey);
        if (StrUtil.isNotBlank(cacheData)) {
            System.out.println("【非遗项目列表】🚀 命中 Redis 缓存");
            Page<com.example.demo.model.vo.IchProjectVO> cachedPage = JSONUtil.toBean(cacheData,
                    new TypeReference<Page<com.example.demo.model.vo.IchProjectVO>>() {
                    }, false);
            return Result.success(cachedPage);
        }

        System.out.println("【非遗项目列表】🐌 缓存未命中，查询 MySQL 数据库...");
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
        Page<com.example.demo.model.vo.IchProjectVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(),
                pageResult.getTotal());

        if (pageResult.getRecords() == null || pageResult.getRecords().isEmpty()) {
            System.out.println("【非遗项目列表】🛡️ 触发防缓存穿透，缓存空对象，TTL 1分钟");
            stringRedisTemplate.opsForValue().set(queryKey, JSONUtil.toJsonStr(voPage), 1, TimeUnit.MINUTES);
            return Result.success(voPage);
        }

        List<com.example.demo.model.vo.IchProjectVO> voList = pageResult.getRecords().stream().map(project -> {
            com.example.demo.model.vo.IchProjectVO vo = new com.example.demo.model.vo.IchProjectVO();
            org.springframework.beans.BeanUtils.copyProperties(project, vo);
            List<IchInheritor> inheritors = inheritorService.list(
                    new QueryWrapper<IchInheritor>().eq("project_id", project.getId()));
            String names = inheritors.stream().map(IchInheritor::getName).collect(Collectors.joining(", "));
            vo.setInheritorNames(names);
            List<Long> ids = inheritors.stream().map(IchInheritor::getId).collect(Collectors.toList());
            vo.setInheritorIds(ids);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);

        long randomMinutes = RandomUtil.randomInt(1, 6);
        stringRedisTemplate.opsForValue().set(queryKey, JSONUtil.toJsonStr(voPage), 30 + randomMinutes,
                TimeUnit.MINUTES);

        return Result.success(voPage);
    }

    // ================== 增删改操作 ==================

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody com.example.demo.model.dto.IchProjectDTO project) {
        // 如果是从非管理员页面提交，通常 audit_status 默认为 0（待审核）。不过这里交由前端或是默认传递 0
        if (project.getAuditStatus() == null) {
            project.setAuditStatus(0);
        }
        // ✅ Bug6修复：调用带 @Transactional 的 Service 方法
        Result<Boolean> res = projectService.saveProjectWithInheritors(project);
        if (Boolean.TRUE.equals(res.getData()))
            clearProjectCache();
        return res;
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody com.example.demo.model.dto.IchProjectDTO project) {
        Result<Boolean> res = projectService.saveProjectWithInheritors(project);
        if (Boolean.TRUE.equals(res.getData()))
            clearProjectCache();
        return res;
    }

    @PutMapping("/audit")
    public Result<Boolean> audit(
            @RequestParam Long id,
            @RequestParam Integer auditStatus,
            @RequestParam(required = false) String auditReason,
            javax.servlet.http.HttpServletRequest request) {

        if (auditStatus == 2 && (auditReason == null || auditReason.trim().isEmpty())) {
            return Result.error("驳回时必须填写驳回原因");
        }

        IchProject project = projectService.getById(id);
        if (project == null) {
            return Result.error("项目不存在");
        }

        project.setAuditStatus(auditStatus);
        if (auditStatus == 2 && auditReason != null) {
            project.setAuditReason(auditReason.trim());
        } else if (auditStatus == 1) {
            project.setAuditReason(null); // 通过时清空驳回原因
        }

        // 记录审核人
        String username = (String) request.getAttribute("username");
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
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = projectService.removeById(id);
        if (success)
            clearProjectCache();
        return Result.success(success);
    }

    @DeleteMapping("/delete/batch")
    public Result<Boolean> deleteBatch(@RequestBody List<Integer> ids) {
        boolean success = projectService.removeByIds(ids);
        if (success)
            clearProjectCache();
        return Result.success(success);
    }

    // ================== 缓存清理器 ==================

    private void clearProjectCache() {
        // 使用 scan 替代危险的 keys 取保 Redis 安全
        Set<String> keys = stringRedisTemplate
                .execute((org.springframework.data.redis.core.RedisCallback<Set<String>>) connection -> {
                    Set<String> keysTmp = new java.util.HashSet<>();
                    try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection
                            .scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                                    .match(PROJECT_PAGE_CACHE_PREFIX + "*").count(100).build())) {
                        while (cursor.hasNext()) {
                            keysTmp.add(new String(cursor.next(), java.nio.charset.StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                    }
                    return keysTmp;
                });
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            System.out.println("【缓存清理】已安全清空非遗项目列表相关的 Redis 缓存，防止脏读");
        }
        stringRedisTemplate.delete(ECHARTS_DATA_KEY);
        System.out.println("【缓存清理】联动清空首页大屏数据 Redis 缓存");
        stringRedisTemplate.convertAndSend("project_update_channel", "PROJECT_DATA_CHANGED");
        System.out.println("【WebSocket广播】已通过 Redis 发送全局数据更新通知");
    }
}