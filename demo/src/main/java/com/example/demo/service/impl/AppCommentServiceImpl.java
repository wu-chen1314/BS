package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.AppComment;
import com.example.demo.mapper.AppCommentMapper;
import com.example.demo.service.AppCommentService;
import org.springframework.stereotype.Service;

@Service
public class AppCommentServiceImpl extends ServiceImpl<AppCommentMapper, AppComment> implements AppCommentService {
}