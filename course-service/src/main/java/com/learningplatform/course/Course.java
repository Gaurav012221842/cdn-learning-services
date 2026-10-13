package com.learningplatform.course;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "instructor_id")
    private UUID instructorId;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String status;

    @Column
    private String category;

    @Column(name = "access_type", nullable = false)
    private String accessType;

    @Column(name = "thumbnail_media_id")
    private UUID thumbnailMediaId;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "course_id")
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private java.util.List<Chapter> chapters = new java.util.ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = ZonedDateTime.now();
        if (updatedAt == null) updatedAt = ZonedDateTime.now();
        if (price == null) price = BigDecimal.ZERO;
        if (status == null) status = "DRAFT";
        if (accessType == null) accessType = "FREE";
    }
}
