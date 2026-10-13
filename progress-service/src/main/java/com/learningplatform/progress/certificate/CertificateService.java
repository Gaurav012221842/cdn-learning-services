package com.learningplatform.progress.certificate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;

    @Transactional
    public Certificate generateCertificate(UUID studentId, String email, UUID courseId) {
        UUID validStudentId = resolveStudentId(studentId, email);

        // Check if certificate already issued for this course
        List<Certificate> existingList = certificateRepository.findByStudentIdAndCourseIdOrderByIssuedAtAsc(validStudentId, courseId);
        if (existingList != null && !existingList.isEmpty()) {
            return existingList.get(0);
        }

        String code = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Certificate certificate = Certificate.builder()
                .id(UUID.randomUUID())
                .studentId(validStudentId)
                .courseId(courseId)
                .certificateCode(code)
                .issuedAt(ZonedDateTime.now())
                .build();

        Certificate saved = certificateRepository.save(certificate);
        log.info("Issued certificate {} for student {} on course {}", code, validStudentId, courseId);
        return saved;
    }

    @Transactional
    public List<Certificate> getStudentCertificates(UUID studentId, String email) {
        UUID validStudentId = resolveStudentId(studentId, email);
        List<Certificate> all = certificateRepository.findByStudentIdOrderByIssuedAtDesc(validStudentId);

        Map<UUID, Certificate> uniqueByCourse = new LinkedHashMap<>();
        for (Certificate c : all) {
            if (c.getCourseId() != null && !uniqueByCourse.containsKey(c.getCourseId())) {
                uniqueByCourse.put(c.getCourseId(), c);
            }
        }
        return new ArrayList<>(uniqueByCourse.values());
    }

    public Optional<Certificate> verifyCertificate(String certificateCode) {
        if (certificateCode == null || certificateCode.isBlank()) {
            return Optional.empty();
        }
        return certificateRepository.findByCertificateCode(certificateCode.trim().toUpperCase());
    }

    public Optional<Certificate> getCertificateForCourse(UUID studentId, String email, UUID courseId) {
        UUID validStudentId = resolveStudentId(studentId, email);
        List<Certificate> list = certificateRepository.findByStudentIdAndCourseIdOrderByIssuedAtAsc(validStudentId, courseId);
        return (list != null && !list.isEmpty()) ? Optional.of(list.get(0)) : Optional.empty();
    }

    private UUID resolveStudentId(UUID studentId, String email) {
        if (studentId != null) return studentId;
        if (email != null && !email.isBlank()) {
            return UUID.nameUUIDFromBytes(email.trim().toLowerCase().getBytes());
        }
        return UUID.randomUUID();
    }
}
