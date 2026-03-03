package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.AiChatHistory;
import com.example.demo.mapper.AiChatHistoryMapper;
import com.example.demo.service.AiChatHistoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiChatHistoryServiceImpl extends ServiceImpl<AiChatHistoryMapper, AiChatHistory>
        implements AiChatHistoryService {

    @Override
    public boolean saveRecord(Long userId, Long chatId, String question, String answer) {
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setChatId(chatId);
        history.setQuestion(question);
        history.setAnswer(answer);
        history.setCreatedAt(LocalDateTime.now());
        return this.save(history);
    }

    @Override
    public List<AiChatHistory> getRecentHistory(Long userId, Integer limit) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId)
                .orderByDesc(AiChatHistory::getCreatedAt)
                .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    @Override
    public List<AiChatHistory> getHistoryByChatId(Long userId, Long chatId, Integer limit) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId);
        if (chatId != null) {
            wrapper.eq(AiChatHistory::getChatId, chatId);
        }
        wrapper.orderByDesc(AiChatHistory::getCreatedAt)
                .last("LIMIT " + limit);
        return this.list(wrapper);
    }
}
