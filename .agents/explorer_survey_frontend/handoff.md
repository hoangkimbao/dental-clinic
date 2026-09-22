# Handoff Report — Frontend Architecture Survey

**Agent**: `explorer_survey_frontend`  
**Recipient**: `parent` (`orchestrator_1`)  
**Timestamp**: 2026-09-12T15:05:00Z  
**Type**: Hard Handoff (Investigation Complete)  

---

## 1. Observation

1. **Repository Structure & Build Config**:
   - `pom.xml` (lines 25-104) contains `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-websocket`, `jjwt-api`, etc.
   - `pom.xml` contains **zero** template engines (no `spring-boot-starter-thymeleaf`, no `jsp`).
   - Directory `src/main/resources/templates` does not exist (`list_dir` on `src/main/resources` returned only `application.yml` and `static/`).
   - Directory `src/main/resources/static` contains: `index.html` (242,975 bytes), `css/` (`clinic-ui-refresh.css`, `tailwind.min.css`), `js/` (`app.js`, 77,121 bytes), `llms.txt`, `robots.txt`, `sitemap.xml`.
   - `mobile-app/` directory exists as a separate Expo React Native project (`App.js`, `package.json`), not serving the web portal.

2. **Web Serving & Security Mappings**:
   - In `SecurityConfig.java:72`:
     `.requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico", "/sitemap.xml", "/robots.txt", "/uploads/**").permitAll()`
   - In `SecurityConfig.java:96`:
     `.requestMatchers("/api/**").authenticated()`

3. **Management Portal HTML & Tab Structure**:
   - In `src/main/resources/static/index.html`:
     - Line 1403: `<div id="management-portal" class="hidden flex-1 bg-slate-900 text-slate-100 min-h-screen">`
     - Lines 1425–1451: Dynamic navigation tabs container:
       - Line 1427: `<button onclick="switchTab('dashboard')" id="tab-dashboard" class="tab-btn py-3.5 px-4 border-b-2 border-brand-400 text-white flex items-center gap-2">`
       - Line 1431: `<button onclick="switchTab('appointments')" id="tab-appointments" class="tab-btn ...">`
       - Line 1435: `<button onclick="switchTab('emr')" id="tab-emr" class="tab-btn ...">`
       - Line 1439: `<button onclick="switchTab('coupons')" id="tab-coupons" class="tab-btn ...">`
       - Line 1443: `<button onclick="switchTab('shifts')" id="tab-shifts" class="tab-btn ...">`
       - Line 1447: `<button onclick="switchTab('notifications')" id="tab-notifications" class="tab-btn ...">`
     - Lines 1454–1686: Portal main body sections:
       - Line 1457: `<div id="section-dashboard" class="space-y-6">`
       - Line 1531: `<div id="section-appointments" class="hidden ...">`
       - Line 1594: `<div id="section-emr" class="hidden ...">`
       - Line 1610: `<div id="section-coupons" class="hidden ...">`
       - Line 1655: `<div id="section-shifts" class="hidden ...">`
       - Line 1676: `<div id="section-notifications" class="hidden ...">`

4. **Tab Switching & RBAC Rendering in `app.js`**:
   - In `app.js:460-482` (`switchTab(tabId)`):
     `const tabs = ['dashboard', 'appointments', 'coupons', 'emr', 'shifts', 'notifications'];`
     Iterates and sets `sec.classList.toggle('hidden', t !== tabId)`.
     Sets active button style: `tab-btn py-3.5 px-4 border-b-2 border-brand-400 text-white flex items-center gap-2`.
     Sets inactive button style: `tab-btn py-3.5 px-4 border-b-2 border-transparent text-slate-400 hover:text-white flex items-center gap-2`.
   - In `app.js:364-457` (`renderDynamicRoleView()`):
     Hides/shows tabs based on `currentUser.role` (`ROLE_PATIENT`, `ROLE_OWNER`, `ROLE_RECEPTIONIST`, `ROLE_DENTIST`, `ROLE_ASSISTANT`, `ROLE_CLEANER`).

5. **CSS & Design System**:
   - `clinic-ui-refresh.css:2-10` sets custom properties: `--clinic-ink: #102f31; --clinic-deep: #0b4241; --clinic-teal: #087a72; --clinic-mint: #dff3ed; --clinic-sand: #f8f5ef; --clinic-line: #dbe8e3; --clinic-coral: #f07c63;`.
   - Lines 95-97:
     `#management-portal { background: #102f31 !important; }`
     `#management-portal .bg-slate-800 { border-color: rgba(192, 226, 216, .15) !important; }`
     `#management-portal .border-brand-400 { border-color: #61d2c1 !important; }`
   - Icon set: FontAwesome 6.4.0 (`fa-solid`, `fa-regular`).
   - Cards use `bg-slate-800`, `rounded-3xl` / `rounded-2xl`, `border border-slate-700`, `shadow-md` / `shadow-xl`.
   - Data tables use `bg-slate-900` thead, `divide-y divide-slate-700` tbody, `hover:bg-slate-800/80` row hover.

6. **Authentication & API Client**:
   - In `app.js:47-81` (`apiFetch(url, options = {})`):
     Attaches `headers['Authorization'] = 'Bearer ' + currentUser.token`.
     Sets `headers['Content-Type'] = 'application/json'` unless FormData.
     Catches 401 (logout + expiry toast) and 403 (forbidden toast).
     Returns `{ ok, status, data }` where `data` is `ApiResponse<T>`.
   - Token storage: `sessionStorage.getItem('DENTAL_USER')` or `localStorage.getItem('DENTAL_USER')`.
   - User entity `Role.java`: `ROLE_OWNER`, `ROLE_RECEPTIONIST`, `ROLE_DENTIST`, `ROLE_ASSISTANT`, `ROLE_CLEANER`, `ROLE_PATIENT`.

7. **Existing 9Router Integration**:
   - `AiBlogService.java:27-42`:
     `@Value("${ninerouter.url:http://localhost:20128}") private String nineRouterUrl;`
     `httpClient.send(request, HttpResponse.BodyHandlers.ofString());`
   - `app.js:1464-1565`: Front-end modal `#ai-writer-modal` calling `/api/articles/ai-generate` powered by 9Router.

---

## 2. Logic Chain

1. **Frontend stack confirmation**: From Observation 1, because `pom.xml` lacks Thymeleaf, `templates/` does not exist, and `static/` contains `index.html`, `css/`, and `js/app.js`, the frontend is 100% client-side HTML5/ES6/Tailwind, served statically by Spring Boot.
2. **Portal integration pattern**: From Observation 3 and 4, the portal uses a unified tab array `tabs = ['dashboard', 'appointments', ...]` and switches visibility via `.hidden` class toggling and button style swapping. Adding a new tab `itteam` requires registering `'itteam'` in the tabs array, adding `#tab-itteam` in the navigation header, and adding `#section-itteam` in the body.
3. **Role authorization**: From Observation 4 and 6, `renderDynamicRoleView()` regulates tab visibility. To protect the IT Team Command Center in accordance with requirement R3, `#tab-itteam` must be visible only when `['ROLE_OWNER', 'ROLE_ADMIN'].includes(currentUser.role)`.
4. **Visual consistency**: From Observation 5, by utilizing existing Tailwind utility classes (`bg-slate-900`, `bg-slate-800`, `border-slate-700`, `text-brand-400`, `rounded-2xl`, `rounded-3xl`) and FontAwesome 6.4.0 icons, the new sub-views will match the existing portal aesthetics with 100% visual parity.
5. **Zero regression**: From Observation 1 and 3, existing features (`#section-dashboard`, `#section-appointments`, etc.) and public booking logic operate independently in dedicated DOM elements. Isolating IT Team logic in an additive script `src/main/resources/static/js/it-team.js` linked at the bottom of `index.html` prevents any regressions to existing pages.

---

## 3. Caveats

1. **`ROLE_ADMIN` vs `ROLE_OWNER` Alignment**: Currently, `Role.java` in the backend contains `ROLE_OWNER` as the highest administrative role, while `ORIGINAL_REQUEST.md` mentions `ROLE_ADMIN`. The backend team must either add `ROLE_ADMIN` to `Role.java` or support both `ROLE_ADMIN` and `ROLE_OWNER` in endpoint `@PreAuthorize`. The frontend will support `['ROLE_OWNER', 'ROLE_ADMIN']`.
2. **WebSocket Event Subscription**: Notifications currently use `/topic/notifications`. If real-time IT Team messages/activities require WebSocket delivery, a new destination `/topic/it-team` can be subscribed to within `it-team.js` using the existing `stompClient`.
3. **External Mobile App**: The `mobile-app/` directory is React Native for mobile devices and does not interact with the web Management Portal.

---

## 4. Conclusion

The DentalCare frontend is well-structured, modern, and easily extensible. Integrating the "IT Team" Command Center requires:
1. Adding `#tab-itteam` to `src/main/resources/static/index.html` within the tab navigation bar.
2. Adding `#section-itteam` containing the 5 sub-views (Nhân Sự, Hội Thoại with hashtag autocomplete, Bộ Nhớ, Nhật Ký Thao Tác, API Monitor) into `index.html`.
3. Creating `src/main/resources/static/js/it-team.js` containing the 5 sub-view renderers, autocomplete engine, and API runner, reusing existing `apiFetch(url, options)` and `showToast(msg)`.
4. Updating `switchTab` and `renderDynamicRoleView` in `app.js` to recognize the new `itteam` tab.
5. Preserving 100% of existing booking, EMR, shifts, and dashboard features without touching their code.

---

## 5. Verification Method

To independently verify this survey:
1. Inspect files directly:
   - Check `pom.xml` for dependencies: verify no `spring-boot-starter-thymeleaf`.
   - View `src/main/resources/static/index.html` lines 1400–1687: verify `#management-portal` and tab layout.
   - View `src/main/resources/static/js/app.js` lines 47–81 (`apiFetch`), 364–457 (`renderDynamicRoleView`), 460–482 (`switchTab`).
   - View `src/main/resources/static/css/clinic-ui-refresh.css` lines 94–97: verify `#management-portal` theme rules.
2. Test commands (post-implementation):
   - Maven build & test: `./mvnw clean test-compile` or `./mvnw test`
   - Run application: `./mvnw spring-boot:run`
   - Browser check at `http://localhost:8080/`:
     - Login with `owner` / `123` (or `admin` account).
     - Enter Management Portal (`#management-portal` visible).
     - Verify tab "IT Team Command Center" appears.
     - Switch between all 5 sub-views (Nhân Sự, Hội Thoại, Bộ Nhớ, Nhật Ký, API Monitor).
     - Test hashtag autocomplete by typing `#it-` in the chat input.
     - Verify existing tabs (Dashboard, Lịch Hẹn, Bệnh Án, Phân Ca, Mã Ưu Đãi) remain 100% functional without console errors.

---
*Handoff report prepared and verified by Frontend Architecture Explorer.*
