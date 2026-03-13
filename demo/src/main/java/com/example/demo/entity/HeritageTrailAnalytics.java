package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("heritage_trail_analytics")
public class HeritageTrailAnalytics {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String trailId;

    private String trailTitle;

    private String routeType;

    private String actionType;

    private String transportMode;

    private String budgetLevel;

    private String durationKey;

    private String interestTags;

    private Integer stopCount;

    private BigDecimal estimatedHours;

    private BigDecimal estimatedCost;

    private String payloadJson;

    private LocalDateTime createdAt;
}
