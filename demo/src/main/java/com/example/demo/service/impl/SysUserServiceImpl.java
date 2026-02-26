package com.example.demo.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.SysUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Map;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public Result<SysUser> register(SysUser user) {
        // 1. 校验基础参数
        if (StrUtil.isBlank(user.getUsername())) {
            return Result.error("账号不能为空");
        }
        // 注意：前端传过来的密码字段可能是 passwordHash，根据你的实体类来定
        if (StrUtil.isBlank(user.getPasswordHash())) {
            return Result.error("密码不能为空");
        }

        // 2. 账号查重校验
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, user.getUsername());
        long count = this.count(queryWrapper);
        if (count > 0) {
            return Result.error("该账号已被注册，请换一个尝试");
        }

        // 3. 密码加密 (前端传明文，后端存 MD5 密文)
        String md5Pwd = DigestUtils.md5DigestAsHex(user.getPasswordHash().getBytes());
        user.setPasswordHash(md5Pwd);

        // 4. 设置默认分配信息
        // 如果没有填昵称，随机生成一个：非遗用户_xxxx
        if (StrUtil.isBlank(user.getNickname())) {
            user.setNickname("非遗用户_" + IdUtil.fastSimpleUUID().substring(0, 5));
        }
        // 默认分配一个统一的默认头像
        if (StrUtil.isBlank(user.getAvatarUrl())) {
            user.setAvatarUrl("https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png");
        }
        // 强制设为普通用户角色，防止有人恶意注册 admin
        user.setRole("user");

        // 5. 保存到数据库
        this.save(user);

        // 注册成功后，出于安全考虑，把密码抹除再返回给前端
        user.setPasswordHash(null);
        return Result.success(user);
    }

    @Override
    public Result<SysUser> login(SysUser user) {
        // 1. 根据账号查询用户
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, user.getUsername());
        SysUser dbUser = this.getOne(queryWrapper);

        if (dbUser == null) {
            return Result.error("账号不存在");
        }

        // 2. 校验密码
        String inputMd5 = DigestUtils.md5DigestAsHex(user.getPasswordHash().getBytes());
        if (!dbUser.getPasswordHash().equalsIgnoreCase(inputMd5) && !dbUser.getPasswordHash().equals(user.getPasswordHash())) {
            return Result.error("账号或密码错误");
        }

        // 3. 登录成功，抹除密码返回
        dbUser.setPasswordHash(null);
        return Result.success(dbUser);
    }

    @Override
    public Result<Boolean> resetPassword(Map<String, String> params) {
        String username = params.get("username");
        String email = params.get("email");
        String newPassword = params.get("newPassword");

        if (StrUtil.hasBlank(username, email, newPassword)) {
            return Result.error("参数填写不完整");
        }

        // 1. 查找对应账号
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        SysUser user = this.getOne(queryWrapper);

        if (user == null) {
            return Result.error("该账号不存在");
        }

        // 2. 核心逻辑：校验预留邮箱是否匹配
        // 如果数据库里没存邮箱，或者填写的邮箱对不上，拒绝重置
        if (StrUtil.isBlank(user.getEmail()) || !user.getEmail().equals(email)) {
            return Result.error("安全验证失败：填写的邮箱与该账号预留邮箱不匹配！");
        }

        // 3. 校验通过，重置密码
        String md5Pwd = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        user.setPasswordHash(md5Pwd);
        this.updateById(user);

        return Result.success(true);
    }
}