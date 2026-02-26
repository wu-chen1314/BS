package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论表实体类
 */
@Data
@TableName("app_comment")
public class AppComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long userId;

    private String content;

    private Long parentId; // 父评论ID (回复功能用)

    private Integer status; // 1:正常 0:屏蔽

    private LocalDateTime createdAt;
}