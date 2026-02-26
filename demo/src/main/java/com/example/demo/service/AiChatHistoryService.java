package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.AiChatHistory;

public interface AiChatHistoryService extends IService<AiChatHistory> {

    /**
     * 便捷保存聊天记录
     * @param userId 用户ID
     * @param question 提问内容
     * @param answer AI回答内容
     * @return 是否保存成功
     */
    boolean saveRecord(Long userId, String question, String answer);
}