package com.learningplatform.progress;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgress {

    @Id
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "is_completed")
    private Boolean isCompleted;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (isCompleted == null) isCompleted = false;
        if (updatedAt == null) updatedAt = ZonedDateTime.now();
    }
}
