package com.learningplatform.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when student is enrolled in a course.
 * Topic: course-enrollment-events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEnrollmentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID enrollmentId;
    private UUID studentId;
    private String studentEmail;
    private UUID courseId;
    private String courseTitle;
    private String paymentReference;
    private Instant enrolledAt;
}
