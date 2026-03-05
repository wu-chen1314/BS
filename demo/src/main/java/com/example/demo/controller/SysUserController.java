package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.result.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.service.SysUserService;

import java.util.Map;

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

    // 新增用户
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody SysUser user) {
        if (StrUtil.isBlank(user.getUsername())) {
            return Result.error("用户名不能为空");
        }

        // 检查用户名是否已存在
        long count = sysUserService
                .count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            return Result.error("用户名已存在");
        }

        // 默认密码为 123456
        String defaultPasswordHash = DigestUtils.md5DigestAsHex("123456".getBytes());
        user.setPasswordHash(defaultPasswordHash);

        return Result.success(sysUserService.save(user));
    }

    // 分页获取用户列表
    @GetMapping("/page")
    public Result<Page<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        
        // 构建查询条件
        LambdaQueryWrapper<SysUser> wrapper = null;
        if (StrUtil.isNotBlank(keyword)) {
            wrapper = new LambdaQueryWrapper<SysUser>()
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getNickname, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword);
        } else {
            wrapper = new LambdaQueryWrapper<>();
        }
        
        // 执行分页查询
        Page<SysUser> resultPage = sysUserService.page(page, wrapper);
        
        // 去除密码信息
        resultPage.getRecords().forEach(user -> user.setPasswordHash(null));
        
        return Result.success(resultPage);
    }

    // 删除用户
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(sysUserService.removeById(id));
    }

    // 重置密码为 123456
    @PutMapping("/reset-password/{id}")
    public Result<Boolean> resetPassword(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        String defaultPasswordHash = DigestUtils.md5DigestAsHex("123456".getBytes());
        user.setPasswordHash(defaultPasswordHash);
        return Result.success(sysUserService.updateById(user));
    }

    @PostMapping("/change-password")
    public Result<Boolean> changePassword(@RequestBody Map<String, String> params) {
        Long id = Long.parseLong(params.get("id"));
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        // 验证旧密码
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 加密旧密码并验证
        String md5OldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!md5OldPassword.equals(user.getPasswordHash())) {
            return Result.error("原密码错误");
        }

        // 加密新密码并更新
        String md5NewPassword = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        user.setPasswordHash(md5NewPassword);
        boolean success = sysUserService.updateById(user);

        return Result.success(success);
    }
}