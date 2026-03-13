package com.example.demo.request;

import lombok.Data;

import java.util.List;

@Data
public class QuizQuestionUpsertRequest {

    private String question;
    private List<String> options;
    private Integer correctAnswer;
    private String explanation;
    private String category;
    private String difficulty;
    private Integer sortOrder;
    private Boolean active;
}
