package com.example.demo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.config.WebConfig;
import com.example.demo.controller.RegionCategoryController;
import com.example.demo.entity.IchCategory;
import com.example.demo.entity.IchProject;
import com.example.demo.entity.IchRegion;
import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.mapper.IchCategoryMapper;
import com.example.demo.mapper.IchRegionMapper;
import com.example.demo.service.IchInheritorService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RegionCategoryController.class)
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000"
})
class RegionCategoryWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private IchProjectService projectService;

    @MockBean
    private IchInheritorService inheritorService;

    @MockBean
    private IchCategoryMapper categoryMapper;

    @MockBean
    private IchRegionMapper regionMapper;

    @Test
    void allEndpointReturnsBootstrapData() throws Exception {
        IchRegion province = new IchRegion();
        province.setId(1L);
        province.setName("浙江省");
        province.setLevel(1);

        IchCategory rootCategory = new IchCategory();
        rootCategory.setId(8L);
        rootCategory.setName("传统技艺");
        rootCategory.setLevel(1);

        IchProject project = new IchProject();
        project.setId(101L);
        project.setCategoryId(8L);
        project.setRegionId(1L);

        when(regionMapper.selectList(any())).thenReturn(Collections.singletonList(province));
        when(categoryMapper.selectList(any())).thenReturn(Collections.singletonList(rootCategory));
        when(projectService.list(any())).thenReturn(Collections.singletonList(project));
        when(inheritorService.count(any())).thenReturn(3L);

        mockMvc.perform(get("/api/region-category/all").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.provinces[0].name").value("浙江省"))
                .andExpect(jsonPath("$.data.categoryTree[0].name").value("传统技艺"))
                .andExpect(jsonPath("$.data.regionStatistics.totalProjects").value(1))
                .andExpect(jsonPath("$.data.categoryStatistics.regionCount").value(1));
    }

    @Test
    void projectsEndpointReturnsPagedRecords() throws Exception {
        IchProject project = new IchProject();
        project.setId(201L);
        project.setName("龙泉青瓷烧制技艺");
        project.setCategoryName("传统技艺");
        project.setRegionName("浙江省");

        Page<IchProject> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(project));
        page.setTotal(1);

        when(projectService.pageWithViewCount(any(Page.class), any())).thenReturn(page);
        doNothing().when(projectService).populateProjectRelations(any());

        mockMvc.perform(get("/api/region-category/projects")
                        .param("page", "1")
                        .param("limit", "10")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].name").value("龙泉青瓷烧制技艺"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    private String bearerToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("username", "admin");
        claims.put("role", "admin");
        return "Bearer " + jwtUtil.generateToken(claims, "admin");
    }
}
