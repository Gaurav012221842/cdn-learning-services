package com.learningplatform.progress.consumer;

import com.learningplatform.common.events.CourseEnrollmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Event-Driven Consumer: Listens to course-enrollment-events to initialize student learning progress.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseEnrollmentConsumer {

    @KafkaListener(topics = "course-enrollment-events", groupId = "progress-service-group")
    public void handleCourseEnrollment(CourseEnrollmentEvent event) {
        if (event == null || event.getStudentId() == null || event.getCourseId() == null) {
            log.warn("Received invalid CourseEnrollmentEvent");
            return;
        }

        log.info("Received CourseEnrollmentEvent: Student {} enrolled in Course {} (Enrollment ID: {})",
                event.getStudentId(), event.getCourseId(), event.getEnrollmentId());
    }
}
