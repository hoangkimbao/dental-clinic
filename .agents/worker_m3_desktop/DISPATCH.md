## 2026-09-22T17:48:02Z
Read D:\java\dental-clinic\ORIGINAL_REQUEST.md, D:\java\dental-clinic\PROJECT.md, and D:\java\dental-clinic\.agents\explorer_survey_staff_pc\report.md (Section 3).
Your working directory is D:\java\dental-clinic\.agents\worker_m3_desktop.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your mission is to implement Milestone 3: PC Desktop App (CMS & Notification Hub):
1. Create standalone Electron application in desktop-app/:
   - desktop-app/package.json: Complete package definition with Electron dependency and start script.
   - desktop-app/main.js: Electron main process with secure contextBridge, system tray, native OS notifications, and window lifecycle.
   - desktop-app/preload.js: Secure contextBridge exposing window.desktopBridge (notifications, native export, window controls).
   - desktop-app/index.html & desktop-app/css/style.css: Enterprise clinic command center dashboard UI.
   - desktop-app/js/desktop-renderer.js:
     * Connects to Spring Boot SockJS/STOMP broker at http://localhost:8080/ws-dental (/topic/notifications).
     * CMS Command Center with 5 dynamic modules: (1) Services CMS, (2) Doctor Directory CMS, (3) Medical Supply Inventory CMS, (4) Satellite/Branch C2 CMS, (5) Menu/Footer Config.
     * Centralized Notification Hub: Real-time alert stream, category filters, audio chime, and native desktop notifications.
     * Export Engine: UTF-8 BOM (\uFEFF) CSV/Excel export for Appointments, Inventory, KPIs, and Leads.
2. Backend support in src/main/java/com/dentalclinic/:
   - CmsConfigController.java (/api/cms/config): Dynamic menu and footer configuration.
   - ExcelExportController.java (/api/export/excel): Multi-table UTF-8 BOM CSV export for Excel compatibility.
3. Unit & Integration test for CMS & Export endpoints in src/test/java/com/dentalclinic/DesktopCmsExportTest.java.
4. Verify compilation with `.\mvnw.cmd test-compile`.
5. Write handoff.md in your working directory and notify the parent orchestrator with send_message.
