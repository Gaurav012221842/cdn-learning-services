package com.learningplatform.progress.certificate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    List<Certificate> findByStudentIdOrderByIssuedAtDesc(UUID studentId);

    Optional<Certificate> findByCertificateCode(String certificateCode);

    List<Certificate> findByStudentIdAndCourseIdOrderByIssuedAtAsc(UUID studentId, UUID courseId);
}
