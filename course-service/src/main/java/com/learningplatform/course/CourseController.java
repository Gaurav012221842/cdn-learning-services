package com.learningplatform.course;

import com.learningplatform.common.dto.ApiResponse;
import com.learningplatform.course.dto.CourseResponse;
import com.learningplatform.course.dto.CreateCourseRequest;
import com.learningplatform.course.dto.UpdateCourseRequest;
import com.learningplatform.course.dto.QuizEvaluationResponse;
import com.learningplatform.course.dto.QuizSubmissionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllCourses() {
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved", courseService.getAllCourses()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(
            @PathVariable UUID id,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String email,
            @RequestHeader(value = com.learningplatform.common.constants.SecurityHeaders.USER_ROLE_HEADER, required = false) String userRole
    ) {
        UUID sId = parseUUID(studentId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole) || "ROLE_ADMIN".equalsIgnoreCase(userRole);
        return ResponseEntity.ok(ApiResponse.success("Course retrieved", courseService.getCourseById(id, sId, email, isAdmin)));
    }

    private UUID parseUUID(String str) {
        if (str == null || str.isBlank() || str.equals("undefined") || str.equals("null")) return null;
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(@RequestBody CreateCourseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Course created", courseService.createCourse(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(@PathVariable UUID id, @RequestBody UpdateCourseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Course updated", courseService.updateCourse(id, request)));
    }

    @PostMapping("/{id}/quiz/submit")
    public ResponseEntity<ApiResponse<QuizEvaluationResponse>> submitQuiz(
            @PathVariable UUID id,
            @RequestBody QuizSubmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Quiz evaluated successfully", courseService.evaluateQuiz(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course deleted successfully", null));
    }
}
