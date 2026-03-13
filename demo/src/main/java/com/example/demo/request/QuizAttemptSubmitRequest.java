package com.example.demo.request;

import lombok.Data;

import java.util.List;

@Data
public class QuizAttemptSubmitRequest {

    private Integer durationSeconds;
    private List<QuizAnswerItem> answers;

    @Data
    public static class QuizAnswerItem {
        private Long questionId;
        private Integer selectedIndex;
    }
}
