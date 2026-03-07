package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.request.LoginRequest;
import com.example.demo.service.CaptchaService;
import com.example.demo.service.IpRateLimitService;
import com.example.demo.service.LoginAttemptService;
import com.example.demo.service.SysUserService;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private IpRateLimitService ipRateLimitService;

    @Autowired
    private CaptchaService captchaService;

    /**
     * 获取客户端 IP（兼容代理场景）
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest loginForm,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        // 0. 基于 IP 的简单限流：同一 IP 60 秒内最多 10 次尝试
        String clientIp = getClientIp(request);
        String ipKey = "login:" + clientIp;
        if (!ipRateLimitService.isAllowed(ipKey, 10, 60)) {
            long ttl = ipRateLimitService.getRemainingSeconds(ipKey);
            response.setStatus(429);
            String msg = ttl > 0
                    ? String.format("登录过于频繁，请 %d 秒后再试", ttl)
                    : "登录过于频繁，请稍后再试";
            return Result.error(msg);
        }
        String username = loginForm.getUsername();

        // 1. 校验图形/计算题验证码
        if (!captchaService.validate(loginForm.getCaptchaId(), loginForm.getCaptchaAnswer(), true)) {
            return Result.error("验证码错误或已过期");
        }

        // 2. 检查账户是否被锁定或失败次数过多
        String lockMessage = loginAttemptService.checkLoginAttempts(username);
        if (lockMessage != null) {
            return Result.error(lockMessage);
        }

        // 3. 查询数据库
        SysUser user = sysUserService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
        );

        // 4. 验证用户和密码（兼容旧版 MD5 + 新版 BCrypt）
        if (user == null) {
            // 用户名不存在，增加失败次数
            loginAttemptService.incrementLoginAttempts(username);
            int remaining = loginAttemptService.getRemainingAttempts(username);
            return Result.error(String.format("用户名不存在，剩余尝试次数：%d", remaining));
        }

        String inputPwd = loginForm.getPassword();
        if (!PasswordUtil.matches(inputPwd, user.getPasswordHash())) {
            // 密码错误，增加失败次数
            loginAttemptService.incrementLoginAttempts(username);
            int remaining = loginAttemptService.getRemainingAttempts(username);
            
            if (remaining > 0) {
                return Result.error(String.format("密码错误，剩余尝试次数：%d", remaining));
            } else {
                return Result.error(String.format("密码错误超过 5 次，账户已被锁定 %d 分钟", 30));
            }
        }

        // 5. 登录成功，重置失败次数
        loginAttemptService.resetLoginAttempts(username);
        
        // 如仍为旧版 MD5，则自动迁移为 BCrypt
        if (PasswordUtil.isLegacyMd5(user.getPasswordHash())) {
            user.setPasswordHash(PasswordUtil.encode(inputPwd));
            sysUserService.updateById(user);
        }
        
        // 6. 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        String token = jwtUtil.generateToken(claims, username);
        
        // 8. 清除密码信息
        user.setPasswordHash(null);
        
        // 9. 返回用户信息和 Token
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("token", token);
        result.put("expiresIn", 86400); // Token 过期时间（秒）
        
        return Result.success(result);
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh-token")
    public Result<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        String newToken = jwtUtil.refreshToken(token);
        if (newToken != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("token", newToken);
            result.put("expiresIn", 86400);
            return Result.success(result);
        } else {
            return Result.error("Token 刷新失败，请重新登录");
        }
    }

    /**
     * 获取账户锁定状态
     */
    @GetMapping("/login/lock-status/{username}")
    public Result<Object> getLockStatus(@PathVariable String username) {
        boolean isLocked = loginAttemptService.isAccountLocked(username);
        
        if (isLocked) {
            long remainingTime = loginAttemptService.getLockRemainingTime(username);
            return Result.error(String.format("账户已被锁定，剩余时间：%d 分钟", remainingTime));
        }
        
        int attempts = loginAttemptService.getLoginAttempts(username);
        int remaining = loginAttemptService.getRemainingAttempts(username);
        
        return Result.success(String.format("当前失败次数：%d，剩余尝试次数：%d", attempts, remaining));
    }
}
