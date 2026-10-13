package com.learningplatform.progress.certificate;

import com.learningplatform.common.dto.ApiResponse;
import com.learningplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Certificate>> generateCertificate(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String email,
            @RequestParam UUID courseId
    ) {
        UUID uid = parseUUID(studentId);
        Certificate cert = certificateService.generateCertificate(uid, email, courseId);
        return ResponseEntity.ok(ApiResponse.success("Certificate generated and issued successfully", cert));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<Certificate>>> getStudentCertificates(
            @PathVariable(required = false) String studentId,
            @RequestParam(required = false) String email
    ) {
        UUID uid = parseUUID(studentId);
        return ResponseEntity.ok(ApiResponse.success("Student certificates retrieved", certificateService.getStudentCertificates(uid, email)));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Certificate>> checkCertificate(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String email,
            @RequestParam UUID courseId
    ) {
        UUID uid = parseUUID(studentId);
        Certificate cert = certificateService.getCertificateForCourse(uid, email, courseId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Certificate status checked", cert));
    }

    @GetMapping("/verify/{certificateCode}")
    public ResponseEntity<ApiResponse<Certificate>> verifyCertificate(@PathVariable String certificateCode) {
        Certificate cert = certificateService.verifyCertificate(certificateCode)
                .orElseThrow(() -> new BusinessException("Certificate code not found or invalid: " + certificateCode));
        return ResponseEntity.ok(ApiResponse.success("Certificate verified as authentic", cert));
    }

    private UUID parseUUID(String str) {
        if (str == null || str.isBlank() || str.equals("undefined") || str.equals("null") || str.equals("current")) {
            return null;
        }
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return null;
        }
    }
}
