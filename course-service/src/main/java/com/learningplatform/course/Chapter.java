package com.learningplatform.course;

import com.learningplatform.course.lesson.Lesson;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chapters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chapter {

    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String title;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "chapter_id")
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = ZonedDateTime.now();
        if (sequenceOrder == null) sequenceOrder = 1;
    }
}
