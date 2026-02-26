package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.AiChatHistory;
import com.example.demo.mapper.AiChatHistoryMapper;
import com.example.demo.service.AiChatHistoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiChatHistoryServiceImpl extends ServiceImpl<AiChatHistoryMapper, AiChatHistory> implements AiChatHistoryService {

    @Override
    public boolean saveRecord(Long userId, String question, String answer) {
        // 1. 创建对象
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setQuestion(question);
        history.setAnswer(answer);

        // 2. 设置时间 (如果数据库字段没有设置 DEFAULT CURRENT_TIMESTAMP，这里需要手动设)
        history.setCreatedAt(LocalDateTime.now());

        // 3. 调用 MyBatis-Plus 的保存方法
        return this.save(history);
    }
}