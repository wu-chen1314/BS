package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.ChatSession;
import java.util.List;

/**
 * 聊天会话 Service 接口
 */
public interface ChatSessionService extends IService<ChatSession> {
    
    /**
     * 获取用户的会话列表（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param limit 每页数量
     * @return 会话列表
     */
    List<ChatSession> getUserSessions(Long userId, int page, int limit);
    
    /**
     * 创建新会话
     * @param userId 用户ID
     * @param title 会话标题
     * @return 创建的会话
     */
    ChatSession createSession(Long userId, String title);
    
    /**
     * 删除会话（验证用户权限）
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteSession(Long sessionId, Long userId);
    
    /**
     * 更新会话的最后消息
     * @param sessionId 会话ID
     * @param lastMessage 最后一条消息
     */
    void updateLastMessage(Long sessionId, String lastMessage);
}
