package com.example.demo.controller;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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

    // 注入 Redis 模板
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 定义企业级缓存 Key 前缀
    private static final String PROJECT_PAGE_CACHE_PREFIX = "ICH:PROJECT:PAGE:";
    // 跨模块联动：大屏数据的缓存 Key
    private static final String ECHARTS_DATA_KEY = "ICH:STATISTICS:HOME_DATA";

    // ================== 查询操作 (高级缓存架构) ==================

    @GetMapping("/page")
    public Result<Page<IchProject>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) String protectLevel) {

        // 1. 构造唯一的 Redis Key
        String queryKey = PROJECT_PAGE_CACHE_PREFIX + pageNum + "_" + pageSize + "_" +
                (StrUtil.isBlank(name) ? "all" : name) + "_" +
                (StrUtil.isBlank(protectLevel) ? "all" : protectLevel);

        // 2. 优先查询 Redis 缓存
        String cacheData = stringRedisTemplate.opsForValue().get(queryKey);
        if (StrUtil.isNotBlank(cacheData)) {
            System.out.println("【非遗项目列表】🚀 命中 Redis 缓存");
            // ✨ 修复点 1：解决泛型擦除问题，利用 TypeReference 进行精准反序列化
            Page<IchProject> cachedPage = JSONUtil.toBean(cacheData, new TypeReference<Page<IchProject>>() {}, false);
            return Result.success(cachedPage);
        }

        // 3. 缓存未命中，查询 MySQL 数据库
        System.out.println("【非遗项目列表】🐌 缓存未命中，查询 MySQL 数据库...");
        QueryWrapper<IchProject> query = new QueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            query.like("name", name);
        }
        if (StrUtil.isNotBlank(protectLevel)) {
            query.eq("protect_level", protectLevel);
        }
        query.orderByDesc("id");

        Page<IchProject> pageResult = projectService.page(new Page<>(pageNum, pageSize), query);

        // ✨ 修复点 2：解决缓存穿透问题 (查询不存在的数据)
        if (pageResult.getRecords() == null || pageResult.getRecords().isEmpty()) {
            System.out.println("【非遗项目列表】🛡️ 触发防缓存穿透，缓存空对象，TTL 1分钟");
            // 存入空数据，并设置极短的过期时间（1分钟），防止恶意刷接口
            stringRedisTemplate.opsForValue().set(queryKey, JSONUtil.toJsonStr(pageResult), 1, TimeUnit.MINUTES);
            return Result.success(pageResult);
        }

        // 填充传承人信息
        pageResult.getRecords().forEach(project -> {
            List<IchInheritor> inheritors = inheritorService.list(
                    new QueryWrapper<IchInheritor>().eq("project_id", project.getId())
            );
            String names = inheritors.stream().map(IchInheritor::getName).collect(Collectors.joining(", "));
            project.setInheritorNames(names);
            List<Long> ids = inheritors.stream().map(IchInheritor::getId).collect(Collectors.toList());
            project.setInheritorIds(ids);
        });

        // ✨ 修复点 3：解决缓存雪崩问题 (TTL 随机抖动)
        // 基础过期时间 30 分钟 + 随机 1~5 分钟的抖动，避免大批量缓存同时失效导致雪崩
        long randomMinutes = RandomUtil.randomInt(1, 6);
        stringRedisTemplate.opsForValue().set(queryKey, JSONUtil.toJsonStr(pageResult), 30 + randomMinutes, TimeUnit.MINUTES);

        return Result.success(pageResult);
    }

    // ================== 增删改操作 ==================

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody IchProject project) {
        Result<Boolean> res = saveProjectWithInheritors(project);
        if (res.getData()) clearProjectCache();
        return res;
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody IchProject project) {
        Result<Boolean> res = saveProjectWithInheritors(project);
        if (res.getData()) clearProjectCache();
        return res;
    }

    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = projectService.removeById(id);
        if (success) clearProjectCache();
        return Result.success(success);
    }

    @DeleteMapping("/delete/batch")
    public Result<Boolean> deleteBatch(@RequestBody List<Integer> ids) {
        boolean success = projectService.removeByIds(ids);
        if (success) clearProjectCache();
        return Result.success(success);
    }

    // ================== 核心业务逻辑与缓存清理器 ==================

    private Result<Boolean> saveProjectWithInheritors(IchProject project) {
        boolean success = projectService.saveOrUpdate(project);

        // 处理传承人关联
        if (success) {
            Long projectId = project.getId();

            // ✨ 修复点 4：解决关联关系更新漏洞
            // 无论前端是否传了传承人，我们都必须先“清空”该项目目前在数据库里的所有传承人绑定！
            // 这样当用户在前端“清空”选项并保存时，数据库也能正确反映。
            UpdateWrapper<IchInheritor> reset = new UpdateWrapper<>();
            reset.eq("project_id", projectId).set("project_id", null);
            inheritorService.update(reset);

            // 如果前端有传新的传承人 ID，再重新绑定进去
            List<Long> newIds = project.getInheritorIds();
            if (newIds != null && !newIds.isEmpty()) {
                List<IchInheritor> updates = newIds.stream().map(id -> {
                    IchInheritor inheritor = new IchInheritor();
                    inheritor.setId(id);
                    inheritor.setProjectId(projectId);
                    return inheritor;
                }).collect(Collectors.toList());
                inheritorService.updateBatchById(updates);
            }
        }
        return Result.success(success);
    }

    /**
     * 清理缓存策略
     * 生产环境中对于分页缓存的删除，由于 keys 指令会阻塞 Redis 线程，
     * 最佳实践是使用 scan，或不缓存分页仅缓存详情，但这里采用简化的 keys 删除满足系统需求。
     */
    private void clearProjectCache() {
        Set<String> keys = stringRedisTemplate.keys(PROJECT_PAGE_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            System.out.println("【缓存清理】已清空非遗项目列表相关的 Redis 缓存，防止脏读");
        }

        // 跨模块联动：清空首页大屏数据 Redis 缓存
        stringRedisTemplate.delete(ECHARTS_DATA_KEY);
        System.out.println("【缓存清理】联动清空首页大屏数据 Redis 缓存");

        // ✨✨ 核心魔法：向 Redis 频道发送更新指令！
        // 只要缓存被清空（意味着数据有变动），立刻广播通知所有 WebSocket 客户端进行无感刷新
        stringRedisTemplate.convertAndSend("project_update_channel", "PROJECT_DATA_CHANGED");
        System.out.println("【WebSocket广播】已通过 Redis 发送全局数据更新通知");
    }
}