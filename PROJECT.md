# Project: DentalCare Clinic Luxury Healthcare Multi-Platform & DevOps Ecosystem

## Architecture
DentalCare Clinic is an enterprise healthcare management ecosystem comprising:
1. **Spring Boot Backend** (`src/main/java/`):
   - Java 17, Spring Boot 3.x, Spring Security (JWT), Spring Data JPA (H2 persistent database file mode `/app/data/dentaldb`), WebSocket STOMP messaging (`/ws-dental`).
   - 36 REST controllers covering appointments, doctors, EMR, dental services, e-commerce orders, warranty certificates, loyalty points, shifts, attendance, inventory, AI vision diagnostics, and Excel/CSV data exports.
2. **Web Portal (Luxury Healthcare SPA & Standalone Pages)** (`src/main/resources/static/`):
   - Dual-mode SPA in `index.html`: `#public-landing-page` (Patient facing) and `#management-portal` (Staff/Admin RBAC 12 tabs).
   - Standalone landing/sub-pages: `booking.html`, `dashboard.html`.
   - Native SVG vector iconography in `js/dental-icons.js` (Diamond Smile logo, feature badges, service icons, 0% installment partner logos).
   - Responsive CSS with luxury theme (Medical Emerald/Teal `#0d9488`, Navy `#0f172a`, Gold `#d97706`).
3. **PC Desktop Application** (`desktop-app/`):
   - Electron 29 runner (`main.js`, `preload.js`, `index.html`, `js/desktop-renderer.js`).
   - CMS Command Center for doctors, services, inventory, and branch configuration.
   - Real-time Notification Hub via STOMP WebSocket (`/ws-dental`) with Web Audio API chime.
   - UTF-8 BOM CSV / Excel export engine.
4. **Mobile Application** (`mobile-app/`):
   - React Native Expo SDK 57 (`App.js`, `src/services/api.js`).
   - Dual-role ecosystem: Patient Mode (Services, Booking, Cart/Shop, Warranty QR, AI Diagnosis, Forum) and Staff Mode (Appointments, EMR, Shifts, Attendance GPS/IP, Field Intake, Revenue Stats).
5. **DevOps & Cloudflare VPS Pipeline**:
   - Multi-stage Dockerfile (Maven builder + Eclipse Temurin 17 JRE runtime, non-root user).
   - `docker-compose.yml` orchestrating `dental-clinic-app` and `dental-clinic-tunnel` (cloudflared).
   - Automated deployment and sync scripts (`deploy-all.sh`, `vps-sync.sh`) with health checks, zero-downtime reloads, and auto-recovery.

---

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Web Iconography SVG Purge | Remove all 230+ FontAwesome `<i class="fa-*">` tags & JS templates; replace 100% with inline SVG / DentalIcons to eliminate Tofu boxes | M1 | Survey 1, Follow-up 2026-09-30 |
| 2 | AI Vision Diagnostic Endpoint Fix | Correct URL in `app.js` line 2996 from `/api/ai/diagnose` to `/api/dental-ai/diagnose` to eliminate HTTP 404 in console | M1 | Survey 1, Acceptance Criteria |
| 3 | Standalone Pages API Integration | Wire `booking.html` and `dashboard.html` to real Spring Boot endpoints (`/api/appointments`, `/api/dentists`, `/api/dashboard/stats`) | M1 | Survey 1, R1 |
| 4 | PWA Service Worker & Static Assets | Update `sw.js` precache list (`dentalcare-redesign.css`, `dental-icons.js`), delete `index.html.bak` and unused CSS | M1 | Survey 1, R1 |
| 5 | Web Luxury UI & Responsive Standards | Verify 0% horizontal overflow (360px-430px), 44px touch targets, modal ESC listener and backdrop overlay | M1 | Survey 1, R1, R2, R5 |
| 6 | Desktop App Dependency & Offline Support | Configure `desktop-app/package.json`, vendorize SockJS/StompJS and FontAwesome locally, create `run.bat` | M2 | Survey 2, R2 |
| 7 | Desktop CMS Command Center & Export | Ensure CMS services/doctors/inventory/branches and UTF-8 BOM CSV/Excel export function smoothly with Spring Boot APIs | M2 | Survey 2, R2 |
| 8 | Desktop STOMP Notification Hub | Verify WebSocket `/ws-dental` listener on `/topic/notifications`, `/topic/appointments`, `/topic/orders` with Web Audio chime | M2 | Survey 2, R2 |
| 9 | Mobile App Fatal Syntax Error Fix | Fix unclosed JSX in `mobile-app/App.js` line 596-598 (`if (!isLoggedIn)`) to resolve Babel parse failure | M3 | Survey 2, R3 |
| 10 | Mobile Dual Roles & Forum Integration | Render Dental Forum/News tab in Patient/Staff mode using `api.js` forum functions; ensure smooth touch UX | M3 | Survey 2, R3 |
| 11 | Mobile Camera Scanner Simulation | Add camera/picker simulation for Porcelain Warranty QR lookup and AI Oral Scanner | M3 | Survey 2, R3 |
| 12 | Mobile App Branding & Assets Config | Update `app.json` branding to "DentalCare Clinic", package name, and provide clean asset placeholders | M3 | Survey 2, R3 |
| 13 | Cloudflare Tunnel Healthcheck in Compose | Add `--metrics 0.0.0.0:2000` and Docker healthcheck probing `/ready` to `dental-clinic-tunnel` in `docker-compose.yml` | M4 | Survey 3, R4 |
| 14 | Ingress Hardening & Custom Domain Support | Bind port 8080 to `127.0.0.1:8080:8080`; parameterize `CLOUDFLARE_TUNNEL_TOKEN` for `nhakhoadentalcare.id.vn` | M4 | Survey 3, R4 |
| 15 | VPS Auto-Sync & Deployment Script | Implement `vps-sync.sh` with concurrency lock, git pull, zero-downtime rebuild, health validation, and rollback | M4 | Survey 3, R4 |
| 16 | Deploy Script Headless Support | Enhance `deploy-all.sh` to extract latest tunnel URL (`tail -n 1`) and bypass GUI runner on headless servers | M4 | Survey 3, R4 |
| 17 | Environment Variable Blueprint | Provide `.env.example` with documented keys for database, JWT, ports, and Cloudflare tokens | M4 | Survey 3, R4 |
| 18 | Dual Track E2E Verification & Gate Audit | Comprehensive opaque-box and white-box test verification across all tiers, ending in forensic audit gate | M5 | Project Pattern, Acceptance Criteria |

---

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Web Portal Luxury UI & REST Fixes | Features 1, 2, 3, 4, 5: SVG icon purge, AI endpoint fix, standalone pages REST wiring, sw.js, UI responsiveness | none | DONE |
| M2 | Desktop App CMS & Notification Hub | Features 6, 7, 8: Electron offline dependencies, CMS Command Center, STOMP notification, UTF-8 BOM CSV | none | DONE |
| M3 | Mobile Expo App Fix & Enhancement | Features 9, 10, 11, 12: Fix fatal syntax error in App.js, forum tab, camera QR/AI simulation, branding | none | DONE |
| M4 | DevOps Cloudflare VPS Pipeline | Features 13, 14, 15, 16, 17: docker-compose healthcheck, 127.0.0.1 binding, vps-sync.sh, deploy-all.sh, .env.example | none | DONE |
| M5 | E2E System Verification & Gate Audit | Feature 18: Build & test suite verification, Reviewer approval, Challenger verification, Forensic Audit pass | M1, M2, M3, M4 | DONE |

---

## Interface Contracts
### Web / Desktop / Mobile ↔ Spring Boot Backend
- **Base REST URL**: `http://localhost:8080` (or Cloudflare Tunnel URL in production)
- **AI Vision Diagnostic**: `POST /api/dental-ai/diagnose` (Accepts multipart/form-data or JSON with image/symptoms, returns diagnosis payload)
- **Appointments**:
  - `GET /api/appointments/available-slots?date={YYYY-MM-DD}&dentistId={id}`
  - `POST /api/appointments/book` (Payload: `{ customerName, phone, serviceId, dentistId, appointmentDate, timeSlot, notes }`)
- **Dashboard Stats**: `GET /api/dashboard/stats` (Returns `{ totalAppointments, todayPatients, revenueMonth, pendingCases, ... }`)
- **WebSocket STOMP Endpoint**: `ws://localhost:8080/ws-dental`
  - Subscriptions: `/topic/notifications`, `/topic/appointments`, `/topic/orders`
- **Excel/CSV Export**: `GET /api/export/excel/{type}` where type in `[appointments, inventory, kpi, intake]`

---

## Code Layout
- `src/main/resources/static/` — Web Portal static assets (HTML, CSS, JS, images, SVG icons, PWA manifest/worker) [Worker M1 - DONE]
- `desktop-app/` — Electron Desktop application runner, preload, UI renderer, package.json [Worker M2 - DONE]
- `mobile-app/` — React Native Expo application, App.js, services, components [Worker M3 - DONE]
- `docker-compose.yml`, `deploy-all.sh`, `vps-sync.sh`, `.env.example` — DevOps and deployment automation [Worker M4 - DONE]
- `src/main/java/com/dentalclinic/` — Spring Boot backend application
- `src/test/java/com/dentalclinic/` — Backend automated test suites
- `.agents/teamwork/` — Agent coordination metadata and reports only (never source code)
