# Handoff Report: Staff/Agent Operations, PC Desktop App & Analytics SDK

**Agent:** `explorer_survey_staff_pc`  
**Parent Agent:** `parent` (`4110e379-52ae-4437-9f08-bb3a919ba41c`)  
**Working Directory:** `D:\java\dental-clinic\.agents\explorer_survey_staff_pc`  
**Date:** 2026-09-22T17:27:00Z  
**Handoff Type:** Hard (Survey and Investigation Task Complete)

---

## 1. Observation

Direct observations from codebase inspection across backend, frontend, mobile app, and configurations:

1. **Staff & Roles Model Baseline**:
   - `D:\java\dental-clinic\src\main\java\com\dentalclinic\model\Role.java:3-11`:
     ```java
     public enum Role {
         ROLE_OWNER, ROLE_ADMIN, ROLE_RECEPTIONIST, ROLE_DENTIST, ROLE_ASSISTANT, ROLE_CLEANER, ROLE_PATIENT
     }
     ```
     *Observed*: Lacks `ROLE_AGENT`, `ROLE_DISTRIBUTOR`, or `ROLE_SATELLITE_CLINIC`.
   - `D:\java\dental-clinic\src\main\java\com\dentalclinic\model\StaffShift.java:8-30`:
     Entity contains `staff` (`User`), `shiftDate` (`LocalDate`), `shiftType` (`ShiftType`: `CA_SANG_8H_12H`, `CA_CHIEU_13H_17H`, `CA_TOI_17H_20H`, `CA_FULL_NGAY`), `roleTitle`, `status` (`ShiftStatus`: `SCHEDULED`, `COMPLETED`, `ABSENT`), and `notes`.
   - `D:\java\dental-clinic\src\main\java\com\dentalclinic\controller\ShiftController.java:30-48`:
     Only has basic `GET /api/shifts` and `POST /api/shifts` for scheduling. Lacks timekeeping/check-in, GPS/IP validation, and doctor revenue/consultation KPI tracking.
   - Codebase scan across `src/main/java/com/dentalclinic/model/` (20 entities):
     *Observed negative finding*: Completely lacks entities for B2B Tier-2 agents (`Tier2Agent`), dental materials inventory (`DentalMaterial`), material orders & approvals (`MaterialOrder`, `MaterialOrderItem`), staff attendance/timekeeping (`StaffAttendance`), doctor KPIs (`DoctorKpiRecord`), and field screening leads (`FieldPatientIntake`).

2. **Mobile Staff App Baseline**:
   - `D:\java\dental-clinic\mobile-app/package.json:2-18`:
     React Native Expo app (`name`: `dental-staff-app`, `expo`: `^57.0.0`, `react`: `19.2.3`, `react-native`: `0.86.3`).
   - `D:\java\dental-clinic\mobile-app/App.js:33-149` & `src/services/api.js:57-103`:
     Implements staff login, appointment filtering, appointment status update, medical records view, orthodontic plans view, shift listing, and CSV export for appointments. Lacks B2B inventory management, check-in timekeeping, and field screening intake flows.

3. **PC Desktop App Baseline**:
   - Filesystem scan of `D:\java\dental-clinic`:
     *Observed negative finding*: No standalone PC desktop codebase currently exists (no `desktop-app/`, no Electron configs, no WebView2 runner).
   - Real-time infrastructure in Spring Boot:
     - `D:\java\dental-clinic\src\main\java\com\dentalclinic\websocket\WebSocketConfig.java:10-24`:
       STOMP broker configured on `/topic`, application prefix `/app`, SockJS endpoint `/ws-dental`.
     - `D:\java\dental-clinic\src\main\java\com\dentalclinic\service\NotificationService.java:23-26`:
       Pushes notifications via `messagingTemplate.convertAndSend("/topic/notifications", saved)`.
   - Excel/CSV export capabilities:
     - `D:\java\dental-clinic\src\main\java\com\dentalclinic\controller\AppointmentController.java:110-129` & `AppointmentService.java:260-298`:
       Exports appointments to CSV with UTF-8 BOM (`\uFEFF`) and CSV cell escaping. Lacks export for inventory, timesheets, doctor KPIs, and field intake leads.

4. **User Behavior Tracking SDK Baseline**:
   - `D:\java\dental-clinic\src\main\resources\static\js\app.js:38-44`:
     ```javascript
     function trackGaEvent(eventName, params = {}) {
         if (typeof gtag === 'function') {
             gtag('event', eventName, params);
             console.log(`📊 [Google Analytics] Event tracked: ${eventName}`, params);
         }
     }
     ```
     *Observed negative finding*: Only a rudimentary 5-line stub checking `window.gtag`. No standalone cross-platform SDK (`agrid-sdk.js`) exists. No backend ingestion endpoint (e.g. `/api/analytics/events`) or `AnalyticsEvent` JPA entity exists.
   - `D:\java\dental-clinic\src\main\java\com\dentalclinic\config\RateLimitingFilter.java:18-58`:
     Applies strict sliding-window rate limit: 60 requests per 10s window per IP returning HTTP 429.

---

## 2. Logic Chain

1. **From Observation 1 (Role & Staff Model Gaps) to B2B Data Model Specification**:
   - Because `Role.java` only contains clinic-internal roles and `model/` contains no inventory or distributor entities, supporting Tier-2 satellite clinics and dental distributors requires adding:
     - Domain entities: `Tier2Agent` (satellite clinics & distributors), `DentalMaterial` (consumables, brackets, implant posts), and `MaterialOrder` / `MaterialOrderItem` (procurement & approval lifecycle).
     - State machine: `DRAFT -> PENDING_APPROVAL -> APPROVED -> DISPATCHED -> RECEIVED / REJECTED`.
     - Timekeeping and KPI entities: `StaffAttendance` (GPS/IP check-in/out) and `DoctorKpiRecord` (consultations, conversion to ortho/implant, revenue attribution).
     - Field intake entity: `FieldPatientIntake` (conference & school screening leads with offline sync).

2. **From Observation 2 & 3 (Desktop Absence & WebSocket Infrastructure) to PC App Architecture**:
   - Spring Boot already possesses a functional STOMP broker at `/ws-dental` and pushes to `/topic/notifications`.
   - Therefore, a standalone PC Desktop App built on Electron with secure preload context bridge (`contextIsolation: true`) can directly connect to `/ws-dental` via SockJS + STOMP.
   - The desktop client can register desktop system tray, background audio chimes, native OS toast notifications on appointment/order alerts, and silent printing for thermal receipts.
   - For CMS Command Center, 6 sub-modules are required to eliminate hardcoded elements in `index.html`: (1) Dental Services CMS, (2) Doctor Directory CMS, (3) Medical Supply Inventory CMS, (4) Branch/Agent C2 Management, (5) Menu/Footer Config, and (6) Operations Hub.
   - The proven UTF-8 BOM CSV export pattern in `AppointmentService.java:263` (`\uFEFF`) should be generalized into an export engine across all clinic tables.

3. **From Observation 4 (Stub Tracking & Rate Limiting) to Analytics SDK Design**:
   - Because `RateLimitingFilter` throttles at 60 req / 10s per IP, sending tracking events one-by-one via standard HTTP POST will risk triggering HTTP 429 and stalling client navigation.
   - To achieve **0ms initial render latency and non-blocking transport**:
     - The SDK must load with `async` / `defer` and initialize on `requestIdleCallback`.
     - Events must be buffered in memory and flushed via micro-batches (every 5 seconds or 10 events).
     - Transport must utilize `navigator.sendBeacon()`, which executes in the browser background thread without blocking UI or delaying unload.
     - Strict client-side sanitization must filter out JWTs, passwords, CCCD/CMND, credit cards, and medical diagnosis/EMR text before events enter the queue.
     - Backend needs `AnalyticsController` with `@Async` execution to reply HTTP 202 Accepted in `< 5ms`.

---

## 3. Caveats

1. **No Source Code Modified (Explorer Read-Only Discipline)**:
   - In accordance with explorer instructions, no Java, JavaScript, or configuration files were modified during this investigation. All architectural models and specifications are documented in `report.md`.
2. **Localhost Execution & User Permission**:
   - Powershell command execution prompted for user permission and timed out; investigation was conducted strictly via deterministic file reading and directory inspection tools.
3. **Database Schema Auto-Generation Assumption**:
   - `application.yml` uses `ddl-auto: update` on H2 file database `./data/dentaldb`. Adding new JPA entities extending `BaseEntity` will automatically create the tables on boot. In production PostgreSQL (`docker-compose.yml`), SQL migration scripts or DDL sync should be validated.

---

## 4. Conclusion

1. **Staff & B2B Tier-2 Operations**:
   - The existing shift system is operational for basic scheduling but entirely lacks B2B distributor/satellite clinic modeling, inventory tracking for brackets/implants/consumables, material order approval workflows, shift timekeeping (check-in/out), doctor KPIs, and field screening intake.
   - All 6 necessary entities, DTOs, and state machine workflows have been fully specified in `report.md` Section 2.
2. **PC Desktop App**:
   - An independent Electron desktop client (`desktop-app/`) with WebSocket real-time sync (`/ws-dental`), native OS notifications, CMS Command Center (6 sub-views), and UTF-8 BOM Excel/CSV export has been fully architected in `report.md` Section 3.
3. **User Behavior Tracking SDK (Agrid / Analytics SDK)**:
   - A unified cross-platform SDK for Web, Mobile, and PC Desktop has been designed with 0ms initial render latency, micro-batching, non-blocking `navigator.sendBeacon`, offline `localStorage` queue, strict medical PII redaction, and an `@Async` backend ingestion endpoint, detailed in `report.md` Section 4.
4. **Actionable Roadmap**:
   - A 5-milestone implementation plan is formulated in `report.md` Section 6, ready for assignment to backend, frontend, desktop, and mobile implementation agents.

---

## 5. Verification Method

To independently verify the observations, logic, and conclusions:

1. **Verify Report Artifacts**:
   - Inspect `D:\java\dental-clinic\.agents\explorer_survey_staff_pc\report.md` (comprehensive 6-section report).
   - Inspect `D:\java\dental-clinic\.agents\explorer_survey_staff_pc\progress.md` and `BRIEFING.md`.
2. **Verify Codebase Alignment**:
   - Check `src/main/java/com/dentalclinic/model/` to confirm absence of `Tier2Agent`, `DentalMaterial`, `MaterialOrder`, and `AnalyticsEvent`.
   - Check `src/main/java/com/dentalclinic/websocket/WebSocketConfig.java` to confirm SockJS endpoint `/ws-dental` and broker `/topic`.
   - Check `src/main/resources/static/js/app.js:38-44` to confirm existing stub `trackGaEvent`.
   - Check `src/main/java/com/dentalclinic/service/AppointmentService.java:260-298` to confirm UTF-8 BOM CSV implementation.
   - Check `mobile-app/package.json` and `mobile-app/App.js` to confirm Expo staff app capabilities.
3. **Invalidation Conditions**:
   - If any new entity in `com.dentalclinic.model` uses Lombok annotations, compilation will fail because Lombok is not in `pom.xml`.
   - If analytics tracking does not batch events, the existing `RateLimitingFilter` (60 req/10s) will trigger HTTP 429 rate limit errors.
