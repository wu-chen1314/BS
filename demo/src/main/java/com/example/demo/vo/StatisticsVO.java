package com.example.demo.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class StatisticsVO implements Serializable {
    private long totalProjects;
    private long categoryCount;
    private long regionCount;
    private long inheritorCount;
}
