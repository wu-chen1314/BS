package com.example.demo.controller;

import com.example.demo.common.result.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.model.dto.LoginRequest;
import com.example.demo.service.LoginAttemptService;
import com.example.demo.service.SysUserService;
import com.example.demo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

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
        String username = loginForm.getUsername();
        
        // 1. 检查账户是否被锁定或失败次数过多
        String lockMessage = loginAttemptService.checkLoginAttempts(username);
        if (lockMessage != null) {
            return Result.error(lockMessage);
        }

        // 2. 获取用户输入的明文密码
        String inputPwd = loginForm.getPassword();

        // 3. 进行 MD5 加密
        String md5Pwd = DigestUtils.md5DigestAsHex(inputPwd.getBytes());

        // 4. 查询数据库
        SysUser user = sysUserService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
        );

        // 5. 验证用户和密码
        if (user == null) {
            // 用户名不存在，增加失败次数
            loginAttemptService.incrementLoginAttempts(username);
            int remaining = loginAttemptService.getRemainingAttempts(username);
            return Result.error(String.format("用户名不存在，剩余尝试次数：%d", remaining));
        }

        if (!user.getPasswordHash().equals(md5Pwd)) {
            // 密码错误，增加失败次数
            loginAttemptService.incrementLoginAttempts(username);
            int remaining = loginAttemptService.getRemainingAttempts(username);
            
            if (remaining > 0) {
                return Result.error(String.format("密码错误，剩余尝试次数：%d", remaining));
            } else {
                return Result.error(String.format("密码错误超过 5 次，账户已被锁定 %d 分钟", 30));
            }
        }

        // 6. 登录成功，重置失败次数
        loginAttemptService.resetLoginAttempts(username);
        
        // 7. 生成 JWT Token
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
