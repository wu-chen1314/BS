package com.example.demo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.config.WebConfig;
import com.example.demo.controller.AppCommentController;
import com.example.demo.controller.SysUserController;
import com.example.demo.entity.AppComment;
import com.example.demo.entity.SysUser;
import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.service.AppCommentService;
import com.example.demo.service.SysUserService;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.RequestAuthUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SysUserController.class, AppCommentController.class})
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000"
})
class AuthorizationWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private SysUserService sysUserService;

    @MockBean
    private AppCommentService commentService;

    @Test
    void adminCanAccessUserPage() throws Exception {
        Page<SysUser> page = new Page<>(1, 10);
        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("visitor");
        page.setRecords(Collections.singletonList(user));
        page.setTotal(1);

        when(sysUserService.page(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/users/page")
                        .header("Authorization", bearerToken(1L, "admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void normalUserCannotAccessUserPage() throws Exception {
        mockMvc.perform(get("/api/users/page")
                        .header("Authorization", bearerToken(6L, "reader", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(sysUserService, never()).page(any(Page.class), any());
    }

    @Test
    void interceptorRejectsMissingTokenForProtectedEndpoint() throws Exception {
        mockMvc.perform(post("/api/comments/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"content\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void commentAddUsesCurrentUserFromJwtInsteadOfPayload() throws Exception {
        when(commentService.save(any(AppComment.class))).thenReturn(true);

        mockMvc.perform(post("/api/comments/add")
                        .header("Authorization", bearerToken(8L, "heritage_user", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"userId\":999,\"content\":\"来自测试的评论\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AppComment> captor = ArgumentCaptor.forClass(AppComment.class);
        verify(commentService).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(8L);
        assertThat(captor.getValue().getProjectId()).isEqualTo(1L);
    }

    private String bearerToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return "Bearer " + jwtUtil.generateToken(claims, username);
    }
}
