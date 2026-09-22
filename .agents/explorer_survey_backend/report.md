# Comprehensive Backend Architecture Survey Report
**Project:** DentalCare Management Portal — IT Team Command Center Integration  
**Working Directory:** `D:\java\dental-clinic`  
**Date & UTC Timestamp:** 2026-09-12T15:05:00Z  
**Author:** Backend Architecture Explorer (`explorer_survey_backend`)  
**Authoritative Requirements Reference:** `D:\java\dental-clinic\ORIGINAL_REQUEST.md`

---

## Executive Summary
This report provides an in-depth survey of the Java / Spring Boot backend for the DentalCare Management Portal. The investigation covers build toolchains, existing module architecture, Spring Data JPA / database configuration, Spring Security & JWT mechanisms, test configurations, and the exact integration blueprint for the upcoming `com.dentalclinic.itteam` package (simulating an active internal IT organization with persistent memory, activity logging, safe API monitoring, hashtag-based inter-agent communication, and 9Router orchestration).

---

## 1. Build System & Dependency Landscape (`pom.xml`)

### 1.1 Core Specifications
- **Spring Boot Version:** `3.2.5` (`spring-boot-starter-parent`)
- **Java Version:** `17` (`<java.version>17</java.version>`)
- **Group ID / Artifact ID:** `com.dentalclinic:dental-clinic:1.0.0`
- **Build Plugin:** `org.springframework.boot:spring-boot-maven-plugin`
- **Wrapper Available:** `mvnw` and `mvnw.cmd` (Maven Wrapper 3.3.4, targeting Apache Maven 3.9.14)

### 1.2 Maven Dependencies Breakdown
| Category | Artifact | Version / Scope | Purpose & Notes |
| :--- | :--- | :--- | :--- |
| **Web & REST** | `spring-boot-starter-web` | 3.2.5 | Spring MVC, Jackson, Embedded Tomcat |
| **Security** | `spring-boot-starter-security` | 3.2.5 | Spring Security 6.2, Stateless Filter Chains |
| **Monitoring** | `spring-boot-starter-actuator` | 3.2.5 | Health, Info, Metrics endpoints (`/actuator/**`) |
| **Validation** | `spring-boot-starter-validation` | 3.2.5 | Jakarta Bean Validation (`@Valid`, `@NotBlank`, etc.) |
| **Data & ORM** | `spring-boot-starter-data-jpa` | 3.2.5 | Spring Data JPA, Hibernate 6.4 |
| **WebSocket** | `spring-boot-starter-websocket` | 3.2.5 | STOMP message broker over SockJS (`/ws-dental`) |
| **Mail** | `spring-boot-starter-mail` | 3.2.5 | JavaMailSender for automated transactional emails |
| **JWT Tokens** | `io.jsonwebtoken:jjwt-api` | `0.12.5` | Modern JJWT API for stateless token creation |
| **JWT Runtime** | `io.jsonwebtoken:jjwt-impl` | `0.12.5` (runtime) | JJWT implementation engine |
| **JWT JSON** | `io.jsonwebtoken:jjwt-jackson` | `0.12.5` (runtime) | JJWT Jackson serializer/deserializer |
| **OpenAPI / UI**| `springdoc-openapi-starter-webmvc-ui` | `2.5.0` | Swagger UI (`/swagger-ui/index.html`) & OpenAPI docs |
| **Embedded DB** | `com.h2database:h2` | (runtime) | H2 in-memory/file embedded database engine |
| **Production DB**| `org.postgresql:postgresql` | (runtime) | PostgreSQL JDBC driver |
| **Testing** | `spring-boot-starter-test` | (test) | JUnit Jupiter 5, Mockito, AssertJ, Spring Test |
| **Security Test**| `spring-security-test` | (test) | `@WithMockUser`, SecurityMockMvcRequestPostProcessors |

### 1.3 Critical Architectural Observations
1. **No Lombok:** The project does **NOT** use Project Lombok. All entities, DTOs, and services define standard Java getters, setters, and constructors explicitly. Implementing `com.dentalclinic.itteam` must adhere to this convention to prevent compilation failures.
2. **No Migration Tool (Flyway / Liquibase):** Database DDL is managed dynamically by Hibernate via `spring.jpa.hibernate.ddl-auto: update` (configured in `application.yml`). When new entities are introduced under `com.dentalclinic.itteam`, Hibernate will automatically generate and update tables on startup.
3. **Database Setup in Development:**
   - URL: `jdbc:h2:file:./data/dentaldb;AUTO_SERVER=TRUE`
   - Driver: `org.h2.Driver`
   - Credentials: username `sa`, password `password`
   - Web Console: Enabled at `/h2-console`
   - Existing DB Files: Located at `data/dentaldb.mv.db`. **Safety constraint:** Must never be deleted or modified outside JPA operations.
4. **JPA Auditing:** Enabled via `com.dentalclinic.config.JpaAuditingConfig` (`@EnableJpaAuditing`). All entities extending `com.dentalclinic.common.BaseEntity` automatically populate `createdAt` and `updatedAt`.

---

## 2. Existing Package Structure & Conventions

Root package: `com.dentalclinic`

```
src/main/java/com/dentalclinic/
├── DentalClinicApplication.java       # @SpringBootApplication entry point
├── common/                            # Shared domain base classes
│   ├── ApiResponse.java               # Standard API response wrapper
│   └── BaseEntity.java                # @MappedSuperclass with createdAt, updatedAt
├── config/                            # Application-level configurations
│   ├── DataInitializer.java           # CommandLineRunner seeding initial clinic data
│   ├── JpaAuditingConfig.java         # @EnableJpaAuditing configuration
│   ├── OpenApiConfig.java             # OpenAPI 3 / Swagger JWT Bearer configuration
│   ├── RateLimitingFilter.java        # In-memory IP rate limiter (60 req/10s window)
│   └── WebMvcConfig.java              # Static resource handler for uploads & @EnableAsync
├── controller/                        # 13 REST Controllers
│   ├── AppointmentController.java     # Booking, deposit payment, calendar queries
│   ├── ArticleController.java         # Dental handbook CMS & 9Router AI blog generation
│   ├── AuthController.java            # JWT login, patient registration, password management
│   ├── CouponController.java          # Discount vouchers & promotion codes
│   ├── DashboardController.java       # Analytics & revenue metrics
│   ├── DoctorReviewController.java    # 5-star doctor reviews
│   ├── EmailController.java           # Email testing & dispatch
│   ├── FileUploadController.java      # EMR X-ray / Panorama dental image upload
│   ├── MedicalRecordController.java   # Electronic Medical Records (EMR)
│   ├── NotificationController.java    # Realtime notification feed
│   ├── OrthodonticController.java     # 3D Orthodontic treatment plans
│   ├── ShiftController.java           # Staff shift scheduling
│   └── StaffController.java           # Staff & doctor directory
├── dto/                               # Request/Response data transfer objects
│   ├── AuthResponse.java, LoginRequest.java, RegisterRequest.java
│   ├── BookingRequest.java, BookingResultDto.java
│   ├── CreateCouponRequest.java, ValidateCouponRequest.java, CouponResultDto.java
│   ├── DashboardStatsDto.java, StaffCreateRequest.java
│   └── ChangePasswordRequest.java, ForgotPasswordRequest.java
├── exception/                         # Error handling architecture
│   ├── BadRequestException.java       # 400 Bad Request
│   ├── ResourceNotFoundException.java # 404 Not Found
│   └── GlobalExceptionHandler.java    # Centralized @RestControllerAdvice returning ApiResponse
├── model/                             # JPA Entities & Enums
│   ├── User.java                      # System user entity (staff, dentists, patients)
│   ├── Role.java                      # Enum: ROLE_OWNER, ROLE_RECEPTIONIST, ROLE_DENTIST, etc.
│   ├── Appointment.java, MedicalRecord.java, OrthodonticPlan.java, Payment.java, StaffShift.java
│   ├── Coupon.java, Article.java, DoctorReview.java, Notification.java, DentalImageAttachment.java
│   └── Status Enums (AppointmentStatus, BracketType, OrthoStage, PaymentMethod, ShiftType, etc.)
├── repository/                        # Spring Data JPA Repositories (11 interfaces)
│   └── UserRepository, AppointmentRepository, MedicalRecordRepository, etc.
├── security/                          # Security filters & JWT handling
│   ├── CustomUserDetails.java         # Spring Security UserDetails wrapper
│   ├── CustomUserDetailsService.java   # UserDetailsService loading from UserRepository
│   ├── JwtAuthenticationFilter.java   # OncePerRequestFilter parsing Authorization: Bearer
│   ├── JwtTokenProvider.java          # JJWT HS256 token generator & validator
│   └── SecurityConfig.java            # WebSecurityFilterChain, PasswordEncoder, RBAC rules
├── service/                           # Business logic layer
│   ├── AiBlogService.java             # 9Router AI client (calls http://localhost:20128/v1/chat/completions)
│   ├── AppointmentService.java, AuthService.java, DashboardService.java
│   ├── StaffService.java, ArticleService.java, DoctorReviewService.java
│   ├── EmailService.java, FileUploadService.java, NotificationService.java
└── websocket/                         # Real-time WebSocket architecture
    └── WebSocketConfig.java           # STOMP broker at /ws-dental, destination prefix /app, topic /topic
```

### 2.1 Entity & DTO Conventions
- **BaseEntity:** Every entity should extend `com.dentalclinic.common.BaseEntity`, which automatically tracks `createdAt` and `updatedAt` via `@CreatedDate` and `@LastModifiedDate`.
- **Response Wrapper:** Every REST controller returns `ResponseEntity<ApiResponse<T>>`.
  ```java
  public static <T> ApiResponse<T> success(String message, T data);
  public static <T> ApiResponse<T> success(T data);
  public static <T> ApiResponse<T> error(String message);
  ```
- **Injection Style:** 100% Constructor Injection is practiced in modern classes, with `@Autowired` present on legacy services. Constructor injection should be strictly used for all new components in `com.dentalclinic.itteam`.

---

## 3. Spring Security Configuration & Role System

### 3.1 Authentication Mechanism
1. Client sends `POST /api/auth/login` with `username` and `password`.
2. `AuthService` delegates to `AuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password))`.
3. `JwtTokenProvider` signs an HS256 token containing claims:
   - `subject`: username
   - `userId`: Long
   - `fullName`: String
   - `roles`: comma-separated granted authorities (e.g. `ROLE_OWNER` or `ROLE_ADMIN`)
   - `issuedAt`, `expiration`: 24-hour expiration (`86400000 ms`)
4. Subsequent requests include header: `Authorization: Bearer <token>`.
5. `JwtAuthenticationFilter` intercepts the request:
   - Validates token signature and expiration.
   - Loads user via `CustomUserDetailsService.loadUserByUsername(...)`.
   - Populates `SecurityContextHolder.getContext().setAuthentication(authentication)`.

### 3.2 Existing Role Enumeration (`com.dentalclinic.model.Role`)
Currently, `Role.java` defines:
```java
public enum Role {
    ROLE_OWNER,        // Nha sĩ chủ / Chủ phòng khám (Toàn quyền quản trị)
    ROLE_RECEPTIONIST, // Lễ tân
    ROLE_DENTIST,      // Bác sĩ nha sĩ
    ROLE_ASSISTANT,    // Phụ tá nha khoa
    ROLE_CLEANER,      // Tạp vụ / vô trùng
    ROLE_PATIENT       // Bệnh nhân / khách hàng
}
```
**CRITICAL FINDING ON `ROLE_ADMIN`:**
- `Role.java` currently contains `ROLE_OWNER` as the highest administrative role, but lacks an explicit `ROLE_ADMIN`.
- The user requirements explicitly dictate:
  > "Implement REST endpoints in `ITTeamController`, strictly protected by JWT authentication with `ROLE_ADMIN`"
  > "Non-ADMIN requests (unauthenticated or `ROLE_PATIENT`) to `/api/it-team/**` return `401 Unauthorized` or `403 Forbidden`."
  > "Valid ADMIN JWT can perform all CRUD operations on IT team endpoints."
- **Required Resolution:**
  1. Add `ROLE_ADMIN` to `com.dentalclinic.model.Role`.
  2. Protect `/api/it-team/**` using `@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")` (or `hasRole('ADMIN')`) so both `ROLE_ADMIN` and `ROLE_OWNER` can access the IT Command Center, while `ROLE_PATIENT`, other roles, and unauthenticated requests are strictly rejected with 401/403.
  3. Update `SecurityConfig.java` to explicitly configure:
     ```java
     .requestMatchers("/api/it-team/**").hasAnyRole("ADMIN", "OWNER")
     ```
  4. Seed an IT administrator account in `DataInitializer` (e.g. `username: "admin"`, `password: "123"`, `role: Role.ROLE_ADMIN`).

### 3.3 Public vs. Protected Endpoints
- **Public (`permitAll()`):**
  - Static Web Assets: `/`, `/index.html`, `/css/**`, `/js/**`, `/favicon.ico`, `/sitemap.xml`, `/robots.txt`, `/uploads/**`
  - Swagger & Docs: `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`
  - H2 Web Console: `/h2-console/**`
  - Health & Metrics: `/actuator/**`
  - WebSocket: `/ws-dental/**`
  - Auth: `/api/auth/**`
  - Public Booking & Coupons: `/api/appointments/book`, `/api/appointments/*/pay-deposit`, `/api/dentists`, `/api/coupons/active`, `/api/coupons/validate`
  - Public Articles & Reviews: `/api/articles/**`, `/api/reviews/**`, `/api/emr/images/**`, `/api/email/**`
- **Protected (`authenticated()` & Role-Restricted via `@PreAuthorize`):**
  - `/api/dashboard/**` (`ROLE_OWNER`, `ROLE_RECEPTIONIST`)
  - `/api/staff/**` (`ROLE_OWNER`, `ROLE_RECEPTIONIST`)
  - `/api/appointments/**` (excluding `/book` and `/pay-deposit`)
  - `/api/medical-records/**` (`ROLE_OWNER`, `ROLE_DENTIST`)
  - `/api/orthodontic-plans/**` (`ROLE_OWNER`, `ROLE_DENTIST`)
  - `/api/shifts/**` (`ROLE_OWNER`, `ROLE_RECEPTIONIST`)
  - `/api/coupons` POST/DELETE (`ROLE_OWNER`, `ROLE_RECEPTIONIST`)
  - `/api/it-team/**` (New: **strictly** `ROLE_ADMIN` / `ROLE_OWNER`)

---

## 4. Current Test Suites & Testing Architecture

### 4.1 Existing Test Files
Located at `src/test/java/com/dentalclinic/DentalClinicApplicationTests.java`:
- Uses `@SpringBootTest` and `@AutoConfigureMockMvc`.
- Tests 4 baseline integration scenarios:
  1. `testUniversalLoginSuccess`: Authenticates as `owner` / `123`, checks `jsonPath("$.data.role").value("ROLE_OWNER")`.
  2. `testUniversalLoginBadCredentials`: Checks 401 Unauthorized for incorrect password.
  3. `testUnauthorizedAccessToDashboardBlocked`: Verifies that an unauthenticated request to `/api/dashboard/stats` returns 403 Forbidden.
  4. `testPublicActiveCoupons`: Verifies public access to active coupons returns 200 OK.

### 4.2 Security Mocking & Test Patterns
- Security is tested via MockMvc with both raw HTTP calls (for public/unauthenticated checks) and standard Spring Security test post-processors or JWT headers.
- For IT Team testing, the suite should utilize:
  - MockMvc with header `Authorization: Bearer <jwt>` generated by `JwtTokenProvider`, or
  - `@WithMockUser(username = "admin", roles = {"ADMIN"})` and `@WithMockUser(username = "patient", roles = {"PATIENT"})`.

### 4.3 Test Runner & Build Status
- Build system: Maven Wrapper (`mvnw test` / `mvn test`).
- Target directory already contains pre-compiled test classes (`target/test-classes`).
- Automated tests for `com.dentalclinic.itteam` will be added to `src/test/java/com/dentalclinic/itteam/ITTeamTests.java`.

---

## 5. Architectural Blueprint for `com.dentalclinic.itteam`

### 5.1 Proposed Subpackage Layout
```
src/main/java/com/dentalclinic/itteam/
├── config/
│   └── ITTeamDataInitializer.java      # ApplicationRunner / CommandLineRunner for 5 profiles
├── controller/
│   └── ITTeamController.java          # REST API layer protected by ROLE_ADMIN / ROLE_OWNER
├── dto/
│   ├── AgentStatusUpdateRequest.java   # DTO to update agent status
│   ├── CreateMemoryRequest.java        # DTO for memory storage
│   ├── DispatchMessageRequest.java     # DTO for hashtag message dispatch
│   ├── RecordBrowserTabRequest.java    # DTO for tab tracking
│   └── ApiRunTestRequest.java          # DTO for internal API testing
├── model/
│   ├── ITAgentProfile.java             # Profile entity (#it-backend, etc.)
│   ├── ITAgentMemory.java              # Key-value memory entity
│   ├── ITAgentMessage.java             # Inter-agent message entity with thread support
│   ├── ITAgentActivity.java            # Activity log entity (e.g. MENTIONED)
│   ├── ITBrowserTabRecord.java         # Tab session record
│   └── ITApiRunLog.java                # API execution monitor record
├── repository/
│   ├── ITAgentProfileRepository.java
│   ├── ITAgentMemoryRepository.java
│   ├── ITAgentMessageRepository.java
│   ├── ITAgentActivityRepository.java
│   ├── ITBrowserTabRecordRepository.java
│   └── ITApiRunLogRepository.java
└── service/
    ├── ITTeamService.java              # Profile, Memory, Tab lifecycle
    ├── ITMessagingService.java         # Hashtag parsing, routing, mention activity trigger
    ├── ITApiRunnerService.java         # Safe localhost API execution tester & latency timer
    ├── SensitiveDataSanitizer.java     # Privacy guardrail redacting passwords, JWTs, PII
    └── NineRouterAiClient.java         # 9Router AI orchestration (http://localhost:20128)
```

### 5.2 Entity Specifications & Table Schemas

#### 1. `ITAgentProfile` (`it_agent_profile`)
- `id` (Long, PK, Auto-increment)
- `agentCode` (String, unique, nullable = false, e.g. `IT-BACKEND`)
- `hashtag` (String, unique, nullable = false, e.g. `#it-backend`)
- `displayName` (String, nullable = false, e.g. `Backend Core Agent`)
- `role` (String, nullable = false, e.g. `Lead Backend Engineer`)
- `status` (String, nullable = false, e.g. `ONLINE`, `BUSY`, `IDLE`, `OFFLINE`)
- `expertise` (String, length = 500, e.g. `Spring Boot, Database, Security, REST APIs`)
- `avatar` (String, e.g. `fa-solid fa-server`)
- `systemPrompt` (String, `columnDefinition = "TEXT"`, agent persona for 9Router)
- Extends `BaseEntity` (`createdAt`, `updatedAt`).

#### 2. `ITAgentMemory` (`it_agent_memory`)
- `id` (Long, PK)
- `agentId` (Long, nullable = false)
- `agentCode` (String, nullable = false)
- `memoryKey` (String, nullable = false, e.g. `SPRING_BOOT_VERSION`, `JWT_POLICY`)
- `memoryContent` (String, `columnDefinition = "TEXT"`, nullable = false, sanitized)
- `priorityLevel` (String, nullable = false, e.g. `HIGH`, `MEDIUM`, `LOW`)
- `lastUpdated` (LocalDateTime, nullable = false)
- Extends `BaseEntity`.

#### 3. `ITAgentMessage` (`it_agent_message`)
- `id` (Long, PK)
- `senderId` (Long, nullable = true)
- `senderCode` (String, nullable = false, e.g. `ADMIN` or `IT-BACKEND`)
- `senderName` (String, nullable = false, e.g. `Quản Trị Viên` or `Backend Agent`)
- `recipientId` (Long, nullable = true)
- `recipientCode` (String, nullable = true, e.g. `IT-QA`)
- `recipientName` (String, nullable = true)
- `messageBody` (String, `columnDefinition = "TEXT"`, nullable = false, sanitized)
- `parsedHashtags` (String, length = 255, e.g. `#it-qa,#it-devops`)
- `readStatus` (Boolean, nullable = false, default `false`)
- `parentMessageId` (Long, nullable = true, for threaded replies)
- `sentTimestamp` (LocalDateTime, nullable = false)
- Extends `BaseEntity`.

#### 4. `ITAgentActivity` (`it_agent_activity`)
- `id` (Long, PK)
- `agentId` (Long, nullable = true)
- `agentCode` (String, nullable = false, e.g. `IT-QA`)
- `actionType` (String, nullable = false, e.g. `MENTIONED`, `API_TEST`, `MEMORY_UPDATE`, `STATUS_CHANGE`, `DISPATCH`)
- `description` (String, `columnDefinition = "TEXT"`, nullable = false)
- `resultSummary` (String, length = 500)
- `relatedEntityLink` (String, length = 255, e.g. `/api/it-team/messages/4`)
- `timestamp` (LocalDateTime, nullable = false)
- Extends `BaseEntity`.

#### 5. `ITBrowserTabRecord` (`it_browser_tab_record`)
- `id` (Long, PK)
- `agentId` (Long, nullable = true)
- `agentCode` (String, nullable = false)
- `tabTitle` (String, nullable = false)
- `urlRoute` (String, nullable = false)
- `tabCategory` (String, nullable = false, e.g. `PROFILES`, `CONVERSATIONS`, `MEMORIES`, `ACTIVITIES`, `API_MONITOR`)
- `status` (String, nullable = false, e.g. `ACTIVE`, `BACKGROUND`, `CLOSED`)
- `openedTimestamp` (LocalDateTime, nullable = false)
- `closedTimestamp` (LocalDateTime, nullable = true)
- Extends `BaseEntity`.

#### 6. `ITApiRunLog` (`it_api_run_log`)
- `id` (Long, PK)
- `endpoint` (String, nullable = false)
- `httpMethod` (String, nullable = false, e.g. `GET`, `POST`, `PUT`, `DELETE`)
- `statusCode` (Integer, nullable = false)
- `executionDurationMs` (Long, nullable = false)
- `sanitizedRequestBody` (String, `columnDefinition = "TEXT"`)
- `sanitizedResponseBody` (String, `columnDefinition = "TEXT"`)
- `errorMessage` (String, length = 1000, nullable = true)
- `initiatedBy` (String, nullable = false, e.g. `ADMIN` or `IT-QA`)
- `runTimestamp` (LocalDateTime, nullable = false)
- Extends `BaseEntity`.

### 5.3 Data Seeder Blueprint (`ITTeamDataInitializer`)
On application startup:
1. Check `itAgentProfileRepository.count() == 0` (guarantees idempotency on repeated restarts).
2. Seed the 5 standard IT Profiles:
   - **`#it-backend`** (Code: `IT-BACKEND`, Name: `Backend Core Agent`, Role: `Lead Backend Engineer`, Status: `ONLINE`, Expertise: `Spring Boot, Database, Security, REST APIs`, Icon: `fa-server`)
   - **`#it-frontend`** (Code: `IT-FRONTEND`, Name: `Frontend UI Agent`, Role: `Senior Frontend Engineer`, Status: `ONLINE`, Expertise: `Management Portal UI, API Client, Responsive UX`, Icon: `fa-laptop-code`)
   - **`#it-qa`** (Code: `IT-QA`, Name: `QA Automation Agent`, Role: `QA & Test Automation Specialist`, Status: `ONLINE`, Expertise: `API Testing, Authorization Checks, Regression`, Icon: `fa-vial-circle-check`)
   - **`#it-devops`** (Code: `IT-DEVOPS`, Name: `DevOps & Reliability Agent`, Role: `Site Reliability Engineer`, Status: `ONLINE`, Expertise: `Build, Server Runtime, Logs, Tunnel Integration`, Icon: `fa-cloud-arrow-up`)
   - **`#it-security`** (Code: `IT-SECURITY`, Name: `Cybersecurity Officer`, Role: `Application Security Architect`, Status: `ONLINE`, Expertise: `RBAC, Data Privacy, Input Validation, Audit Logs`, Icon: `fa-shield-halved`)
3. Seed baseline system memories for each agent to make the "Bộ Nhớ" tab populated on launch.
4. Seed introductory activity records and an initial conversation welcoming the team.

### 5.4 Privacy Guardrail (`SensitiveDataSanitizer`)
Strictly prohibits storing plaintext passwords, JWTs, cookies, or real patient PII in memory/activity/tab/API run logs:
- **JWT Pattern:** `eyJ[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+` -> `[REDACTED_JWT]`
- **Password Pattern:** `"password"\s*:\s*"[^"]*"` -> `"password": "[REDACTED]"`
- **Authorization Header Pattern:** `Bearer\s+[^\s]+` -> `Bearer [REDACTED]`
- **Cookie Header Pattern:** `Cookie:\s*[^;\n]+` -> `Cookie: [REDACTED]`
- **Credit Card / ID Patterns:** Redacted to `[REDACTED_FINANCIAL]` or `[REDACTED_PII]`.

### 5.5 Hashtag Parsing & Routing Engine
- Regex: `#it-(backend|frontend|qa|devops|security)\b` (case-insensitive).
- When a message is dispatched:
  1. Extract all matching hashtags.
  2. Link recipient to the first matching agent profile (or broadcast if multiple).
  3. Save message record with `parsedHashtags`.
  4. Automatically create an `ITAgentActivity` record for each tagged agent:
     - `actionType = "MENTIONED"`
     - `description = "Nhắc đến agent trong hội thoại: " + preview`
     - `relatedEntityLink = "/api/it-team/messages/" + savedMessage.getId()`
  5. Support threaded replies via `parentMessageId`.
  6. Optional: Trigger 9Router AI response for mentioned agent if configured.

### 5.6 9Router AI Integration (`http://localhost:20128`)
Reference implementation from existing `AiBlogService.java`:
- Java 11 `HttpClient` sending POST to `http://localhost:20128/v1/chat/completions`.
- Standard OpenAI Chat Completion JSON format:
  ```json
  {
    "model": "fast-combo",
    "messages": [
      {"role": "system", "content": "<agent.systemPrompt> + Context memories"},
      {"role": "user", "content": "<incomingMessageBody>"}
    ],
    "temperature": 0.7
  }
  ```
- Graceful degradation: If 9Router is unavailable or returns an error, fallback to a local synthetic response or log the event without interrupting the messaging flow.

### 5.7 Safe Internal API Execution Tester
- Whitelist protection:
  - Path must start with `/api/` or `http://localhost:8080/api/`.
  - Strictly deny any external URL, AWS/GCP metadata URLs (`169.254.169.254`), or private LAN IPs to prevent SSRF vulnerabilities.
- Executes request using internal Spring MockMvc or `HttpClient`.
- Measures duration in milliseconds.
- Applies `SensitiveDataSanitizer` to request and response bodies.
- Persists entry into `it_api_run_log`.

### 5.8 REST Endpoints Specification
Base path: `/api/it-team`  
Security: `@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/it-team/agents` | List all 5 IT agent profiles |
| `PUT` | `/api/it-team/agents/{id}/status` | Update agent status (`ONLINE`, `BUSY`, etc.) |
| `GET` | `/api/it-team/memories` | Retrieve agent memories (supports query by `agentCode` / `agentId`) |
| `POST`| `/api/it-team/memories` | Create or update agent memory (sanitized) |
| `DELETE` | `/api/it-team/memories/{id}` | Remove an agent memory item |
| `GET` | `/api/it-team/messages` | Retrieve messages / threads chronologically |
| `POST`| `/api/it-team/messages` | Dispatch message, parse hashtags, route, log mention activity |
| `GET` | `/api/it-team/activities` | Paginated activity feed (filterable by agent and action type) |
| `GET` | `/api/it-team/browser-tabs` | Retrieve active / recent tab session records |
| `POST`| `/api/it-team/browser-tabs` | Record opening or switching of a command center tab |
| `POST`| `/api/it-team/api-runs` | Safe execution tester for internal localhost APIs |
| `GET` | `/api/it-team/api-runs` | Retrieve recent API run logs |

---

## 6. Frontend Integration Points (`index.html` & `app.js`)

1. **Navigation Bar (`#management-portal`):**
   - Add new tab button `id="tab-itteam"`:
     ```html
     <button onclick="switchTab('itteam')" id="tab-itteam" class="tab-btn py-3.5 px-4 border-b-2 border-transparent text-slate-400 hover:text-white flex items-center gap-2">
         <i class="fa-solid fa-terminal text-brand-400"></i> IT Team Command Center
     </button>
     ```
   - Only visible when logged in as `ROLE_ADMIN` or `ROLE_OWNER`.
2. **Main Section Container (`#section-itteam`):**
   - 5 sub-views:
     1. **Nhân Sự (Team Profiles):** 5 agent cards with status badge, role, competencies, and quick actions.
     2. **Hội Thoại (Conversations):** Interactive chat room with hashtag autocomplete (`#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security`), threaded view, and live message dispatch.
     3. **Bộ Nhớ (Agent Memories):** Structured memory cards grouped by agent with priority indicators (`HIGH`, `MEDIUM`, `LOW`).
     4. **Nhật Ký Thao Tác (Activity Logs):** Filterable chronological audit stream displaying agent actions and mentions.
     5. **API Monitor:** Interactive API testing console with endpoint selector, method badge, duration timer, and sanitized JSON payload inspector.
3. **API Client (`apiFetch`):**
   - Utilizes existing `apiFetch` in `app.js`, which automatically attaches `Authorization: Bearer <token>`.

---

## 7. Risk Analysis & Safety Guardrails
1. **Safety & Non-Interference:**
   - Clinic data in `data/dentaldb.mv.db` must remain intact.
   - Hibernate `ddl-auto: update` safely adds new `it_*` tables without altering existing clinic tables (`users`, `appointments`, `medical_records`, etc.).
2. **SSRF Prevention on API Tester:**
   - Must strictly validate destination URLs. Disallow requests to non-localhost hosts.
3. **PII and Credential Leakage:**
   - `SensitiveDataSanitizer` must run on both request payloads and response payloads before saving to database logs.
4. **9Router Availability:**
   - 9Router call must have connection timeouts (e.g. 15s) and catch exceptions gracefully so agent messaging never blocks or fails if 9Router is temporarily offline.

---

## 8. Conclusion
The DentalCare Spring Boot codebase is clean, well-structured, and ready for the `com.dentalclinic.itteam` module. Adding `ROLE_ADMIN` to `Role.java`, creating the 6 domain entities, implementing hashtag parsing, privacy sanitization, and REST controllers will cleanly fulfill all requirements in `ORIGINAL_REQUEST.md`.
