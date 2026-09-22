# Frontend Architecture & Management Portal Survey Report

**Project**: DentalCare Clinic Management Portal — "IT Team Command Center"  
**Author**: Frontend Architecture Explorer  
**Date**: 2026-09-12  
**Working Directory**: `D:\java\dental-clinic\.agents\explorer_survey_frontend`  
**Target Application**: `D:\java\dental-clinic`  

---

## 1. Executive Summary

A comprehensive investigation was conducted on the frontend architecture of the DentalCare Clinic application at `D:\java\dental-clinic`. 

Key conclusions:
1. **Frontend Architecture**: The web application is **NOT** Thymeleaf, nor an external React/Vue/Angular web SPA. It is a **pure Single Page Application (SPA) using vanilla modern ES6+ JavaScript, HTML5, and Tailwind CSS v3.4.17**, served statically by Spring Boot 3 (`src/main/resources/static/`).
2. **Management Portal UI**: Located inside `src/main/resources/static/index.html` under `<div id="management-portal">`, toggled via `togglePortal()` in `static/js/app.js`. It features an RBAC-driven workspace that renders tabs dynamically based on user role (`ROLE_OWNER`, `ROLE_RECEPTIONIST`, `ROLE_DENTIST`, `ROLE_ASSISTANT`, `ROLE_CLEANER`, `ROLE_PATIENT`).
3. **Design System**: Built on Tailwind CSS v3.4.17 with an inlined critical CSS layer, augmented by `static/css/clinic-ui-refresh.css` and FontAwesome 6.4.0 icons. The Management Portal uses a distinct **dark luxury slate/teal theme** (`#102f31`, `bg-slate-900`, `bg-slate-800`, `border-slate-700`, `text-brand-400`).
4. **Authentication & API Client**: Centrally orchestrated by `apiFetch(url, options)` in `app.js`. JWT Bearer tokens are persisted in `sessionStorage` (or `localStorage` when "Remember Me" is selected) under key `DENTAL_USER`. Backend responses adhere strictly to the `ApiResponse<T>` envelope.
5. **IT Team Command Center Integration**: Can be integrated with **zero regression** by adding a dedicated tab button `#tab-itteam` in the portal navigation bar and a new section container `#section-itteam` containing 5 sub-views (Nhân Sự, Hội Thoại with hashtag autocomplete, Bộ Nhớ, Nhật Ký Thao Tác, API Monitor). A modular companion script (`static/js/it-team.js`) or clean extension to `app.js` can cleanly encapsulate all IT Team operations while reusing existing `apiFetch`, `showToast`, and auth state.

---

## 2. Frontend Architecture & Technology Stack

### 2.1 Technology Matrix

| Dimension | Specification in DentalCare | Verification Source |
| :--- | :--- | :--- |
| **Server-Side Template Engine** | **None** (No Thymeleaf, no JSP, no FreeMarker) | `pom.xml` (no `spring-boot-starter-thymeleaf`); `src/main/resources/templates` does not exist |
| **Client Framework** | **Vanilla ES6+ JavaScript** (No React/Vue/Angular on web) | `src/main/resources/static/js/app.js` (1,658 lines of modular vanilla JS) |
| **Auxiliary Mobile App** | React Native / Expo (Separate project for mobile only) | `mobile-app/` directory (separate app, not part of web portal) |
| **CSS Framework** | **Tailwind CSS v3.4.17** | `static/css/tailwind.min.css` & inlined `<style id="critical-styles">` |
| **Custom Styling Layer** | **DentalCare Luxury UI Refresh CSS** | `static/css/clinic-ui-refresh.css` (custom CSS variables & teal theme) |
| **Iconography** | **FontAwesome 6.4.0** | Linked via CDN `<link rel="preload" href="...font-awesome/6.4.0/css/all.min.css">` |
| **Typography** | `Plus Jakarta Sans` (UI) & `Playfair Display` (Luxury accents) | Google Fonts loaded asynchronously in `<head>` |
| **Chart Visualization** | **Chart.js** (Loaded on-demand for staff) | Dynamically loaded via `ensurePortalLibraries()` in `app.js:15-27` |
| **Realtime WebSockets** | **SockJS + STOMP over WebSocket** (`/ws-dental`) | `app.js:555-576` (staff-only notification channel) |
| **AI Integration** | **9Router Local Instance** (`http://localhost:20128`) | Already integrated for AI blog generation in `AiBlogService.java` & `app.js:1464-1565` |

### 2.2 Serving Pipeline

Spring Boot 3 serves static assets directly from `src/main/resources/static/`.
In `SecurityConfig.java:72`:
```java
.requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico", 
                 "/sitemap.xml", "/robots.txt", "/uploads/**").permitAll()
```
The entire application loads on single initial request to `/` or `/index.html`. No build step or node server is needed at runtime; changes to `index.html`, `app.js`, and `css/` take immediate effect upon page refresh.

---

## 3. Existing Management Portal UI Analysis

### 3.1 Dual-Mode Architecture

The page `index.html` hosts two mutually exclusive top-level view containers:
1. `<div id="public-landing-page">` (Lines 270–1400): Public patient portal with Hero, Service breakdown, Transformation carousel, Doctor profiles, Promotions, Pricing calculator, Booking form with QR deposit, and Medical Handbook.
2. `<div id="management-portal">` (Lines 1403–1687): The internal staff & patient dashboard workspace.

The transition between modes is handled by `togglePortal(forceOpen)` in `app.js:331-356`:
- Authenticated user clicks `#btn-toggle-portal` ("Khu Vực Làm Việc" or "Hồ Sơ Của Tôi").
- Adds `.hidden` to `#public-landing-page`, removes `.hidden` from `#management-portal`.
- Hides the floating contact widget `#floating-contact-widget`.
- Calls `renderDynamicRoleView()` to configure tabs according to role.
- Clicking "Ra Trang Chủ" or `showLandingPage()` reverses the state.

### 3.2 Portal Header & Sub-Bar Structure

The portal subheader (`index.html:1406-1422`):
```html
<div class="bg-slate-950 px-4 sm:px-6 lg:px-8 py-3.5 border-b border-slate-800 flex flex-wrap justify-between items-center gap-4">
    <div class="flex items-center space-x-3">
        <span id="portal-title-text" class="text-xs font-black text-brand-400 uppercase tracking-widest flex items-center gap-1.5">
            <i class="fa-solid fa-id-badge"></i> Cổng Không Gian Làm Việc
        </span>
        <span class="text-slate-700">|</span>
        <span id="portal-user-badge" class="text-xs font-bold text-sky-300 bg-sky-950/80 px-3 py-1 rounded-lg border border-sky-800">
            Người Dùng
        </span>
    </div>
    <div class="flex items-center gap-3">
        <button onclick="showLandingPage()" class="text-xs font-bold text-slate-300 hover:text-white bg-slate-800 px-3 py-1.5 rounded-lg border border-slate-700 transition">
            <i class="fa-solid fa-arrow-left mr-1"></i> Ra Trang Chủ
        </button>
    </div>
</div>
```

### 3.3 Dynamic Navigation Tabs & Sections

Navigation tabs container (`index.html:1425-1451`):
```html
<div class="bg-slate-900 px-4 sm:px-6 lg:px-8 border-b border-slate-800 flex space-x-2 overflow-x-auto text-xs font-bold">
    <button onclick="switchTab('dashboard')" id="tab-dashboard" class="tab-btn py-3.5 px-4 border-b-2 border-brand-400 text-white flex items-center gap-2">...</button>
    <button onclick="switchTab('appointments')" id="tab-appointments" class="tab-btn ...">...</button>
    <button onclick="switchTab('emr')" id="tab-emr" class="tab-btn ...">...</button>
    <button onclick="switchTab('coupons')" id="tab-coupons" class="tab-btn ...">...</button>
    <button onclick="switchTab('shifts')" id="tab-shifts" class="tab-btn ...">...</button>
    <button onclick="switchTab('notifications')" id="tab-notifications" class="tab-btn ...">...</button>
</div>
```

Corresponding sections inside `<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">`:
- `#section-dashboard`: Metric stat cards (Revenue, Appointments, Ortho, Staff) + Chart.js canvas.
- `#section-appointments`: Appointments table, CSV export, quick date filter preset bar (`all`, `today`, `tomorrow`, `next7days`, `future`, custom range).
- `#section-emr`: Two-column layout for Medical Records (EMR) and Orthodontic Plans.
- `#section-coupons`: Voucher creation action bar + coupons table with toggle/delete.
- `#section-shifts`: Staff shift calendar + Staff Account creation bar (`#staff-admin-bar`).
- `#section-notifications`: Realtime push notification audit feed.

### 3.4 Tab Switching & RBAC Rendering Logic

In `app.js:460-482`:
```javascript
function switchTab(tabId) {
    const tabs = ['dashboard', 'appointments', 'coupons', 'emr', 'shifts', 'notifications'];
    tabs.forEach(t => {
        const sec = document.getElementById(`section-${t}`);
        const btn = document.getElementById(`tab-${t}`);
        if (sec) sec.classList.toggle('hidden', t !== tabId);
        if (btn) {
            if (t === tabId) {
                btn.className = "tab-btn py-3.5 px-4 border-b-2 border-brand-400 text-white flex items-center gap-2";
            } else {
                btn.className = "tab-btn py-3.5 px-4 border-b-2 border-transparent text-slate-400 hover:text-white flex items-center gap-2";
            }
        }
    });
    // Triggers specific tab loaders...
}
```

In `app.js:364-457` (`renderDynamicRoleView()`):
Tabs are selectively hidden (`display: 'none'`) or shown (`display: 'flex'`) based on `currentUser.role`.
- `ROLE_PATIENT`: Only sees `appointments` (labeled "Lịch Hẹn Của Tôi") and `emr` ("Bệnh Án & Tiến Trình Niềng Răng"). Dashboard and Shifts are hidden.
- `ROLE_OWNER` & `ROLE_RECEPTIONIST`: See `dashboard`, `appointments`, `coupons`, `emr`, `shifts`, and `notifications`. Revenue card is displayed only if `role === 'ROLE_OWNER'`.
- `ROLE_DENTIST`: Sees `appointments` ("Lịch Khám Được Phân Công") and `emr`.
- `ROLE_ASSISTANT` & `ROLE_CLEANER`: Only see `shifts`.

---

## 4. CSS & Design System Specification

The DentalCare design system provides a polished, professional aesthetic with strict visual hierarchy.

### 4.1 Color Palette & Theme Tokens

From `clinic-ui-refresh.css:2-10` and Tailwind overrides:
```css
:root {
    --clinic-ink: #102f31;    /* Primary dark text & deep accents */
    --clinic-deep: #0b4241;   /* Brand luxury forest teal */
    --clinic-teal: #087a72;   /* Main primary brand button / action */
    --clinic-mint: #dff3ed;   /* Soft background / highlight */
    --clinic-sand: #f8f5ef;   /* Warm light card background */
    --clinic-line: #dbe8e3;   /* Gentle border line */
    --clinic-coral: #f07c63;  /* Accent warm coral / alerts */
}
```

Portal Specific Palette:
- **Portal Canvas**: `#102f31` / `bg-slate-900`
- **Subheader & Table Headers**: `bg-slate-950`
- **Card Containers**: `bg-slate-800` with border `border-slate-700` (`rgba(192, 226, 216, .15)`)
- **Active Accents**: `#61d2c1` (`border-brand-400`), `#0ea5e9` (`brand-500`), `#10b981` (`emerald-500`)
- **Text**: `text-white` (headings), `text-slate-200` (body), `text-slate-400` (muted labels)

### 4.2 Standard UI Component Patterns

#### 1. Metric / Stat Card Pattern
```html
<div class="bg-slate-800 p-6 rounded-3xl border border-slate-700 flex items-center justify-between shadow-sm">
    <div>
        <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">Tiêu Đề Chỉ Số</span>
        <div class="text-2xl font-black text-white mt-1">42</div>
        <span class="text-[11px] text-emerald-400 font-bold"><i class="fa-solid fa-circle-check mr-1"></i> Trạng thái tốt</span>
    </div>
    <div class="w-12 h-12 rounded-2xl bg-cyan-500/20 text-cyan-400 flex items-center justify-center text-xl font-bold">
        <i class="fa-solid fa-server"></i>
    </div>
</div>
```

#### 2. Data Table Pattern
```html
<div class="bg-slate-800 rounded-3xl border border-slate-700 overflow-hidden shadow-xl p-6 space-y-4">
    <div class="overflow-x-auto">
        <table class="w-full text-left text-xs text-slate-300">
            <thead class="bg-slate-900 uppercase font-extrabold text-[11px] text-slate-400 border-b border-slate-700">
                <tr>
                    <th class="py-3 px-4">Cột 1</th>
                    <th class="py-3 px-4">Cột 2</th>
                    <th class="py-3 px-4 text-right">Thao Tác</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-700 text-slate-200">
                <tr class="hover:bg-slate-800/80 transition font-medium">...</tr>
            </tbody>
        </table>
    </div>
</div>
```

#### 3. Filter Bar Pattern
```html
<div class="flex flex-wrap items-center justify-between gap-3 bg-slate-900/90 p-3.5 rounded-2xl border border-slate-700/80">
    <div class="flex flex-wrap items-center gap-1.5 text-xs font-bold">
        <span class="text-slate-400 mr-1 text-[11px] uppercase tracking-wider">
            <i class="fa-solid fa-filter text-brand-400 mr-1"></i> Bộ lọc:
        </span>
        <button class="px-3 py-1.5 rounded-lg bg-brand-600 text-white font-bold transition">Tất Cả</button>
        <button class="px-3 py-1.5 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 transition">Tùy Chọn</button>
    </div>
</div>
```

#### 4. Status Badges & Pills
- **Success / Active**: `<span class="bg-emerald-500/20 text-emerald-300 font-bold px-2.5 py-0.5 rounded-full text-[11px] border border-emerald-500/30">ONLINE</span>`
- **Warning / Busy**: `<span class="bg-amber-500/20 text-amber-300 font-bold px-2.5 py-0.5 rounded-full text-[11px] border border-amber-500/30">BUSY</span>`
- **Danger / High**: `<span class="bg-rose-500/20 text-rose-300 font-bold px-2.5 py-0.5 rounded-full text-[11px] border border-rose-500/30">HIGH</span>`
- **Info / Mention**: `<span class="bg-cyan-500/20 text-cyan-300 font-bold px-2.5 py-0.5 rounded-full text-[11px] border border-cyan-500/30">#it-backend</span>`

---

## 5. Authentication & API Client Mechanism

### 5.1 Token Persistence

The application employs a hybrid storage strategy (`app.js:83-101`, `app.js:151-166`):
1. **Session Login (Default)**: Persisted in `sessionStorage.getItem('DENTAL_USER')`. Automatically purged when the user closes the browser tab, enforcing strict security on shared workstations.
2. **Persistent Login ("Ghi nhớ 24h")**: Persisted in `localStorage.getItem('DENTAL_USER')`. Survives tab closing.

The stored object matches Java `AuthResponse.java`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "id": 1,
  "username": "owner",
  "fullName": "BS.CKII Trần Văn Thắng",
  "role": "ROLE_OWNER",
  "phone": "0901234567",
  "email": "owner@dental.vn"
}
```

### 5.2 Universal HTTP Interceptor (`apiFetch`)

All AJAX requests across the portal use `apiFetch` (`app.js:47-81`):
```javascript
async function apiFetch(url, options = {}) {
    const headers = options.headers || {};
    
    // Auto attach JWT Bearer Token if logged in
    if (currentUser && currentUser.token) {
        headers['Authorization'] = `Bearer ${currentUser.token}`;
    }
    
    if (!headers['Content-Type'] && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    options.headers = headers;

    try {
        const response = await fetch(url, options);

        if (response.status === 401) {
            showToast('⚠️ Phiên đăng nhập đã hết hạn! Vui lòng đăng nhập lại.');
            handleLogout();
            throw new Error('Unauthorized');
        }

        if (response.status === 403) {
            showToast('⛔ Bạn không có quyền truy cập chức năng này!');
            throw new Error('Forbidden');
        }

        const data = await response.json();
        return { ok: response.ok, status: response.status, data };
    } catch (err) {
        console.error('API Error:', err);
        throw err;
    }
}
```

### 5.3 Backend Response Structure

The backend standardizes responses via `ApiResponse<T>`:
```json
{
  "success": true,
  "message": "Thành công",
  "data": { ... },
  "timestamp": "2026-09-12T22:00:00"
}
```
When consuming responses: `const res = await apiFetch(...)`; if `res.ok && res.data.success`, the payload is at `res.data.data`.

---

## 6. Detailed Integration Design: "IT Team" Command Center

### 6.1 Tab Integration & Navigation Injection

#### Location 1: Navigation Bar (`index.html:1446`)
Add the `#tab-itteam` button immediately after `#tab-shifts` and before `#tab-notifications`:
```html
<!-- Tab IT Team Command Center (Chỉ Admin / Chủ Phòng) -->
<button onclick="switchTab('itteam')" id="tab-itteam" class="tab-btn py-3.5 px-4 border-b-2 border-transparent text-slate-400 hover:text-white flex items-center gap-2">
    <i class="fa-solid fa-terminal text-cyan-400"></i>
    <span id="label-tab-itteam">IT Team Command Center</span>
    <span class="bg-cyan-500/20 text-cyan-300 text-[10px] font-mono px-1.5 py-0.2 rounded border border-cyan-500/30">AI</span>
</button>
```

#### Location 2: Main Body Section (`index.html:1685`)
Insert `<div id="section-itteam" class="hidden space-y-6">` before `</div><!-- end portal body -->`.

#### Location 3: Tab Switching Registration (`app.js:461`)
Update `tabs` array in `switchTab(tabId)`:
```javascript
const tabs = ['dashboard', 'appointments', 'coupons', 'emr', 'shifts', 'notifications', 'itteam'];
```
Add hook:
```javascript
if (tabId === 'itteam') {
    loadItTeamCommandCenter();
}
```

#### Location 4: RBAC Visibility in `renderDynamicRoleView()` (`app.js:378`)
```javascript
const tabItTeam = document.getElementById('tab-itteam');
if (tabItTeam) {
    // Only ROLE_OWNER or ROLE_ADMIN can see the IT Team Command Center
    const isAuthorized = currentUser && ['ROLE_OWNER', 'ROLE_ADMIN'].includes(currentUser.role);
    tabItTeam.style.display = isAuthorized ? 'flex' : 'none';
}
```

---

### 6.2 The 5 Sub-Views Architecture

Inside `#section-itteam`, a sub-navigation bar switches between the 5 sub-views:
```html
<div class="flex flex-wrap items-center justify-between gap-3 bg-slate-900/90 p-3.5 rounded-2xl border border-slate-700/80">
    <div class="flex flex-wrap items-center gap-2 text-xs font-bold" id="itteam-subnav">
        <button onclick="switchItSubView('profiles')" id="subnav-profiles" class="px-3.5 py-2 rounded-xl bg-cyan-600 text-white font-extrabold flex items-center gap-2 shadow-sm transition">
            <i class="fa-solid fa-users-gear"></i> 1. Nhân Sự (Profiles)
        </button>
        <button onclick="switchItSubView('conversations')" id="subnav-conversations" class="px-3.5 py-2 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 font-bold flex items-center gap-2 transition">
            <i class="fa-solid fa-comments"></i> 2. Hội Thoại (Chat)
        </button>
        <button onclick="switchItSubView('memories')" id="subnav-memories" class="px-3.5 py-2 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 font-bold flex items-center gap-2 transition">
            <i class="fa-solid fa-brain"></i> 3. Bộ Nhớ (Memories)
        </button>
        <button onclick="switchItSubView('activities')" id="subnav-activities" class="px-3.5 py-2 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 font-bold flex items-center gap-2 transition">
            <i class="fa-solid fa-clock-rotate-left"></i> 4. Nhật Ký (Logs)
        </button>
        <button onclick="switchItSubView('api-monitor')" id="subnav-api-monitor" class="px-3.5 py-2 rounded-xl bg-slate-800 text-slate-300 hover:bg-slate-700 font-bold flex items-center gap-2 transition">
            <i class="fa-solid fa-gauge-simple-high"></i> 5. API Monitor
        </button>
    </div>
    <div class="flex items-center gap-2 text-xs">
        <span class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 border border-slate-700 text-slate-300 font-mono text-[11px]">
            <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            9Router: <span id="ninerouter-status" class="text-emerald-400 font-bold">localhost:20128</span>
        </span>
    </div>
</div>
```

---

### 6.3 Sub-View 1: Nhân Sự (Team Profiles)

Displays the 5 seeded IT agent profiles in an interactive grid:
1. `#it-backend` (Spring Boot, Database, Security, REST APIs)
2. `#it-frontend` (Management Portal UI, API Client, Responsive UX)
3. `#it-qa` (API Testing, Authorization Checks, Regression)
4. `#it-devops` (Build, Server Runtime, Logs, Tunnel Integration)
5. `#it-security` (RBAC, Data Privacy, Input Validation, Audit Logs)

**Card Layout**:
- Top: Agent avatar gradient icon + Status badge (`ONLINE` with pulsing dot, `BUSY`, `STANDBY`).
- Middle: Display name, hashtag pill (`font-mono text-cyan-400 bg-cyan-950/80 px-2 py-0.5 rounded border border-cyan-800`), role title.
- Core Competencies: Tag list with micro badges.
- Bottom actions:
  - Status toggle (`PUT /api/it-team/agents/{id}/status`).
  - "Gửi tin nhắn" (deep links to Conversations tab with `#hashtag` pre-filled).
  - "Xem bộ nhớ" (deep links to Memories tab filtered to this agent).

---

### 6.4 Sub-View 2: Hội Thoại (Conversations) with Hashtag Autocomplete

#### Layout:
- **Left Sidebar (Channels)**:
  - `#all-channels` (Tất cả tin nhắn)
  - Agent channels: `#it-backend`, `#it-frontend`, `#it-qa`, `#it-devops`, `#it-security`
  - Unread mention indicators
- **Right Main Panel (Chat & Thread Stream)**:
  - Thread Header: Active channel/thread title and agent details.
  - Message Stream: Chronological list of message cards.
    - Sender name, avatar, timestamp.
    - Message body with parsed hashtags styled as clickable badges:
      `<span class="bg-cyan-500/20 text-cyan-300 font-mono font-bold px-1.5 py-0.5 rounded cursor-pointer hover:bg-cyan-500/30" onclick="filterByHashtag('#it-backend')">#it-backend</span>`
    - Recipient indicator: `→ Recipient: #it-qa`.
    - Thread reply count + "Trả lời" toggle button.
    - Collapsible child reply stream for threaded discussions.
- **Message Composer & Hashtag Autocomplete Engine**:
  - A relative textarea `#it-message-body`.
  - Floating Autocomplete Dropdown `#it-hashtag-autocomplete`:
    - Activates when user types `#` or `#it-`.
    - Filters list of 5 agents matching input.
    - Supports keyboard navigation: `ArrowUp`, `ArrowDown`, `Enter` to select, `Escape` to close.
    - On selection, replaces the partial hashtag with the complete tag and appends a space.
  - Action buttons:
    - "Gửi Tin Nhắn" (`POST /api/it-team/messages`).
    - "🤖 AI Phản Hồi (9Router)": Triggers 9Router integration to generate an intelligent agent reply simulating inter-agent assistance.

---

### 6.5 Sub-View 3: Bộ Nhớ (Agent Memories)

- **Filter Bar**: Filter by Agent (`Tất Cả`, `#it-backend`, ...), Priority filter (`ALL`, `HIGH`, `MEDIUM`, `LOW`), Search key keyword input.
- **Add Memory Modal / Quick Form**: Agent select, Memory Key (alphanumeric with dots/underscores), Memory Content, Priority selector (`HIGH`, `MEDIUM`, `LOW`).
- **Memory Store Table**:
  - Agent Badge: `#it-backend`
  - Key: `font-mono text-cyan-400 font-bold` (e.g. `jwt.expiration.ms`, `theme.portal.primary`)
  - Content: Sanitized string with expand/collapse for long values.
  - Priority Badge:
    - `HIGH`: Rose pill
    - `MEDIUM`: Amber pill
    - `LOW`: Slate pill
  - Last Updated: `formatDate(...)`
  - Privacy compliance badge: "🔒 Sanitized / No PII"

---

### 6.6 Sub-View 4: Nhật Ký Thao Tác (Activity Logs)

- **Chronological Audit Trail**:
  - Filter controls: Filter by Agent, Action Type (`MENTIONED`, `RUN_API`, `UPDATE_MEMORY`, `STATUS_CHANGE`), and Date range picker.
  - Audit feed entries:
    - Left: Icon badge indicating action type (e.g. `@` icon for `MENTIONED`, lightning icon for `RUN_API`, database icon for `UPDATE_MEMORY`).
    - Center: Activity description, Agent hashtag, Result summary.
    - Right: Formatted timestamp and related entity link button.

---

### 6.7 Sub-View 5: API Monitor (Safe Tester & Dashboard)

- **Metrics Overview Bar**:
  - Total Runs today, Success Rate (%), Average Duration (ms), Active Local Endpoints.
- **Interactive Safe API Tester**:
  - Safe Endpoint preset dropdown:
    - `GET /api/it-team/agents` (Agent profile listing)
    - `GET /api/it-team/memories` (Memory store audit)
    - `GET /api/it-team/messages` (Message history)
    - `GET /api/it-team/activities` (Activity audit log)
    - `GET /api/dashboard/stats` (Clinic stats)
    - `GET /api/dentists` (Dentist directory)
    - `GET /api/coupons/active` (Active promotions)
  - Method selector: `GET`, `POST` (restricted to safe localhost test execution).
  - "Chạy Kiểm Thử (Safe Run)" button:
    - Calls `POST /api/it-team/api-runs` with `{ endpoint, method, payload }`.
    - Live latency timer indicator.
    - Status code pill: `200 OK` (green), `401 Unauthorized` (amber), `500 Error` (red).
    - Latency display in milliseconds (`font-mono text-emerald-400 font-bold`).
  - **Payload Inspector**:
    - Dual split or tabbed view: Request Payload vs Sanitized Response Payload.
    - Rendered in syntax-highlighted / pretty-printed JSON box (`bg-slate-950 font-mono text-xs text-emerald-400 p-4 rounded-2xl border border-slate-800`).
    - Safety Guarantee Notice: "🔒 Privacy Guardrail: All passwords, JWTs, cookies, and patient medical PII are automatically redacted before logging and display."
- **Historical API Run Logs Table**:
  - Shows recent `it_api_run_log` records with Endpoint, Method, Status, Latency (ms), and Execution Timestamp.

---

## 7. Safety, Privacy Guardrails & Zero Regression Strategy

### 7.1 Strict Privacy Guardrail Implementation
Per Requirement R1 & R5:
- Memory content, Activity logs, Tab records, and API logs **MUST NEVER** store or display:
  - Cookies or HTTP headers containing credentials
  - Plaintext passwords or secret keys
  - Raw JWT Bearer tokens
  - Real patient Personally Identifiable Information (PII) or Electronic Medical Records (EMR)
- In the frontend UI:
  - Display sanitized payloads.
  - If a test endpoint returns patient data, the API Monitor renders redacted fields (e.g. `phone: "090****567"`, `email: "n***@dental.vn"`).

### 7.2 Zero Regression Guarantees
- **No modification to existing tabs**: `#section-dashboard`, `#section-appointments`, `#section-emr`, `#section-coupons`, `#section-shifts`, `#section-notifications` are left completely untouched.
- **No modification to existing booking/auth flows**: `handleUniversalLogin`, `handleBookingSubmit`, `apiFetch` remain the single source of truth.
- **Isolated File Strategy**:
  - HTML addition: Only adds `#tab-itteam` in the tab header and `#section-itteam` in the portal body.
  - JS addition: Place all IT Team logic in a dedicated file `src/main/resources/static/js/it-team.js` and link it via `<script defer src="js/it-team.js"></script>` in `index.html`. This ensures zero risk of syntax collision or variable clobbering with `app.js`.

---

## 8. Concrete Implementation Code Sketches

### 8.1 Autocomplete Script (`static/js/it-team.js`)
```javascript
// IT Agent definitions for autocomplete and tagging
const IT_AGENTS = [
    { code: 'it-backend', tag: '#it-backend', name: 'IT Backend', role: 'Spring Boot & APIs', icon: 'fa-server' },
    { code: 'it-frontend', tag: '#it-frontend', name: 'IT Frontend', role: 'Portal UI & UX', icon: 'fa-code' },
    { code: 'it-qa', tag: '#it-qa', name: 'IT QA', role: 'Testing & Regression', icon: 'fa-vial-circle-check' },
    { code: 'it-devops', tag: '#it-devops', name: 'IT DevOps', role: 'Build & Runtime', icon: 'fa-network-wired' },
    { code: 'it-security', tag: '#it-security', name: 'IT Security', role: 'RBAC & Privacy', icon: 'fa-shield-halved' }
];

function setupHashtagAutocomplete(textareaId, dropdownId) {
    const input = document.getElementById(textareaId);
    const dropdown = document.getElementById(dropdownId);
    if (!input || !dropdown) return;

    let activeIndex = 0;
    let matchingAgents = [];

    input.addEventListener('input', () => {
        const text = input.value;
        const cursor = input.selectionStart;
        const match = text.slice(0, cursor).match(/#([a-zA-Z0-9_-]*)$/);

        if (match) {
            const query = match[1].toLowerCase();
            matchingAgents = IT_AGENTS.filter(a => a.code.toLowerCase().includes(query) || a.tag.toLowerCase().includes(query));
            if (matchingAgents.length > 0) {
                renderDropdown(matchingAgents);
                dropdown.classList.remove('hidden');
                activeIndex = 0;
                highlightActiveItem();
                return;
            }
        }
        dropdown.classList.add('hidden');
    });

    input.addEventListener('keydown', (e) => {
        if (dropdown.classList.contains('hidden')) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            activeIndex = (activeIndex + 1) % matchingAgents.length;
            highlightActiveItem();
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            activeIndex = (activeIndex - 1 + matchingAgents.length) % matchingAgents.length;
            highlightActiveItem();
        } else if (e.key === 'Enter' || e.key === 'Tab') {
            e.preventDefault();
            if (matchingAgents[activeIndex]) {
                selectHashtag(matchingAgents[activeIndex].tag);
            }
        } else if (e.key === 'Escape') {
            dropdown.classList.add('hidden');
        }
    });

    function renderDropdown(agents) {
        dropdown.innerHTML = agents.map((a, idx) => `
            <div class="autocomplete-item p-2 rounded-xl flex items-center gap-2.5 cursor-pointer hover:bg-slate-800 transition" data-index="${idx}" onclick="selectHashtag('${a.tag}')">
                <div class="w-7 h-7 rounded-lg bg-cyan-500/20 text-cyan-400 flex items-center justify-center text-xs">
                    <i class="fa-solid ${a.icon}"></i>
                </div>
                <div>
                    <div class="text-xs font-bold text-white">${a.tag} <span class="text-slate-400 font-normal">(${a.name})</span></div>
                    <div class="text-[10px] text-slate-400">${a.role}</div>
                </div>
            </div>
        `).join('');
    }

    function highlightActiveItem() {
        const items = dropdown.querySelectorAll('.autocomplete-item');
        items.forEach((item, idx) => {
            if (idx === activeIndex) {
                item.classList.add('bg-slate-800', 'ring-1', 'ring-cyan-400');
            } else {
                item.classList.remove('bg-slate-800', 'ring-1', 'ring-cyan-400');
            }
        });
    }

    window.selectHashtag = function(tag) {
        const text = input.value;
        const cursor = input.selectionStart;
        const before = text.slice(0, cursor).replace(/#([a-zA-Z0-9_-]*)$/, tag + ' ');
        const after = text.slice(cursor);
        input.value = before + after;
        input.selectionStart = input.selectionEnd = before.length;
        input.focus();
        dropdown.classList.add('hidden');
    };
}
```

---

## 9. Alignment with Backend & Database Explorers

1. **Role Definition**:
   - `ORIGINAL_REQUEST.md` specifies Admin RBAC for `/api/it-team/**`.
   - Current `Role.java` has `ROLE_OWNER`, `ROLE_RECEPTIONIST`, `ROLE_DENTIST`, `ROLE_ASSISTANT`, `ROLE_CLEANER`, `ROLE_PATIENT`.
   - Backend Explorer should ensure `ROLE_ADMIN` is added to `Role.java` (or endpoints allow `hasAnyRole('ADMIN', 'OWNER')`).
   - Frontend `renderDynamicRoleView` checks `['ROLE_OWNER', 'ROLE_ADMIN'].includes(currentUser.role)`.
2. **API Endpoint Uniformity**:
   - All endpoints follow `/api/it-team/...` as specified in R3.
   - All endpoints wrap results in `ApiResponse<T>`, directly consumable by frontend `apiFetch`.
3. **9Router Compatibility**:
   - Local 9Router runs at `http://localhost:20128`. The backend calls 9Router via `HttpClient` (following pattern in `AiBlogService.java`), preventing CORS issues and securing internal communication.

---
*Report completed and verified.*
