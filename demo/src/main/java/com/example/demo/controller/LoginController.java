package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.result.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.model.dto.LoginRequest;
import com.example.demo.service.LoginAttemptService;
import com.example.demo.service.SysUserService;
import com.example.demo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest loginForm) {
        if (loginForm == null || !StringUtils.hasText(loginForm.getUsername())) {
            return Result.error("Username is required");
        }

        String username = loginForm.getUsername().trim();
        String rawPassword = StringUtils.hasText(loginForm.getPassword())
                ? loginForm.getPassword()
                : loginForm.getPasswordHash();
        if (!StringUtils.hasText(rawPassword)) {
            return Result.error("Password is required");
        }

        String lockMessage = loginAttemptService.checkLoginAttempts(username);
        if (lockMessage != null) {
            return Result.error(lockMessage);
        }

        String md5Pwd = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));

        SysUser user = sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );

        if (user == null) {
            loginAttemptService.incrementLoginAttempts(username);
            int remaining = loginAttemptService.getRemainingAttempts(username);
            return Result.error(String.format("Invalid username or password. Remaining attempts: %d", remaining));
        }

        if (!md5Pwd.equals(user.getPasswordHash())) {
            loginAttemptService.incrementLoginAttempts(username);
            int remaining = loginAttemptService.getRemainingAttempts(username);
            if (remaining > 0) {
                return Result.error(String.format("Invalid username or password. Remaining attempts: %d", remaining));
            }
            return Result.error("Account is locked for 30 minutes due to too many failed attempts");
        }

        loginAttemptService.resetLoginAttempts(username);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        String token = jwtUtil.generateToken(claims, username);

        user.setPasswordHash(null);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("token", token);
        result.put("expiresIn", 86400);
        return Result.success(result);
    }

    @PostMapping("/refresh-token")
    public Result<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String newToken = jwtUtil.refreshToken(token);
        if (newToken == null) {
            return Result.error("Failed to refresh token");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("expiresIn", 86400);
        return Result.success(result);
    }

    @GetMapping("/login/lock-status/{username}")
    public Result<Object> getLockStatus(@PathVariable String username) {
        boolean isLocked = loginAttemptService.isAccountLocked(username);
        if (isLocked) {
            long remainingMinutes = loginAttemptService.getLockRemainingTime(username);
            return Result.error(String.format("Account is locked. Remaining minutes: %d", remainingMinutes));
        }

        int attempts = loginAttemptService.getLoginAttempts(username);
        int remaining = loginAttemptService.getRemainingAttempts(username);
        return Result.success(String.format("Failed attempts: %d, remaining attempts: %d", attempts, remaining));
    }
}