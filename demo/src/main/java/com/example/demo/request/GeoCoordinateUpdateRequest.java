package com.example.demo.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GeoCoordinateUpdateRequest {
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private String contactPhone;
    private String openingHours;
}
