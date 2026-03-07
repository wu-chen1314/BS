package com.example.demo.controller;

import com.example.demo.common.result.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.service.SysUserService;
import com.example.demo.service.VerificationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @PostMapping("/send-code")
    public Result<String> sendCode(@RequestBody Map<String, String> params) {
        return verificationCodeService.sendCode(params.get("email"));
    }

    @PostMapping("/register")
    public Result<SysUser> register(@RequestBody SysUser user, @RequestParam String code) {
        if (!StringUtils.hasText(user.getEmail())) {
            return Result.error("Email is required");
        }
        if (!verificationCodeService.verifyCode(user.getEmail(), code)) {
            return Result.error("Verification code is invalid or expired");
        }
        return sysUserService.register(user);
    }

    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String code = params.get("code");
        if (!verificationCodeService.verifyCode(email, code)) {
            return Result.error("Verification code is invalid or expired");
        }
        return sysUserService.resetPassword(params);
    }
}