package com.example.demo.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.service.SysUserService;
import com.example.demo.util.PasswordUtil;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private RequestAuthUtil requestAuthUtil;

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("未登录或令牌无效");
        }
        if (!requestAuthUtil.isAdmin(request) && !requestAuthUtil.isCurrentUser(request, id)) {
            return Result.error("无权限查看其他用户信息");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPasswordHash(null);
        return Result.success(user);
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody SysUser user, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("未登录或令牌无效");
        }
        if (user.getId() == null || (!requestAuthUtil.isAdmin(request) && !requestAuthUtil.isCurrentUser(request, user.getId()))) {
            return Result.error("无权限修改其他用户信息");
        }

        user.setPasswordHash(null);
        user.setRole(null);
        user.setUsername(null);
        return Result.success(sysUserService.updateById(user));
    }

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody SysUser user, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限创建用户");
        }
        if (StrUtil.isBlank(user.getUsername())) {
            return Result.error("用户名不能为空");
        }

        long count = sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            return Result.error("用户名已存在");
        }

        user.setPasswordHash(PasswordUtil.encode("123456"));
        return Result.success(sysUserService.save(user));
    }

    @GetMapping("/page")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) String keyword,
                                      HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限查看用户列表");
        }

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper;
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

        Page<SysUser> resultPage = sysUserService.page(page, wrapper);
        resultPage.getRecords().forEach(item -> item.setPasswordHash(null));
        return Result.success(resultPage);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限删除用户");
        }
        return Result.success(sysUserService.removeById(id));
    }

    @PutMapping("/reset-password/{id}")
    public Result<Boolean> resetPassword(@PathVariable Long id, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("无权限重置其他用户密码");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPasswordHash(PasswordUtil.encode("123456"));
        return Result.success(sysUserService.updateById(user));
    }

    @PostMapping("/change-password")
    public Result<Boolean> changePassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("未登录或令牌无效");
        }

        Long targetUserId;
        try {
            targetUserId = Long.parseLong(params.get("id"));
        } catch (Exception ignored) {
            targetUserId = null;
        }
        if (targetUserId == null || !targetUserId.equals(currentUserId)) {
            return Result.error("不能修改他人密码");
        }

        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        SysUser user = sysUserService.getById(targetUserId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!PasswordUtil.matches(oldPassword, user.getPasswordHash())) {
            return Result.error("原密码错误");
        }

        user.setPasswordHash(PasswordUtil.encode(newPassword));
        return Result.success(sysUserService.updateById(user));
    }
}
