package com.learningplatform.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    private UUID id;
    private String title;
    private String description;
    private UUID instructorId;
    private BigDecimal price;
    private String status;
    private String category;
    private String thumbnailUrl;
    private ZonedDateTime createdAt;
    @Builder.Default
    private List<ChapterDTO> chapters = new ArrayList<>();
}
