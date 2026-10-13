package com.learningplatform.course;

import com.learningplatform.course.dto.ChapterDTO;
import com.learningplatform.course.dto.CourseResponse;
import com.learningplatform.course.dto.LessonDTO;
import com.learningplatform.course.lesson.Lesson;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CourseMapper {

    public CourseResponse toResponse(Course course) {
        if (course == null) return null;
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .instructorId(course.getInstructorId())
                .price(course.getPrice())
                .status(course.getStatus())
                .category(course.getCategory())
                .thumbnailUrl(course.getThumbnailUrl())
                .createdAt(course.getCreatedAt())
                .chapters(course.getChapters() == null ? java.util.Collections.emptyList() :
                        course.getChapters().stream().map(this::toChapterDTO).collect(Collectors.toList()))
                .build();
    }

    public ChapterDTO toChapterDTO(Chapter chapter) {
        if (chapter == null) return null;
        return ChapterDTO.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .sequenceOrder(chapter.getSequenceOrder())
                .lessons(chapter.getLessons() == null ? java.util.Collections.emptyList() :
                        chapter.getLessons().stream().map(this::toLessonDTO).collect(Collectors.toList()))
                .build();
    }

    public LessonDTO toLessonDTO(Lesson lesson) {
        if (lesson == null) return null;
        return LessonDTO.builder()
                .id(lesson.getId())
                .chapterId(lesson.getChapterId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .lessonType(lesson.getLessonType())
                .contentUrl(lesson.getContentUrl())
                .videoThumbnailUrl(lesson.getVideoThumbnailUrl())
                .quizData(sanitizeQuizData(lesson.getQuizData()))
                .sequenceOrder(lesson.getSequenceOrder())
                .build();
    }

    private String sanitizeQuizData(String rawQuizData) {
        if (rawQuizData == null || rawQuizData.trim().isEmpty()) {
            return rawQuizData;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(rawQuizData);
            if (rootNode.has("questions") && rootNode.get("questions").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode qNode : rootNode.get("questions")) {
                    if (qNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                        com.fasterxml.jackson.databind.node.ObjectNode obj = (com.fasterxml.jackson.databind.node.ObjectNode) qNode;
                        obj.remove("correctIndex");
                        obj.remove("explanation");
                    }
                }
            }
            return mapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            return rawQuizData;
        }
    }
}
