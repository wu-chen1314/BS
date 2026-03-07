package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.AiChatHistory;
import com.example.demo.entity.ChatSession;
import com.example.demo.mapper.AiChatHistoryMapper;
import com.example.demo.mapper.ChatSessionMapper;
import com.example.demo.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>
        implements ChatSessionService {

    @Autowired
    private AiChatHistoryMapper aiChatHistoryMapper;

    @Override
    public List<ChatSession> getUserSessions(Long userId, int page, int limit) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdatedAt)
                .last("LIMIT " + limit + " OFFSET " + ((page - 1) * limit));
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSession createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title != null && !title.isEmpty() ? title : "New Chat");
        session.setMessageCount(0);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        this.save(session);
        return session;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSession(Long sessionId, Long userId) {
        ChatSession session = this.getById(sessionId);
        if (session == null) {
            return false;
        }
        if (!session.getUserId().equals(userId)) {
            throw new SecurityException("No permission to delete another user's chat session");
        }

        aiChatHistoryMapper.delete(new LambdaQueryWrapper<AiChatHistory>()
                .eq(AiChatHistory::getChatId, sessionId)
                .eq(AiChatHistory::getUserId, userId));
        return this.removeById(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastMessage(Long sessionId, String lastMessage) {
        ChatSession session = this.getById(sessionId);
        if (session == null) {
            return;
        }

        String truncatedMessage = lastMessage != null && lastMessage.length() > 200
                ? lastMessage.substring(0, 200) + "..."
                : lastMessage;
        session.setLastMessage(truncatedMessage);
        session.setMessageCount(session.getMessageCount() + 1);
        session.setUpdatedAt(LocalDateTime.now());
        this.updateById(session);
    }
}
