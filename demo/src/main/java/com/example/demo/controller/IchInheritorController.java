package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.result.Result;
import com.example.demo.entity.IchInheritor;
import com.example.demo.entity.IchProject;
import com.example.demo.model.dto.BatchDeleteRequest;
import com.example.demo.model.vo.IchInheritorVO;
import com.example.demo.service.IchInheritorService;
import com.example.demo.service.IchProjectService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
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

    @GetMapping("/page")
    @PostMapping("/page")
    public Result<Page<IchInheritorVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
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
        Page<IchInheritorVO> voPage = new Page<>(pageNum, pageSize, pageResult.getTotal());

        if (!pageResult.getRecords().isEmpty()) {
            List<Long> projectIds = pageResult.getRecords().stream()
                    .map(IchInheritor::getProjectId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, String> projectNameMap = new HashMap<>();
            if (!projectIds.isEmpty()) {
                List<IchProject> projects = projectService.listByIds(projectIds);
                projectNameMap = projects.stream().collect(Collectors.toMap(IchProject::getId, IchProject::getName));
            }

            final Map<Long, String> finalProjectNameMap = projectNameMap;
            List<IchInheritorVO> voList = pageResult.getRecords().stream().map(record -> {
                IchInheritorVO vo = new IchInheritorVO();
                org.springframework.beans.BeanUtils.copyProperties(record, vo);
                if (vo.getProjectId() != null) {
                    vo.setProjectName(finalProjectNameMap.getOrDefault(vo.getProjectId(), ""));
                }
                return vo;
            }).collect(Collectors.toList());
            voPage.setRecords(voList);
        }

        return Result.success(voPage);
    }

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody IchInheritor inheritor, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can add inheritors");
        }
        return Result.success(inheritorService.save(inheritor));
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody IchInheritor inheritor, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can update inheritors");
        }
        return Result.success(inheritorService.updateById(inheritor));
    }

    @DeleteMapping("/delete/{id}")
    @PostMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can delete inheritors");
        }
        return Result.success(inheritorService.removeById(id));
    }

    @RequestMapping(value = { "/batch", "/delete/batch" }, method = RequestMethod.POST)
    public Result<Integer> batchDelete(@RequestBody Object requestBody, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can batch delete inheritors");
        }

        List<Long> ids = null;
        if (requestBody instanceof List) {
            List<?> rawList = (List<?>) requestBody;
            ids = rawList.stream().map(this::castToLong).collect(Collectors.toList());
        } else if (requestBody instanceof Map) {
            Object idsObj = ((Map<?, ?>) requestBody).get("ids");
            if (idsObj instanceof List) {
                ids = ((List<?>) idsObj).stream().map(this::castToLong).collect(Collectors.toList());
            }
        } else if (requestBody instanceof BatchDeleteRequest) {
            ids = ((BatchDeleteRequest) requestBody).getIds();
        }

        if (ids == null || ids.isEmpty()) {
            return Result.error("Please select inheritors to delete");
        }

        int count = 0;
        for (Long id : ids) {
            if (inheritorService.removeById(id)) {
                count++;
            }
        }

        Result<Integer> result = Result.success(count);
        result.setMsg(count > 0 ? "Deleted " + count + " inheritors" : "No matching inheritors were deleted");
        return result;
    }

    @DeleteMapping("/batch-by-project/{projectId}")
    @PostMapping("/batch-by-project/{projectId}")
    public Result<Integer> batchDeleteByProject(@PathVariable Long projectId, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can batch delete inheritors");
        }

        QueryWrapper<IchInheritor> query = new QueryWrapper<>();
        query.eq("project_id", projectId);

        List<IchInheritor> inheritors = inheritorService.list(query);
        if (inheritors.isEmpty()) {
            return Result.error("No inheritors found for this project");
        }

        boolean success = inheritorService.remove(query);
        if (!success) {
            return Result.error("Delete failed");
        }

        Result<Integer> result = Result.success(inheritors.size());
        result.setMsg("Deleted " + inheritors.size() + " inheritors");
        return result;
    }

    private Long castToLong(Object item) {
        if (item instanceof Long) {
            return (Long) item;
        }
        if (item instanceof Integer) {
            return ((Integer) item).longValue();
        }
        if (item instanceof Number) {
            return ((Number) item).longValue();
        }
        throw new IllegalArgumentException("Invalid ID type: " + item.getClass());
    }
}