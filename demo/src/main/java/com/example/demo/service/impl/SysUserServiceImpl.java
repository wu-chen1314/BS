package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.result.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.SysUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public Result<SysUser> register(SysUser user) {
        // 检查用户名是否存在
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        SysUser existUser = this.getOne(query);
        
        if (existUser != null) {
            return Result.error("用户名已存在");
        }
        
        // 加密密码
        String passwordHash = DigestUtils.md5DigestAsHex(user.getPasswordHash().getBytes(StandardCharsets.UTF_8));
        user.setPasswordHash(passwordHash);
        
        // 设置默认值
        user.setRole("user");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // 保存用户
        if (this.save(user)) {
            user.setPasswordHash(null); // 不返回密码
            return Result.success(user);
        } else {
            return Result.error("注册失败");
        }
    }

    @Override
    public Result<SysUser> login(SysUser user) {
        // 验证输入参数
        if (user.getUsername() == null || user.getPasswordHash() == null) {
            return Result.error("用户名或密码不能为空");
        }
        
        // 查询用户
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        SysUser existUser = this.getOne(query);
        
        if (existUser == null) {
            return Result.error("用户名或密码错误");
        }
        
        // 验证密码
        String passwordHash = DigestUtils.md5DigestAsHex(user.getPasswordHash().getBytes(StandardCharsets.UTF_8));
        if (!existUser.getPasswordHash().equals(passwordHash)) {
            return Result.error("用户名或密码错误");
        }
        
        // 检查用户状态
        if (existUser.getStatus() == 0) {
            return Result.error("用户已被禁用");
        }
        
        // 更新最后登录时间
        existUser.setLastLoginAt(LocalDateTime.now());
        this.updateById(existUser);
        
        // 不返回密码
        existUser.setPasswordHash(null);
        return Result.success(existUser);
    }

    @Override
    public Result<Boolean> resetPassword(Map<String, String> params) {
        String email = params.get("email");
        String code = params.get("code");
        String newPassword = params.get("newPassword");
        
        // 查询用户
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("email", email);
        SysUser user = this.getOne(query);
        
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // TODO: 验证验证码
        
        // 更新密码
        String passwordHash = DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8));
        user.setPasswordHash(passwordHash);
        user.setUpdatedAt(LocalDateTime.now());
        
        if (this.updateById(user)) {
            return Result.success(true);
        } else {
            return Result.error("重置密码失败");
        }
    }
}
