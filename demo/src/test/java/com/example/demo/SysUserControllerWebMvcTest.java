package com.example.demo;

import com.example.demo.config.WebConfig;
import com.example.demo.controller.SysUserController;
import com.example.demo.entity.SysUser;
import com.example.demo.interceptor.JwtInterceptor;
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SysUserController.class)
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000"
})
class SysUserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private SysUserService sysUserService;

    @Test
    void adminUpdateOtherUserKeepsRoleAndStatusChanges() throws Exception {
        SysUser existing = new SysUser();
        existing.setId(2L);
        existing.setRole("user");
        existing.setStatus(1);

        when(sysUserService.getById(2L)).thenReturn(existing);
        when(sysUserService.updateById(any(SysUser.class))).thenReturn(true);

        mockMvc.perform(put("/api/users/update")
                        .header("Authorization", bearerToken(1L, "admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":2,\"nickname\":\"审核员\",\"email\":\"auditor@example.com\",\"role\":\"admin\",\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).updateById(captor.capture());
        SysUser payload = captor.getValue();
        assertThat(payload.getId()).isEqualTo(2L);
        assertThat(payload.getNickname()).isEqualTo("审核员");
        assertThat(payload.getEmail()).isEqualTo("auditor@example.com");
        assertThat(payload.getRole()).isEqualTo("admin");
        assertThat(payload.getStatus()).isEqualTo(0);
        assertThat(payload.getUpdatedAt()).isNotNull();
    }

    @Test
    void adminCannotDeleteSelf() throws Exception {
        mockMvc.perform(delete("/api/users/delete/1")
                        .header("Authorization", bearerToken(1L, "admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("不能删除当前登录用户"));

        verify(sysUserService, never()).removeById(1L);
    }

    private String bearerToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return "Bearer " + jwtUtil.generateToken(claims, username);
    }
}
