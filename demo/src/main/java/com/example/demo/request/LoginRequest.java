package com.example.demo.request;

import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码（明文）
     */
    private String password;

    /**
     * 图形/计算题验证码 ID
     */
    private String captchaId;

    /**
     * 图形/计算题验证码答案
     */
    private String captchaAnswer;
}
