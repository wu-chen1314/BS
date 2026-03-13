package com.example.demo.vo;

import com.example.demo.entity.IchCategory;
import com.example.demo.entity.IchRegion;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class RegionCategoryVO implements Serializable {
    private List<IchRegion> provinces;
    private List<IchCategory> categoryTree;
    private StatisticsVO regionStatistics;
    private StatisticsVO categoryStatistics;
}
