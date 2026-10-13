package com.learningplatform.enrollment;

import com.learningplatform.enrollment.dto.EnrollmentDTO;
import com.learningplatform.enrollment.producer.EnrollmentKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentKafkaProducer enrollmentKafkaProducer;

    public Enrollment enrollStudent(UUID studentId, String email, UUID courseId) {
        UUID validStudentId = (studentId != null) ? studentId : UUID.randomUUID();

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(validStudentId, courseId)
                .orElseGet(() -> {
                    Enrollment newEnrollment = Enrollment.builder()
                            .studentId(validStudentId)
                            .courseId(courseId)
                            .enrolledAt(ZonedDateTime.now())
                            .build();
                    Enrollment saved = enrollmentRepository.save(newEnrollment);
                    log.info("✅ Student {} successfully enrolled in course {}", validStudentId, courseId);

                    // Publish Kafka event for progress and notifications
                    enrollmentKafkaProducer.publishEnrollmentCreated(
                            saved.getId(),
                            validStudentId,
                            email != null ? email : "student@gauravlearn.com",
                            courseId,
                            "RAZORPAY_ENROLLMENT"
                    );

                    return saved;
                });

        return enrollment;
    }

    public boolean isStudentEnrolled(UUID studentId, UUID courseId) {
        if (studentId == null || courseId == null) return false;
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).isPresent();
    }

    public List<Enrollment> getStudentEnrollments(UUID studentId, String email) {
        if (studentId == null) return List.of();
        return enrollmentRepository.findByStudentId(studentId);
    }

    public List<EnrollmentDTO> getAllEnrollmentsDTO() {
        return enrollmentRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<EnrollmentDTO> getCourseEnrollmentsDTO(UUID courseId) {
        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void removeEnrollment(UUID id) {
        log.info("Revoking enrollment ID: {}", id);
        enrollmentRepository.deleteById(id);
    }

    private EnrollmentDTO toDTO(Enrollment e) {
        return EnrollmentDTO.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .studentName("Student " + e.getStudentId().toString().substring(0, 8))
                .studentEmail("student@" + e.getStudentId().toString().substring(0, 8) + ".com")
                .courseId(e.getCourseId())
                .courseTitle("Enrolled Course " + e.getCourseId().toString().substring(0, 8))
                .enrolledAt(e.getEnrolledAt())
                .build();
    }
}
