package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_plan_favorite")
public class LearningPlanFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String planId;

    private String planTitle;

    private String trackId;

    private LocalDateTime createdAt;
}
