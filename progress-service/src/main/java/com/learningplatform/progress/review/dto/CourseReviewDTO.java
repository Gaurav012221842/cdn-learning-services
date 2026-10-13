package com.learningplatform.progress.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseReviewDTO {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String avatarUrl;
    private UUID courseId;
    private Integer rating;
    private String comment;
    private ZonedDateTime createdAt;
}
