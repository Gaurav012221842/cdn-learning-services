package com.learningplatform.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDTO {
    private UUID id;
    private UUID chapterId;
    private String title;
    private String description;
    private String lessonType; // VIDEO, IMAGE, PDF, QUIZ
    private String contentUrl;
    private String videoThumbnailUrl;
    private String quizData;
    private Integer sequenceOrder;
}
