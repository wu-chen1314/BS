package com.example.demo.controller;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
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
    public Result<Page<IchProject>> page(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String protectLevel) {

        String queryKey = PROJECT_PAGE_CACHE_PREFIX + pageNum + "_" + pageSize + "_" +
                (StrUtil.isBlank(name) ? "all" : name) + "_" +
                (StrUtil.isBlank(protectLevel) ? "all" : protectLevel);

        String cacheData = stringRedisTemplate.opsForValue().get(queryKey);
        if (StrUtil.isNotBlank(cacheData)) {
            System.out.println("【非遗项目列表】🚀 命中 Redis 缓存");
            Page<IchProject> cachedPage = JSONUtil.toBean(cacheData, new TypeReference<Page<IchProject>>() {
            }, false);
            return Result.success(cachedPage);
        }

        System.out.println("【非遗项目列表】🐌 缓存未命中，查询 MySQL 数据库...");
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
            System.out.println("【非遗项目列表】🛡️ 触发防缓存穿透，缓存空对象，TTL 1分钟");
            stringRedisTemplate.opsForValue().set(queryKey, JSONUtil.toJsonStr(pageResult), 1, TimeUnit.MINUTES);
            return Result.success(pageResult);
        }

        pageResult.getRecords().forEach(project -> {
            List<IchInheritor> inheritors = inheritorService.list(
                    new QueryWrapper<IchInheritor>().eq("project_id", project.getId()));
            String names = inheritors.stream().map(IchInheritor::getName).collect(Collectors.joining(", "));
            project.setInheritorNames(names);
            List<Long> ids = inheritors.stream().map(IchInheritor::getId).collect(Collectors.toList());
            project.setInheritorIds(ids);
        });

        long randomMinutes = RandomUtil.randomInt(1, 6);
        stringRedisTemplate.opsForValue().set(queryKey, JSONUtil.toJsonStr(pageResult), 30 + randomMinutes,
                TimeUnit.MINUTES);

        return Result.success(pageResult);
    }

    // ================== 增删改操作 ==================

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody IchProject project) {
        // ✅ Bug6修复：调用带 @Transactional 的 Service 方法
        Result<Boolean> res = projectService.saveProjectWithInheritors(project);
        if (Boolean.TRUE.equals(res.getData()))
            clearProjectCache();
        return res;
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody IchProject project) {
        Result<Boolean> res = projectService.saveProjectWithInheritors(project);
        if (Boolean.TRUE.equals(res.getData()))
            clearProjectCache();
        return res;
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
        Set<String> keys = stringRedisTemplate.keys(PROJECT_PAGE_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            System.out.println("【缓存清理】已清空非遗项目列表相关的 Redis 缓存，防止脏读");
        }
        stringRedisTemplate.delete(ECHARTS_DATA_KEY);
        System.out.println("【缓存清理】联动清空首页大屏数据 Redis 缓存");
        stringRedisTemplate.convertAndSend("project_update_channel", "PROJECT_DATA_CHANGED");
        System.out.println("【WebSocket广播】已通过 Redis 发送全局数据更新通知");
    }
}