# DentalCare Comprehensive Dental Ecosystem & Enterprise Security
# Test Infrastructure & Specification Document (TEST_INFRA.md)

**Project:** DentalCare Luxury Clinic Management Portal & Omnichannel Ecosystem  
**Root Directory:** `D:\java\dental-clinic`  
**Test Suites Path:** `src/test/java/com/dentalclinic/e2e/`  
**Author:** E2E Test Track Specialist (`test_writer_dental_e2e`)  
**Parent Orchestrator:** `4110e379-52ae-4437-9f08-bb3a919ba41c`  
**Status:** PUBLISHED & READY FOR VERIFICATION (Track A Complete)  
**Version:** 2.0.0  

---

## 1. Executive Summary & Dual Track Architecture

The DentalCare Clinic ecosystem is an enterprise dental healthcare platform encompassing:
1. **Patient / Customer Ecosystem (Web & Mobile)**: Service catalog, dynamic slot booking, dental e-commerce with box/combo packaging, porcelain crown QR warranty verification, dental loyalty program, AI dental pathology diagnostic vision, community forum, and clinic branches map.
2. **Staff & B2B Operations**: Tier-2 satellite clinics / distributors, dental material inventory, procurement orders, staff timekeeping check-in/out, doctor consultation & revenue KPIs, and field patient intake / school screening.
3. **Enterprise Medical Security**: 20 security standards including IDOR protection on medical records, strict RBAC, AES-256 GCM encryption of clinical fields, MIME/magic byte file upload validation, rate limiting brute-force defense, and security headers.
4. **IT Team Command Center**: Persistent memory, inter-agent messaging, audit logging, and safe API monitoring.

To ensure zero regression of existing clinic operations while driving rapid, verifiable implementation, the project follows the **Dual Track Testing Strategy**:
- **Track A (Test Engineering / Test Writer):** Operates independently. Designs the complete testing infrastructure, defines formal interface contracts, establishes the 4-tier (+ Tier 5 adversarial) methodology, and writes opaque-box E2E test suites against REST contracts before or alongside implementation.
- **Track B (Feature Implementation):** Executes feature milestones (M1: Customer, M2: Staff/B2B, M3: Desktop CMS, M4: Analytics SDK, M5: 20 Security Standards).

```
   Dual Track Execution Flow
   =========================
   Track A (Test Track)                 Track B (Implementation Track)
   --------------------                 ------------------------------
   TEST_INFRA.md Updated                M1: Customer Dental Ecosystem
            │                                     │
            ▼                                     ▼
   DentalCustomerE2ETest.java           M2: Staff & B2B Satellite Ops
   StaffAndOperationsE2ETest.java                 │
   MedicalSecurityE2ETest.java                    ▼
   (Tiers 1-4 Opaque-Box Suites)        M3: PC Desktop App & CMS
            │                                     │
            ▼                                     ▼
   TEST_READY.md Published              M4: Agrid / Analytics SDK
            │                                     │
            ▼                                     ▼
   mvnw test-compile Validated          M5: 20 Enterprise Security
            │                                     │
            └─────────────► M6 ◄──────────────────┘
                      100% E2E Execution
                      Zero Regressions
```

---

## 2. Test Environment & Harness Architecture

### 2.1 Framework & Toolchain
- **Runtime:** Java 17 LTS, Spring Boot 3.2.5
- **Testing Framework:** JUnit Jupiter 5 (`org.junit.jupiter.api.*`)
- **Integration Test Context:** `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)`
- **HTTP Mocking Engine:** `@AutoConfigureMockMvc` (`org.springframework.test.web.servlet.MockMvc`)
- **JSON Serialization:** Jackson `ObjectMapper` (`com.fasterxml.jackson.databind.ObjectMapper`)
- **JSON Assertion:** Jayway `jsonPath` (`org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath`)
- **Multipart Upload Engine:** `MockMultipartFile` (`org.springframework.mock.web.MockMultipartFile`)
- **Database:** Embedded H2 database (`jdbc:h2:file:./data/dentaldb;AUTO_SERVER=TRUE`) with JPA auditing enabled.

### 2.2 Opaque-Box Isolation & Rate-Limiting Strategy
1. **Opaque-Box Philosophy:** Tests interact exclusively via HTTP JSON and Multipart REST endpoints (`/api/**`). Tests avoid coupling directly to uncommitted internal class signatures, ensuring clean compilation and forward-compatibility across all implementation stages.
2. **Rate Limiting Resilience:** The application enforces `RateLimitingFilter` (60 requests per 10 seconds per IP). The test harness injects rotating `X-Forwarded-For` headers per test group/method (`192.168.20.x`, `192.168.30.x`, `192.168.40.x`), preventing false-positive HTTP 429 errors during high-volume suite execution.
3. **Deterministic Auth Fixture:** Dynamic `obtainToken(username, password)` routine acquires authentic JWT tokens for:
   - `ROLE_OWNER` (`owner` / `123`) — Full administrative access.
   - `ROLE_ADMIN` (`admin` / `123`) — System administrator access.
   - `ROLE_DENTIST` (`bs_tuan` / `123`) — Clinical dentist access.
   - `ROLE_RECEPTIONIST` (`letan` / `123`) — Receptionist and shift coordination access.
   - `ROLE_PATIENT` (`benhnhan` / `123`) — Client/patient role for IDOR and RBAC enforcement.

---

## 3. The 4-Tier Testing Methodology

The test suites strictly implement the project's 4-tier testing hierarchy:

```
┌─────────────────────────────────────────────────────────────┐
│  Tier 4: Real-World Workload Scenarios (End-to-End Journeys)│
├─────────────────────────────────────────────────────────────┤
│  Tier 3: Pairwise Combinatorial (Cross-Feature Pipelines)   │
├─────────────────────────────────────────────────────────────┤
│  Tier 2: Boundary Value Analysis (Corner & Extreme Cases)   │
├─────────────────────────────────────────────────────────────┤
│  Tier 1: Category-Partition (Equivalence Classes & Features)│
└─────────────────────────────────────────────────────────────┘
```

### 3.1 Tier 1: Category-Partition (Primary Feature Coverage)
Divides the input domain of each feature into non-overlapping equivalence classes and exercises the nominal positive behavior:
- **Customer Track (`DentalCustomerE2ETest`):**
  - Service catalog listing, category filtering, dynamic slot calculation, booking, deposit payment (F31).
  - Oral care product catalog, packaging options (single box, combo pack), SKU codes, featured products (F32).
  - Cart management (add, update quantity, remove) and order checkout (F32).
  - Porcelain crown warranty QR code and path token verification, FDI tooth positions, labo supplier, scan claiming (F33).
  - Loyalty point balances, tier thresholds (Standard, Silver, Gold, Diamond), rewards catalog, point redemption (F34).
  - AI dental diagnostic image upload, pathology classification (caries, tartar, gingivitis, wisdom teeth), health score, referral service (F35).
  - Community forum discussions, doctor verified replies, clinic branches GPS listing, navigation links (F36).
  - Mobile client payload contracts (F37).
- **Staff Track (`StaffAndOperationsE2ETest`):**
  - Tier-2 satellite clinic and B2B distributor registration, credit limits, commission rates (F38).
  - Dental material inventory (consumables, brackets, implants), safety stock tracking (F39).
  - Material procurement order creation, approval, dispatch, and receiving lifecycle (F39).
  - Staff attendance check-in/out with GPS and IP verification, shift schedule matching (F40).
  - Doctor consultation and revenue KPIs, conversion metrics, performance ranking (F41).
  - Field patient intake for school screening events, lead capture, voucher generation (F42).
- **Security Track (`MedicalSecurityE2ETest`):**
  - IDOR protection on medical records and appointments (F48).
  - Strict RBAC on articles, EMR image uploads, and IT Team command center (F48).
  - AES-256 GCM encryption of sensitive clinical fields (F49).
  - File upload whitelist (JPEG, PNG, WebP) and rejection of executables/scripts (F50).
  - Rate limiting enforcement and IP rotation verification (F51).
  - Security headers (nosniff, X-Frame-Options, HSTS, Referrer-Policy) and error suppression (F52).

### 3.2 Tier 2: Boundary Value Analysis (Corner & Extreme Cases)
Explores boundaries, extreme numbers, empty payloads, and invalid states:
- Zero, negative, or excessive values (negative stock, negative cart quantity, credit limit overflow).
- Non-existent IDs, foreign keys, or codes (404 Not Found handling).
- File size boundary (5MB limit: 5,242,880 bytes allowed vs 5,242,881 bytes rejected).
- Rate limiting threshold (60 requests allowed vs 61st request returning 429).
- Missing mandatory fields (empty phone, empty title, empty notes on order rejection).
- Token edge cases (expired JWT, malformed signatures, empty Bearer headers).

### 3.3 Tier 3: Pairwise Combinatorial (Cross-Feature Pipelines)
Tests multi-module interactions across system boundaries:
- **T3-CUS-01:** Service Catalog -> Slot Availability -> Dynamic Booking -> Deposit Payment.
- **T3-CUS-02:** Product Catalog -> Packaging Selection -> Cart -> Order Checkout -> Loyalty Point Crediting.
- **T3-CUS-03:** AI Dental Pathology Diagnostic -> Detected Caries -> Service Booking Referral.
- **T3-CUS-04:** Porcelain Crown QR Scan -> Warranty Details Verification -> Loyalty Scan Claim.
- **T3-CUS-05:** Community Forum Post -> Doctor Verified Reply -> Clinic Branch Navigation.
- **T3-OPS-01:** Satellite Clinic Procurement -> Central Approval -> Stock Decrement -> Agent Balance Sync.
- **T3-OPS-02:** School Screening Lead -> Voucher Issuance -> Dynamic Booking -> Doctor Consultation -> KPI Credit.
- **T3-OPS-03:** Staff Check-In On-Time -> Shift Completion -> Doctor Consultations -> Daily KPI Sync.
- **T3-OPS-04:** Satellite Clinic Registration -> Credit Limit Assignment -> First Order Creation.
- **T3-SEC-01:** Valid Patient Token + Spoofed X-Forwarded-For attempting IDOR on EMR -> Blocked.
- **T3-SEC-02:** Disguised File Extension (.png) with Embedded PHP Shell Script -> Rejected.
- **T3-SEC-03:** Receptionist Token attempting IT Team Command Center & Article Deletion -> 403.
- **T3-SEC-04:** SQL Injection and XSS Stored Payload in Article Content handled safely as literals.

### 3.4 Tier 4: Real-World Workload Scenarios (End-to-End User Journeys)
Full operational workflows simulating actual clinic operations:
- **T4-CUS-01 (New Patient Digital Onboarding):** AI Smile Scan -> Promotional Coupon Validation -> Dynamic Booking -> Deposit Payment -> Branch Directions.
- **T4-CUS-02 (Post-Treatment Care & Warranty):** Physical Porcelain Crown Warranty QR Scan -> Verification of Katana Zirconia / Detec Labo origin -> Loyalty Bonus Claim -> Points Redemption -> Oral-B Sonic Toothbrush Combo Order -> Home Delivery.
- **T4-OPS-01 (Full Operational Practice & Inventory Lifecycle):** Morning Staff Attendance Check-In -> Warehouse Inventory Audit -> Satellite Replenishment Order & Approval -> Evening Check-Out -> Nightly KPI Reconciliation.
- **T4-OPS-02 (Field School Screening Conversion Pipeline):** School Screening Intake -> Lead Recording with Pediatric Caries -> Voucher Issuance -> Parent Booking -> Receptionist Confirmation -> Appointment Status Update.
- **T4-SEC-01 (External Penetration Attack Simulation):** Reconnaissance -> Directory Enumeration -> Credential Stuffing Brute Force -> Malicious Shell Upload Attempt.
- **T4-SEC-02 (Compromised Patient Account Insider Threat):** Stolen Credentials Login -> Attempted IDOR Medical Record Harvesting -> Attempted Article Tampering -> Rogue Agent Registration Attempt.

---

## 4. Test Suite Inventory & Traceability Matrix

| Test Suite Class | Milestone | Functional Scope | Test Count | Key Features Covered |
| :--- | :---: | :--- | :---: | :--- |
| `DentalCustomerE2ETest.java` | M1 | Customer Dental Ecosystem | **55** | F31 (Catalog & Booking), F32 (E-Commerce & Packaging), F33 (Warranty QR), F34 (Loyalty), F35 (AI Diagnostic), F36 (Forum & Maps), F37 (Mobile App) |
| `StaffAndOperationsE2ETest.java` | M2 | Staff & B2B Operations | **39** | F38 (Satellite Clinics & Distributors), F39 (Materials & Procurement), F40 (Attendance & Shifts), F41 (Doctor KPIs), F42 (Field Intake & Screening) |
| `MedicalSecurityE2ETest.java` | M5 | 20 Enterprise Security Standards | **31** | F48 (IDOR & RBAC), F49 (AES-256 Medical Encryption), F50 (File Upload Validation), F51 (Rate Limiting), F52 (Security Headers & Suppression) |
| `ITTeamE2ETestSuite.java` | IT-Prev | IT Team Command Center | **73** | F01-F30 (Profiles, Memories, Messaging, Hashtag Engine, Activity Audit, Safe API Runner, Sanitizer) |
| **Total Automated E2E Tests** | | | **198 Tests** | **100% Coverage of Ecosystem Requirements** |

---

## 5. Execution Commands

### 5.1 Test Compilation Check
Verify all test suites compile without syntactic or class-level errors:
```bash
./mvnw test-compile
```
*(Windows PowerShell: `.\mvnw.cmd test-compile`)*

### 5.2 Execute Customer Dental E2E Suite
```bash
./mvnw test -Dtest=DentalCustomerE2ETest
```

### 5.3 Execute Staff & Operations E2E Suite
```bash
./mvnw test -Dtest=StaffAndOperationsE2ETest
```

### 5.4 Execute Medical Security E2E Suite
```bash
./mvnw test -Dtest=MedicalSecurityE2ETest
```

### 5.5 Execute Full E2E Test Suite
```bash
./mvnw test -Dtest=DentalCustomerE2ETest,StaffAndOperationsE2ETest,MedicalSecurityE2ETest,ITTeamE2ETestSuite
```
