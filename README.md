# CDN-Powered Learning Platform & Microservices Architecture

A state-of-the-art, high-performance E-Learning Cloud Platform built with **Spring Boot 3**, **React 19**, **Apache Kafka**, **Redis**, **PostgreSQL**, **Cloudflare R2 Storage**, and **Razorpay Payment Gateway**.

---

## 🚀 Key Platform Features

### 🎥 1. Cloudflare R2 & CDN Media Streaming
- **Direct Multipart R2 Storage Uploads**: Upload videos, high-resolution images, PDFs, reels, and audio directly to Cloudflare R2 bucket with zero backend latency.
- **Short-Lived Presigned Token Security**: Media URLs expire automatically after 60 seconds to prevent link sharing and unauthorized downloads.
- **Anti-Theft Floating Watermark Overlay**: Dynamic overlay featuring student email and IP address floating across video playback to prevent screen recording and content theft.

### 📚 2. Admin Course & Curriculum Builder (`/admin/course/create`)
- Dynamic Chapter and Module hierarchy management.
- Multi-format Lesson Types dropdown:
  - 🎥 **VIDEO**: Supports custom video upload + custom video thumbnail poster.
  - 📄 **PDF**: Supports embedded PDF reader.
  - 🖼️ **IMAGE**: Supports architectural diagram viewer.
  - 📝 **QUIZ**: Interactive question & option builder with automatic student grading.

### 💳 3. Razorpay Payment Gateway Integration
- Official Razorpay JavaScript Checkout Window (`checkout.js`) with support for:
  - 💳 **Credit / Debit Cards**
  - 📱 **UPI QR & App Selectors** (Google Pay, PhonePe, Paytm)
  - 🏛️ **Netbanking** (HDFC, SBI, ICICI, Axis)
  - 👛 **Paytm Wallets & Pay Later**
- HMAC-SHA256 signature verification in Spring Boot (`POST /api/v1/payments/verify`).
- Instant automated PostgreSQL enrollment registration (`POST /api/v1/enrollments`).

### 🔒 4. Access Control & Student Management (`/admin/enrollments`)
- **Strict Course Locking**: Non-enrolled students see `🔒 Complete Razorpay Payment to Unlock` on lessons.
- **Free Preview**: Chapter 1, Lesson 1 features an `👁️ Free Preview` badge.
- **Admin Revocation Portal**: One-click student access revocation (`DELETE /api/v1/enrollments/{id}`) with real-time database cache eviction.

### ⚙️ 5. Asynchronous Kafka Video Worker (`video-worker`)
- Background microservice listening to Kafka topic `media-transcoding-events`.
- Automatically executes FFmpeg multi-bitrate HLS segmentation (`.m3u8` & `.ts`) and uploads segments back to Cloudflare R2.
- Exposes health status endpoint at `http://localhost:8081/api/v1/worker/status`.

---

## 🛠️ Microservices & Workspace Structure

```text
CDN-Powered Learning Platform/
├── learning-common/                  # Shared DTOs, Kafka Event Contracts & Security Headers
├── api-gateway/                      # Spring Cloud Gateway (Port 8080)
├── user-service/                     # Identity, Authentication & Profile Microservice (Port 8082)
├── course-service/                   # Course Catalog, Curriculum & Pricing Microservice (Port 8083)
├── media-service/                    # Cloudflare R2 Direct Multipart & Streaming CDN (Port 8084)
├── payment-service/                  # Razorpay Billing, Webhooks & Coupon Microservice (Port 8085)
├── enrollment-service/               # Course Entitlements & Access Revocation (Port 8086)
├── progress-service/                 # Learning Progress, Certificates, Reviews & Wishlist (Port 8087)
├── video-worker/                     # FFmpeg HLS Transcoding Microservice (Port 8081)
├── learningapplicationfrontend/     # Modern React 19 Frontend Web Application (Port 3000)
├── infrastructure/                   # Docker Compose, Kafka, Redis & Prometheus Configs
└── docs/                             # System Architecture, Database Schema & API Docs
```

---

## ⚡ Quick Start

### 1. Run via Docker Compose
```bash
docker-compose up -d --build
```

### 2. Service Endpoints Matrix
| Component | Service | Port / Internal URL | Technology |
| :--- | :--- | :--- | :--- |
| **API Gateway** | Spring Cloud Gateway | `http://localhost:8080` | Netty / Reactive WebFlux |
| **Frontend Web App** | React 19 SPA | `http://localhost:3000` | React 19 + Nginx |
| **Identity & Users** | `user-service` | `http://localhost:8082` | Spring Boot 3 + Redis + JWT |
| **Course Catalog** | `course-service` | `http://localhost:8083` | Spring Boot 3 + PostgreSQL |
| **Media & CDN** | `media-service` | `http://localhost:8084` | Spring Boot 3 + AWS S3 v2 / Cloudflare R2 |
| **Payments & Billing** | `payment-service` | `http://localhost:8085` | Spring Boot 3 + Razorpay |
| **Enrollments** | `enrollment-service` | `http://localhost:8086` | Spring Boot 3 + Kafka Saga |
| **Progress & Certs** | `progress-service` | `http://localhost:8087` | Spring Boot 3 + PostgreSQL |
| **Video Worker** | `video-worker` | `http://localhost:8081` | Spring Boot 3 + FFmpeg |
| **PostgreSQL Database**| PostgreSQL 15 | `localhost:5433` / `5432` | Relational Storage |
| **Redis Cache** | Redis 7 | `localhost:6379` | In-Memory Token & State Cache |
| **Kafka Event Broker** | Apache Kafka | `localhost:9092` | Distributed Event Streaming |
| **Prometheus** | Monitoring Server | `http://localhost:9090` | Metrics Collector |
| **Grafana** | Visual Dashboards | `http://localhost:3001` | Visualization |

---

## 📖 System Documentation Links

- 📐 [System Architecture & Design](file:///Users/gauravkumar/Pictures/CDN-Powered%20Learning%20Platform/docs/architecture/system-design.md)
- 🗄️ [Database Schema & Migration Design](file:///Users/gauravkumar/Pictures/CDN-Powered%20Learning%20Platform/docs/database/database-design.md)
- 🔌 [Complete REST API Specification](file:///Users/gauravkumar/Pictures/CDN-Powered%20Learning%20Platform/docs/api/api-specification.md)
- 📊 [Architectural Mermaid Diagrams](file:///Users/gauravkumar/Pictures/CDN-Powered%20Learning%20Platform/docs/diagrams/system-architecture.md)
