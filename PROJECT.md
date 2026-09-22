# Project: DentalCare Clinic — Comprehensive Dental Ecosystem & Enterprise Security

## Architecture
The DentalCare Clinic Ecosystem is an omnichannel enterprise dental platform built with:
- **Backend**: Spring Boot 3.2.5 (Java 17), Spring Data JPA, Spring Security (JWT), WebSocket STOMP (`/ws-dental`), H2 (dev) / PostgreSQL (prod).
- **Web Portal**: Responsive client & management web portal (`src/main/resources/static/`).
- **Mobile App**: React Native Expo hybrid application (`mobile-app/`) supporting both Patient and Staff roles.
- **PC Desktop App**: Standalone Electron / Desktop runner (`desktop-app/`) with WebSocket real-time sync, CMS Command Center, and Notification Hub.
- **Agrid / Analytics SDK**: Asynchronous, non-blocking user behavior tracking SDK (`agrid-sdk.js`) with 0ms initial render latency and `@Async` ingestion endpoint (`/api/analytics/**`).
- **Enterprise Medical Security**: 20 security standards including AES-256 encryption for clinical data, RBAC, RLS tenant isolation, DTO binding protection, MIME/magic byte upload validation, rate limiting, and security headers.

## Feature Inventory
| # | Feature | Description | Milestone | Source | Status |
|---|---------|-------------|-----------|--------|--------|
| F01-F30 | IT Team Command Center | Profiles, messaging, hashtag parser, activity audit, API monitor, sanitizer | IT-Prev | PREV_ORCH | DONE |
| F31 | Dental Service Catalog | Entity DentalServiceCatalog, dynamic booking endpoints, pricing, duration, category | M1 | ORIGINAL_REQUEST §1 | DONE |
| F32 | Dental E-Commerce & Cart | Product catalog, packaging options (box/combo), shopping cart, checkout | M1 | ORIGINAL_REQUEST §1 | DONE |
| F33 | Porcelain Crown Warranty & QR | PorcelainCrownWarranty entity, QR verification, FDI tooth numbering, labo origin | M1 | ORIGINAL_REQUEST §1 | DONE |
| F34 | Dental Loyalty Program | LoyaltyAccount, point accumulation, tiering, redemption rules | M1 | ORIGINAL_REQUEST §1 | DONE |
| F35 | AI Dental Diagnostic Vision | Pathology detection (caries, tartar, gingivitis, wisdom teeth) with 9Router & deterministic fallback | M1 | ORIGINAL_REQUEST §1 | DONE |
| F36 | Dental Forum & Clinic Map | Community discussion forum & multi-branch directory with GPS turn-by-turn directions | M1 | ORIGINAL_REQUEST §1 | DONE |
| F37 | Customer Mobile App Experience | Dual-role Expo app adding patient tabs: Services, Booking, E-Commerce, Warranty, AI Diagnostic | M1 | ORIGINAL_REQUEST §1 | DONE |
| F38 | B2B Tier-2 Agent & Satellite Clinics | Tier2Agent entity, satellite clinic directory, partner distributor management | M2 | ORIGINAL_REQUEST §2 | DONE |
| F39 | Dental Inventory & Material Orders | DentalMaterial (brackets, implants, consumables), procurement order & approval workflow | M2 | ORIGINAL_REQUEST §2 | DONE |
| F40 | Staff Timekeeping & Shift Check-In | StaffAttendance entity, GPS/IP verified check-in/out, shift matching | M2 | ORIGINAL_REQUEST §2 | DONE |
| F41 | Doctor Consultation & Revenue KPIs | DoctorKpiRecord entity, consultation counts, treatment conversions, revenue attribution | M2 | ORIGINAL_REQUEST §2 | DONE |
| F42 | Field Patient Intake & School Screening | FieldPatientIntake entity, event screening leads, offline sync capability | M2 | ORIGINAL_REQUEST §2 | DONE |
| F43 | Standalone PC Desktop App Runner | Electron desktop client in desktop-app/, contextIsolation, system tray, real-time STOMP sync | M3 | ORIGINAL_REQUEST §3 | DONE |
| F44 | Desktop CMS Command Center | Dynamic CMS for Services, Doctors, Inventory, Branches, Menu & Footer configuration | M3 | ORIGINAL_REQUEST §3 | DONE |
| F45 | Notification Hub & Excel/CSV Export | Centralized desktop alert panel, sound chimes, UTF-8 BOM Excel/CSV multi-table export | M3 | ORIGINAL_REQUEST §3 | DONE |
| F46 | Agrid / Analytics Tracking SDK | Cross-platform agrid-sdk.js, non-blocking sendBeacon, micro-batching (5s/10 events), 0ms render latency | M4 | ORIGINAL_REQUEST §4 | DONE |
| F47 | Analytics Ingestion & Privacy Guard | @Async /api/analytics/events endpoint, AnalyticsEvent entity, medical PII redaction | M4 | ORIGINAL_REQUEST §4 | DONE |
| F48 | Security: IDOR & RBAC Hardening | Fix MedicalRecordController IDOR, lock down /api/articles/** & /api/emr/images/** | M5 | ORIGINAL_REQUEST §5 | DONE |
| F49 | Security: AES-256 Medical Encryption | JPA AttributeConverter AES-256 GCM for diagnosis, treatment, prescription | M5 | ORIGINAL_REQUEST §5 | DONE |
| F50 | Security: File Upload Validation | Whitelist image MIME types (png, jpg, webp), magic bytes inspection, 5MB limit | M5 | ORIGINAL_REQUEST §5 | DONE |
| F51 | Security: Rate Limiting & Bot Throttling | Brute-force protection, account lockout, honeypot/captcha on public endpoints | M5 | ORIGINAL_REQUEST §5 | DONE |
| F52 | Security: Headers & Secrets Protection | CSP, HSTS, X-Frame-Options, externalized secrets, disable public h2-console | M5 | ORIGINAL_REQUEST §5 | DONE |
| F53 | E2E Automated Test Suite Pass | 100% pass across Tiers 1-4 for Customer, Staff, Desktop, Analytics, and 20 Security Standards | M6 | ORIGINAL_REQUEST §Acceptance | PLANNED |
| F54 | Adversarial Hardening & Forensic Audit | Tier 5 adversarial tests, binary forensic integrity verification | M6 | ORIGINAL_REQUEST §Acceptance | PLANNED |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Customer Dental Ecosystem (Mobile & Web) | F31, F32, F33, F34, F35, F36, F37 | none | DONE |
| M2 | Staff, B2B Tier-2 Agent & EMR/Field Ops | F38, F39, F40, F41, F42 | M1 | DONE |
| M3 | PC Desktop App (CMS & Notification Hub) | F43, F44, F45 | M1, M2 | DONE |
| M4 | Agrid / Analytics Tracking SDK | F46, F47 | none | DONE |
| M5 | 20 Enterprise Medical Security Standards | F48, F49, F50, F51, F52 | M1, M2, M3, M4 | DONE |
| M6 | Final E2E Test Suite Pass & Hardening | F53, F54 | M1, M2, M3, M4, M5 | IN_PROGRESS |

## Interface Contracts
### M1: Customer Dental Experience
- Entities: `DentalServiceCatalog`, `DentalProduct`, `ProductPackagingOption`, `DentalOrder`, `PorcelainCrownWarranty`, `LoyaltyAccount`, `AiDentalDiagnosticLog`, `DentalCommunityPost`, `ClinicBranch`.
- REST Endpoints:
  - `GET /api/dental-services`: Public catalog list
  - `GET /api/dental-products`: Public product list with packaging options
  - `POST /api/dental-orders`: Place product order
  - `GET /api/warranties/verify?code=...`: Public warranty lookup
  - `POST /api/dental-ai/diagnose`: Image upload & oral pathology diagnostic result
  - `GET /api/forum/posts` & `POST /api/forum/posts`: Community discussions
  - `GET /api/branches`: Clinic locations with GPS coordinates & amenities

### M2: Staff & B2B Tier-2 Agent Operations
- Entities: `Tier2Agent`, `DentalMaterial`, `MaterialOrder`, `StaffAttendance`, `DoctorKpiRecord`, `FieldPatientIntake`.
- REST Endpoints:
  - `GET /api/tier2-agents` & `POST /api/tier2-agents`: Satellite clinics & distributors
  - `GET /api/dental-materials` & `PUT /api/dental-materials/{id}/stock`: Material inventory
  - `POST /api/material-orders` & `PUT /api/material-orders/{id}/status`: Procurement orders & approvals
  - `POST /api/attendance/check-in` & `POST /api/attendance/check-out`: Staff attendance
  - `GET /api/staff-kpi/doctor/{id}`: Doctor consultation & conversion KPIs
  - `POST /api/field-intake`: Conference & school screening patient capture

### M3: PC Desktop App
- Client Directory: `desktop-app/` (Electron main process, preload script, renderer UI).
- Real-time STOMP: `/ws-dental`, subscribe to `/topic/notifications`, `/topic/appointments`, `/topic/orders`.
- REST Endpoints:
  - `GET /api/cms/config` & `PUT /api/cms/config`: Navigation & footer configuration
  - `GET /api/export/excel?type={appointments|inventory|kpi|intake}`: UTF-8 BOM CSV / Excel streams

### M4: Agrid / Analytics SDK
- Client SDK: `src/main/resources/static/js/agrid-sdk.js`
- API Endpoint: `POST /api/analytics/events` (returns HTTP 202 Accepted, non-blocking `@Async`)
- Schema: `{ eventType, eventName, pageUrl, referrer, clientSessionId, timestamp, metadata }`
- Sanitization: Rejects/masks any field containing passwords, tokens, CCCD, or medical diagnosis text.

### M5: 20 Enterprise Security Checklist
- AES-256 Attribute Converter: `com.dentalclinic.security.crypto.Aes256GcmAttributeConverter`
- Access Control: `SecurityConfig.java` enforcing strict RBAC (`ROLE_OWNER`, `ROLE_ADMIN`, `ROLE_DENTIST`, `ROLE_RECEPTIONIST`, `ROLE_PATIENT`, `ROLE_AGENT`).
- File Upload: Whitelist `image/jpeg`, `image/png`, `image/webp`, magic byte checks, 5MB limit.
- Security Headers: `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: SAMEORIGIN`, `Referrer-Policy: strict-origin-when-cross-origin`.

## Code Layout
- Backend Source: `src/main/java/com/dentalclinic/`
  - `model/`: Domain entities extending `BaseEntity`
  - `repository/`: Spring Data JPA repositories
  - `service/`: Transactional business logic
  - `controller/`: REST API controllers
  - `dto/`: Request/Response data transfer objects
  - `security/`: JWT, crypto, filters, security headers
- Static Web Assets: `src/main/resources/static/`
  - `index.html`, `js/app.js`, `js/agrid-sdk.js`, `js/it-team.js`
- Mobile Application: `mobile-app/`
- PC Desktop Application: `desktop-app/`
- Test Suites: `src/test/java/com/dentalclinic/`
