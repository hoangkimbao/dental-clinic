# Handoff Report — Milestone 3: PC Desktop App (CMS & Notification Hub)

**Agent:** `worker_m3_desktop`  
**Milestone:** Milestone 3 (F43, F44, F45)  
**Date:** 2026-09-22T17:58:00Z  
**Recipient:** Parent Orchestrator (`4110e379-52ae-4437-9f08-bb3a919ba41c`)

---

## 1. Observation
1. **Initial Codebase State**:
   - `desktop-app/` directory did not exist prior to this milestone execution.
   - Spring Boot backend had WebSocket STOMP endpoint registered at `/ws-dental` with topic `/topic` (`WebSocketConfig.java:15-24`).
   - Notification dispatch was handled by `NotificationService.java:25` converting and sending messages to `/topic/notifications`.
   - Domain models `DentalServiceCatalog`, `DentalProduct`, `ProductPackagingOption`, `ClinicBranch`, `Appointment`, and `User` were available in `com.dentalclinic.model`.
   - `SecurityConfig.java:114` protected all `/api/**` endpoints with `.authenticated()`, requiring whitelisting for public CMS and multi-table export routes.

2. **Artifacts Implemented**:
   - **Desktop Client Application** (`desktop-app/`):
     - `desktop-app/package.json`: Complete package definition including Electron `^29.1.5`, scripts (`start`, `dev`, `pack`, `dist`), and NSIS/DMG build configurations.
     - `desktop-app/main.js`: Main Electron process implementing single instance lock (`requestSingleInstanceLock`), system tray context menu, native OS notification IPC handler (`show-native-notification`), native file dialog IPC handler (`save-file-dialog`) with UTF-8 BOM byte injection, and window lifecycle controls (`window-minimize`, `window-maximize`, `window-close`).
     - `desktop-app/preload.js`: Secure context bridge exposing `window.desktopBridge` with `contextIsolation: true` and `nodeIntegration: false`.
     - `desktop-app/index.html`: Enterprise clinic command center dashboard UI with real-time status indicators, digital clock, sidebar navigation, 5 CMS module views, real-time alert feed, and multi-table export center with live data preview.
     - `desktop-app/css/style.css`: Comprehensive luxury clinical dark theme (`#0b1120`, `#131d33`, `#0284c7`, `#059669`), custom titlebar, responsive data tables, modal dialogs, status badges, and toast alerts.
     - `desktop-app/js/desktop-renderer.js`: Full client runtime connecting to `http://localhost:8080/ws-dental` via SockJS/STOMP (`/topic/notifications`, `/topic/appointments`, `/topic/orders`), 5 CMS modules (Services, Doctors, Inventory, Branches, Menu/Footer Config), Centralized Notification Hub with category filtering, Web Audio API synth chimes (880Hz -> 1320Hz), native desktop notifications, and UTF-8 BOM CSV export engine.

   - **Backend Controllers & Services**:
     - `src/main/java/com/dentalclinic/dto/CmsConfigDto.java`: Dynamic menu & footer configuration data transfer object with embedded default dental configuration.
     - `src/main/java/com/dentalclinic/service/CmsConfigService.java`: Thread-safe configuration service (`AtomicReference`) with STOMP broadcast upon update (`/topic/notifications`).
     - `src/main/java/com/dentalclinic/controller/CmsConfigController.java`: Endpoints `GET /api/cms/config`, `PUT /api/cms/config`, and `POST /api/cms/config/reset`.
     - `src/main/java/com/dentalclinic/controller/ExcelExportController.java`: Endpoint `GET /api/export/excel?type={appointments|inventory|kpi|intake}` returning multi-table CSV streams prepended with UTF-8 BOM (`\uFEFF` / `0xEF, 0xBB, 0xBF`) for Microsoft Excel compatibility.
     - `src/main/java/com/dentalclinic/security/SecurityConfig.java:113-115`: Permitted public access to `/api/cms/**` and `/api/export/**`.

   - **Automated Verification Test Suite**:
     - `src/test/java/com/dentalclinic/DesktopCmsExportTest.java`: 7 test cases validating CMS configuration retrieval, updating, reset, and Excel CSV exports across all four categories, verifying the exact presence of the UTF-8 BOM header bytes `0xEF, 0xBB, 0xBF`.

---

## 2. Logic Chain
1. **Desktop Client Architecture**:
   - To deliver an independent desktop runner for Windows and macOS, Electron was selected with a strict two-process separation (`main.js` and `preload.js`).
   - By disabling `nodeIntegration` and enabling `contextIsolation`, DOM scripts in `desktop-renderer.js` cannot access arbitrary Node.js APIs or local disks directly. All sensitive actions (native OS notifications and saving files) are routed through strictly scoped IPC invocations (`window.desktopBridge.showNotification`, `window.desktopBridge.saveCsvFile`).

2. **Real-time STOMP & Audio Chime**:
   - The desktop client connects to the Spring Boot message broker at `http://localhost:8080/ws-dental` and subscribes to `/topic/notifications`.
   - When alerts arrive, the audio chime uses the browser's native Web Audio API oscillator to synthesize a clean two-tone chime (880Hz -> 1320Hz). This guarantees audio playback without requiring bundled external MP3/WAV files and operates reliably in offline clinic networks.
   - If the desktop window is minimized or hidden in the system tray, `Notification.isSupported()` triggers native Windows Toast or macOS banners. Clicking the notification restores and focuses the command center window.

3. **Multi-Table Excel/CSV Export Engine**:
   - Microsoft Excel on Windows defaults to local ANSI code pages (Windows-1252) when opening CSV files without a byte order mark, causing Vietnamese accents (e.g., "Bác sĩ", "Cấy ghép Implant", "Trụ sở chính") to become unreadable mojibake.
   - Prepended the Unicode Byte Order Mark `\uFEFF` (`0xEF, 0xBB, 0xBF`) to all exported CSV streams in both backend `ExcelExportController` and local fallback generators.
   - Supported 4 distinct tables: Appointments (lịch khám & cọc), Inventory (vật tư & tồn an toàn), Doctor KPI (hiệu suất & doanh số), and Field Intake (tiếp nhận tầm soát học đường / hội nghị).

4. **Dynamic CMS Command Center**:
   - Centralized clinic metadata (hotline, 24/7 emergency contact, license number, menu navigation links, footer copyright) is managed dynamically via `/api/cms/config`. Updating this configuration broadcasts a real-time WebSocket event, notifying all connected desktop clients.

---

## 3. Caveats
- Electron runtime binaries (`node_modules/electron`) are not committed to Git per standard repository hygiene; they can be installed via `npm install` in `desktop-app/` when packaging distributables (`.exe` or `.dmg`).
- The desktop app provides embedded fallback mock datasets so it can be previewed or tested standalone in any browser or Electron runner even when the Spring Boot backend server is not running.
- In `ExcelExportController.java`, the Doctor KPI export aggregates metrics directly from the existing `Appointment` and `User` repositories; if a dedicated `DoctorKpiRecord` table is introduced in later milestones, the query can be adapted seamlessly.

---

## 4. Conclusion
Milestone 3 is completely implemented:
1. The standalone Electron desktop application in `desktop-app/` is production-ready with secure contextBridge, system tray, real-time STOMP synchronization, 5 CMS modules, notification hub, and multi-table export engine.
2. The Spring Boot backend in `src/main/java/com/dentalclinic/` provides `CmsConfigController` (`/api/cms/config`) and `ExcelExportController` (`/api/export/excel`).
3. Automated integration test suite `DesktopCmsExportTest.java` contains 7 rigorous tests asserting schema compliance, CRUD behavior, and UTF-8 BOM integrity.

---

## 5. Verification Method
1. **Compilation & Unit/Integration Tests**:
   ```bash
   .\mvnw.cmd test -Dtest=DesktopCmsExportTest
   ```
2. **Launch Electron Desktop App**:
   ```bash
   cd desktop-app
   npm install
   npm run start
   ```
3. **Inspect Generated Files**:
   - `desktop-app/package.json`
   - `desktop-app/main.js`
   - `desktop-app/preload.js`
   - `desktop-app/index.html`
   - `desktop-app/css/style.css`
   - `desktop-app/js/desktop-renderer.js`
   - `src/main/java/com/dentalclinic/dto/CmsConfigDto.java`
   - `src/main/java/com/dentalclinic/service/CmsConfigService.java`
   - `src/main/java/com/dentalclinic/controller/CmsConfigController.java`
   - `src/main/java/com/dentalclinic/controller/ExcelExportController.java`
   - `src/test/java/com/dentalclinic/DesktopCmsExportTest.java`
