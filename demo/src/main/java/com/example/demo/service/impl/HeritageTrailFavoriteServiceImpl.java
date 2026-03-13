package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.HeritageTrailFavorite;
import com.example.demo.mapper.HeritageTrailFavoriteMapper;
import com.example.demo.service.HeritageTrailFavoriteService;
import org.springframework.stereotype.Service;

@Service
public class HeritageTrailFavoriteServiceImpl
        extends ServiceImpl<HeritageTrailFavoriteMapper, HeritageTrailFavorite>
        implements HeritageTrailFavoriteService {
}
