# Complete REST API Specification

All external requests are routed through the **API Gateway** on port `8080`:
Base Endpoint: `http://localhost:8080/api/v1`

---

## 🔐 1. Authentication & Users (`user-service` - Port 8082)

### `POST /api/v1/auth/register`
Registers a new user (Student or Admin).
- **Request Body**:
  ```json
  {
    "email": "student@example.com",
    "password": "Password123!",
    "fullName": "Gaurav Student",
    "role": "STUDENT"
  }
  ```
- **Response `200 OK`**: `ApiResponse<AuthResponse>` containing JWT token, ID, email, role.

### `POST /api/v1/auth/login`
Authenticates a user and issues a JWT token. Also registers/updates active device session.
- **Request Body**:
  ```json
  {
    "email": "student@example.com",
    "password": "Password123!"
  }
  ```
- **Response `200 OK`**: `ApiResponse<AuthResponse>`

### `GET /api/v1/users/me`
Retrieves currently authenticated user profile based on `X-User-Id` / JWT.

### `GET /api/v1/users/sessions`
Returns active device sessions for current user.

---

## 📚 2. Courses & Curriculum (`course-service` - Port 8083)

### `GET /api/v1/courses` (Public)
Retrieves all published courses for the Explore Courses catalog.

### `GET /api/v1/courses/{id}` (Public)
Retrieves full course curriculum including chapters and lessons.

### `POST /api/v1/courses` (Admin)
Creates a new course with chapters and multi-format lessons.
- **Request Body**:
  ```json
  {
    "title": "Full-Stack Microservices Masterclass",
    "description": "Master distributed systems, Kafka, Redis, and Spring Cloud.",
    "price": 49.99,
    "thumbnailUrl": "https://pub-xxx.r2.dev/thumbnail.jpg",
    "chapters": [
      {
        "title": "Chapter 1: Architecture",
        "sequenceOrder": 1,
        "lessons": [
          {
            "title": "Lesson 1: Microservices vs Monolith",
            "lessonType": "VIDEO",
            "contentUrl": "https://pub-xxx.r2.dev/video.mp4",
            "videoThumbnailUrl": "https://pub-xxx.r2.dev/thumb.jpg",
            "sequenceOrder": 1
          }
        ]
      }
    ]
  }
  ```

### `GET /api/v1/courses/search?q={query}`
Searches published courses by title or keyword.

### `GET /api/v1/courses/{id}/pricing`
Gets dynamic price calculations, active campaign discounts, and currency details.

---

## 📁 3. Media Upload & CDN Streaming (`media-service` - Port 8084)

### `POST /api/v1/media/presigned-url`
Generates a signed Cloudflare R2 upload URL for direct client multipart uploads.

### `POST /api/v1/media/upload-direct`
Direct multipart upload fallback to Cloudflare R2 bucket. Publishes `MediaTranscodingEvent` to Kafka.

### `GET /api/v1/media/signed-url?objectKey={key}`
Generates a 60-second presigned streaming URL for secure HLS playback.

---

## 💳 4. Payments & Coupons (`payment-service` - Port 8085)

### `POST /api/v1/payments/initiate`
Initiates a Razorpay order.
- **Query Params**: `userId`, `userEmail`, `courseId`, `amount`
- **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "razorpayOrderId": "order_xxx",
      "amount": 49.99,
      "status": "PENDING"
    }
  }
  ```

### `POST /api/v1/payments/verify`
Verifies HMAC-SHA256 signature and publishes `PaymentCompletedEvent` to Kafka topic `payment-completed-events`.
- **Query Params**: `razorpayOrderId`, `razorpayPaymentId`, `signature`, `userId`, `userEmail`, `courseId`, `amount`

### `POST /api/v1/coupons/validate`
Validates promotional coupon code for discount eligibility.

---

## 🎓 5. Enrollments (`enrollment-service` - Port 8086)

### `GET /api/v1/enrollments/student/{studentId}`
Returns list of enrolled course IDs for access entitlement checks.

### `GET /api/v1/enrollments/check?studentId={id}&courseId={courseId}`
Returns boolean whether student has active enrollment.

### `GET /api/v1/enrollments/all` (Admin)
Retrieves all student enrollments with student name, email, and course title.

### `DELETE /api/v1/enrollments/{id}` (Admin)
Revokes student course access with immediate cache invalidation.

---

## 📈 6. Progress, Certificates, Reviews & Wishlist (`progress-service` - Port 8087)

### `POST /api/v1/progress`
Marks a lesson as completed for the student.
- **Query Params**: `studentId`, `lessonId`, `completed`

### `GET /api/v1/progress/course/{courseId}?studentId={studentId}`
Returns student completion percentage and completed lesson list.

### `POST /api/v1/certificates/issue`
Issues a course completion certificate if 100% completed.

### `POST /api/v1/reviews`
Submits student course rating and review.

### `GET /api/v1/wishlists/student/{studentId}`
Retrieves bookmarked courses for student.

---

## ⚙️ 7. Video Worker (`video-worker` - Port 8081)

### `GET http://localhost:8081/api/v1/worker/status`
Exposes video transcoding worker health, worker concurrency, and Kafka consumer group lag.
