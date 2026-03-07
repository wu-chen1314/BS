package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.service.CaptchaService;
import com.example.demo.service.IpRateLimitService;
import com.example.demo.service.SysUserService;
import com.example.demo.service.VerificationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private IpRateLimitService ipRateLimitService;

    @Autowired
    private CaptchaService captchaService;

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

    /**
     * 获取一个简单的计算题验证码
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        // 生成两个 1-9 的随机数，避免出现 0
        int a = 1 + (int) (Math.random() * 9);
        int b = 1 + (int) (Math.random() * 9);
        String question = a + " + " + b + " = ?";
        String answer = String.valueOf(a + b);

        String captchaId = captchaService.saveAnswer(answer);

        Map<String, String> data = new HashMap<>();
        data.put("captchaId", captchaId);
        data.put("question", question);
        return Result.success(data);
    }

    @PostMapping("/send-code")
    public Result<Boolean> sendCode(@RequestBody Map<String, String> params,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        String email = params.get("email");
        if (!StringUtils.hasText(email)) {
            return Result.error("邮箱不能为空");
        }

        // 限制同一 IP 在 5 分钟内发送验证码次数，默认最多 5 次
        String clientIp = getClientIp(request);
        String ipKey = "auth:send-code:" + clientIp;
        if (!ipRateLimitService.isAllowed(ipKey, 5, 300)) {
            long ttl = ipRateLimitService.getRemainingSeconds(ipKey);
            response.setStatus(429);
            String msg = ttl > 0
                    ? String.format("获取验证码过于频繁，请 %d 秒后再试", ttl)
                    : "获取验证码过于频繁，请稍后再试";
            return Result.error(msg);
        }

        return verificationCodeService.sendCode(email);
    }

    @PostMapping("/register")
    public Result<SysUser> register(@RequestBody SysUser user,
                                    @RequestParam String code,
                                    @RequestParam String captchaId,
                                    @RequestParam String captchaAnswer,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        if (!StringUtils.hasText(user.getEmail())) {
            return Result.error("邮箱不能为空");
        }
        // 校验图形/计算题验证码（优先）
        if (!captchaService.validate(captchaId, captchaAnswer, true)) {
            return Result.error("验证码错误或已过期");
        }
        if (!verificationCodeService.verifyCode(user.getEmail(), code, true)) {
            return Result.error("Verification code is invalid or expired");
        }

        // 限制同一 IP 的注册频率：1 小时内最多 3 次
        String clientIp = getClientIp(request);
        String ipKey = "auth:register:" + clientIp;
        if (!ipRateLimitService.isAllowed(ipKey, 3, 3600)) {
            long ttl = ipRateLimitService.getRemainingSeconds(ipKey);
            response.setStatus(429);
            String msg = ttl > 0
                    ? String.format("注册请求过于频繁，请 %d 秒后再试", ttl)
                    : "注册请求过于频繁，请稍后再试";
            return Result.error(msg);
        }

        return sysUserService.register(user);
    }

    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@RequestBody Map<String, String> params,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        String email = params.get("email");
        String code = params.get("code");
        if (!verificationCodeService.verifyCode(email, code, true)) {
            return Result.error("Verification code is invalid or expired");
        }

        // 限制同一 IP 重置密码频率：10 分钟内最多 5 次
        String clientIp = getClientIp(request);
        String ipKey = "auth:reset:" + clientIp;
        if (!ipRateLimitService.isAllowed(ipKey, 5, 600)) {
            long ttl = ipRateLimitService.getRemainingSeconds(ipKey);
            response.setStatus(429);
            String msg = ttl > 0
                    ? String.format("重置密码请求过于频繁，请 %d 秒后再试", ttl)
                    : "重置密码请求过于频繁，请稍后再试";
            return Result.error(msg);
        }

        return sysUserService.resetPassword(params);
    }
}