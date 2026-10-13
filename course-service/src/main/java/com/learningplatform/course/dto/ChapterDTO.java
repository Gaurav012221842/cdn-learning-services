package com.learningplatform.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterDTO {
    private UUID id;
    private String title;
    private Integer sequenceOrder;
    @Builder.Default
    private List<LessonDTO> lessons = new ArrayList<>();
}
