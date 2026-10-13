package com.learningplatform.course.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CreateLessonRequest {
    private UUID courseId;
    private String title;
    private String content;
    private Integer sequenceOrder;
    private UUID mediaId;
}
