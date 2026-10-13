package com.learningplatform.progress.certificate;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "certificate_code", nullable = false, unique = true)
    private String certificateCode;

    @Column(name = "issued_at")
    private ZonedDateTime issuedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (issuedAt == null) issuedAt = ZonedDateTime.now();
        if (certificateCode == null) certificateCode = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
