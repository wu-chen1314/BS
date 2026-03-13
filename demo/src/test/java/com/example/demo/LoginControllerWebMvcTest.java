package com.example.demo;

import com.example.demo.config.WebConfig;
import com.example.demo.controller.LoginController;
import com.example.demo.entity.SysUser;
import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.service.CaptchaService;
import com.example.demo.service.IpRateLimitService;
import com.example.demo.service.LoginAttemptService;
import com.example.demo.service.SysUserService;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.PasswordUtil;
import com.example.demo.util.RequestAuthUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoginController.class)
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000"
})
class LoginControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SysUserService sysUserService;

    @MockBean
    private LoginAttemptService loginAttemptService;

    @MockBean
    private IpRateLimitService ipRateLimitService;

    @MockBean
    private CaptchaService captchaService;

    @Test
    void disabledUserCannotLogin() throws Exception {
        SysUser disabledUser = new SysUser();
        disabledUser.setId(10L);
        disabledUser.setUsername("disabled-user");
        disabledUser.setPasswordHash(PasswordUtil.encode("secret123"));
        disabledUser.setStatus(0);
        disabledUser.setRole("user");

        when(ipRateLimitService.isAllowed(any(), anyInt(), anyLong())).thenReturn(true);
        when(captchaService.validate(any(), any(), anyBoolean())).thenReturn(true);
        when(loginAttemptService.checkLoginAttempts("disabled-user")).thenReturn(null);
        when(sysUserService.getOne(any())).thenReturn(disabledUser);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"disabled-user\",\"password\":\"secret123\",\"captchaId\":\"cap-1\",\"captchaAnswer\":\"2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("用户已被禁用"));

        verify(loginAttemptService, never()).resetLoginAttempts("disabled-user");
    }
}
