package com.example.demo;

import com.example.demo.config.WebConfig;
import com.example.demo.controller.ViewCountController;
import com.example.demo.entity.IchProject;
import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.service.IchProjectService;
import com.example.demo.service.IchProjectViewService;
import com.example.demo.service.ProjectViewCooldownService;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.RequestAuthUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ViewCountController.class)
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000"
})
class ViewCountControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private IchProjectService projectService;

    @MockBean
    private IchProjectViewService viewService;

    @MockBean
    private ProjectViewCooldownService projectViewCooldownService;

    @Test
    void adminViewAlsoParticipatesInViewCountAndRanking() throws Exception {
        IchProject project = new IchProject();
        project.setId(3L);
        project.setName("古琴艺术");

        when(projectService.getById(3L)).thenReturn(project);
        when(viewService.increaseViewCount(3L)).thenReturn(10L);

        mockMvc.perform(get("/api/view/count")
                        .param("projectId", "3")
                        .header("Authorization", bearerToken(1L, "admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(10));

        verify(viewService).increaseViewCount(3L);
        verify(projectViewCooldownService, never()).shouldCountView(anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void authenticatedUserViewIgnoresCooldownAndStillIncrements() throws Exception {
        IchProject project = new IchProject();
        project.setId(5L);
        project.setName("木版年画");

        when(projectService.getById(5L)).thenReturn(project);
        when(viewService.increaseViewCount(5L)).thenReturn(13L);

        mockMvc.perform(get("/api/view/count")
                        .param("projectId", "5")
                        .header("Authorization", bearerToken(8L, "reader", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(13));

        verify(viewService).increaseViewCount(5L);
        verify(projectViewCooldownService, never()).shouldCountView(anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void firstQualifiedViewIncrementsCount() throws Exception {
        IchProject project = new IchProject();
        project.setId(7L);
        project.setName("皮影戏");

        when(projectService.getById(7L)).thenReturn(project);
        when(viewService.increaseViewCount(7L)).thenReturn(4L);

        mockMvc.perform(get("/api/view/count")
                        .param("projectId", "7")
                        .header("Authorization", bearerToken(9L, "reader", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(4));

        verify(viewService).increaseViewCount(7L);
        verify(projectViewCooldownService, never()).shouldCountView(anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    private String bearerToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return "Bearer " + jwtUtil.generateToken(claims, username);
    }
}
