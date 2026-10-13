# Comprehensive Database Schema Design & Microservices Domain Mapping

The platform runs on **PostgreSQL** with logical domain separation per microservice, allowing either a single shared PostgreSQL instance (development/staging) or isolated per-service databases (production):

---

## 🗄️ Domain-to-Service Table Mapping

| Microservice | Port | Tables Owned | Domain Responsibility |
| :--- | :--- | :--- | :--- |
| **`user-service`** | 8082 | `users`, `user_device_sessions`, `password_reset_tokens` | Identity, Auth, Device Tracking |
| **`course-service`** | 8083 | `courses`, `chapters`, `lessons`, `course_pricing`, `campaigns` | Course Catalog, Curriculum, Pricing |
| **`media-service`** | 8084 | `media` | Cloudflare R2 Upload Metadata & CDN Keys |
| **`payment-service`** | 8085 | `payments`, `coupons` | Razorpay Orders, Signatures, Vouchers |
| **`enrollment-service`** | 8086 | `enrollments` | Student Entitlements & Access Grants |
| **`progress-service`** | 8087 | `course_progress`, `certificates`, `reviews`, `wishlists` | Student Progress, Certs, Ratings |

---

## 📋 Microservice Tables Detail

### 1. `user-service`
#### `users`
- `id` (UUID, Primary Key)
- `email` (VARCHAR(255), Unique, Not Null)
- `password_hash` (VARCHAR(255), Not Null)
- `full_name` (VARCHAR(255))
- `role` (VARCHAR(50), Default 'STUDENT')
- `avatar_url` (VARCHAR(1000))
- `created_at` (TIMESTAMP WITH TIME ZONE)
- `updated_at` (TIMESTAMP WITH TIME ZONE)

#### `user_device_sessions`
- `id` (UUID, Primary Key)
- `user_id` (UUID, Not Null)
- `device_fingerprint` (VARCHAR(255))
- `device_name` (VARCHAR(255))
- `ip_address` (VARCHAR(100))
- `last_active_at` (TIMESTAMP WITH TIME ZONE)

---

### 2. `course-service`
#### `courses`
- `id` (UUID, Primary Key)
- `title` (VARCHAR(255), Not Null)
- `description` (TEXT)
- `instructor_id` (UUID)
- `price` (DECIMAL(38,2), Not Null)
- `status` (VARCHAR(50), Default 'DRAFT')
- `thumbnail_url` (VARCHAR(1000))
- `created_at` (TIMESTAMP WITH TIME ZONE)
- `updated_at` (TIMESTAMP WITH TIME ZONE)

#### `chapters`
- `id` (UUID, Primary Key)
- `course_id` (UUID, Not Null)
- `title` (VARCHAR(255), Not Null)
- `sequence_order` (INTEGER, Not Null, Default 1)
- `created_at` (TIMESTAMP WITH TIME ZONE)

#### `lessons`
- `id` (UUID, Primary Key)
- `course_id` (UUID, Not Null)
- `chapter_id` (UUID)
- `title` (VARCHAR(255), Not Null)
- `description` (TEXT)
- `lesson_type` (VARCHAR(50)) -- VIDEO, IMAGE, PDF, QUIZ
- `content_url` (TEXT)
- `video_thumbnail_url` (TEXT)
- `quiz_data` (TEXT) -- JSON Encoded Quiz
- `sequence_order` (INTEGER)
- `created_at` (TIMESTAMP WITH TIME ZONE)

#### `course_pricing` & `campaigns`
- Manages pricing tiers, seasonal discounts, and campaign periods.

---

### 3. `media-service`
#### `media`
- `id` (UUID, Primary Key)
- `file_name` (VARCHAR(255), Not Null)
- `file_type` (VARCHAR(50), Not Null) -- VIDEO, IMAGE, PDF, REEL
- `object_key` (VARCHAR(500), Not Null)
- `file_size` (BIGINT)
- `duration` (INTEGER)
- `status` (VARCHAR(50), Not Null, Default 'READY')
- `uploader_id` (UUID)
- `created_at` (TIMESTAMP WITH TIME ZONE)

---

### 4. `payment-service`
#### `payments`
- `id` (UUID, Primary Key)
- `user_id` (UUID, Not Null)
- `course_id` (UUID, Not Null)
- `amount` (DECIMAL(38,2), Not Null)
- `razorpay_order_id` (VARCHAR(255))
- `razorpay_payment_id` (VARCHAR(255))
- `status` (VARCHAR(50), Not Null, Default 'PENDING') -- PENDING, COMPLETED, FAILED
- `created_at` (TIMESTAMP WITH TIME ZONE)

#### `coupons`
- `id` (UUID, Primary Key)
- `code` (VARCHAR(50), Unique, Not Null)
- `discount_amount` (DECIMAL(38,2), Not Null)
- `expiry_date` (TIMESTAMP WITH TIME ZONE)
- `usage_limit` (INTEGER)

---

### 5. `enrollment-service`
#### `enrollments`
- `id` (UUID, Primary Key)
- `student_id` (UUID, Not Null)
- `course_id` (UUID, Not Null)
- `enrolled_at` (TIMESTAMP WITH TIME ZONE)
- `CONSTRAINT unique_student_course UNIQUE(student_id, course_id)`

---

### 6. `progress-service`
#### `course_progress`
- `id` (UUID, Primary Key)
- `student_id` (UUID, Not Null)
- `lesson_id` (UUID, Not Null)
- `is_completed` (BOOLEAN, Default false)
- `completed_at` (TIMESTAMP WITH TIME ZONE)

#### `certificates`
- `id` (UUID, Primary Key)
- `student_id` (UUID, Not Null)
- `course_id` (UUID, Not Null)
- `certificate_url` (VARCHAR(500))
- `issued_at` (TIMESTAMP WITH TIME ZONE)

#### `reviews` & `wishlists`
- Ratings, reviews comments, and student wishlist items.
