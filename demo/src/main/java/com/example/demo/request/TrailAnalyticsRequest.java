package com.example.demo.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class TrailAnalyticsRequest {

    private String trailId;

    private String trailTitle;

    private String routeType;

    private String actionType;

    private String transportMode;

    private String budgetLevel;

    private String durationKey;

    private List<String> interests;

    private Integer stopCount;

    private BigDecimal estimatedHours;

    private BigDecimal estimatedCost;

    private Map<String, Object> payload;
}
