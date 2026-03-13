package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.LearningPlanAnalytics;
import com.example.demo.mapper.LearningPlanAnalyticsMapper;
import com.example.demo.service.LearningPlanAnalyticsService;
import org.springframework.stereotype.Service;

@Service
public class LearningPlanAnalyticsServiceImpl
        extends ServiceImpl<LearningPlanAnalyticsMapper, LearningPlanAnalytics>
        implements LearningPlanAnalyticsService {
}
