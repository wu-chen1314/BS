package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils; // ✨ 引入 Spring 自带加密工具
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/login")
    public Result<SysUser> login(@RequestBody SysUser loginForm) {
        // 1. 获取用户输入的明文密码
        String inputPwd = loginForm.getPasswordHash();

        // 2. 进行 MD5 加密 (Spring 自带工具)
        // DigestUtils.md5DigestAsHex 要求输入 byte[]
        String md5Pwd = DigestUtils.md5DigestAsHex(inputPwd.getBytes());

        // 3. 调用 Service 查库 (注意：Service 里的逻辑不需要变，因为我们传进去的已经是密文了)
        // 但为了严谨，建议直接用 MyBatis-Plus 查
        SysUser user = sysUserService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginForm.getUsername()));

        if (user != null && user.getPasswordHash().equals(md5Pwd)) {
            // 登录成功
            user.setPasswordHash(null); // 安全起见，返回给前端时不带密码
            return Result.success(user);
        } else {
            return Result.error("用户名或密码错误");
        }
    }
}