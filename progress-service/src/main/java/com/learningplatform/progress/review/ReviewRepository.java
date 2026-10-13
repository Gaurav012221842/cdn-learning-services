package com.learningplatform.progress.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<CourseReview, UUID> {
    List<CourseReview> findByCourseId(UUID courseId);
}
