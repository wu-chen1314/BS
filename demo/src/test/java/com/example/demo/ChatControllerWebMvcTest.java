package com.example.demo;

import com.example.demo.config.WebConfig;
import com.example.demo.controller.ChatController;
import com.example.demo.entity.ChatSession;
import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.service.AiChatHistoryService;
import com.example.demo.service.ChatSessionService;
import com.example.demo.service.DeepSeekChatService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class)
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000"
})
class ChatControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private AiChatHistoryService aiChatHistoryService;

    @MockBean
    private ChatSessionService chatSessionService;

    @MockBean
    private DeepSeekChatService deepSeekChatService;

    @Test
    void sendReturnsUnifiedFailureWhenAiServiceFails() throws Exception {
        when(deepSeekChatService.generateReply("你好")).thenThrow(new IllegalStateException("AI 服务暂时不可用，请稍后重试"));

        mockMvc.perform(post("/api/chat/send")
                        .header("Authorization", bearerToken(3L, "reader", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\",\"title\":\"非遗问答\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("AI 服务暂时不可用，请稍后重试"));

        verify(chatSessionService, never()).createSession(eq(3L), anyString());
        verify(aiChatHistoryService, never()).saveRecord(eq(3L), eq(0L), anyString(), anyString());
        verify(chatSessionService, never()).updateLastMessage(eq(0L), anyString());
    }

    @Test
    void sendCreatesSessionOnlyAfterAiReplySucceeds() throws Exception {
        ChatSession session = new ChatSession();
        session.setId(21L);
        session.setTitle("非遗问答");

        when(deepSeekChatService.generateReply("龙泉青瓷有什么特点？")).thenReturn("龙泉青瓷以釉色温润和烧制技艺见长。");
        when(chatSessionService.createSession(3L, "非遗问答")).thenReturn(session);
        when(aiChatHistoryService.saveRecord(3L, 21L, "龙泉青瓷有什么特点？", "龙泉青瓷以釉色温润和烧制技艺见长。"))
                .thenReturn(true);

        mockMvc.perform(post("/api/chat/send")
                        .header("Authorization", bearerToken(3L, "reader", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"龙泉青瓷有什么特点？\",\"title\":\"非遗问答\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.chatId").value(21))
                .andExpect(jsonPath("$.data.reply").value("龙泉青瓷以釉色温润和烧制技艺见长。"));

        verify(chatSessionService).createSession(3L, "非遗问答");
        verify(aiChatHistoryService).saveRecord(3L, 21L, "龙泉青瓷有什么特点？", "龙泉青瓷以釉色温润和烧制技艺见长。");

        ArgumentCaptor<String> lastMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatSessionService).updateLastMessage(eq(21L), lastMessageCaptor.capture());
        assertThat(lastMessageCaptor.getValue()).contains("龙泉青瓷有什么特点？");
    }

    private String bearerToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return "Bearer " + jwtUtil.generateToken(claims, username);
    }
}
