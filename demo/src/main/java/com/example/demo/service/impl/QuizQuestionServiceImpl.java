package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.QuizQuestion;
import com.example.demo.mapper.QuizQuestionMapper;
import com.example.demo.service.QuizQuestionService;
import org.springframework.stereotype.Service;

@Service
public class QuizQuestionServiceImpl extends ServiceImpl<QuizQuestionMapper, QuizQuestion> implements QuizQuestionService {
}
