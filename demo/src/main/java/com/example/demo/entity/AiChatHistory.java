package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 问答历史实体类
 * 对应数据库表: ai_chat_history
 */
@Data
@TableName("ai_chat_history")
public class AiChatHistory {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID (user_id)
     */
    private Long userId;

    /**
     * 用户提问 (question)
     */
    private String question;

    /**
     * AI回答 (answer)
     */
    private String answer;

    /**
     * 关联的项目ID (project_id) - 可选
     */
    private Long projectId;

    /**
     * 创建时间 (created_at)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}