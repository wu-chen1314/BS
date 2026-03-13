package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.QuizAttempt;
import com.example.demo.mapper.QuizAttemptMapper;
import com.example.demo.service.QuizAttemptService;
import org.springframework.stereotype.Service;

@Service
public class QuizAttemptServiceImpl extends ServiceImpl<QuizAttemptMapper, QuizAttempt> implements QuizAttemptService {
}
