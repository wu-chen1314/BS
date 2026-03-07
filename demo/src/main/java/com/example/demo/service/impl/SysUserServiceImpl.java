package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.result.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.SysUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public Result<SysUser> register(SysUser user) {
        if (user == null || !StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPasswordHash())) {
            return Result.error("Username and password are required");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            return Result.error("Email is required");
        }

        QueryWrapper<SysUser> duplicateQuery = new QueryWrapper<>();
        duplicateQuery.eq("username", user.getUsername()).or().eq("email", user.getEmail());
        SysUser existUser = this.getOne(duplicateQuery);
        if (existUser != null) {
            if (user.getUsername().equals(existUser.getUsername())) {
                return Result.error("Username already exists");
            }
            return Result.error("Email is already in use");
        }

        user.setPasswordHash(DigestUtils.md5DigestAsHex(user.getPasswordHash().getBytes(StandardCharsets.UTF_8)));
        user.setRole("user");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        if (this.save(user)) {
            user.setPasswordHash(null);
            return Result.success(user);
        }
        return Result.error("Registration failed");
    }

    @Override
    public Result<SysUser> login(SysUser user) {
        if (user.getUsername() == null || user.getPasswordHash() == null) {
            return Result.error("Username and password are required");
        }

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        SysUser existUser = this.getOne(query);

        if (existUser == null) {
            return Result.error("Invalid username or password");
        }

        String passwordHash = DigestUtils.md5DigestAsHex(user.getPasswordHash().getBytes(StandardCharsets.UTF_8));
        if (!existUser.getPasswordHash().equals(passwordHash)) {
            return Result.error("Invalid username or password");
        }

        if (existUser.getStatus() == 0) {
            return Result.error("User is disabled");
        }

        existUser.setLastLoginAt(LocalDateTime.now());
        this.updateById(existUser);
        existUser.setPasswordHash(null);
        return Result.success(existUser);
    }

    @Override
    public Result<Boolean> resetPassword(Map<String, String> params) {
        String username = params.get("username");
        String email = params.get("email");
        String newPassword = params.get("newPassword");

        if (!StringUtils.hasText(username) || !StringUtils.hasText(email) || !StringUtils.hasText(newPassword)) {
            return Result.error("Username, email and new password are required");
        }

        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("email", email);
        SysUser user = this.getOne(query);

        if (user == null) {
            return Result.error("User not found");
        }
        if (!username.equals(user.getUsername())) {
            return Result.error("Username and email do not match");
        }

        user.setPasswordHash(DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8)));
        user.setUpdatedAt(LocalDateTime.now());

        if (this.updateById(user)) {
            return Result.success(true);
        }
        return Result.error("Password reset failed");
    }
}