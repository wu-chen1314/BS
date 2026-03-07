package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.IchInheritor;
import com.example.demo.entity.IchProject;
import com.example.demo.request.BatchDeleteRequest;
import com.example.demo.service.IchInheritorService;
import com.example.demo.service.IchProjectService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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

    @Autowired
    private RequestAuthUtil requestAuthUtil;

    @GetMapping("/page")
    @PostMapping("/page")
    public Result<Page<IchInheritor>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false) Long projectId) {

        QueryWrapper<IchInheritor> query = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            query.like("name", name);
        }
        if (projectId != null) {
            query.eq("project_id", projectId);
        }
        query.orderByDesc("id");

        Page<IchInheritor> pageResult = inheritorService.page(new Page<>(pageNum, pageSize), query);
        if (!pageResult.getRecords().isEmpty()) {
            List<Long> projectIds = pageResult.getRecords().stream()
                    .map(IchInheritor::getProjectId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());

            if (!projectIds.isEmpty()) {
                List<IchProject> projects = projectService.listByIds(projectIds);
                Map<Long, String> projectNameMap = projects.stream()
                        .collect(Collectors.toMap(IchProject::getId, IchProject::getName));
                pageResult.getRecords().forEach(record -> {
                    if (record.getProjectId() != null) {
                        record.setProjectName(projectNameMap.getOrDefault(record.getProjectId(), ""));
                    }
                });
            }
        }

        return Result.success(pageResult);
    }

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody IchInheritor inheritor, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限新增传承人");
        }
        return Result.success(inheritorService.save(inheritor));
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody IchInheritor inheritor, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限修改传承人");
        }
        return Result.success(inheritorService.updateById(inheritor));
    }

    @DeleteMapping("/delete/{id}")
    @PostMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限删除传承人");
        }
        return Result.success(inheritorService.removeById(id));
    }

    @RequestMapping(value = {"/batch", "/delete/batch"}, method = RequestMethod.POST)
    public Result<Integer> batchDelete(@RequestBody Object requestBody, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限批量删除传承人");
        }

        List<Long> ids = null;
        if (requestBody instanceof java.util.List) {
            List<?> rawList = (List<?>) requestBody;
            ids = rawList.stream().map(this::toLong).collect(Collectors.toList());
        } else if (requestBody instanceof java.util.Map) {
            Object idsObject = ((Map<?, ?>) requestBody).get("ids");
            if (idsObject instanceof List) {
                ids = ((List<?>) idsObject).stream().map(this::toLong).collect(Collectors.toList());
            }
        } else if (requestBody instanceof BatchDeleteRequest) {
            ids = ((BatchDeleteRequest) requestBody).getIds();
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
        Result<Integer> result = Result.success(count);
        result.setMsg(count > 0 ? "成功删除 " + count + " 位传承人" : "没有找到匹配的记录进行删除");
        return result;
    }

    @DeleteMapping("/batch-by-project/{projectId}")
    @PostMapping("/batch-by-project/{projectId}")
    public Result<Integer> batchDeleteByProject(@PathVariable Long projectId, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限删除项目下传承人");
        }

        QueryWrapper<IchInheritor> query = new QueryWrapper<>();
        query.eq("project_id", projectId);

        List<IchInheritor> inheritors = inheritorService.list(query);
        if (inheritors.isEmpty()) {
            return Result.error("该项目下没有传承人");
        }

        boolean success = inheritorService.remove(query);
        if (!success) {
            return Result.error("删除失败");
        }

        Result<Integer> result = Result.success(inheritors.size());
        result.setMsg("成功删除 " + inheritors.size() + " 位传承人");
        return result;
    }

    private Long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalArgumentException("Invalid ID type: " + value.getClass());
    }
}
