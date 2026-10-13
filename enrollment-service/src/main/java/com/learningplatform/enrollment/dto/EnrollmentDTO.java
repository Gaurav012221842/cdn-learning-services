package com.learningplatform.enrollment.dto;

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
public class EnrollmentDTO {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private UUID courseId;
    private String courseTitle;
    private ZonedDateTime enrolledAt;
}
