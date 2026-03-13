package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quiz_attempt")
public class QuizAttempt {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer score;
    private Integer correctCount;
    private Integer totalQuestions;
    private Integer durationSeconds;
    private String answerDetailsJson;
    private LocalDateTime createdAt;
}
