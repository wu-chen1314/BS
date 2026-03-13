package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.NewsReadLog;
import com.example.demo.mapper.NewsReadLogMapper;
import com.example.demo.service.NewsReadLogService;
import org.springframework.stereotype.Service;

@Service
public class NewsReadLogServiceImpl extends ServiceImpl<NewsReadLogMapper, NewsReadLog> implements NewsReadLogService {
}
