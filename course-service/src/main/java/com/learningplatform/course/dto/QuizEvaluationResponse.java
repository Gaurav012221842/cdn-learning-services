package com.learningplatform.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizEvaluationResponse {
    private String title;
    private int totalQuestions;
    private int correctCount;
    private int scorePercentage;
    private int passingScore;
    private int strikes;
    private boolean isDisqualified;

    @JsonProperty("isPassed")
    private boolean isPassed;

    @JsonProperty("passed")
    public boolean getPassed() {
        return isPassed;
    }

    private List<QuestionResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResult {
        private int questionIndex;
        private String question;
        private List<String> options;
        private Integer selectedOption;
        private int correctIndex;

        @JsonProperty("isCorrect")
        private boolean isCorrect;

        @JsonProperty("correct")
        public boolean getCorrect() {
            return isCorrect;
        }

        private String explanation;
    }
}

