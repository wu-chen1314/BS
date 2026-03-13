package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.HeritageTrailAnalytics;
import com.example.demo.mapper.HeritageTrailAnalyticsMapper;
import com.example.demo.service.HeritageTrailAnalyticsService;
import org.springframework.stereotype.Service;

@Service
public class HeritageTrailAnalyticsServiceImpl
        extends ServiceImpl<HeritageTrailAnalyticsMapper, HeritageTrailAnalytics>
        implements HeritageTrailAnalyticsService {
}
