package com.learningplatform.course.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateCourseRequest {
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private String status;
    private String thumbnailUrl;
    private java.util.List<ChapterDTO> chapters;
}
