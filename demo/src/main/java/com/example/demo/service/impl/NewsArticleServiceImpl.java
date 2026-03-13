package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.NewsArticle;
import com.example.demo.mapper.NewsArticleMapper;
import com.example.demo.service.NewsArticleService;
import org.springframework.stereotype.Service;

@Service
public class NewsArticleServiceImpl extends ServiceImpl<NewsArticleMapper, NewsArticle> implements NewsArticleService {
}
