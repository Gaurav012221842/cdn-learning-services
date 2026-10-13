package com.learningplatform.course.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CreateCourseRequest {
    private String title;
    private String description;
    private String category;
    private UUID instructorId;
    private BigDecimal price;
    private String thumbnailUrl;
    private List<ChapterDTO> chapters = new ArrayList<>();
}
