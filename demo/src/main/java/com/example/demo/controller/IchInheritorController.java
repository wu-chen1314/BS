package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.IchInheritor;
import com.example.demo.entity.IchProject;
import com.example.demo.service.IchInheritorService;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inheritors")
public class IchInheritorController {

    @Autowired
    private IchInheritorService inheritorService;
    @Autowired
    private IchProjectService projectService;

    // 分页查询列表
    @GetMapping("/page")
    public Result<Page<IchInheritor>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {

        QueryWrapper<IchInheritor> query = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            query.like("name", name);
        }
        query.orderByDesc("id");

        // 1. 先查出分页数据 (此时 projectName 是空的)
        Page<IchInheritor> pageResult = inheritorService.page(new Page<>(pageNum, pageSize), query);

        // ✨✨✨ 2. 关键补丁：遍历每一条数据，手动填入项目名字
        for (IchInheritor record : pageResult.getRecords()) {
            if (record.getProjectId() != null) {
                // 根据 ID 去项目表查名字
                IchProject project = projectService.getById(record.getProjectId());
                if (project != null) {
                    record.setProjectName(project.getName());
                }
            }
        }

        return Result.success(pageResult);
    }
    // 新增
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody IchInheritor inheritor) {
        // 只要前端传了 avatarUrl，这里就会自动保存进数据库
        return Result.success(inheritorService.save(inheritor));
    }

    // 修改
    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody IchInheritor inheritor) {
        // 只要前端传了 avatarUrl，这里就会自动更新
        return Result.success(inheritorService.updateById(inheritor));
    }

    // 删除
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(inheritorService.removeById(id));
    }
}