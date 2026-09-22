# Handoff Report: Backend Architecture Survey for IT Team Command Center

**Agent Folder:** `D:\java\dental-clinic\.agents\explorer_survey_backend`  
**Author:** Backend Architecture Explorer  
**Date:** 2026-09-12T15:05:00Z  
**Target Recipient:** Orchestrator / Implementer Agent  

---

## 1. Observation

1. **Build Tool & Dependencies (`pom.xml:6-23`):**
   - Spring Boot Starter Parent: `3.2.5`
   - Java version: `17`
   - Group ID / Artifact ID: `com.dentalclinic:dental-clinic:1.0.0`
   - Key Dependencies:
     - `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`, `spring-boot-starter-websocket`, `spring-boot-starter-mail`
     - JJWT 0.12.5 (`io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
     - OpenAPI / Swagger 3 (`springdoc-openapi-starter-webmvc-ui:2.5.0`)
     - Database: `h2`, `postgresql` (both runtime scope)
     - Test: `spring-boot-starter-test`, `spring-security-test`
   - **Crucial Negative Observation:** Lombok is NOT in `pom.xml`. Flyway / Liquibase is NOT in `pom.xml`.

2. **Database & Configuration (`src/main/resources/application.yml:11-27`):**
   - `spring.datasource.url`: `jdbc:h2:file:./data/dentaldb;AUTO_SERVER=TRUE`
   - `spring.datasource.driverClassName`: `org.h2.Driver`
   - `spring.jpa.hibernate.ddl-auto`: `update`
   - `spring.h2.console.enabled`: `true` at path `/h2-console`
   - `app.jwt.secret`: `dentalcareluxurysecuresecretkey2026superstrongforproductiontokengeneration32bytes`
   - `app.jwt.expiration-ms`: `86400000` (24 hours)

3. **Auditing & Base Entity (`src/main/java/com/dentalclinic/common/BaseEntity.java:1-28`):**
   - Mapped superclass with `@EntityListeners(AuditingEntityListener.class)`
   - Fields: `createdAt` (`@CreatedDate`, updatable=false), `updatedAt` (`@LastModifiedDate`)
   - Explicit getters/setters (no Lombok)
   - JPA Auditing enabled via `@EnableJpaAuditing` in `src/main/java/com/dentalclinic/config/JpaAuditingConfig.java:7`

4. **Security & Role System:**
   - In `src/main/java/com/dentalclinic/model/Role.java:3-10`:
     ```java
     public enum Role {
         ROLE_OWNER, ROLE_RECEPTIONIST, ROLE_DENTIST, ROLE_ASSISTANT, ROLE_CLEANER, ROLE_PATIENT
     }
     ```
     *Notice:* `ROLE_ADMIN` is currently missing from `Role.java`.
   - In `src/main/java/com/dentalclinic/security/CustomUserDetails.java:23-25`:
     ```java
     @Override
     public Collection<? extends GrantedAuthority> getAuthorities() {
         return List.of(new SimpleGrantedAuthority(user.getRole().name()));
     }
     ```
   - In `src/main/java/com/dentalclinic/security/SecurityConfig.java:23,70-98`:
     - `@EnableMethodSecurity(prePostEnabled = true)` is active.
     - `.requestMatchers("/api/**").authenticated()`
     - Rate limiter `RateLimitingFilter` and `JwtAuthenticationFilter` attached before `UsernamePasswordAuthenticationFilter`.
   - In `src/main/java/com/dentalclinic/exception/GlobalExceptionHandler.java:44-47`:
     - Catches `AccessDeniedException.class`, returns HTTP 403 with `ApiResponse.error("Bạn không có quyền truy cập chức năng này!")`.

5. **Existing 9Router Reference Implementation (`src/main/java/com/dentalclinic/service/AiBlogService.java:27-41,77-93`):**
   - Connects to 9Router at `@Value("${ninerouter.url:http://localhost:20128}")`.
   - Uses standard Java `HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()`.
   - Sends POST to `nineRouterUrl + "/v1/chat/completions"` with JSON payload containing `model`, `messages`, `temperature`.

6. **Existing Test Suite (`src/test/java/com/dentalclinic/DentalClinicApplicationTests.java`):**
   - Uses `@SpringBootTest` and `@AutoConfigureMockMvc`.
   - Tests login, bad credentials, forbidden access without token to `/api/dashboard/stats`, and public active coupons.

---

## 2. Logic Chain

1. **From Observation 1 & 2:** Because `spring.jpa.hibernate.ddl-auto` is set to `update`, introducing new JPA `@Entity` classes under `com.dentalclinic.itteam.model` will automatically create the tables (`it_agent_profile`, `it_agent_memory`, `it_agent_message`, `it_agent_activity`, `it_browser_tab_record`, `it_api_run_log`) on startup without manual SQL scripts or Flyway migrations.
2. **From Observation 1 & 3:** Because Lombok is absent across the entire repository, any new entities, DTOs, or services in `com.dentalclinic.itteam` must write explicit getters, setters, and constructors. Using Lombok annotations would cause missing symbols and compilation failures.
3. **From Observation 3:** Because `BaseEntity` already provides audited `createdAt` and `updatedAt`, all 6 IT Team entities should extend `com.dentalclinic.common.BaseEntity`.
4. **From Observation 4:** Requirements specify that `/api/it-team/**` must be strictly protected by JWT authentication with `ROLE_ADMIN`, and non-ADMIN requests (unauthenticated or `ROLE_PATIENT`) must return 401 or 403. Because `Role.java` currently only contains `ROLE_OWNER`, `ROLE_RECEPTIONIST`, etc., `ROLE_ADMIN` must be added to `Role.java`, an admin account should be seeded, and `/api/it-team/**` should be secured via `@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")` or `@PreAuthorize("hasRole('ADMIN')")`.
5. **From Observation 4 & GlobalExceptionHandler:** Calling `/api/it-team/**` without a token returns 401/403, and calling with `ROLE_PATIENT` triggers `AccessDeniedException` which returns 403 Forbidden, matching the acceptance criteria.
6. **From Observation 5:** The existing `AiBlogService` provides a proven pattern for 9Router HTTP communication at `http://localhost:20128/v1/chat/completions` using Java's built-in `HttpClient` and Jackson's `ObjectMapper`.

---

## 3. Caveats

1. **Maven Wrapper Execution in Restricted Environments:** During the survey, executing commands directly timed out waiting for user approval. The offline Maven repository `.m2/` is present, and `target/classes` exists. Code changes should be crafted cleanly to guarantee immediate compilation without relying on network downloads.
2. **SSRF Caution on Localhost API Tester:** Requirement R3 includes `POST /api/it-team/api-runs` to test internal APIs. The implementation must strictly whitelist localhost paths (`/api/**`) and reject external IP addresses/hostnames.
3. **9Router Dependency:** 9Router runs as an optional local process at `http://localhost:20128`. All 9Router API calls must have a connection timeout and fallback logic to avoid hanging requests if 9Router is not started.

---

## 4. Conclusion

The existing Spring Boot 3.2.5 backend is fully equipped to support the IT Team Command Center. The required changes are well-isolated within a new package `com.dentalclinic.itteam`:
1. Add `ROLE_ADMIN` to `com.dentalclinic.model.Role`.
2. Create the 6 domain entities in `com.dentalclinic.itteam.model` extending `BaseEntity`.
3. Create the 6 repositories in `com.dentalclinic.itteam.repository`.
4. Implement `ITTeamService`, `ITMessagingService` (with hashtag parsing & MENTIONED activity trigger), `ITApiRunnerService`, `SensitiveDataSanitizer`, and `NineRouterAiClient` in `com.dentalclinic.itteam.service`.
5. Implement `ITTeamController` at `/api/it-team/**` protected by `@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")`.
6. Implement `ITTeamDataInitializer` to seed the 5 standard IT profiles (`#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security`) idempotently.
7. Add integration test suite in `src/test/java/com/dentalclinic/itteam/ITTeamTests.java`.

---

## 5. Verification Method

1. **Codebase Inspection:**
   - Check `src/main/java/com/dentalclinic/model/Role.java` for `ROLE_ADMIN`.
   - Check `src/main/java/com/dentalclinic/itteam/` contains all 6 entities, repositories, services, controller, and seeder.
   - Check `src/main/resources/application.yml` for database and port settings.
2. **Test Execution Command:**
   - Execute: `./mvnw test` or `mvn test`
   - Verify all existing tests in `DentalClinicApplicationTests` and new tests in `ITTeamTests` pass:
     - Profile seeding: 5 profiles present.
     - Hashtag parsing: `#it-qa` extracts properly.
     - Activity mention: MENTIONED log generated when tagged.
     - Security RBAC: Unauthenticated/ROLE_PATIENT rejected with 401/403; ROLE_ADMIN succeeds.
     - Sensitive data sanitizer: Passwords, JWTs, and headers redacted.
3. **Invalidation Conditions:**
   - If any new entity fails to extend `BaseEntity` or uses Lombok annotations, compilation will fail.
   - If `ROLE_ADMIN` is not added to `Role.java`, any test expecting `ROLE_ADMIN` authority will fail.
