package com.learningplatform.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmissionRequest {
    private UUID lessonId;
    private Map<String, Integer> answers;
    private Integer strikes;
    private Boolean isDisqualified;
}
