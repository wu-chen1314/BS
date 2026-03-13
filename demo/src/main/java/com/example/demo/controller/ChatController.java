package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.AiChatHistory;
import com.example.demo.entity.ChatSession;
import com.example.demo.service.AiChatHistoryService;
import com.example.demo.service.ChatSessionService;
import com.example.demo.service.DeepSeekChatService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private AiChatHistoryService aiChatHistoryService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private RequestAuthUtil requestAuthUtil;

    @Autowired
    private DeepSeekChatService deepSeekChatService;

    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("用户未登录，请先登录");
        }

        String message = params.get("message") == null ? null : params.get("message").toString();
        if (message == null || message.trim().isEmpty()) {
            return Result.error("请输入内容");
        }

        Long chatId = toLong(params.get("chatId"));
        String title = params.get("title") == null ? null : params.get("title").toString();
        ChatSession session = null;
        if (chatId != null) {
            session = chatSessionService.getById(chatId);
            if (session == null || !currentUserId.equals(session.getUserId())) {
                return Result.error("会话不存在或无权访问");
            }
        }

        final String aiReply;
        try {
            aiReply = deepSeekChatService.generateReply(message);
        } catch (IllegalStateException exception) {
            return Result.error(exception.getMessage());
        }

        if (chatId == null) {
            session = chatSessionService.createSession(currentUserId, title);
            chatId = session.getId();
        }

        aiChatHistoryService.saveRecord(currentUserId, chatId, message, aiReply);
        chatSessionService.updateLastMessage(chatId, message + " | " + aiReply);

        Map<String, Object> result = new HashMap<>();
        result.put("reply", aiReply);
        result.put("chatId", chatId);
        result.put("sessionTitle", session.getTitle());
        return Result.success(result);
    }

    @GetMapping("/sessions")
    public Result<List<ChatSession>> getSessions(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer limit,
                                                 HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("用户ID不能为空");
        }

        if (page < 1) {
            page = 1;
        }
        if (limit < 1 || limit > 100) {
            limit = 10;
        }

        return Result.success(chatSessionService.getUserSessions(currentUserId, page, limit));
    }

    @DeleteMapping("/sessions/{id}")
    public Result<String> deleteSession(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("用户ID不能为空");
        }
        if (id == null) {
            return Result.error("会话ID不能为空");
        }

        try {
            boolean deleted = chatSessionService.deleteSession(id, currentUserId);
            return deleted ? Result.success(null) : Result.error("会话不存在");
        } catch (SecurityException exception) {
            return Result.error(exception.getMessage());
        }
    }

    @PostMapping("/sessions")
    public Result<ChatSession> createSession(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("用户ID不能为空");
        }
        String title = params.get("title") == null ? null : params.get("title").toString();
        return Result.success(chatSessionService.createSession(currentUserId, title));
    }

    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(@RequestParam(required = false) Long chatId,
                                                     @RequestParam(defaultValue = "20") Integer limit,
                                                     HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("用户ID不能为空");
        }

        List<AiChatHistory> historyList;
        if (chatId != null) {
            historyList = aiChatHistoryService.getHistoryByChatId(currentUserId, chatId, limit);
        } else {
            historyList = aiChatHistoryService.getRecentHistory(currentUserId, limit);
        }

        Collections.reverse(historyList);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (AiChatHistory history : historyList) {
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", history.getQuestion());
            userMessage.put("timestamp", history.getCreatedAt());
            messages.add(userMessage);

            Map<String, Object> aiMessage = new HashMap<>();
            aiMessage.put("role", "assistant");
            aiMessage.put("content", history.getAnswer());
            aiMessage.put("timestamp", history.getCreatedAt());
            messages.add(aiMessage);
        }

        return Result.success(messages);
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
