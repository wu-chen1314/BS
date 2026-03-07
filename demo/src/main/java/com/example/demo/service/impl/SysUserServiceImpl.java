package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.SysUserService;
import com.example.demo.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public Result<SysUser> register(SysUser user) {
        if (user == null || !StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPasswordHash())) {
            return Result.error("用户名或密码不能为空");
        }

        QueryWrapper<SysUser> usernameQuery = new QueryWrapper<>();
        usernameQuery.eq("username", user.getUsername().trim());
        SysUser existUser = this.getOne(usernameQuery);
        if (existUser != null) {
            return Result.error("用户名已存在");
        }

        if (StringUtils.hasText(user.getEmail())) {
            QueryWrapper<SysUser> emailQuery = new QueryWrapper<>();
            emailQuery.eq("email", user.getEmail().trim());
            if (this.getOne(emailQuery) != null) {
                return Result.error("邮箱已存在");
            }
        }

        String passwordHash = PasswordUtil.encode(user.getPasswordHash());
        user.setUsername(user.getUsername().trim());
        user.setPasswordHash(passwordHash);
        user.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname().trim() : user.getUsername());
        user.setEmail(StringUtils.hasText(user.getEmail()) ? user.getEmail().trim() : null);
        user.setRole("user");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        if (this.save(user)) {
            user.setPasswordHash(null);
            return Result.success(user);
        }
        return Result.error("注册失败");
    }

    @Override
    public Result<SysUser> login(SysUser user) {
        if (user.getUsername() == null || user.getPasswordHash() == null) {
            return Result.error("用户名或密码不能为空");
        }

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        SysUser existUser = this.getOne(query);

        if (existUser == null) {
            return Result.error("用户名或密码错误");
        }

        // 支持旧版 MD5 与新版 BCrypt 的校验
        if (!PasswordUtil.matches(user.getPasswordHash(), existUser.getPasswordHash())) {
            return Result.error("用户名或密码错误");
        }

        // 如果仍为旧版 MD5，登录成功后自动迁移为 BCrypt
        if (PasswordUtil.isLegacyMd5(existUser.getPasswordHash())) {
            existUser.setPasswordHash(PasswordUtil.encode(user.getPasswordHash()));
        }

        if (existUser.getStatus() == 0) {
            return Result.error("用户已被禁用");
        }

        existUser.setLastLoginAt(LocalDateTime.now());
        this.updateById(existUser);

        existUser.setPasswordHash(null);
        return Result.success(existUser);
    }

    @Override
    public Result<Boolean> resetPassword(Map<String, String> params) {
        String email = params.get("email");
        String username = params.get("username");
        String newPassword = params.get("newPassword");

        if (!StringUtils.hasText(email) || !StringUtils.hasText(newPassword)) {
            return Result.error("参数不完整");
        }

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("email", email.trim());
        if (StringUtils.hasText(username)) {
            query.eq("username", username.trim());
        }
        SysUser user = this.getOne(query);

        if (user == null) {
            return Result.error("用户不存在");
        }

        String passwordHash = PasswordUtil.encode(newPassword);
        user.setPasswordHash(passwordHash);
        user.setUpdatedAt(LocalDateTime.now());

        if (this.updateById(user)) {
            return Result.success(true);
        }
        return Result.error("重置密码失败");
    }
}