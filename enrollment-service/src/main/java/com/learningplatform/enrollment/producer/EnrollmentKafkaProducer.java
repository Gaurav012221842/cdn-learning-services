package com.learningplatform.enrollment.producer;

import com.learningplatform.common.events.CourseEnrollmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentKafkaProducer {

    public static final String COURSE_ENROLLMENT_TOPIC = "course-enrollment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishEnrollmentCreated(UUID enrollmentId, UUID studentId, String studentEmail, UUID courseId, String paymentRef) {
        CourseEnrollmentEvent event = CourseEnrollmentEvent.builder()
                .enrollmentId(enrollmentId)
                .studentId(studentId)
                .studentEmail(studentEmail)
                .courseId(courseId)
                .courseTitle("Enrolled Course")
                .paymentReference(paymentRef)
                .enrolledAt(Instant.now())
                .build();

        try {
            log.info("Emitting CourseEnrollmentEvent to Kafka topic [{}]: studentId={}, courseId={}",
                    COURSE_ENROLLMENT_TOPIC, studentId, courseId);
            kafkaTemplate.send(COURSE_ENROLLMENT_TOPIC, studentId.toString(), event);
        } catch (Exception e) {
            log.warn("Could not dispatch CourseEnrollmentEvent to Kafka: {}", e.getMessage());
        }
    }
}
