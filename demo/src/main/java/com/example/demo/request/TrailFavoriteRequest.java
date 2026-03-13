package com.example.demo.request;

import lombok.Data;

@Data
public class TrailFavoriteRequest {

    private String trailId;

    private String trailTitle;

    private String routeType;
}
