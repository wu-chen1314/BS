package com.example.demo.controller;

import com.example.demo.common.result.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/register")
    public Result<SysUser> register(@RequestBody SysUser user) {
        return sysUserService.register(user);
    }

    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@RequestBody Map<String, String> params) {
        return sysUserService.resetPassword(params);
    }
}
