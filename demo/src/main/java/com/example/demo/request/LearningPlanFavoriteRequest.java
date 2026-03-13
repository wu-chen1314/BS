package com.example.demo.request;

import lombok.Data;

@Data
public class LearningPlanFavoriteRequest {

    private String planId;

    private String planTitle;

    private String trackId;
}
