package com.example.demo.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.demo.common.Result;
import com.example.demo.service.AiChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private AiChatHistoryService aiChatHistoryService;

    // ✨✨ 请将你的 DeepSeek API Key 填在这里，或者写在 application.yml 里
    // 格式通常是 "sk-xxxxxxxxxxxxxxxx"
    @Value("${deepseek.api.key:sk-ea1c953852ff41498f117594e99b9507}")
    private String apiKey;

    // DeepSeek 的官方接口地址
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    /**
     * 发送消息接口
     * POST /api/chat/send
     */
    @PostMapping("/send")
    public Result<String> send(@RequestBody Map<String, Object> params) {
        String message = (String) params.get("message");
        // 注意：前端传过来的 userId 可能是 Integer 也可能是 Long，转一下比较稳
        Long userId = Long.valueOf(params.get("userId").toString());

        if (message == null || message.trim().isEmpty()) {
            return Result.error("请输入内容");
        }

        // 1. 调用 DeepSeek API 获取回复
        String aiReply = callDeepSeekApi(message);

        // 2. ✨✨ 保存到数据库 (使用刚才写的 Service)
        aiChatHistoryService.saveRecord(userId, message, aiReply);

        // 3. 返回给前端
        return Result.success(aiReply);
    }

    /**
     * 私有方法：封装 DeepSeek 的 HTTP 调用逻辑
     */
    private String callDeepSeekApi(String userMessage) {
        try {
            // A. 构建请求体 (DeepSeek 要求 OpenAI 兼容格式)
            JSONObject body = new JSONObject();
            body.set("model", "deepseek-chat"); // 模型名称
            body.set("temperature", 0.7);       // 温度（创造性）

            // 消息列表
            JSONArray messages = new JSONArray();
            // 系统预设 (人设)
            messages.add(new JSONObject().set("role", "system").set("content", "你是一个专业的非物质文化遗产科普助手，请用生动有趣的语言回答用户关于非遗的问题。"));
            // 用户提问
            messages.add(new JSONObject().set("role", "user").set("content", userMessage));

            body.set("messages", messages);

            // B. 发送 POST 请求
            System.out.println("正在请求 DeepSeek...");
            HttpResponse response = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + apiKey) // 鉴权头
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(20000) // 设置超时时间 20秒
                    .execute();

            // C. 解析响应
            String resultStr = response.body();
            System.out.println("DeepSeek 响应: " + resultStr);

            if (response.getStatus() != 200) {
                return "AI 响应异常: " + response.getStatus();
            }

            // D. 提取回复内容
            JSONObject json = JSONUtil.parseObj(resultStr);
            // 路径: choices[0].message.content
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