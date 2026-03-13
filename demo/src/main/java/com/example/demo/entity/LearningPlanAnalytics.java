package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_plan_analytics")
public class LearningPlanAnalytics {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String planId;

    private String planTitle;

    private String trackId;

    private String actionType;

    private String audienceTag;

    private String durationLabel;

    private String linkedThemeId;

    private String regionKeyword;

    private Integer projectCount;

    private String keywordTags;

    private String payloadJson;

    private LocalDateTime createdAt;
}
