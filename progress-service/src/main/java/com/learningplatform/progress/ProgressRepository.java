package com.learningplatform.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgressRepository extends JpaRepository<CourseProgress, UUID> {
    List<CourseProgress> findByStudentIdAndCourseId(UUID studentId, UUID courseId);
    Optional<CourseProgress> findByStudentIdAndLessonId(UUID studentId, UUID lessonId);
}
