package com.learningplatform.progress;

import com.learningplatform.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<ApiResponse<List<CourseProgress>>> getProgress(
            @PathVariable String studentId,
            @PathVariable String courseId,
            @RequestParam(required = false) String email
    ) {
        return ResponseEntity.ok(ApiResponse.success("Progress retrieved", progressService.getProgress(studentId, courseId, email)));
    }

    /**
     * Get set of all marked completed lesson IDs directly from Redis RAM
     */
    @GetMapping("/marked-lessons")
    public ResponseEntity<ApiResponse<Set<String>>> getMarkedLessonsFromRedis(
            @RequestParam String studentId,
            @RequestParam String courseId,
            @RequestParam(required = false) String email
    ) {
        Set<String> markedIds = progressService.getMarkedLessonIdsFromRedis(studentId, courseId, email);
        return ResponseEntity.ok(ApiResponse.success("Marked lessons retrieved from Redis", markedIds));
    }

    /**
     * Check if specific lesson is marked completed via Redis
     */
    @GetMapping("/is-marked")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> isLessonMarked(
            @RequestParam String studentId,
            @RequestParam String courseId,
            @RequestParam String lessonId,
            @RequestParam(required = false) String email
    ) {
        boolean marked = progressService.isLessonMarkedCompleted(studentId, courseId, lessonId, email);
        return ResponseEntity.ok(ApiResponse.success("Marked status checked via Redis", Map.of("marked", marked)));
    }

    @PostMapping("/mark-completed")
    public ResponseEntity<ApiResponse<CourseProgress>> markCompleted(
            @RequestParam String studentId,
            @RequestParam String courseId,
            @RequestParam String lessonId,
            @RequestParam(required = false) String email
    ) {
        return ResponseEntity.ok(ApiResponse.success("Lesson marked completed", progressService.toggleLessonProgress(studentId, courseId, lessonId, true, email)));
    }

    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<CourseProgress>> toggleProgress(
            @RequestParam String studentId,
            @RequestParam String courseId,
            @RequestParam String lessonId,
            @RequestParam(required = false, defaultValue = "true") Boolean completed,
            @RequestParam(required = false) String email
    ) {
        return ResponseEntity.ok(ApiResponse.success("Progress updated in DB & Redis", progressService.toggleLessonProgress(studentId, courseId, lessonId, completed, email)));
    }
}
