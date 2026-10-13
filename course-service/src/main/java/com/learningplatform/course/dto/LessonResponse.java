package com.learningplatform.course.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class LessonResponse {
    private UUID id;
    private UUID courseId;
    private String title;
    private String content;
    private Integer sequenceOrder;
    private UUID mediaId;
}
