/**
 * DentalCare Clinic — Progressive Web App (PWA) Service Worker
 * Version: 1.0.0
 * Scope: /
 */

const CACHE_VERSION = 'dentalcare-pwa-v1';
const STATIC_CACHE = `dentalcare-static-${CACHE_VERSION}`;
const RUNTIME_CACHE = `dentalcare-runtime-${CACHE_VERSION}`;

// Core Application Shell assets required for immediate offline boot
const PRECACHE_URLS = [
    '/',
    '/index.html',
    '/dashboard.html',
    '/booking.html',
    '/manifest.json',
    '/css/tailwind.min.css',
    '/css/clinic-ui-refresh.css',
    '/js/app.js',
    '/js/it-team.js',
    '/js/agrid-sdk.js',
    '/icons/icon-192.png',
    '/icons/icon-512.png',
    '/icons/icon.svg',
    '/images/partners/vpbank.svg',
    '/images/partners/shinhan.svg',
    '/images/partners/fe-credit.svg',
    '/images/partners/home-credit.svg',
    '/images/partners/hd-saison.svg',
    '/images/partners/techcombank.svg',
    '/images/doctors/doctor-thang.svg',
    '/images/doctors/doctor-tuan.svg',
    '/images/doctors/doctor-lan.svg',
    '/images/articles/article-ortho.svg',
    '/images/articles/article-implant.svg',
    '/images/articles/article-wisdom.svg',
    '/images/articles/article-veneer.svg',
    'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css',
    'https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.5/gsap.min.js'
];

// 1. Install Event: Pre-cache App Shell and activate immediately
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(STATIC_CACHE)
            .then((cache) => {
                console.log('[SW] Pre-caching static app shell...');
                return Promise.allSettled(
                    PRECACHE_URLS.map((url) =>
                        cache.add(url).catch((err) => {
                            console.warn('[SW] Failed to pre-cache item:', url, err);
                        })
                    )
                );
            })
            .then(() => self.skipWaiting())
    );
});

// 2. Activate Event: Clean up outdated cache stores and claim clients
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) => {
            return Promise.all(
                keys.map((key) => {
                    if (key !== STATIC_CACHE && key !== RUNTIME_CACHE) {
                        console.log('[SW] Removing old cache store:', key);
                        return caches.delete(key);
                    }
                })
            );
        }).then(() => self.clients.claim())
    );
});

// 3. Fetch Event: Intelligent routing based on URL and request type
self.addEventListener('fetch', (event) => {
    const { request } = event;
    const url = new URL(request.url);

    // Bypass non-GET requests (POST, PUT, DELETE) immediately to network
    if (request.method !== 'GET') {
        return;
    }

    // Bypass WebSockets, Spring Actuator, H2 Console, and Sensitive REST APIs
    // Strict Medical Privacy Guard: Never cache patient EMR, auth tokens, or live bookings
    if (
        url.pathname.startsWith('/api/') ||
        url.pathname.startsWith('/ws-dental') ||
        url.pathname.startsWith('/actuator') ||
        url.pathname.startsWith('/h2-console') ||
        url.pathname.startsWith('/swagger')
    ) {
        event.respondWith(
            fetch(request).catch(() => {
                return new Response(
                    JSON.stringify({
                        success: false,
                        message: 'Bạn đang ngoại tuyến. Vui lòng kết nối Internet để đồng bộ dữ liệu phòng khám.',
                        offline: true
                    }),
                    {
                        status: 503,
                        headers: { 'Content-Type': 'application/json; charset=utf-8' }
                    }
                );
            })
        );
        return;
    }

    // HTML Navigation requests: Network-First with App Shell fallback
    if (request.mode === 'navigate' || (request.headers.get('accept') && request.headers.get('accept').includes('text/html'))) {
        event.respondWith(
            fetch(request)
                .then((networkResponse) => {
                    if (networkResponse && networkResponse.status === 200) {
                        const responseClone = networkResponse.clone();
                        caches.open(STATIC_CACHE).then((cache) => cache.put(request, responseClone));
                    }
                    return networkResponse;
                })
                .catch(async () => {
                    const cachedResponse = await caches.match(request);
                    if (cachedResponse) return cachedResponse;
                    return caches.match('/index.html');
                })
        );
        return;
    }

    // Static Assets (JS, CSS, Images, Fonts): Stale-While-Revalidate
    event.respondWith(
        caches.match(request).then((cachedResponse) => {
            const fetchPromise = fetch(request)
                .then((networkResponse) => {
                    if (networkResponse && networkResponse.status === 200) {
                        const responseClone = networkResponse.clone();
                        caches.open(RUNTIME_CACHE).then((cache) => cache.put(request, responseClone));
                    }
                    return networkResponse;
                })
                .catch(() => cachedResponse);

            return cachedResponse || fetchPromise;
        })
    );
});
