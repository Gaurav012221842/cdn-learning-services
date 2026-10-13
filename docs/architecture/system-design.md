# Comprehensive System Design & Microservices Architecture

## 🏗️ Architecture Blueprint

The platform has been decomposed from a monolithic architecture into a cloud-native microservices architecture designed for high availability, fault isolation, independent scalability, and zero-downtime video streaming.

```text
                                  [ React 19 Frontend Web App (Port 3000) ]
                                                     │
                                                     │ HTTPS REST
                                                     ▼
                                      [ API Gateway (Spring Cloud - Port 8080) ]
                                          ├── JWT Verification & Claim Enrichment
                                          ├── Correlation ID Tracing
                                          └── Dynamic Reverse Proxy Dispatching
                                                     │
        ┌──────────────┬──────────────┬──────────────┼──────────────┬──────────────┬──────────────┐
        ▼              ▼              ▼              ▼              ▼              ▼              ▼
  [user-service] [course-service] [media-service] [payment-service] [enrollment-service] [progress-service] [video-worker]
    (Port 8082)    (Port 8083)     (Port 8084)     (Port 8085)       (Port 8086)       (Port 8087)     (Port 8081)
        │              │               │               │                 │                 │               │
        │              │               │ Direct R2     │ Razorpay        │ Entitlement     │ Certs         │ FFmpeg
        │              │               ▼               ▼                 ▼                 ▼               ▼
        │              │      [Cloudflare R2 Bucket] [Razorpay API]      │                 │      [Cloudflare R2]
        │              │               ▲                                 │                 │               ▲
        │              │               │                                 │                 │               │
        └──────────────┴───────────────┴───────────────┬─────────────────┴─────────────────┴───────────────┘
                                                       │
                                           [ Apache Kafka Cluster ]
                                      Topics:
                                        ├── media-transcoding-events
                                        ├── payment-completed-events
                                        ├── course-enrollment-events
                                        └── user-registered-events
                                                       │
                                   ┌───────────────────┴───────────────────┐
                                   ▼                                       ▼
                         [ PostgreSQL Database ]                   [ Redis Cluster ]
                           (Port 5433 / 5432)                         (Port 6379)
```

---

## ⚡ Microservices Breakdown & Boundaries

### 1. `learning-common` (Shared Library)
- Common response wrappers (`ApiResponse<T>`, `PageResponse<T>`).
- Cross-cutting security headers (`X-User-Id`, `X-User-Email`, `X-User-Role`, `X-Correlation-Id`).
- Domain-wide Kafka Event contracts:
  - `PaymentCompletedEvent`
  - `MediaTranscodingEvent`
  - `CourseEnrollmentEvent`
  - `UserRegisteredEvent`
- Centralized exception hierarchy (`BusinessException`, `ResourceNotFoundException`, `UnauthorizedException`).

### 2. `api-gateway` (Port 8080)
- **Technology**: Spring Cloud Gateway (Reactive WebFlux / Netty).
- Single entry-point for all frontend API traffic.
- Validates JWT tokens, extracts claims, and injects secure downstream headers (`X-User-Id`, `X-User-Email`, `X-User-Role`).
- Automatically handles CORS preflights and distributes unique `X-Correlation-Id` headers.

### 3. `user-service` (Port 8082)
- User registration, authentication, and BCrypt password encryption.
- Multi-device active session tracking (`UserDeviceSession`) with concurrent session control.
- Password reset token management and welcome email notifications.
- Publishes `UserRegisteredEvent` to Kafka.

### 4. `course-service` (Port 8083)
- Course catalog CRUD, draft/publish lifecycle, and full-text search.
- Curriculum builder: Chapters and multi-format Lessons (Video, PDF, Image, Quiz).
- Pricing tiers, dynamic discounts, and promotional campaigns.

### 5. `media-service` (Port 8084)
- Direct multipart file uploads to Cloudflare R2 object storage.
- Generates 60-second short-lived signed streaming URLs for secure HLS playback.
- Publishes `MediaTranscodingEvent` to Kafka for asynchronous HLS transcoding.

### 6. `payment-service` (Port 8085)
- Razorpay order creation (`/api/v1/payments/initiate`).
- HMAC-SHA256 signature verification (`/api/v1/payments/verify`).
- Promotional coupon code validation and discount calculation.
- Publishes `PaymentCompletedEvent` upon verified payment to orchestrate asynchronous enrollment.

### 7. `enrollment-service` (Port 8086)
- Course access entitlement verification and enrollment checks.
- Consumes `PaymentCompletedEvent` from Kafka for automatic course provisioning (Saga pattern).
- Admin enrollment revocation portal with real-time course access revocation.
- Publishes `CourseEnrollmentEvent` to initialize student progress tracking.

### 8. `progress-service` (Port 8087)
- Student lesson completion tracking and percentage calculation.
- Course completion certificate generation and verification.
- Student ratings and reviews submission.
- Student personal wishlists and bookmarking.
- Consumes `CourseEnrollmentEvent` from Kafka to pre-seed course progress tracking.

### 9. `video-worker` (Port 8081)
- Asynchronous FFmpeg HLS segmentation engine.
- Consumes `media-transcoding-events` from Kafka.
- Downloads raw video from Cloudflare R2, generates multi-bitrate HLS streams (`index.m3u8` and `.ts` chunks), and uploads back to R2.

---

## 🔄 Event-Driven Saga Flow

```text
[User Checkout]
      │
      ▼
[payment-service] ──(HMAC Validated)──► [Kafka: payment-completed-events]
                                                        │
                                                        ▼
                                              [enrollment-service]
                                                        │ (Provisions Enrollment)
                                                        ▼
                                          [Kafka: course-enrollment-events]
                                                        │
                                                        ▼
                                              [progress-service]
                                                 (Initializes Course Progress)
```
