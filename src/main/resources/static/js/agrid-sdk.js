/**
 * Agrid Analytics Tracking SDK v1.0
 * Lightweight, zero-latency, cross-platform behavior tracking SDK.
 * Features:
 *  - 0ms initial render latency (deferred via requestIdleCallback / setTimeout)
 *  - Non-blocking transport (navigator.sendBeacon with fetch keepalive fallback)
 *  - Micro-batching (5s interval or 10 events threshold, plus pagehide/visibilitychange)
 *  - Offline queue (persists up to 50 events in localStorage, auto-retries when online)
 *  - Strict client-side sanitization (PII, credentials, CCCD, credit cards, medical terms)
 */
(function (global) {
    'use strict';

    // Default Configuration
    const DEFAULT_CONFIG = {
        endpoint: '/api/analytics/events',
        batchSize: 10,
        flushIntervalMs: 5000,
        maxOfflineQueue: 50,
        storageKey: 'AGRID_OFFLINE_QUEUE',
        sessionKey: 'AGRID_SESSION_ID',
        autoTrackClicks: true,
        autoTrackPageview: true,
        debug: false
    };

    let config = { ...DEFAULT_CONFIG };
    let eventBuffer = [];
    let flushTimer = null;
    let isInitialized = false;
    let sessionId = null;

    // --- Strict Sanitization Engine ---
    const Sanitizer = {
        // Redaction Patterns
        patterns: {
            bearerJwt: /(Bearer\s+)ey[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-+]+/gi,
            standaloneJwt: /\beyJ[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-+]{10,}\b/g,
            bearerGeneric: /(Bearer\s+)(?!\[REDACTED)[^\s"',;}{]+/gi,
            passwords: /(password|passwd|pwd|secret|api_key|token)\s*[:=]\s*["']?(?!\\[REDACTED)[^"',\s}]+/gi,
            creditCards: /\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|(?:2131|1800|35\d{3})\d{11}|\d{4}[-\s]\d{4}[-\s]\d{4}[-\s]\d{4})\b/g,
            cccd: /(?<!\d)0\d{11}(?!\d)/g,
            cmnd: /\b(CMND|CCCD|citizenId|idCard)\b\s*[:=]?\s*["']?\d{9}(?!\d)/gi,
            // Vietnamese clinical & dental terms
            clinicalTerms: /\b(sâu\s*răng(?:\s*(?:độ|cấp)?\s*[0-4])?|viêm\s*(?:tủy|tuỷ|nướu|nha\s*chu|chóp)|áp\s*xe|abcess|răng\s*khôn(?:\s*(?:mọc\s*lệch|mọc\s*ngầm))?|khớp\s*cắn\s*ngược|hoại\s*tử\s*(?:tủy|tuỷ)|chẩn\s*đoán\s*y\s*khoa|đơn\s*thuốc|bệnh\s*án|amoxicillin|ibuprofen|paracetamol|lidocaine|kháng\s*sinh)\b/gi,
            sensitiveKeys: /^(password|passwd|pwd|secret|token|authToken|accessToken|refreshToken|diagnosis|prescription|treatmentDone|medicalHistory|symptoms|doctorNotes)$/i
        },

        cleanString(str) {
            if (typeof str !== 'string' || !str) return str;
            return str
                .replace(this.patterns.bearerJwt, '$1[REDACTED_JWT]')
                .replace(this.patterns.standaloneJwt, '[REDACTED_JWT]')
                .replace(this.patterns.bearerGeneric, '$1[REDACTED]')
                .replace(this.patterns.passwords, '$1=[REDACTED]')
                .replace(this.patterns.creditCards, '[REDACTED_CARD]')
                .replace(this.patterns.cccd, '[REDACTED_ID]')
                .replace(this.patterns.cmnd, '$1: [REDACTED_ID]')
                .replace(this.patterns.clinicalTerms, '[REDACTED_CLINICAL]');
        },

        maskPhone(phone) {
            if (typeof phone !== 'string' || !phone) return null;
            const digits = phone.replace(/[^0-9]/g, '');
            if (digits.length >= 10) {
                return digits.substring(0, 2) + '****' + digits.substring(digits.length - 4);
            }
            return '***';
        },

        cleanObject(obj, depth = 0) {
            if (depth > 6 || obj === null || typeof obj !== 'object') {
                return typeof obj === 'string' ? this.cleanString(obj) : obj;
            }

            if (Array.isArray(obj)) {
                return obj.map(item => this.cleanObject(item, depth + 1));
            }

            const cleaned = {};
            for (const key of Object.keys(obj)) {
                if (this.patterns.sensitiveKeys.test(key)) {
                    cleaned[key] = '[REDACTED]';
                } else {
                    cleaned[key] = this.cleanObject(obj[key], depth + 1);
                }
            }
            return cleaned;
        }
    };

    // --- Helper Utilities ---
    function generateUuid() {
        if (typeof crypto !== 'undefined' && crypto.randomUUID) {
            return crypto.randomUUID();
        }
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    function getSessionId() {
        if (sessionId) return sessionId;
        try {
            if (typeof sessionStorage !== 'undefined') {
                sessionId = sessionStorage.getItem(config.sessionKey);
                if (!sessionId) {
                    sessionId = generateUuid();
                    sessionStorage.setItem(config.sessionKey, sessionId);
                }
            }
        } catch (e) {
            sessionId = generateUuid();
        }
        return sessionId || generateUuid();
    }

    function getCurrentUserInfo() {
        try {
            let saved = null;
            if (typeof sessionStorage !== 'undefined') {
                saved = sessionStorage.getItem('DENTAL_USER');
            }
            if (!saved && typeof localStorage !== 'undefined') {
                saved = localStorage.getItem('DENTAL_USER');
            }
            if (saved) {
                const user = JSON.parse(saved);
                return {
                    userRole: user.role || 'ANONYMOUS',
                    userPhone: Sanitizer.maskPhone(user.phone)
                };
            }
        } catch (e) {
            // Ignore parse errors
        }
        return { userRole: 'ANONYMOUS', userPhone: null };
    }

    // --- Offline Storage Queue (LocalStorage) ---
    function getOfflineQueue() {
        try {
            if (typeof localStorage !== 'undefined') {
                const raw = localStorage.getItem(config.storageKey);
                return raw ? JSON.parse(raw) : [];
            }
        } catch (e) {
            // storage disabled
        }
        return [];
    }

    function saveOfflineQueue(queue) {
        try {
            if (typeof localStorage !== 'undefined') {
                const trimmed = queue.slice(-config.maxOfflineQueue);
                localStorage.setItem(config.storageKey, JSON.stringify(trimmed));
            }
        } catch (e) {
            // storage quota exceeded or disabled
        }
    }

    function pushToOffline(events) {
        if (!events || !events.length) return;
        const current = getOfflineQueue();
        const merged = current.concat(events);
        saveOfflineQueue(merged);
    }

    function flushOfflineQueue() {
        if (typeof navigator !== 'undefined' && navigator.onLine === false) return;
        const offline = getOfflineQueue();
        if (offline && offline.length > 0) {
            if (config.debug) console.log(`[Agrid] Flushing ${offline.length} offline events`);
            sendPayload(offline, function (success) {
                if (success) {
                    try {
                        localStorage.removeItem(config.storageKey);
                    } catch (e) {}
                }
            });
        }
    }

    // --- Non-Blocking Transport Layer ---
    function sendPayload(batch, callback) {
        if (!batch || !batch.length) return;

        const payloadStr = JSON.stringify(batch);

        // 1. Primary: navigator.sendBeacon
        if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
            try {
                const blob = new Blob([payloadStr], { type: 'text/plain;charset=UTF-8' });
                const sent = navigator.sendBeacon(config.endpoint, blob);
                if (sent) {
                    if (callback) callback(true);
                    return;
                }
            } catch (e) {
                // sendBeacon failed, fall through to fetch
            }
        }

        // 2. Fallback: fetch with keepalive: true
        if (typeof fetch === 'function') {
            fetch(config.endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: payloadStr,
                keepalive: true
            }).then(function (res) {
                if (callback) callback(res.ok);
            }).catch(function (err) {
                if (config.debug) console.warn('[Agrid] Fetch transport failed:', err);
                if (callback) callback(false);
            });
        } else {
            if (callback) callback(false);
        }
    }

    // --- Micro-Batching Buffer & Flusher ---
    function flush() {
        if (eventBuffer.length === 0) return;

        const batch = eventBuffer.splice(0, eventBuffer.length);
        sendPayload(batch, function (success) {
            if (!success) {
                // If failed, enqueue to offline storage
                pushToOffline(batch);
            }
        });
    }

    function scheduleFlush() {
        if (eventBuffer.length >= config.batchSize) {
            flush();
        } else if (!flushTimer) {
            flushTimer = setTimeout(function () {
                flushTimer = null;
                flush();
            }, config.flushIntervalMs);
        }
    }

    // --- Core Tracking Function ---
    function track(eventType, eventName, properties = {}) {
        const userInfo = getCurrentUserInfo();
        const currentSessionId = getSessionId();

        // Strict Client-Side Sanitization
        const sanitizedProperties = Sanitizer.cleanObject(properties);
        const sanitizedEventName = Sanitizer.cleanString(eventName || 'unknown_event');
        const pageUrl = typeof window !== 'undefined' ? Sanitizer.cleanString(window.location.href) : '';
        const referrer = typeof document !== 'undefined' ? Sanitizer.cleanString(document.referrer) : '';

        const eventPayload = {
            eventType: eventType || 'CLICK',
            eventName: sanitizedEventName,
            pageUrl: pageUrl,
            referrer: referrer,
            clientSessionId: currentSessionId,
            userPhone: userInfo.userPhone,
            userRole: userInfo.userRole,
            metadata: sanitizedProperties,
            occurredAt: new Date().toISOString()
        };

        eventBuffer.push(eventPayload);
        scheduleFlush();

        if (config.debug) {
            console.log(`[Agrid] Tracked ${eventType}: ${sanitizedEventName}`, eventPayload);
        }
    }

    // --- Public API Object ---
    const Agrid = {
        init: function (userConfig = {}) {
            config = { ...config, ...userConfig };

            if (isInitialized) return;
            isInitialized = true;

            // Setup recurring batch timer
            setInterval(function () {
                if (eventBuffer.length > 0) flush();
            }, config.flushIntervalMs);

            // Lifecycle flush triggers: visibilitychange & beforeunload / pagehide
            if (typeof window !== 'undefined') {
                window.addEventListener('visibilitychange', function () {
                    if (document.visibilityState === 'hidden') flush();
                });
                window.addEventListener('beforeunload', function () {
                    flush();
                });
                window.addEventListener('pagehide', function () {
                    flush();
                });
                window.addEventListener('online', function () {
                    flushOfflineQueue();
                });

                // Auto-track click handler
                if (config.autoTrackClicks) {
                    document.addEventListener('click', function (e) {
                        try {
                            const target = e.target.closest('button, a, [data-agrid-click], [data-track], input[type="submit"]');
                            if (target) {
                                const clickName = target.getAttribute('data-agrid-click')
                                    || target.getAttribute('data-track')
                                    || target.id
                                    || (target.innerText ? target.innerText.trim().substring(0, 32) : 'button_click');

                                Agrid.trackClick(target, clickName, {
                                    tagName: target.tagName,
                                    targetId: target.id || null,
                                    className: target.className ? String(target.className).substring(0, 64) : null,
                                    href: target.href ? target.href.substring(0, 200) : null
                                });
                            }
                        } catch (err) {
                            // Non-fatal
                        }
                    }, true);
                }

                // Initial pageview tracking
                if (config.autoTrackPageview) {
                    Agrid.trackPageView();
                }

                // Flush pending offline events if online
                flushOfflineQueue();
            }

            if (config.debug) {
                console.log('[Agrid] SDK Initialized with 0ms latency impact');
            }
        },

        track: track,

        trackPageView: function (pageUrl, title, properties = {}) {
            track('PAGE_VIEW', 'page_view', {
                title: title || (typeof document !== 'undefined' ? document.title : ''),
                url: pageUrl || (typeof window !== 'undefined' ? window.location.href : ''),
                ...properties
            });
        },

        trackClick: function (element, name, properties = {}) {
            let elInfo = {};
            if (element && typeof element === 'object' && element.nodeType === 1) {
                elInfo = {
                    tag: element.tagName,
                    id: element.id || null,
                    text: element.innerText ? element.innerText.trim().substring(0, 50) : null
                };
            }
            track('CLICK', name || 'element_click', { ...elInfo, ...properties });
        },

        trackFunnel: function (step, data = {}) {
            const funnelType = (data && data.funnelType === 'ORDER_FUNNEL') ? 'ORDER_FUNNEL' : 'BOOKING_FUNNEL';
            const stepName = typeof step === 'number' ? `step_${step}` : String(step);
            track(funnelType, `${funnelType.toLowerCase()}_${stepName}`, {
                step: step,
                ...data
            });
        },

        trackSearch: function (query, resultCount = 0, category = 'all') {
            track('SEARCH', 'search_query', {
                query: Sanitizer.cleanString(query),
                resultCount: resultCount,
                category: category
            });
        },

        trackQrScan: function (code, qrType = 'WARRANTY_CHECK') {
            track('QR_SCAN', 'qr_scanned', {
                code: Sanitizer.cleanString(code),
                qrType: qrType
            });
        },

        trackNav: function (from, to, method = 'link') {
            track('NAVIGATION', 'nav_change', {
                from: Sanitizer.cleanString(from),
                to: Sanitizer.cleanString(to),
                method: method
            });
        },

        flush: flush,
        Sanitizer: Sanitizer
    };

    // --- 0ms Initial Render Latency Initialization ---
    function deferredInit() {
        if (typeof window !== 'undefined') {
            if ('requestIdleCallback' in window) {
                window.requestIdleCallback(function () {
                    Agrid.init();
                }, { timeout: 2000 });
            } else {
                setTimeout(function () {
                    Agrid.init();
                }, 0);
            }
        }
    }

    deferredInit();

    // Export globally
    global.Agrid = Agrid;

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = Agrid;
    }
})(typeof window !== 'undefined' ? window : this);
