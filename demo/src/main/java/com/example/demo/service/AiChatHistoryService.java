package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.AiChatHistory;

import java.util.List;

public interface AiChatHistoryService extends IService<AiChatHistory> {

    /**
     * 便捷保存聊天记录
     */
    boolean saveRecord(Long userId, Long chatId, String question, String answer);

    /**
     * 查询用户最近的 AI 对话历史
     */
    List<AiChatHistory> getRecentHistory(Long userId, Integer limit);

    /**
     * 根据聊天会话ID查询历史
     */
    List<AiChatHistory> getHistoryByChatId(Long userId, Long chatId, Integer limit);
}
