package com.learningplatform.course.lesson;

import com.learningplatform.course.dto.CreateLessonRequest;
import com.learningplatform.course.dto.LessonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;

    @Cacheable(value = "course_lessons", key = "#courseId")
    public List<LessonResponse> getLessonsByCourse(UUID courseId) {
        return lessonRepository.findByCourseIdOrderBySequenceOrderAsc(courseId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = {"course_lessons", "course_details", "courses_all"}, allEntries = true)
    public LessonResponse createLesson(CreateLessonRequest request) {
        Lesson lesson = Lesson.builder()
                .courseId(request.getCourseId())
                .title(request.getTitle())
                .content(request.getContent())
                .sequenceOrder(request.getSequenceOrder() != null ? request.getSequenceOrder() : 1)
                .mediaId(request.getMediaId())
                .build();
        return mapToResponse(lessonRepository.save(lesson));
    }

    private LessonResponse mapToResponse(Lesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .courseId(lesson.getCourseId())
                .title(lesson.getTitle())
                .content(lesson.getContent())
                .sequenceOrder(lesson.getSequenceOrder())
                .mediaId(lesson.getMediaId())
                .build();
    }
}
