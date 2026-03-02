package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.ChatSession;
import com.example.demo.mapper.ChatSessionMapper;
import com.example.demo.service.ChatSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话 Service 实现
 */
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> 
        implements ChatSessionService {

    @Override
    public List<ChatSession> getUserSessions(Long userId, int page, int limit) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdatedAt)
                .last("LIMIT " + limit + " OFFSET " + ((page - 1) * limit));
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public ChatSession createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title != null && !title.isEmpty() ? title : "新会话");
        session.setMessageCount(0);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        
        this.save(session);
        return session;
    }

    @Override
    @Transactional
    public boolean deleteSession(Long sessionId, Long userId) {
        // 验证会话是否属于当前用户
        ChatSession session = this.getById(sessionId);
        if (session == null) {
            return false;
        }
        
        // 权限验证：只能删除自己的会话
        if (!session.getUserId().equals(userId)) {
            throw new SecurityException("无权删除其他用户的会话");
        }
        
        return this.removeById(sessionId);
    }

    @Override
    @Transactional
    public void updateLastMessage(Long sessionId, String lastMessage) {
        ChatSession session = this.getById(sessionId);
        if (session != null) {
            // 截断消息内容避免过长
            String truncatedMessage = lastMessage != null && lastMessage.length() > 200 
                    ? lastMessage.substring(0, 200) + "..." 
                    : lastMessage;
            
            session.setLastMessage(truncatedMessage);
            session.setMessageCount(session.getMessageCount() + 1);
            session.setUpdatedAt(LocalDateTime.now());
            this.updateById(session);
        }
    }
}
