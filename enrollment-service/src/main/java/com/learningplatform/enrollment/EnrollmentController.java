package com.learningplatform.enrollment;

import com.learningplatform.common.constants.SecurityHeaders;
import com.learningplatform.common.dto.ApiResponse;
import com.learningplatform.enrollment.dto.EnrollmentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Enrollment>> enroll(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String studentEmail,
            @RequestParam UUID courseId,
            @RequestHeader(value = SecurityHeaders.USER_ID_HEADER, required = false) String headerUserId,
            @RequestHeader(value = SecurityHeaders.USER_EMAIL_HEADER, required = false) String headerUserEmail
    ) {
        UUID sId = parseUUID(studentId != null ? studentId : headerUserId);
        String email = studentEmail != null ? studentEmail : headerUserEmail;
        return ResponseEntity.ok(ApiResponse.success("Student enrolled successfully", enrollmentService.enrollStudent(sId, email, courseId)));
    }

    @PostMapping("/admin/grant")
    public ResponseEntity<ApiResponse<Enrollment>> adminGrantEnrollment(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String studentEmail,
            @RequestParam UUID courseId,
            @RequestHeader(value = SecurityHeaders.USER_ROLE_HEADER, required = false) String userRole
    ) {
        if (!isAdmin(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Forbidden: Only administrators can grant enrollments."));
        }
        UUID sId = parseUUID(studentId);
        return ResponseEntity.ok(ApiResponse.success("Course access granted", enrollmentService.enrollStudent(sId, studentEmail, courseId)));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<Enrollment>>> getStudentEnrollments(
            @PathVariable(required = false) String studentId,
            @RequestParam(required = false) String email,
            @RequestHeader(value = SecurityHeaders.USER_ID_HEADER, required = false) String headerUserId
    ) {
        UUID id = parseUUID(studentId != null ? studentId : headerUserId);
        return ResponseEntity.ok(ApiResponse.success("Enrollments retrieved", enrollmentService.getStudentEnrollments(id, email)));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkAccess(
            @RequestParam UUID studentId,
            @RequestParam UUID courseId
    ) {
        boolean enrolled = enrollmentService.isStudentEnrolled(studentId, courseId);
        return ResponseEntity.ok(ApiResponse.success(enrolled ? "Student is enrolled" : "Not enrolled", enrolled));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getAllEnrollments() {
        return ResponseEntity.ok(ApiResponse.success("All enrollments retrieved", enrollmentService.getAllEnrollmentsDTO()));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getCourseEnrollments(@PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success("Course enrollments retrieved", enrollmentService.getCourseEnrollmentsDTO(courseId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeEnrollment(
            @PathVariable UUID id,
            @RequestHeader(value = SecurityHeaders.USER_ROLE_HEADER, required = false) String userRole
    ) {
        if (!isAdmin(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Forbidden: Only administrators can revoke student enrollments."));
        }
        enrollmentService.removeEnrollment(id);
        return ResponseEntity.ok(ApiResponse.success("Student enrollment revoked successfully", null));
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
    }

    private UUID parseUUID(String str) {
        if (str == null || str.isBlank() || str.equals("undefined") || str.equals("null")) return null;
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return null;
        }
    }
}
