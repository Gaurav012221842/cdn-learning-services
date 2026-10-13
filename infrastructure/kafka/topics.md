# Distributed Kafka Topics Specification

The microservices communicate asynchronously via Apache Kafka using structured JSON event contracts defined in `learning-common`.

---

## 1. `media-transcoding-events`
- **Partitions**: 3
- **Replication Factor**: 1 (Dev) / 3 (Prod)
- **Producer**: `media-service`
- **Consumer**: `video-worker`
- **Contract Class**: `com.learningplatform.common.event.MediaTranscodingEvent`
- **Payload Schema**:
  ```json
  {
    "mediaId": "c0a80123-8675-4321-a1b2-c3d4e5f60718",
    "objectKey": "raw/lecture-hls.mp4",
    "targetQualities": ["1080p", "720p", "480p"],
    "timestamp": 1723812000000
  }
  ```

---

## 2. `payment-completed-events`
- **Partitions**: 3
- **Replication Factor**: 1 (Dev) / 3 (Prod)
- **Producer**: `payment-service`
- **Consumer**: `enrollment-service`
- **Contract Class**: `com.learningplatform.common.event.PaymentCompletedEvent`
- **Payload Schema**:
  ```json
  {
    "paymentId": "pay_9876543210",
    "razorpayOrderId": "order_1234567890",
    "userId": "a1b2c3d4-0000-1111-2222-333344445555",
    "courseId": "f9e8d7c6-5555-4444-3333-222211110000",
    "amount": 49.99,
    "timestamp": 1723812005000
  }
  ```

---

## 3. `course-enrollment-events`
- **Partitions**: 3
- **Replication Factor**: 1 (Dev) / 3 (Prod)
- **Producer**: `enrollment-service`
- **Consumer**: `progress-service`
- **Contract Class**: `com.learningplatform.common.event.CourseEnrollmentEvent`
- **Payload Schema**:
  ```json
  {
    "enrollmentId": "e1f2a3b4-1111-2222-3333-444455556666",
    "studentId": "a1b2c3d4-0000-1111-2222-333344445555",
    "courseId": "f9e8d7c6-5555-4444-3333-222211110000",
    "enrolledAt": 1723812010000
  }
  ```

---

## 4. `user-registered-events`
- **Partitions**: 3
- **Replication Factor**: 1 (Dev) / 3 (Prod)
- **Producer**: `user-service`
- **Consumer**: `user-service` (Notification/Email worker), future analytics workers
- **Contract Class**: `com.learningplatform.common.event.UserRegisteredEvent`
- **Payload Schema**:
  ```json
  {
    "userId": "a1b2c3d4-0000-1111-2222-333344445555",
    "email": "student@example.com",
    "fullName": "Gaurav Student",
    "registeredAt": 1723812015000
  }
  ```
