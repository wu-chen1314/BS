package com.example.demo.util;

import java.nio.charset.StandardCharsets;

import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 统一的密码加密与校验工具：
 * - 新密码全部使用 BCrypt 存储
 * - 兼容旧的 MD5 密码，登录成功后自动迁移为 BCrypt
 */
public class PasswordUtil {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 使用 BCrypt 加密密码
     */
    public static String encode(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("rawPassword must not be empty");
        }
        return ENCODER.encode(rawPassword);
    }

    /**
     * 判断一个已存储的密码是否为旧版 MD5
     */
    public static boolean isLegacyMd5(String encodedPassword) {
        if (!StringUtils.hasText(encodedPassword)) {
            return false;
        }
        return encodedPassword.length() == 32 && encodedPassword.matches("^[0-9a-fA-F]{32}$");
    }

    /**
     * 校验密码是否匹配（支持旧版 MD5 和新版 BCrypt）
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(encodedPassword)) {
            return false;
        }

        if (isLegacyMd5(encodedPassword)) {
            String md5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
            return md5.equalsIgnoreCase(encodedPassword);
        }

        return ENCODER.matches(rawPassword, encodedPassword);
    }
}

