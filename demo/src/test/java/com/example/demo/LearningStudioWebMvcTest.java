package com.example.demo;

import com.example.demo.config.WebConfig;
import com.example.demo.controller.LearningStudioController;
import com.example.demo.entity.LearningPlanAnalytics;
import com.example.demo.entity.LearningPlanFavorite;
import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.service.LearningPlanAnalyticsService;
import com.example.demo.service.LearningPlanFavoriteService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LearningStudioController.class)
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000"
})
class LearningStudioWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private LearningPlanFavoriteService learningPlanFavoriteService;

    @MockBean
    private LearningPlanAnalyticsService learningPlanAnalyticsService;

    @Test
    void favoriteListRequiresToken() throws Exception {
        mockMvc.perform(get("/api/learning-studio/favorites/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void toggleFavoriteUsesCurrentUserFromJwt() throws Exception {
        when(learningPlanFavoriteService.getOne(any())).thenReturn(null);
        when(learningPlanFavoriteService.save(any(LearningPlanFavorite.class))).thenReturn(true);

        mockMvc.perform(post("/api/learning-studio/favorites/toggle")
                        .header("Authorization", bearerToken(9L, "studio_user", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"learning~campus~all~11.12\",\"planTitle\":\"校园研学计划方案\",\"trackId\":\"campus\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        ArgumentCaptor<LearningPlanFavorite> captor = ArgumentCaptor.forClass(LearningPlanFavorite.class);
        verify(learningPlanFavoriteService).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(9L);
        assertThat(captor.getValue().getTrackId()).isEqualTo("campus");
    }

    @Test
    void analyticsSummaryReturnsAggregatedPayload() throws Exception {
        LearningPlanAnalytics analytics = new LearningPlanAnalytics();
        analytics.setPlanId("learning~campus~all~11.12");
        analytics.setPlanTitle("校园研学计划方案");
        analytics.setTrackId("campus");
        analytics.setActionType("view");
        analytics.setAudienceTag("教师、学生社团、课程策划者");
        analytics.setLinkedThemeId("performance");
        analytics.setKeywordTags("课程导入,案例教学");

        when(learningPlanAnalyticsService.list(any())).thenReturn(Collections.singletonList(analytics));
        when(learningPlanFavoriteService.count()).thenReturn(2L);

        mockMvc.perform(get("/api/learning-studio/analytics/summary")
                        .header("Authorization", bearerToken(1L, "admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.favoriteCount").value(2))
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.data.topPlans[0].trackId").value("campus"));
    }

    private String bearerToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return "Bearer " + jwtUtil.generateToken(claims, username);
    }
}
