package com.example.demo.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.demo.common.Result;
import com.example.demo.entity.AiChatHistory;
import com.example.demo.entity.ChatSession;
import com.example.demo.service.AiChatHistoryService;
import com.example.demo.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

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

    @Value("${deepseek.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    /**
     * 发送消息接口（增强版：支持chatId参数）
     * POST /api/chat/send
     * 
     * 请求参数：
     * - message: 消息内容（必填）
     * - userId: 用户ID（必填）
     * - chatId: 会话ID（可选，不传则创建新会话）
     * - title: 会话标题（可选，仅在新会话时有效）
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestBody Map<String, Object> params) {
        String message = (String) params.get("message");
        Object userIdObj = params.get("userId");
        Object chatIdObj = params.get("chatId");
        String title = (String) params.get("title");

        if (userIdObj == null) {
            return Result.error("用户未登录，请先登录");
        }
        Long userId = Long.valueOf(userIdObj.toString());

        if (message == null || message.trim().isEmpty()) {
            return Result.error("请输入内容");
        }

        // 处理会话ID
        Long chatId = null;
        ChatSession session = null;

        if (chatIdObj != null) {
            // 使用已有会话
            chatId = Long.valueOf(chatIdObj.toString());
            session = chatSessionService.getById(chatId);
            if (session == null || !session.getUserId().equals(userId)) {
                return Result.error("会话不存在或无权访问");
            }
        } else {
            // 创建新会话
            session = chatSessionService.createSession(userId, title);
            chatId = session.getId();
        }

        // 1. 调用 DeepSeek API 获取回复
        String aiReply = callDeepSeekApi(message);

        // 2. 保存到数据库
        aiChatHistoryService.saveRecord(userId, chatId, message, aiReply);

        // 3. 更新会话的最后消息和时间
        chatSessionService.updateLastMessage(chatId, message + " | " + aiReply);

        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("reply", aiReply);
        result.put("chatId", chatId);
        result.put("sessionTitle", session.getTitle());

        return Result.success(result);
    }

    /**
     * 获取用户的聊天会话列表
     * GET /api/chat/sessions
     * 
     * 请求参数：
     * - userId: 用户ID（必填）
     * - page: 页码（默认1）
     * - limit: 每页数量（默认10）
     */
    @GetMapping("/sessions")
    public Result<List<ChatSession>> getSessions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {

        if (userId == null) {
            return Result.error("用户ID不能为空");
        }

        if (page < 1) {
            page = 1;
        }
        if (limit < 1 || limit > 100) {
            limit = 10;
        }

        List<ChatSession> sessions = chatSessionService.getUserSessions(userId, page, limit);
        return Result.success(sessions);
    }

    /**
     * 删除指定聊天会话
     * DELETE /api/chat/sessions/{id}
     * 
     * URL参数：
     * - id: 会话ID
     * 
     * 请求参数：
     * - userId: 用户ID（必填，用于权限验证）
     */
    @DeleteMapping("/sessions/{id}")
    public Result<String> deleteSession(
            @PathVariable Long id,
            @RequestParam Long userId) {

        if (userId == null) {
            return Result.error("用户ID不能为空");
        }

        if (id == null) {
            return Result.error("会话ID不能为空");
        }

        try {
            boolean deleted = chatSessionService.deleteSession(id, userId);
            if (deleted) {
                return Result.success(null);
            } else {
                return Result.error("会话不存在");
            }
        } catch (SecurityException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建新会话
     * POST /api/chat/sessions
     * 
     * 请求参数：
     * - userId: 用户ID（必填）
     * - title: 会话标题（可选，默认"新会话"）
     */
    @PostMapping("/sessions")
    public Result<ChatSession> createSession(@RequestBody Map<String, Object> params) {
        Object userIdObj = params.get("userId");
        String title = (String) params.get("title");

        if (userIdObj == null) {
            return Result.error("用户ID不能为空");
        }

        Long userId = Long.valueOf(userIdObj.toString());
        ChatSession session = chatSessionService.createSession(userId, title);
        return Result.success(session);
    }

    /**
     * 查询用户 AI 对话历史记录
     * GET /api/chat/history?userId=1&chatId=123&limit=20
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(
            @RequestParam Long userId,
            @RequestParam(required = false) Long chatId,
            @RequestParam(defaultValue = "20") Integer limit) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }

        List<AiChatHistory> list;
        if (chatId != null) {
            list = aiChatHistoryService.getHistoryByChatId(userId, chatId, limit);
        } else {
            list = aiChatHistoryService.getRecentHistory(userId, limit);
        }

        java.util.Collections.reverse(list);

        List<Map<String, Object>> messages = new java.util.ArrayList<>();
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

    /**
     * 私有方法：封装 DeepSeek 的 HTTP 调用逻辑
     */
    private String callDeepSeekApi(String userMessage) {
        try {
            JSONObject body = new JSONObject();
            body.set("model", "deepseek-chat");
            body.set("temperature", 0.7);

            JSONArray messages = new JSONArray();
            messages.add(
                    new JSONObject().set("role", "system").set("content", "你是一个专业的非物质文化遗产科普助手，请用生动有趣的语言回答用户关于非遗的问题。"));
            messages.add(new JSONObject().set("role", "user").set("content", userMessage));

            body.set("messages", messages);

            System.out.println("正在请求 DeepSeek...");
            HttpResponse response = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(20000)
                    .execute();

            String resultStr = response.body();
            System.out.println("DeepSeek 响应: " + resultStr);

            if (response.getStatus() != 200) {
                return "AI 响应异常: " + response.getStatus();
            }

            JSONObject json = JSONUtil.parseObj(resultStr);
            String content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");

            return content;

        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，AI 开小差了，请稍后再试。(网络请求失败)";
        }
    }
}
