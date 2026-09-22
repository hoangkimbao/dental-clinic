## 2026-09-22T17:25:35Z
Read D:\java\dental-clinic\ORIGINAL_REQUEST.md, D:\java\dental-clinic\PROJECT.md, and D:\java\dental-clinic\.agents\explorer_survey_staff_pc\report.md (Section 4).
Your working directory is D:\java\dental-clinic\.agents\worker_m4_analytics.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your mission is to implement Milestone 4: Agrid / Analytics Tracking SDK & Ingestion Engine:
1. Domain Entity in src/main/java/com/dentalclinic/analytics/model/AnalyticsEvent.java (extending BaseEntity):
   - eventType (PAGE_VIEW, CLICK, BOOKING_FUNNEL, ORDER_FUNNEL, SEARCH, QR_SCAN, NAVIGATION)
   - eventName, pageUrl, referrer, clientSessionId, userPhone, userRole, metadataJson, ipAddress, userAgent, occurredAt.
2. Repository in src/main/java/com/dentalclinic/analytics/repository/AnalyticsRepository.java.
3. Service in src/main/java/com/dentalclinic/analytics/service/AnalyticsService.java:
   - Non-blocking asynchronous processing with @Async.
   - Batch event persistence.
   - Strict medical PII / password / token redaction before saving.
4. Controller in src/main/java/com/dentalclinic/analytics/controller/AnalyticsController.java:
   - POST /api/analytics/events: Accepts single event or list of events, returns HTTP 202 Accepted immediately.
   - GET /api/analytics/summary: RBAC protected (ROLE_ADMIN, ROLE_OWNER) returning event metrics and funnel counts.
5. Client SDK in src/main/resources/static/js/agrid-sdk.js:
   - Lightweight, standalone cross-platform tracking SDK.
   - 0ms initial render latency: wraps initialization in requestIdleCallback / setTimeout.
   - Non-blocking transport: uses navigator.sendBeacon(url, blob) with fetch(keepalive: true) fallback.
   - Micro-batching: buffers events in memory and flushes every 5 seconds, or when buffer reaches 10 events, or on visibilitychange (hidden) and beforeunload.
   - Offline queue: persists unsent events in localStorage up to 50 items and retries when online.
   - Event tracking methods: Agrid.trackPageView(), Agrid.trackClick(element, name), Agrid.trackFunnel(step, data), Agrid.trackSearch(query), Agrid.trackQrScan(code), Agrid.trackNav(from, to).
   - Strict client-side sanitization: Redacts passwords, JWTs, Bearer headers, CCCD/CMND numbers, credit cards, and clinical medical terms before buffering.
6. Integrate agrid-sdk.js into src/main/resources/static/index.html (before closing </body> with non-blocking script tag).
7. Verify compilation with `.\mvnw.cmd test-compile`.
8. Write handoff.md in your working directory and notify the parent orchestrator with send_message.
