package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.LearningPlanFavorite;
import com.example.demo.mapper.LearningPlanFavoriteMapper;
import com.example.demo.service.LearningPlanFavoriteService;
import org.springframework.stereotype.Service;

@Service
public class LearningPlanFavoriteServiceImpl
        extends ServiceImpl<LearningPlanFavoriteMapper, LearningPlanFavorite>
        implements LearningPlanFavoriteService {
}
