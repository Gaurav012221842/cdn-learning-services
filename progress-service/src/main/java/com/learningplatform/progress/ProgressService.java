package com.learningplatform.progress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final ProgressRepository progressRepository;

    @Cacheable(value = "student_progress", key = "(#studentIdStr ?: '') + '_' + (#courseIdStr ?: '') + '_' + (#email ?: '')")
    public List<CourseProgress> getProgress(String studentIdStr, String courseIdStr, String email) {
        UUID studentId = resolveStudentId(studentIdStr, email);
        UUID courseId = parseUuid(courseIdStr);

        if (studentId == null || courseId == null) {
            return Collections.emptyList();
        }

        return progressRepository.findByStudentIdAndCourseId(studentId, courseId);
    }

    public Set<String> getMarkedLessonIdsFromRedis(String studentIdStr, String courseIdStr, String email) {
        UUID studentId = resolveStudentId(studentIdStr, email);
        UUID courseId = parseUuid(courseIdStr);

        if (studentId == null || courseId == null) return Collections.emptySet();

        return progressRepository.findByStudentIdAndCourseId(studentId, courseId).stream()
                .filter(cp -> Boolean.TRUE.equals(cp.getIsCompleted()))
                .map(cp -> cp.getLessonId().toString())
                .collect(Collectors.toSet());
    }

    public boolean isLessonMarkedCompleted(String studentIdStr, String courseIdStr, String lessonIdStr, String email) {
        UUID studentId = resolveStudentId(studentIdStr, email);
        UUID courseId = parseUuid(courseIdStr);
        UUID lessonId = parseUuid(lessonIdStr);

        if (studentId == null || courseId == null || lessonId == null) return false;

        return progressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .map(cp -> Boolean.TRUE.equals(cp.getIsCompleted()))
                .orElse(false);
    }

    @CacheEvict(value = "student_progress", allEntries = true)
    public CourseProgress toggleLessonProgress(String studentIdStr, String courseIdStr, String lessonIdStr, Boolean completed, String email) {
        UUID studentId = resolveStudentId(studentIdStr, email);
        UUID courseId = parseUuid(courseIdStr);
        UUID lessonId = parseUuid(lessonIdStr);

        if (studentId == null) {
            studentId = UUID.nameUUIDFromBytes("default-student".getBytes());
        }
        if (courseId == null) {
            courseId = UUID.nameUUIDFromBytes((courseIdStr != null ? courseIdStr : "default-course").getBytes());
        }
        if (lessonId == null) {
            lessonId = UUID.nameUUIDFromBytes((lessonIdStr != null ? lessonIdStr : "default-lesson").getBytes());
        }

        final UUID finalStudentId = studentId;
        final UUID finalLessonId = lessonId;
        final UUID finalCourseId = courseId;
        boolean markCompleted = completed != null ? completed : true;

        CourseProgress progress = progressRepository.findByStudentIdAndLessonId(finalStudentId, finalLessonId)
                .orElseGet(() -> CourseProgress.builder()
                        .studentId(finalStudentId)
                        .courseId(finalCourseId)
                        .lessonId(finalLessonId)
                        .build());

        progress.setIsCompleted(markCompleted);
        progress.setUpdatedAt(ZonedDateTime.now());
        CourseProgress saved = progressRepository.save(progress);
        log.info("Saved lesson progress in PostgreSQL: student={}, lesson={}, completed={}", finalStudentId, finalLessonId, markCompleted);
        return saved;
    }

    private UUID resolveStudentId(String studentIdStr, String email) {
        UUID id = parseUuid(studentIdStr);
        if (id != null) return id;

        if (email != null && !email.isBlank() && !email.equals("null") && !email.equals("undefined")) {
            return UUID.nameUUIDFromBytes(email.trim().toLowerCase().getBytes());
        }

        if (studentIdStr != null && !studentIdStr.isBlank() && !studentIdStr.equals("null") && !studentIdStr.equals("undefined")) {
            return UUID.nameUUIDFromBytes(studentIdStr.getBytes());
        }

        return null;
    }

    private UUID parseUuid(String str) {
        if (str == null || str.isBlank() || str.equals("undefined") || str.equals("null") || str.equals("current")) {
            return null;
        }
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(str.getBytes());
        }
    }
}
