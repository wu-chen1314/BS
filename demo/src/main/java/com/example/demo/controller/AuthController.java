package com.example.demo.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.example.demo.common.Result;
import com.example.demo.entity.SysUser;
import com.example.demo.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private JavaMailSender mailSender;

    // ✨ 注入刚刚配置好的 Redis
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ✨ 定义存入 Redis 的验证码 Key 前缀
    private static final String EMAIL_CODE_PREFIX = "ICH:AUTH:EMAIL_CODE:";

    /**
     * 发送邮箱验证码 (Redis + TTL)
     */
    @PostMapping("/send-code")
    public Result<Boolean> sendCode(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        if (StrUtil.isBlank(email)) {
            return Result.error("邮箱不能为空");
        }

        // 1. 生成 6 位随机验证码
        String code = RandomUtil.randomNumbers(6);

        // 2. 发送真实邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("【非遗推广系统】安全验证码");
            message.setText("尊敬的用户您好：\n\n您的验证码为：【" + code + "】。\n该验证码 5 分钟内有效。如非本人操作，请忽略此邮件。");
            mailSender.send(message);

            // 3. ✨✨✨ 存入 Redis，并设置 5 分钟自动过期
            String redisKey = EMAIL_CODE_PREFIX + email;
            stringRedisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);

            System.out.println("【安全日志】验证码已存入Redis -> 邮箱: " + email + ", 验证码: " + code);

            return Result.success(true);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("验证码发送失败，请检查邮件服务器配置");
        }
    }

    /**
     * 注册接口 (校验 Redis 验证码)
     */
    @PostMapping("/register")
    public Result<SysUser> register(@RequestBody SysUser user, @RequestParam String code) {
        String redisKey = EMAIL_CODE_PREFIX + user.getEmail();

        // 1. 从 Redis 中读取验证码
        String savedCode = stringRedisTemplate.opsForValue().get(redisKey);

        // 2. 校验比对
        if (StrUtil.isBlank(savedCode) || !savedCode.equals(code)) {
            return Result.error("验证码错误或已过期(5分钟有效)");
        }

        // 3. 执行注册
        Result<SysUser> res = sysUserService.register(user);

        // 4. ✨ 注册成功后，主动销毁 Redis 中的验证码，防止被二次利用 (防重放攻击)
        if (res.getCode() == 200) {
            stringRedisTemplate.delete(redisKey);
        }
        return res;
    }

    /**
     * 登录接口 (保持不变)
     */
    @PostMapping("/login")
    public Result<SysUser> login(@RequestBody SysUser user) {
        return sysUserService.login(user);
    }

    /**
     * 忘记密码接口 (校验 Redis 验证码)
     */
    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String inputCode = params.get("code");

        String redisKey = EMAIL_CODE_PREFIX + email;

        // 1. 从 Redis 中读取验证码
        String savedCode = stringRedisTemplate.opsForValue().get(redisKey);

        // 2. 校验比对
        if (StrUtil.isBlank(savedCode) || !savedCode.equals(inputCode)) {
            return Result.error("验证码错误或已过期(5分钟有效)");
        }

        // 3. 执行重置
        Result<Boolean> res = sysUserService.resetPassword(params);

        // 4. ✨ 成功后销毁验证码
        if (res.getCode() == 200) {
            stringRedisTemplate.delete(redisKey);
        }
        return res;
    }
}