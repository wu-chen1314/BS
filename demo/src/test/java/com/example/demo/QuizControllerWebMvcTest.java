package com.example.demo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.config.WebConfig;
import com.example.demo.controller.QuizController;
import com.example.demo.entity.QuizAttempt;
import com.example.demo.entity.QuizQuestion;
import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.service.QuizAttemptService;
import com.example.demo.service.QuizQuestionService;
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

@WebMvcTest(controllers = QuizController.class)
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000"
})
class QuizControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private QuizQuestionService quizQuestionService;

    @MockBean
    private QuizAttemptService quizAttemptService;

    @Test
    void questionListRequiresToken() throws Exception {
        mockMvc.perform(get("/api/quiz/questions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void questionListReturnsQuestions() throws Exception {
        QuizQuestion question = new QuizQuestion();
        question.setId(2L);
        question.setQuestionText("中国古琴通常有几根弦？");
        question.setOptionsJson("[\"五根\",\"六根\",\"七根\",\"八根\"]");
        question.setCorrectIndex(2);
        question.setExplanation("古琴也称七弦琴。");
        question.setCategory("器乐");
        question.setDifficulty("easy");
        question.setActive(true);

        when(quizQuestionService.list(any())).thenReturn(Arrays.asList(question));

        mockMvc.perform(get("/api/quiz/questions")
                        .header("Authorization", bearerToken(11L, "quiz_user", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].question").value("中国古琴通常有几根弦？"))
                .andExpect(jsonPath("$.data[0].options[2]").value("七根"));
    }

    @Test
    void adminCanManageQuestionBank() throws Exception {
        QuizQuestion existing = new QuizQuestion();
        existing.setId(7L);
        existing.setQuestionText("活态传承更强调什么？");
        existing.setOptionsJson("[\"静态展示\",\"现实生活中的持续实践\",\"仅限节庆活动\",\"只存档\"]");
        existing.setCorrectIndex(1);
        existing.setExplanation("活态传承强调在现实场景中持续实践。");
        existing.setCategory("保护理念");
        existing.setDifficulty("medium");
        existing.setSortOrder(6);
        existing.setActive(true);
        existing.setCreatedAt(LocalDateTime.of(2026, 3, 13, 11, 0));
        existing.setUpdatedAt(LocalDateTime.of(2026, 3, 13, 11, 0));

        Page<QuizQuestion> page = new Page<>(1, 8);
        page.setTotal(1);
        page.setRecords(Collections.singletonList(existing));

        when(quizQuestionService.page(any(Page.class), any())).thenReturn(page);
        when(quizQuestionService.save(any(QuizQuestion.class))).thenAnswer(invocation -> {
            QuizQuestion payload = invocation.getArgument(0);
            payload.setId(8L);
            return true;
        });
        when(quizQuestionService.getById(7L)).thenReturn(existing);
        when(quizQuestionService.updateById(any(QuizQuestion.class))).thenReturn(true);
        when(quizQuestionService.removeById(7L)).thenReturn(true);

        mockMvc.perform(get("/api/quiz/questions/manage")
                        .header("Authorization", bearerToken(1L, "admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].question").value("活态传承更强调什么？"));

        mockMvc.perform(post("/api/quiz/questions/manage")
                        .header("Authorization", bearerToken(1L, "admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"新题目\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctAnswer\":2,\"explanation\":\"解析\",\"category\":\"综合\",\"difficulty\":\"easy\",\"sortOrder\":9,\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(8));

        mockMvc.perform(put("/api/quiz/questions/manage/7")
                        .header("Authorization", bearerToken(1L, "admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"更新后的题目\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctAnswer\":1,\"explanation\":\"新解析\",\"category\":\"综合\",\"difficulty\":\"medium\",\"sortOrder\":7,\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(7));

        mockMvc.perform(delete("/api/quiz/questions/manage/7")
                        .header("Authorization", bearerToken(1L, "admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void submitAttemptPersistsServerSideScore() throws Exception {
        QuizQuestion question = new QuizQuestion();
        question.setId(7L);
        question.setQuestionText("活态传承更强调什么？");
        question.setCorrectIndex(1);
        question.setExplanation("强调在现实生活中持续实践。");
        question.setCategory("保护理念");

        when(quizQuestionService.listByIds(any())).thenReturn(Arrays.asList(question));
        when(quizAttemptService.save(any(QuizAttempt.class))).thenReturn(true);

        mockMvc.perform(post("/api/quiz/attempts")
                        .header("Authorization", bearerToken(11L, "quiz_user", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationSeconds\":42,\"answers\":[{\"questionId\":7,\"selectedIndex\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.correctCount").value(1));

        ArgumentCaptor<QuizAttempt> captor = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(quizAttemptService).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(11L);
        assertThat(captor.getValue().getDurationSeconds()).isEqualTo(42);
    }

    @Test
    void summaryReturnsUserMetrics() throws Exception {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(9L);
        attempt.setUserId(4L);
        attempt.setScore(80);
        attempt.setCorrectCount(4);
        attempt.setTotalQuestions(5);
        attempt.setCreatedAt(LocalDateTime.of(2026, 3, 13, 11, 0));

        Page<QuizAttempt> page = new Page<>(1, 6);
        page.setRecords(Collections.singletonList(attempt));

        when(quizAttemptService.list(any())).thenReturn(Collections.singletonList(attempt));
        when(quizAttemptService.page(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/quiz/summary")
                        .header("Authorization", bearerToken(4L, "quiz_user", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bestScore").value(80))
                .andExpect(jsonPath("$.data.attemptCount").value(1));
    }

    private String bearerToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return "Bearer " + jwtUtil.generateToken(claims, username);
    }
}
