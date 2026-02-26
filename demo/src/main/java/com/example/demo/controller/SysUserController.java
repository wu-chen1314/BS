package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import cn.hutool.core.util.StrUtil; // 如果报错，请确保引入了 Hutool 或换用 StringUtils

@RestController
@RequestMapping("/api/users")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    // ✨✨✨ 必须有的接口：获取详情用于回显 ✨✨✨
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            user.setPasswordHash(null); // 隐私保护
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }

    // 更新用户信息 (修改头像、昵称)
    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody SysUser user) {
        // 1. 禁止修改密码
        user.setPasswordHash(null);
        // 2. 禁止修改角色
        user.setRole(null);
        // 3. 禁止修改账号
        user.setUsername(null);

        // MyBatis-Plus 会自动更新前端传过来的非空字段 (比如 avatarUrl)
        return Result.success(sysUserService.updateById(user));
    }

    // ... 其他接口 (page, add, delete, change-password) 保持你原来的代码即可 ...
    // 为了完整性，这里放一个分页查询的简化版，防止你报错
    @GetMapping("/page")
    public Result<Page<SysUser>> getPage(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         @RequestParam(required = false) String username) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotEmpty(username)) {
            wrapper.like(SysUser::getUsername, username);
        }
        wrapper.orderByDesc(SysUser::getId);
        return Result.success(sysUserService.page(page, wrapper));
    }
}