package com.example.demo.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.demo.service.DeepSeekChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeepSeekChatServiceImpl implements DeepSeekChatService {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String CONFIG_MISSING_MESSAGE = "AI 服务未配置，请先设置 DEEPSEEK_API_KEY";
    private static final String FAILURE_MESSAGE = "AI 服务暂时不可用，请稍后重试";

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Override
    public String generateReply(String userMessage) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(CONFIG_MISSING_MESSAGE);
        }

        try {
            JSONObject body = new JSONObject();
            body.set("model", "deepseek-chat");
            body.set("temperature", 0.7);

            JSONArray messages = new JSONArray();
            messages.add(new JSONObject()
                    .set("role", "system")
                    .set("content", "你是一位专业的非物质文化遗产科普助手，请用准确、清晰、友好的语言回答用户关于非遗的问题。"));
            messages.add(new JSONObject().set("role", "user").set("content", userMessage));
            body.set("messages", messages);

            HttpResponse response = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(20000)
                    .execute();

            if (response.getStatus() == 401 || response.getStatus() == 403) {
                throw new IllegalStateException(CONFIG_MISSING_MESSAGE);
            }

            if (response.getStatus() != 200) {
                throw new IllegalStateException(FAILURE_MESSAGE);
            }

            JSONObject json = JSONUtil.parseObj(response.body());
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException(FAILURE_MESSAGE);
            }

            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message == null ? null : message.getStr("content");
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException(FAILURE_MESSAGE);
            }

            return content;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(FAILURE_MESSAGE, exception);
        }
    }
}
