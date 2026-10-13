package com.learningplatform.course.lesson;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "lessons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    private UUID id;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "chapter_id")
    private UUID chapterId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "lesson_type")
    private String lessonType; // VIDEO, IMAGE, PDF, QUIZ

    @Column(name = "content_url", columnDefinition = "TEXT")
    private String contentUrl;

    @Column(name = "video_thumbnail_url", columnDefinition = "TEXT")
    private String videoThumbnailUrl;

    @Column(name = "quiz_data", columnDefinition = "TEXT")
    private String quizData;

    @Column(name = "sequence_order")
    private Integer sequenceOrder;

    @Column(name = "media_id")
    private UUID mediaId;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = ZonedDateTime.now();
        if (lessonType == null) lessonType = "VIDEO";
        if (sequenceOrder == null) sequenceOrder = 1;
    }
}
