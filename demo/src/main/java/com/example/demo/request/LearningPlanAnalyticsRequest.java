package com.example.demo.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LearningPlanAnalyticsRequest {

    private String planId;

    private String planTitle;

    private String trackId;

    private String actionType;

    private String audienceTag;

    private String durationLabel;

    private String linkedThemeId;

    private String regionKeyword;

    private Integer projectCount;

    private List<String> keywords;

    private Map<String, Object> payload;
}
