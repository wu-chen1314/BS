package com.example.demo;

import com.example.demo.config.WebConfig;
import com.example.demo.controller.GeoCoordinateController;
import com.example.demo.entity.IchProject;
import com.example.demo.entity.IchRegion;
import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.mapper.IchProjectMapper;
import com.example.demo.mapper.IchRegionMapper;
import com.example.demo.service.IchProjectService;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.RequestAuthUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GeoCoordinateController.class)
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000"
})
class GeoCoordinateWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private IchProjectService projectService;

    @MockBean
    private IchProjectMapper projectMapper;

    @MockBean
    private IchRegionMapper regionMapper;

    @Test
    void summaryCountsEffectiveProjectCoordinatesFromRegionFallback() throws Exception {
        IchProject directProject = new IchProject();
        directProject.setId(1L);
        directProject.setRegionId(11L);
        directProject.setLongitude(new BigDecimal("116.391297"));
        directProject.setLatitude(new BigDecimal("39.905714"));

        IchProject fallbackProject = new IchProject();
        fallbackProject.setId(2L);
        fallbackProject.setRegionId(35L);

        IchProject missingProject = new IchProject();
        missingProject.setId(3L);
        missingProject.setRegionId(44L);

        IchRegion regionWithCoordinates = new IchRegion();
        regionWithCoordinates.setId(35L);
        regionWithCoordinates.setLongitude(new BigDecimal("118.220872"));
        regionWithCoordinates.setLatitude(new BigDecimal("26.193218"));

        IchRegion regionWithoutCoordinates = new IchRegion();
        regionWithoutCoordinates.setId(44L);

        when(projectService.list(any())).thenReturn(Arrays.asList(directProject, fallbackProject, missingProject));
        when(regionMapper.selectBatchIds(any())).thenReturn(Arrays.asList(regionWithCoordinates, regionWithoutCoordinates));
        when(regionMapper.selectCount(any())).thenReturn(14L);

        mockMvc.perform(get("/api/geo/summary").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalProjects").value(3))
                .andExpect(jsonPath("$.data.projectsWithCoordinates").value(2))
                .andExpect(jsonPath("$.data.projectsMissingCoordinates").value(1))
                .andExpect(jsonPath("$.data.directProjectsWithCoordinates").value(1))
                .andExpect(jsonPath("$.data.directProjectsMissingCoordinates").value(2))
                .andExpect(jsonPath("$.data.regionsWithCoordinates").value(14));
    }

    @Test
    void summaryRejectsNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/geo/summary").header("Authorization", nonAdminBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("仅管理员可查看地理数据概览"));
    }

    private String bearerToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("username", "admin");
        claims.put("role", "admin");
        return "Bearer " + jwtUtil.generateToken(claims, "admin");
    }

    private String nonAdminBearerToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 2L);
        claims.put("username", "user");
        claims.put("role", "user");
        return "Bearer " + jwtUtil.generateToken(claims, "user");
    }
}
