package com.example.demo.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.demo.common.result.Result;
import com.example.demo.entity.AiChatHistory;
import com.example.demo.entity.ChatSession;
import com.example.demo.service.AiChatHistoryService;
import com.example.demo.service.ChatSessionService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private AiChatHistoryService aiChatHistoryService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Value("${deepseek.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    @PostMapping("/send")
    public CompletableFuture<Result<Map<String, Object>>> send(@RequestBody Map<String, Object> params,
                                                               HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String message = (String) params.get("message");
            Long userId = resolveUserId(request, params.get("userId"));
            if (userId == null) {
                return Result.error("Please log in first");
            }
            if (message == null || message.trim().isEmpty()) {
                return Result.error("Message cannot be empty");
            }

            Object chatIdObj = params.get("chatId");
            String title = (String) params.get("title");
            Long chatId;
            ChatSession session;

            if (chatIdObj != null) {
                chatId = Long.valueOf(chatIdObj.toString());
                session = chatSessionService.getById(chatId);
                if (session == null || !session.getUserId().equals(userId)) {
                    return Result.error("Chat session was not found or access was denied");
                }
            } else {
                session = chatSessionService.createSession(userId, title);
                chatId = session.getId();
            }

            String aiReply = callDeepSeekApi(message);
            aiChatHistoryService.saveRecord(userId, chatId, message, aiReply);
            chatSessionService.updateLastMessage(chatId, message + " | " + aiReply);

            Map<String, Object> result = new HashMap<>();
            result.put("reply", aiReply);
            result.put("chatId", chatId);
            result.put("sessionTitle", session.getTitle());
            return Result.success(result);
        });
    }

    @GetMapping("/sessions")
    public Result<List<ChatSession>> getSessions(@RequestParam(required = false) Long userId,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer limit,
                                                 HttpServletRequest request) {
        Long effectiveUserId = resolveUserId(request, userId);
        if (effectiveUserId == null) {
            return Result.error("Please log in first");
        }

        if (page < 1) {
            page = 1;
        }
        if (limit < 1 || limit > 100) {
            limit = 10;
        }

        List<ChatSession> sessions = chatSessionService.getUserSessions(effectiveUserId, page, limit);
        return Result.success(sessions);
    }

    @DeleteMapping("/sessions/{id}")
    public Result<String> deleteSession(@PathVariable Long id,
                                        @RequestParam(required = false) Long userId,
                                        HttpServletRequest request) {
        Long effectiveUserId = resolveUserId(request, userId);
        if (effectiveUserId == null) {
            return Result.error("Please log in first");
        }
        if (id == null) {
            return Result.error("Chat session ID is required");
        }

        try {
            boolean deleted = chatSessionService.deleteSession(id, effectiveUserId);
            if (deleted) {
                return Result.success(null);
            }
            return Result.error("Chat session was not found");
        } catch (SecurityException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/sessions")
    public Result<ChatSession> createSession(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long effectiveUserId = resolveUserId(request, params.get("userId"));
        if (effectiveUserId == null) {
            return Result.error("Please log in first");
        }

        String title = (String) params.get("title");
        ChatSession session = chatSessionService.createSession(effectiveUserId, title);
        return Result.success(session);
    }

    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(@RequestParam(required = false) Long userId,
                                                     @RequestParam(required = false) Long chatId,
                                                     @RequestParam(defaultValue = "20") Integer limit,
                                                     HttpServletRequest request) {
        Long effectiveUserId = resolveUserId(request, userId);
        if (effectiveUserId == null) {
            return Result.error("Please log in first");
        }

        List<AiChatHistory> list;
        if (chatId != null) {
            list = aiChatHistoryService.getHistoryByChatId(effectiveUserId, chatId, limit);
        } else {
            list = aiChatHistoryService.getRecentHistory(effectiveUserId, limit);
        }

        Collections.reverse(list);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (AiChatHistory history : list) {
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", history.getQuestion());
            userMsg.put("timestamp", history.getCreatedAt());
            messages.add(userMsg);

            Map<String, Object> aiMsg = new HashMap<>();
            aiMsg.put("role", "assistant");
            aiMsg.put("content", history.getAnswer());
            aiMsg.put("timestamp", history.getCreatedAt());
            messages.add(aiMsg);
        }

        return Result.success(messages);
    }

    private Long resolveUserId(HttpServletRequest request, Object requestedUserId) {
        Long currentUserId = RequestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return null;
        }
        if (requestedUserId == null) {
            return currentUserId;
        }

        Long targetUserId = Long.valueOf(String.valueOf(requestedUserId));
        if (!RequestAuthUtil.isSelfOrAdmin(request, targetUserId)) {
            return null;
        }
        return targetUserId;
    }

    private String callDeepSeekApi(String userMessage) {
        try {
            JSONObject body = new JSONObject();
            body.set("model", "deepseek-chat");
            body.set("temperature", 0.7);

            JSONArray messages = new JSONArray();
            messages.add(new JSONObject()
                    .set("role", "system")
                    .set("content", "You are a professional assistant for intangible cultural heritage topics. Answer clearly and helpfully."));
            messages.add(new JSONObject().set("role", "user").set("content", userMessage));
            body.set("messages", messages);

            HttpResponse response = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(20000)
                    .execute();

            if (response.getStatus() != 200) {
                return "AI response error: " + response.getStatus();
            }

            JSONObject json = JSONUtil.parseObj(response.body());
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");
        } catch (Exception e) {
            e.printStackTrace();
            return "The AI service is temporarily unavailable. Please try again later.";
        }
    }
}