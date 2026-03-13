package com.example.demo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.config.WebConfig;
import com.example.demo.controller.NewsController;
import com.example.demo.entity.NewsArticle;
import com.example.demo.entity.NewsReadLog;
import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.service.NewsArticleService;
import com.example.demo.service.NewsReadLogService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NewsController.class)
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000"
})
class NewsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private NewsArticleService newsArticleService;

    @MockBean
    private NewsReadLogService newsReadLogService;

    @Test
    void articleListRequiresToken() throws Exception {
        mockMvc.perform(get("/api/news/articles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void articleListReturnsPagedPayload() throws Exception {
        NewsArticle article = new NewsArticle();
        article.setId(3L);
        article.setTitle("社区工坊成为城市更新中的非遗新场景");
        article.setSummary("摘要");
        article.setSource("城市文化观察");
        article.setTag("城市更新");
        article.setTagType("warning");
        article.setCoverImageUrl("https://example.com/cover.jpg");
        article.setVideoUrl("https://example.com/video.mp4");
        article.setPublishedAt(LocalDateTime.of(2026, 3, 13, 10, 0));
        article.setViewCount(12);

        Page<NewsArticle> page = new Page<>(1, 4);
        page.setTotal(1);
        page.setRecords(Arrays.asList(article));

        when(newsArticleService.page(any(Page.class), any())).thenReturn(page);
        when(newsArticleService.list(any())).thenReturn(Arrays.asList(article));

        mockMvc.perform(get("/api/news/articles")
                        .header("Authorization", bearerToken(7L, "reader", "user"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].title").value("社区工坊成为城市更新中的非遗新场景"))
                .andExpect(jsonPath("$.data.records[0].coverImageUrl").value("https://example.com/cover.jpg"))
                .andExpect(jsonPath("$.data.records[0].videoUrl").value("https://example.com/video.mp4"))
                .andExpect(jsonPath("$.data.availableTags[0]").value("城市更新"));
    }

    @Test
    void recordReadUsesCurrentUserAndReturnsUpdatedViewCount() throws Exception {
        NewsArticle article = new NewsArticle();
        article.setId(5L);
        article.setTitle("阅读测试");
        article.setViewCount(4);

        when(newsArticleService.getById(5L)).thenReturn(article);
        when(newsArticleService.updateById(any(NewsArticle.class))).thenReturn(true);
        when(newsReadLogService.save(any(NewsReadLog.class))).thenReturn(true);

        mockMvc.perform(post("/api/news/articles/5/read")
                        .header("Authorization", bearerToken(19L, "reader", "user"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.viewCount").value(5));

        ArgumentCaptor<NewsReadLog> captor = ArgumentCaptor.forClass(NewsReadLog.class);
        verify(newsReadLogService).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(19L);
        assertThat(captor.getValue().getArticleId()).isEqualTo(5L);
    }

    @Test
    void adminCanCreateUpdateAndDeleteArticle() throws Exception {
        NewsArticle created = new NewsArticle();
        created.setId(12L);
        created.setTitle("新建资讯");
        created.setSummary("摘要");
        created.setContent("正文内容");
        created.setSource("内容中心");
        created.setTag("活动");
        created.setTagType("success");
        created.setCoverImageUrl("/files/news/covers/created-cover.jpg");
        created.setVideoUrl("/files/news/videos/created-video.mp4");
        created.setPublishedAt(LocalDateTime.of(2026, 3, 13, 12, 0));
        created.setViewCount(0);

        when(newsArticleService.save(any(NewsArticle.class))).thenAnswer(invocation -> {
            NewsArticle article = invocation.getArgument(0);
            article.setId(12L);
            return true;
        });
        when(newsArticleService.getById(12L)).thenReturn(created);
        when(newsArticleService.updateById(any(NewsArticle.class))).thenReturn(true);
        when(newsArticleService.removeById(12L)).thenReturn(true);
        when(newsArticleService.list(any())).thenReturn(Collections.singletonList(created));

        mockMvc.perform(post("/api/news/articles")
                        .header("Authorization", bearerToken(1L, "admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"新建资讯\",\"content\":\"正文内容\",\"summary\":\"摘要\",\"source\":\"内容中心\",\"tag\":\"活动\",\"tagType\":\"success\",\"coverImageUrl\":\"/files/news/covers/created-cover.jpg\",\"videoUrl\":\"/files/news/videos/created-video.mp4\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("新建资讯"))
                .andExpect(jsonPath("$.data.coverImageUrl").value("/files/news/covers/created-cover.jpg"))
                .andExpect(jsonPath("$.data.videoUrl").value("/files/news/videos/created-video.mp4"));

        mockMvc.perform(put("/api/news/articles/12")
                        .header("Authorization", bearerToken(1L, "admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"更新后的资讯\",\"content\":\"更新后的正文\",\"tag\":\"活动\",\"coverImageUrl\":\"/files/news/covers/updated-cover.jpg\",\"videoUrl\":\"/files/news/videos/updated-video.mp4\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.coverImageUrl").value("/files/news/covers/updated-cover.jpg"))
                .andExpect(jsonPath("$.data.videoUrl").value("/files/news/videos/updated-video.mp4"));

        mockMvc.perform(delete("/api/news/articles/12")
                        .header("Authorization", bearerToken(1L, "admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void nonAdminCannotCreateArticle() throws Exception {
        mockMvc.perform(post("/api/news/articles")
                        .header("Authorization", bearerToken(8L, "reader", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"新建资讯\",\"content\":\"正文内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("只有管理员可以发布资讯"));
    }

    private String bearerToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return "Bearer " + jwtUtil.generateToken(claims, username);
    }
}
