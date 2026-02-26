package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.AppFavorite;
import com.example.demo.mapper.AppFavoriteMapper;
import com.example.demo.service.AppFavoriteService;
import org.springframework.stereotype.Service;

@Service
public class AppFavoriteServiceImpl extends ServiceImpl<AppFavoriteMapper, AppFavorite> implements AppFavoriteService {
}