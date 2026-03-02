package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.IchInheritor;
import com.example.demo.entity.IchProject;
import com.example.demo.request.BatchDeleteRequest;
import com.example.demo.service.IchInheritorService;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inheritors")
public class IchInheritorController {

    @Autowired
    private IchInheritorService inheritorService;

    @Autowired
    private IchProjectService projectService;

    /**
     * 获取传承人列表
     * GET /api/inheritors/page
     * POST /api/inheritors/page (也支持 POST 方法)
     */
    @GetMapping("/page")
    @PostMapping("/page")
    public Result<Page<IchInheritor>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {

        QueryWrapper<IchInheritor> query = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            query.like("name", name);
        }
        query.orderByDesc("id");

        Page<IchInheritor> pageResult = inheritorService.page(new Page<>(pageNum, pageSize), query);

        if (!pageResult.getRecords().isEmpty()) {
            // ✅ Bug4修复：一次批量 IN 查询取出所有项目名，避免 N+1
            List<Long> projectIds = pageResult.getRecords().stream()
                    .map(IchInheritor::getProjectId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());

            if (!projectIds.isEmpty()) {
                List<IchProject> projects = projectService.listByIds(projectIds);
                Map<Long, String> nameMap = projects.stream()
                        .collect(Collectors.toMap(IchProject::getId, IchProject::getName));
                pageResult.getRecords().forEach(record -> {
                    if (record.getProjectId() != null) {
                        record.setProjectName(nameMap.getOrDefault(record.getProjectId(), ""));
                    }
                });
            }
        }

        return Result.success(pageResult);
    }

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody IchInheritor inheritor) {
        return Result.success(inheritorService.save(inheritor));
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody IchInheritor inheritor) {
        return Result.success(inheritorService.updateById(inheritor));
    }

    /**
     * 删除传承人
     * DELETE /api/inheritors/delete/{id}
     * POST /api/inheritors/delete/{id} (也支持 POST 方法)
     */
    @DeleteMapping("/delete/{id}")
    @PostMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(inheritorService.removeById(id));
    }

    /**
     * 批量删除传承人（支持数组和对象两种格式）
     * POST /api/inheritors/batch
     * POST /api/inheritors/delete/batch
     * 
     * 支持两种请求格式：
     * 1. 数组格式：[1, 2, 3]
     * 2. 对象格式：{ "ids": [1, 2, 3] }
     */
    @RequestMapping(value = {"/batch", "/delete/batch"}, method = RequestMethod.POST)
    public Result<Integer> batchDelete(@RequestBody Object request) {
        List<Long> ids = null;
        
        // 处理不同的请求格式
        if (request instanceof java.util.List) {
            // 数组格式：[1, 2, 3]
            List<?> rawList = (List<?>) request;
            ids = rawList.stream()
                    .map(item -> {
                        if (item instanceof Long) {
                            return (Long) item;
                        } else if (item instanceof Integer) {
                            return ((Integer) item).longValue();
                        } else if (item instanceof Number) {
                            return ((Number) item).longValue();
                        } else {
                            throw new IllegalArgumentException("Invalid ID type: " + item.getClass());
                        }
                    })
                    .collect(Collectors.toList());
        } else if (request instanceof java.util.Map) {
            // 对象格式：{ "ids": [1, 2, 3] }
            Map<?, ?> map = (Map<?, ?>) request;
            Object idsObj = map.get("ids");
            if (idsObj instanceof List) {
                List<?> rawList = (List<?>) idsObj;
                ids = rawList.stream()
                        .map(item -> {
                            if (item instanceof Long) {
                                return (Long) item;
                            } else if (item instanceof Integer) {
                                return ((Integer) item).longValue();
                            } else if (item instanceof Number) {
                                return ((Number) item).longValue();
                            } else {
                                throw new IllegalArgumentException("Invalid ID type: " + item.getClass());
                            }
                        })
                        .collect(Collectors.toList());
            }
        } else if (request instanceof BatchDeleteRequest) {
            // BatchDeleteRequest 对象格式
            ids = ((BatchDeleteRequest) request).getIds();
        }
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的传承人");
        }
        
        int count = 0;
        for (Long id : ids) {
            if (inheritorService.removeById(id)) {
                count++;
            }
        }
        
        if (count > 0) {
            Result<Integer> result = Result.success(count);
            result.setMsg("成功删除 " + count + " 位传承人");
            return result;
        } else {
            // 即使没有删除成功的记录，也返回成功但提示用户
            Result<Integer> result = Result.success(count);
            result.setMsg("没有找到匹配的记录进行删除");
            return result;
        }
    }

    /**
     * 根据项目 ID 批量删除传承人
     * DELETE /api/inheritors/batch-by-project/{projectId}
     * POST /api/inheritors/batch-by-project/{projectId} (也支持 POST 方法)
     */
    @DeleteMapping("/batch-by-project/{projectId}")
    @PostMapping("/batch-by-project/{projectId}")
    public Result<Integer> batchDeleteByProject(@PathVariable Long projectId) {
        QueryWrapper<IchInheritor> query = new QueryWrapper<>();
        query.eq("project_id", projectId);
        
        List<IchInheritor> inheritors = inheritorService.list(query);
        if (inheritors.isEmpty()) {
            return Result.error("该项目下没有传承人");
        }
        
        boolean success = inheritorService.remove(query);
        if (success) {
            Result<Integer> result = Result.success(inheritors.size());
            result.setMsg("成功删除 " + inheritors.size() + " 位传承人");
            return result;
        } else {
            return Result.error("删除失败");
        }
    }
}
