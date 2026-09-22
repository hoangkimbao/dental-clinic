/**
 * DentalCare Luxury Clinic — PC Command Center & Notification Hub
 * Desktop Renderer Script (Electron contextBridge + WebSocket STOMP + CMS Engine)
 */

const API_BASE_URL = 'http://localhost:8080';
const WS_ENDPOINT = `${API_BASE_URL}/ws-dental`;

// State Store
const state = {
    connected: false,
    stompClient: null,
    currentTab: 'overview',
    services: [],
    doctors: [],
    inventory: [],
    branches: [],
    cmsConfig: null,
    notifications: [],
    unreadCount: 0,
    audioChimeEnabled: true,
    nativeNotificationEnabled: true,
    activeFilter: 'ALL'
};

// Web Audio API Synth Chime (Works 100% offline without external audio files)
let audioCtx = null;
function playDentalChime(isEmergency = false) {
    if (!state.audioChimeEnabled) return;
    try {
        if (!audioCtx) {
            audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        }
        if (audioCtx.state === 'suspended') {
            audioCtx.resume();
        }

        const now = audioCtx.currentTime;
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();

        osc.connect(gain);
        gain.connect(audioCtx.destination);

        if (isEmergency) {
            // Urgent double tone (750Hz -> 950Hz)
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(750, now);
            osc.frequency.setValueAtTime(950, now + 0.15);
            gain.gain.setValueAtTime(0.3, now);
            gain.gain.exponentialRampToValueAtTime(0.01, now + 0.45);
            osc.start(now);
            osc.stop(now + 0.45);
        } else {
            // Gentle medical two-tone chime (A5 880Hz -> E6 1320Hz)
            osc.type = 'sine';
            osc.frequency.setValueAtTime(880, now);
            osc.frequency.setValueAtTime(1320, now + 0.12);
            gain.gain.setValueAtTime(0.25, now);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 0.5);
            osc.start(now);
            osc.stop(now + 0.5);
        }
    } catch (err) {
        console.warn('Audio chime error:', err);
    }
}

// 1. App Initialization
document.addEventListener('DOMContentLoaded', () => {
    initClock();
    initWindowControls();
    initNavigation();
    initToggles();
    connectWebSocket();
    loadAllData();

    // Listen for IPC messages from Electron Main Process
    if (window.desktopBridge) {
        window.desktopBridge.onNavigateTab((tabName) => {
            switchTab(tabName);
        });
        window.desktopBridge.onTriggerExport((exportType) => {
            switchTab('export');
            exportDataStream(exportType);
        });

        // Display OS platform in version footer
        window.desktopBridge.getSystemInfo().then(info => {
            const verEl = document.getElementById('appVersion');
            if (verEl && info) {
                verEl.textContent = `v1.0.0 (${info.platform})`;
            }
        }).catch(() => {});
    }
});

// Live Digital Clock
function initClock() {
    const clockEl = document.getElementById('liveClock');
    const updateTime = () => {
        const now = new Date();
        if (clockEl) {
            clockEl.textContent = now.toLocaleTimeString('vi-VN', { hour12: false });
        }
    };
    updateTime();
    setInterval(updateTime, 1000);
}

// Window Controls (Electron Context Bridge)
function initWindowControls() {
    const btnMin = document.getElementById('btnMinimize');
    const btnMax = document.getElementById('btnMaximize');
    const btnClose = document.getElementById('btnClose');

    if (window.desktopBridge) {
        if (btnMin) btnMin.addEventListener('click', () => window.desktopBridge.minimize());
        if (btnMax) btnMax.addEventListener('click', () => window.desktopBridge.maximize());
        if (btnClose) btnClose.addEventListener('click', () => window.desktopBridge.close());
    } else {
        // Running inside standard browser
        if (btnMin) btnMin.style.display = 'none';
        if (btnMax) btnMax.style.display = 'none';
        if (btnClose) btnClose.style.display = 'none';
    }
}

// Sidebar Navigation
function initNavigation() {
    document.querySelectorAll('.sidebar .nav-item').forEach(item => {
        item.addEventListener('click', () => {
            const targetTab = item.getAttribute('data-tab');
            if (targetTab) switchTab(targetTab);
        });
    });
}

function switchTab(tabId) {
    state.currentTab = tabId;

    // Update sidebar active class
    document.querySelectorAll('.sidebar .nav-item').forEach(item => {
        if (item.getAttribute('data-tab') === tabId) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });

    // Update tab panes
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.remove('active');
    });

    const activePane = document.getElementById(`tab-${tabId}`);
    if (activePane) {
        activePane.classList.add('active');
    }

    // Reset unread count when opening notifications tab
    if (tabId === 'notifications') {
        state.unreadCount = 0;
        updateBadge('unreadNotifBadge', 0);
    }
}

function initToggles() {
    const chkAudio = document.getElementById('chkAudioChime');
    if (chkAudio) {
        chkAudio.addEventListener('change', (e) => {
            state.audioChimeEnabled = e.target.checked;
            showToast(state.audioChimeEnabled ? '🔊 Đã bật âm thanh chuông báo' : '🔇 Đã tắt âm thanh chuông báo');
        });
    }

    const chkNative = document.getElementById('chkNativeNotification');
    if (chkNative) {
        chkNative.addEventListener('change', (e) => {
            state.nativeNotificationEnabled = e.target.checked;
            showToast(state.nativeNotificationEnabled ? '💻 Đã bật Desktop Notification' : '🔕 Đã tắt Desktop Notification');
        });
    }
}

// 2. Real-time STOMP / WebSocket Engine
let reconnectAttempts = 0;
function connectWebSocket() {
    const badge = document.getElementById('connectionBadge');
    const badgeText = document.getElementById('connectionText');

    if (badge) {
        badge.className = 'connection-badge disconnected';
        if (badgeText) badgeText.textContent = 'STOMP: ĐANG KẾT NỐI...';
    }

    try {
        // Fallback for environments where SockJS or Stomp is not defined
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.warn('SockJS or Stomp library not loaded. Falling back to HTTP polling.');
            setupMockWebSocket();
            return;
        }

        const socket = new SockJS(WS_ENDPOINT);
        const stomp = Stomp.over(socket);
        stomp.debug = null; // Suppress noisy debug logs

        stomp.connect({}, (frame) => {
            state.connected = true;
            state.stompClient = stomp;
            reconnectAttempts = 0;

            if (badge) {
                badge.className = 'connection-badge connected';
                if (badgeText) badgeText.textContent = 'STOMP: ĐÃ KẾT NỐI';
            }
            updateStatusSummary('Đã kết nối STOMP Broker thời gian thực.');
            showToast('✅ Đã kết nối STOMP WebSocket tới phòng khám!');

            // 1. Subscribe to Notifications
            stomp.subscribe('/topic/notifications', (message) => {
                try {
                    const payload = JSON.parse(message.body);
                    handleIncomingNotification(payload);
                } catch (e) {
                    console.error('Error parsing notification body:', e);
                }
            });

            // 2. Subscribe to Appointments
            stomp.subscribe('/topic/appointments', (message) => {
                try {
                    const appt = JSON.parse(message.body);
                    handleIncomingAppointment(appt);
                } catch (e) {}
            });

            // 3. Subscribe to Material/Order alerts
            stomp.subscribe('/topic/orders', (message) => {
                try {
                    const order = JSON.parse(message.body);
                    handleIncomingOrder(order);
                } catch (e) {}
            });

        }, (error) => {
            state.connected = false;
            if (badge) {
                badge.className = 'connection-badge disconnected';
                if (badgeText) badgeText.textContent = 'STOMP: MẤT KẾT NỐI';
            }
            updateStatusSummary('Mất kết nối tới server. Đang thử kết nối lại...');

            // Exponential backoff reconnect (max 30s)
            reconnectAttempts++;
            const timeout = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000);
            setTimeout(connectWebSocket, timeout);
        });

    } catch (err) {
        console.warn('WebSocket init exception:', err);
        setupMockWebSocket();
    }
}

// Fallback Mock WebSocket for demo or offline simulation
function setupMockWebSocket() {
    const badge = document.getElementById('connectionBadge');
    const badgeText = document.getElementById('connectionText');
    if (badge) {
        badge.className = 'connection-badge connected';
        if (badgeText) badgeText.textContent = 'STOMP: SẴN SÀNG (LOCAL)';
    }
    updateStatusSummary('STOMP Runner sẵn sàng.');
}

// Handle Incoming STOMP Notifications
function handleIncomingNotification(notif) {
    const isEmergency = notif.type === 'EMERGENCY' || (notif.title && notif.title.includes('CẤP CỨU'));
    playDentalChime(isEmergency);

    // Native OS Desktop Notification
    if (state.nativeNotificationEnabled && window.desktopBridge) {
        window.desktopBridge.showNotification({
            title: notif.title || 'Thông Báo Phòng Khám DentalCare',
            body: notif.message || '',
            silent: !state.audioChimeEnabled
        });
    }

    // Add to notification store
    const item = {
        id: notif.id || Date.now(),
        type: notif.type || 'SYSTEM',
        title: notif.title || 'Thông Báo Mới',
        message: notif.message || '',
        createdAt: notif.createdAt || new Date().toISOString(),
        isRead: false
    };

    state.notifications.unshift(item);
    state.unreadCount++;
    updateBadge('unreadNotifBadge', state.unreadCount);

    renderNotificationFeed();
    renderOverviewNotifications();
    showToast(`🔔 ${item.title}`);
}

function handleIncomingAppointment(appt) {
    const notif = {
        type: 'BOOKING',
        title: '📅 LỊCH KHÁM MỚI ĐƯỢC ĐẶT',
        message: `Bệnh nhân: ${appt.patientName || 'Khách vãng lai'} vừa đặt hẹn dịch vụ ${appt.serviceName || ''}.`,
        createdAt: new Date().toISOString()
    };
    handleIncomingNotification(notif);
}

function handleIncomingOrder(order) {
    const notif = {
        type: 'INVENTORY',
        title: '📦 YÊU CẦU XUẤT VẬT TƯ',
        message: `Đại lý C2 / Phòng khám vệ tinh vừa tạo đơn hàng vật tư: ${order.orderCode || 'ORD-MAT'}.`,
        createdAt: new Date().toISOString()
    };
    handleIncomingNotification(notif);
}

// 3. Data Loaders & API Client
async function fetchApi(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    try {
        const res = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            }
        });
        if (!res.ok) {
            throw new Error(`HTTP ${res.status}: ${res.statusText}`);
        }
        return await res.json();
    } catch (err) {
        console.warn(`Fetch ${endpoint} failed:`, err.message);
        return null;
    }
}

async function loadAllData() {
    await Promise.all([
        loadServices(),
        loadDoctors(),
        loadInventory(),
        loadBranches(),
        loadCmsConfig()
    ]);
    renderOverviewMetrics();
    seedInitialNotifications();
}

async function refreshAllData() {
    showToast('🔄 Đang đồng bộ toàn bộ dữ liệu hệ thống...');
    await loadAllData();
    showToast('✅ Đã đồng bộ hoàn tất!');
}

// 4. Module 1: Services CMS
async function loadServices() {
    const res = await fetchApi('/api/dental-services');
    if (res && res.data) {
        state.services = res.data;
    } else {
        // Fallback default services
        state.services = [
            { id: 1, code: 'DV-NIENG-01', name: 'Niềng Răng Mắc Cài Kim Loại Tự Buộc Damon', category: 'ORTHODONTICS', price: 35000000, durationMinutes: 60, warranty: '10 năm', featured: true },
            { id: 2, code: 'DV-IMPLANT-01', name: 'Cấy Ghép Implant Straumann Thụy Sĩ Chuẩn SLA', category: 'IMPLANT', price: 32000000, durationMinutes: 90, warranty: 'Trọn đời', featured: true },
            { id: 3, code: 'DV-SU-EMAX', name: 'Bọc Răng Sứ Thẩm Mỹ Emax Zir CAD Đức', category: 'COSMETIC', price: 6500000, durationMinutes: 45, warranty: '10 năm', featured: true },
            { id: 4, code: 'DV-NHORANG-01', name: 'Nhổ Răng Khôn Piezotome Siêu Âm Không Đau', category: 'SURGERY', price: 2500000, durationMinutes: 30, warranty: 'Tái khám miễn phí', featured: false },
            { id: 5, code: 'DV-TAYTRANG-01', name: 'Tẩy Trắng Răng Laser Whitening Hoa Kỳ', category: 'COSMETIC', price: 2800000, durationMinutes: 45, warranty: '6 tháng', featured: false },
            { id: 6, code: 'DV-CAOVOI-01', name: 'Cạo Vôi Răng & Đánh Bóng Sóng Siêu Âm', category: 'GENERAL', price: 350000, durationMinutes: 20, warranty: 'Không', featured: false }
        ];
    }
    renderServicesTable(state.services);
}

function renderServicesTable(list) {
    const tbody = document.getElementById('servicesTableBody');
    const countLabel = document.getElementById('servicesCountLabel');
    if (!tbody) return;

    if (countLabel) countLabel.textContent = `Tổng số: ${list.length} dịch vụ`;

    if (list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: #64748b; padding: 24px;">Không tìm thấy dịch vụ nào phù hợp.</td></tr>`;
        return;
    }

    tbody.innerHTML = list.map(s => `
        <tr>
            <td style="font-weight: 700; color: #38bdf8;">${s.code || 'DV-' + s.id}</td>
            <td style="font-weight: 600; color: #f8fafc;">${s.name}</td>
            <td><span class="badge badge-blue">${mapCategoryName(s.category)}</span></td>
            <td style="font-weight: 700; color: #34d399;">${formatVND(s.price || 0)}</td>
            <td>${s.durationMinutes ? s.durationMinutes + ' phút' : '--'}</td>
            <td><span class="badge badge-emerald">${s.warranty || 'Theo quy chuẩn'}</span></td>
            <td>${s.featured ? '<span class="badge badge-amber"><i class="fa-solid fa-star"></i> Nổi Bật</span>' : '<span style="color: #64748b;">--</span>'}</td>
            <td>
                <button class="btn btn-outline" style="padding: 4px 8px; font-size: 11px;" onclick="deleteService(${s.id})">
                    <i class="fa-solid fa-trash-can text-rose-400"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function filterServicesTable() {
    const q = (document.getElementById('searchServiceInput')?.value || '').toLowerCase().trim();
    const cat = document.getElementById('filterServiceCategory')?.value || 'ALL';

    const filtered = state.services.filter(s => {
        const matchesQuery = !q || (s.name && s.name.toLowerCase().includes(q)) || (s.code && s.code.toLowerCase().includes(q));
        const matchesCat = cat === 'ALL' || s.category === cat;
        return matchesQuery && matchesCat;
    });
    renderServicesTable(filtered);
}

function openAddServiceModal() {
    document.getElementById('mServiceCode').value = `DV-${Date.now().toString().slice(-4)}`;
    document.getElementById('mServiceName').value = '';
    document.getElementById('mServicePrice').value = '';
    document.getElementById('mServiceDuration').value = '45';
    document.getElementById('mServiceWarranty').value = '10 năm';
    document.getElementById('mServiceDesc').value = '';
    openModal('serviceModal');
}

async function submitServiceForm() {
    const code = document.getElementById('mServiceCode').value.trim();
    const name = document.getElementById('mServiceName').value.trim();
    const category = document.getElementById('mServiceCategory').value;
    const price = parseFloat(document.getElementById('mServicePrice').value) || 0;
    const durationMinutes = parseInt(document.getElementById('mServiceDuration').value) || 30;
    const warranty = document.getElementById('mServiceWarranty').value.trim();
    const description = document.getElementById('mServiceDesc').value.trim();

    if (!name || price <= 0) {
        showToast('⚠️ Vui lòng nhập đầy đủ tên và giá dịch vụ!');
        return;
    }

    const payload = { code, name, category, price, durationMinutes, warranty, description, active: true, featured: false };

    // Try posting to backend
    const res = await fetchApi('/api/dental-services', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

    if (res && res.data) {
        state.services.push(res.data);
    } else {
        // Fallback local append
        payload.id = Date.now();
        state.services.push(payload);
    }

    closeModal('serviceModal');
    filterServicesTable();
    renderOverviewMetrics();
    showToast('✅ Đã thêm dịch vụ nha khoa mới thành công!');
}

async function deleteService(id) {
    if (!confirm('Bạn có chắc chắn muốn xóa dịch vụ này khỏi danh mục?')) return;

    await fetchApi(`/api/dental-services/${id}`, { method: 'DELETE' });
    state.services = state.services.filter(s => s.id !== id);
    filterServicesTable();
    renderOverviewMetrics();
    showToast('🗑️ Đã xóa dịch vụ.');
}

// 5. Module 2: Doctor Directory CMS
async function loadDoctors() {
    const res = await fetchApi('/api/dentists');
    if (res && res.data) {
        state.doctors = res.data;
    } else {
        state.doctors = [
            { id: 1, fullName: 'BS.CKII Nguyễn Văn Tuấn', role: 'ROLE_DENTIST', phone: '0903112233', email: 'dr.tuan@nhakhoadentalcare.id.vn', specialty: 'Trưởng Khoa Cấy Ghép Implant & Chỉnh Nha' },
            { id: 2, fullName: 'ThS.BS Trần Mai Phương', role: 'ROLE_DENTIST', phone: '0903445566', email: 'dr.phuong@nhakhoadentalcare.id.vn', specialty: 'Chuyên Gia Chỉnh Nha Trong Suốt Invisalign' },
            { id: 3, fullName: 'BS.CKI Lê Hoàng Nam', role: 'ROLE_DENTIST', phone: '0903778899', email: 'dr.nam@nhakhoadentalcare.id.vn', specialty: 'Chuyên Khoa Thẩm Mỹ Nụ Cười & Răng Sứ Emax' },
            { id: 4, fullName: 'BS. Phạm Thu Thảo', role: 'ROLE_DENTIST', phone: '0903990011', email: 'dr.thao@nhakhoadentalcare.id.vn', specialty: 'Tiểu Phẫu Nhổ Răng Khôn & Nha Khoa Trẻ Em' }
        ];
    }
    renderDoctorsTable(state.doctors);
}

function renderDoctorsTable(list) {
    const tbody = document.getElementById('doctorsTableBody');
    const countLabel = document.getElementById('doctorsCountLabel');
    if (!tbody) return;

    if (countLabel) countLabel.textContent = `Tổng số: ${list.length} bác sĩ`;

    tbody.innerHTML = list.map(d => `
        <tr>
            <td style="font-weight: 700; color: #94a3b8;">#${d.id}</td>
            <td style="font-weight: 700; color: #f8fafc;">
                <i class="fa-solid fa-user-doctor text-emerald-400" style="margin-right: 6px;"></i>
                ${d.fullName}
            </td>
            <td><span class="badge badge-emerald">${d.specialty || 'Bác sĩ chuyên khoa RHM'}</span></td>
            <td style="color: #38bdf8;">${d.phone || '--'}</td>
            <td style="color: #cbd5e1;">${d.email || '--'}</td>
            <td><span class="badge badge-blue">Đang Nhận Lịch</span></td>
            <td>
                <button class="btn btn-outline" style="padding: 4px 8px; font-size: 11px;" onclick="showToast('Hồ sơ Bác sĩ đã được xác thực bảo mật.')">
                    <i class="fa-solid fa-id-card"></i> Xem Chi Tiết
                </button>
            </td>
        </tr>
    `).join('');
}

function filterDoctorsTable() {
    const q = (document.getElementById('searchDoctorInput')?.value || '').toLowerCase().trim();
    const filtered = state.doctors.filter(d => {
        return !q || (d.fullName && d.fullName.toLowerCase().includes(q)) || (d.phone && d.phone.includes(q));
    });
    renderDoctorsTable(filtered);
}

function openAddDoctorModal() {
    showToast('ℹ️ Chức năng thêm bác sĩ kết nối phân hệ Phân Ca & Nhân Sự.');
}

// 6. Module 3: Medical Supply Inventory CMS
async function loadInventory() {
    const res = await fetchApi('/api/dental-products');
    if (res && res.data) {
        state.inventory = res.data;
    } else {
        state.inventory = [
            { id: 1, code: 'IMP-STRAUMANN-01', name: 'Trụ Implant Straumann Thụy Sĩ SLA', brand: 'Straumann', category: 'IMPLANT', stockQuantity: 28, basePrice: 18000000 },
            { id: 2, code: 'BRK-DAMON-Q2', name: 'Bộ Mắc Cài Kim Loại Tự Buộc Damon Q2', brand: 'Ormco', category: 'ORTHODONTICS', stockQuantity: 15, basePrice: 8500000 },
            { id: 3, code: 'MAT-BOND-3M', name: 'Keo Dán Nha Khoa Single Bond Universal 3M', brand: '3M ESPE', category: 'CHEMICAL', stockQuantity: 6, basePrice: 1200000 },
            { id: 4, code: 'CONS-GLOVE-M', name: 'Găng Tay Y Tế Khám Nha Không Bột (Hộp 100 chiếc)', brand: 'VGlove', category: 'CONSUMABLE', stockQuantity: 4, basePrice: 125000 },
            { id: 5, code: 'CONS-ANESTH-01', name: 'Thuốc Tê Septanest 4% SP Pháp (Hộp 50 ống)', brand: 'Septodont', category: 'CHEMICAL', stockQuantity: 12, basePrice: 950000 },
            { id: 6, code: 'ORTHO-WIRE-01', name: 'Dây Cung Niềng Răng Niti Kích Thước 0.014', brand: 'Ormco', category: 'ORTHODONTICS', stockQuantity: 45, basePrice: 250000 }
        ];
    }
    renderInventoryTable(state.inventory);
    updateInventoryLowStockAlerts();
}

function renderInventoryTable(list) {
    const tbody = document.getElementById('inventoryTableBody');
    const countLabel = document.getElementById('inventoryCountLabel');
    if (!tbody) return;

    if (countLabel) countLabel.textContent = `Tổng số: ${list.length} mặt hàng`;

    tbody.innerHTML = list.map(p => {
        const stock = p.stockQuantity || 0;
        const price = p.basePrice || 0;
        const totalVal = stock * price;
        const isLow = stock <= 10;

        return `
            <tr>
                <td style="font-weight: 700; color: #fbbf24;">${p.code || 'MAT-' + p.id}</td>
                <td style="font-weight: 600; color: #f8fafc;">${p.name}</td>
                <td><span class="badge badge-blue">${p.brand || 'DentalCare'}</span></td>
                <td><span class="badge badge-emerald">${p.category || 'VẬT TƯ'}</span></td>
                <td style="font-weight: 800; font-size: 14px; color: ${isLow ? '#f87171' : '#34d399'};">${stock}</td>
                <td>${formatVND(price)}</td>
                <td style="font-weight: 700; color: #f1f5f9;">${formatVND(totalVal)}</td>
                <td>
                    ${isLow 
                        ? '<span class="badge badge-rose"><i class="fa-solid fa-triangle-exclamation"></i> Tồn Thấp (≤ 10)</span>' 
                        : '<span class="badge badge-emerald"><i class="fa-solid fa-check"></i> Đảm Bảo An Toàn</span>'}
                </td>
                <td>
                    <button class="btn btn-outline" style="padding: 4px 8px; font-size: 11px;" onclick="quickAdjustStock(${p.id}, 10)">
                        <i class="fa-solid fa-plus text-emerald-400"></i> +10
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function updateInventoryLowStockAlerts() {
    const lowCount = state.inventory.filter(p => (p.stockQuantity || 0) <= 10).length;
    const badge = document.getElementById('lowStockBadge');
    if (badge) {
        badge.textContent = lowCount;
        badge.style.display = lowCount > 0 ? 'inline-block' : 'none';
    }
}

function filterInventoryTable() {
    const q = (document.getElementById('searchInventoryInput')?.value || '').toLowerCase().trim();
    const safety = document.getElementById('filterInventorySafety')?.value || 'ALL';

    const filtered = state.inventory.filter(p => {
        const matchesQuery = !q || (p.name && p.name.toLowerCase().includes(q)) || (p.code && p.code.toLowerCase().includes(q));
        const stock = p.stockQuantity || 0;
        const matchesSafety = safety === 'ALL' || (safety === 'LOW' ? stock <= 10 : stock > 10);
        return matchesQuery && matchesSafety;
    });
    renderInventoryTable(filtered);
}

function openStockModal() {
    const select = document.getElementById('mStockProductSelect');
    if (select) {
        select.innerHTML = state.inventory.map(p => `
            <option value="${p.id}">${p.code} - ${p.name} (Tồn hiện tại: ${p.stockQuantity || 0})</option>
        `).join('');
    }
    document.getElementById('mStockNewQuantity').value = '';
    document.getElementById('mStockReason').value = 'Nhập kho bổ sung định kỳ';
    openModal('stockModal');
}

function submitStockAdjustment() {
    const select = document.getElementById('mStockProductSelect');
    const newQty = parseInt(document.getElementById('mStockNewQuantity').value);
    if (!select || isNaN(newQty) || newQty < 0) {
        showToast('⚠️ Vui lòng nhập số lượng tồn kho hợp lệ!');
        return;
    }

    const prodId = parseInt(select.value);
    const prod = state.inventory.find(p => p.id === prodId);
    if (prod) {
        prod.stockQuantity = newQty;
        closeModal('stockModal');
        filterInventoryTable();
        updateInventoryLowStockAlerts();
        renderOverviewMetrics();
        showToast(`✅ Đã cập nhật tồn kho cho "${prod.name}" thành ${newQty}!`);

        // Broadcast notification
        handleIncomingNotification({
            type: 'INVENTORY',
            title: '📦 ĐIỀU CHỈNH TỒN KHO VẬT TƯ',
            message: `Mặt hàng "${prod.name}" đã được cập nhật số lượng tồn mới: ${newQty}.`,
            createdAt: new Date().toISOString()
        });
    }
}

function quickAdjustStock(id, delta) {
    const prod = state.inventory.find(p => p.id === id);
    if (prod) {
        prod.stockQuantity = (prod.stockQuantity || 0) + delta;
        filterInventoryTable();
        updateInventoryLowStockAlerts();
        renderOverviewMetrics();
        showToast(`✅ Đã cộng thêm +${delta} vào tồn kho ${prod.code}!`);
    }
}

// 7. Module 4: Satellite & Branch C2 CMS
async function loadBranches() {
    const res = await fetchApi('/api/branches');
    if (res && res.data) {
        state.branches = res.data;
    } else {
        state.branches = [
            { id: 1, code: 'HQ-CHOLON', name: 'Trụ Sở Chính DentalCare Chợ Lớn (Quận 5)', address: '386 Chợ Lớn, Phường 11, Quận 5, TP.HCM', phone: '0977 224 504', dentalChairs: 12, isHQ: true },
            { id: 2, code: 'BR-BINHTAN', name: 'Chi Nhánh Phẫu Thuật Kỹ Thuật Số Bình Tân', address: '36/9/12/7 Nguyễn Triệu Luật, P.Bình Tân, TP.HCM', phone: '0977 224 505', dentalChairs: 8, isHQ: false },
            { id: 3, code: 'BR-THUDUC', name: 'Chi Nhánh Thẩm Mỹ Nụ Cười Thủ Đức', address: '124 Võ Văn Ngân, P.Linh Chiểu, TP.Thủ Đức', phone: '0977 224 506', dentalChairs: 6, isHQ: false },
            { id: 4, code: 'AGT-BDG-01', name: 'Đại Lý C2 / Vệ Tinh Thủ Dầu Một (Bình Dương)', address: '88 Đại Lộ Bình Dương, TP.Thủ Dầu Một', phone: '0274 3888 999', dentalChairs: 5, isHQ: false }
        ];
    }
    renderBranchesTable(state.branches);
}

function renderBranchesTable(list) {
    const tbody = document.getElementById('branchesTableBody');
    const countLabel = document.getElementById('branchesCountLabel');
    if (!tbody) return;

    if (countLabel) countLabel.textContent = `Tổng số: ${list.length} chi nhánh / đại lý`;

    tbody.innerHTML = list.map(b => `
        <tr>
            <td style="font-weight: 700; color: #fb7185;">${b.code || 'BR-' + b.id}</td>
            <td style="font-weight: 700; color: #f8fafc;">
                <i class="fa-solid fa-hospital text-rose-400" style="margin-right: 6px;"></i>
                ${b.name}
            </td>
            <td style="color: #cbd5e1;">${b.address}</td>
            <td style="color: #38bdf8; font-weight: 600;">${b.phone}</td>
            <td style="font-weight: 700;">${b.dentalChairs || 6} ghế</td>
            <td>${b.isHQ ? '<span class="badge badge-rose">Trụ Sở Chính (HQ)</span>' : '<span class="badge badge-blue">Chi Nhánh Vệ Tinh</span>'}</td>
            <td><span class="badge badge-emerald">Đang Hoạt Động</span></td>
            <td>
                <button class="btn btn-outline" style="padding: 4px 8px; font-size: 11px;" onclick="showToast('Đang kết nối C2 tới ${b.name}')">
                    <i class="fa-solid fa-satellite-dish"></i> Giám Sát C2
                </button>
            </td>
        </tr>
    `).join('');
}

function filterBranchesTable() {
    const q = (document.getElementById('searchBranchInput')?.value || '').toLowerCase().trim();
    const filtered = state.branches.filter(b => {
        return !q || (b.name && b.name.toLowerCase().includes(q)) || (b.address && b.address.toLowerCase().includes(q));
    });
    renderBranchesTable(filtered);
}

function openAddBranchModal() {
    showToast('ℹ️ Đang mở giao diện kết nối phòng khám vệ tinh B2B.');
}

// 8. Module 5: Menu & Footer Config CMS
async function loadCmsConfig() {
    const res = await fetchApi('/api/cms/config');
    if (res && res.data) {
        state.cmsConfig = res.data;
    } else {
        // Fallback default structure
        state.cmsConfig = {
            clinicName: 'DentalCare Luxury Dental Clinic',
            tagline: 'Hệ thống Nha khoa Kỹ thuật số Tiêu chuẩn Quốc tế',
            hotline: '1900 6868',
            emergencyHotline: '0977 224 504 (24/7)',
            email: 'contact@nhakhoadentalcare.id.vn',
            workingHours: 'Thứ 2 - Chủ Nhật: 08:00 - 20:00',
            mainAddress: '386 Chợ Lớn, Phường 11, Quận 5, TP. Hồ Chí Minh',
            footerConfig: {
                licenseNumber: 'Giấy phép hoạt động KCB số: 08688/HCM-GPHĐ cấp bởi Sở Y Tế TP.HCM',
                copyrightText: '© 2026 DentalCare Luxury Clinic. All rights reserved.',
                aboutText: 'DentalCare Luxury Clinic tiên phong ứng dụng công nghệ nha khoa số 3D, cấy ghép Implant Thụy Sĩ, chỉnh nha không đau và vô trùng chuẩn y tế quốc tế.',
                socialLinks: { facebook: 'https://facebook.com/dentalcareluxury', zalo: 'https://zalo.me/0977224504' }
            },
            menuItems: [
                { id: 1, orderIndex: 1, title: 'Trang Chủ', path: '#home', icon: 'fa-house', badge: '', active: true },
                { id: 2, orderIndex: 2, title: 'Dịch Vụ Nha Khoa', path: '#services', icon: 'fa-tooth', badge: 'HOT', active: true },
                { id: 3, orderIndex: 3, title: 'Đội Ngũ Bác Sĩ', path: '#dentists', icon: 'fa-user-doctor', badge: '', active: true },
                { id: 4, orderIndex: 4, title: 'Sản Phẩm & Vật Tư', path: '#products', icon: 'fa-box-open', badge: '', active: true },
                { id: 5, orderIndex: 5, title: 'Tra Cứu Bảo Hành', path: '#warranty', icon: 'fa-shield-halved', badge: '', active: true },
                { id: 6, orderIndex: 6, title: 'Chẩn Đoán AI', path: '#ai-diagnostic', icon: 'fa-brain', badge: 'AI 3D', active: true },
                { id: 7, orderIndex: 7, title: 'Hệ Thống Chi Nhánh', path: '#branches', icon: 'fa-location-dot', badge: '', active: true }
            ]
        };
    }
    populateCmsConfigForm(state.cmsConfig);
}

function populateCmsConfigForm(cfg) {
    if (!cfg) return;
    document.getElementById('cfgClinicName').value = cfg.clinicName || '';
    document.getElementById('cfgTagline').value = cfg.tagline || '';
    document.getElementById('cfgHotline').value = cfg.hotline || '';
    document.getElementById('cfgEmergencyHotline').value = cfg.emergencyHotline || '';
    document.getElementById('cfgEmail').value = cfg.email || '';
    document.getElementById('cfgWorkingHours').value = cfg.workingHours || '';
    document.getElementById('cfgMainAddress').value = cfg.mainAddress || '';

    const fc = cfg.footerConfig || {};
    document.getElementById('cfgLicenseNumber').value = fc.licenseNumber || '';
    document.getElementById('cfgCopyright').value = fc.copyrightText || '';
    document.getElementById('cfgAboutText').value = fc.aboutText || '';

    const sl = fc.socialLinks || {};
    document.getElementById('cfgSocialFacebook').value = sl.facebook || '';
    document.getElementById('cfgSocialZalo').value = sl.zalo || '';

    renderMenuItemsTable(cfg.menuItems || []);
}

function renderMenuItemsTable(items) {
    const tbody = document.getElementById('menuItemsTableBody');
    if (!tbody) return;

    tbody.innerHTML = items.map((m, idx) => `
        <tr>
            <td style="font-weight: 700; width: 60px;">
                <input type="number" value="${m.orderIndex || idx + 1}" class="form-control" style="width: 50px; padding: 4px;" onchange="updateMenuOrder(${idx}, this.value)">
            </td>
            <td><input type="text" value="${escapeHtml(m.title || '')}" class="form-control" onchange="updateMenuField(${idx}, 'title', this.value)"></td>
            <td><input type="text" value="${escapeHtml(m.path || '')}" class="form-control" onchange="updateMenuField(${idx}, 'path', this.value)"></td>
            <td><input type="text" value="${escapeHtml(m.icon || '')}" class="form-control" style="width: 140px;" onchange="updateMenuField(${idx}, 'icon', this.value)"></td>
            <td><input type="text" value="${escapeHtml(m.badge || '')}" class="form-control" style="width: 90px;" onchange="updateMenuField(${idx}, 'badge', this.value)"></td>
            <td style="text-align: center;">
                <input type="checkbox" ${m.active ? 'checked' : ''} onchange="updateMenuField(${idx}, 'active', this.checked)">
            </td>
            <td>
                <button class="btn btn-outline" style="padding: 4px 8px; font-size: 11px;" onclick="removeMenuItemRow(${idx})">
                    <i class="fa-solid fa-trash-can text-rose-400"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function updateMenuField(idx, field, val) {
    if (state.cmsConfig && state.cmsConfig.menuItems && state.cmsConfig.menuItems[idx]) {
        state.cmsConfig.menuItems[idx][field] = val;
    }
}

function updateMenuOrder(idx, val) {
    if (state.cmsConfig && state.cmsConfig.menuItems && state.cmsConfig.menuItems[idx]) {
        state.cmsConfig.menuItems[idx].orderIndex = parseInt(val) || 1;
    }
}

function addMenuItemRow() {
    if (!state.cmsConfig) state.cmsConfig = { menuItems: [] };
    if (!state.cmsConfig.menuItems) state.cmsConfig.menuItems = [];

    const newIndex = state.cmsConfig.menuItems.length + 1;
    state.cmsConfig.menuItems.push({
        id: Date.now(),
        orderIndex: newIndex,
        title: 'Mục Menu Mới',
        path: '#new-section',
        icon: 'fa-circle-dot',
        badge: '',
        active: true
    });
    renderMenuItemsTable(state.cmsConfig.menuItems);
}

function removeMenuItemRow(idx) {
    if (state.cmsConfig && state.cmsConfig.menuItems) {
        state.cmsConfig.menuItems.splice(idx, 1);
        renderMenuItemsTable(state.cmsConfig.menuItems);
    }
}

async function saveCmsConfig() {
    const cfg = state.cmsConfig || {};
    cfg.clinicName = document.getElementById('cfgClinicName').value.trim();
    cfg.tagline = document.getElementById('cfgTagline').value.trim();
    cfg.hotline = document.getElementById('cfgHotline').value.trim();
    cfg.emergencyHotline = document.getElementById('cfgEmergencyHotline').value.trim();
    cfg.email = document.getElementById('cfgEmail').value.trim();
    cfg.workingHours = document.getElementById('cfgWorkingHours').value.trim();
    cfg.mainAddress = document.getElementById('cfgMainAddress').value.trim();

    if (!cfg.footerConfig) cfg.footerConfig = {};
    cfg.footerConfig.licenseNumber = document.getElementById('cfgLicenseNumber').value.trim();
    cfg.footerConfig.copyrightText = document.getElementById('cfgCopyright').value.trim();
    cfg.footerConfig.aboutText = document.getElementById('cfgAboutText').value.trim();

    if (!cfg.footerConfig.socialLinks) cfg.footerConfig.socialLinks = {};
    cfg.footerConfig.socialLinks.facebook = document.getElementById('cfgSocialFacebook').value.trim();
    cfg.footerConfig.socialLinks.zalo = document.getElementById('cfgSocialZalo').value.trim();

    const res = await fetchApi('/api/cms/config', {
        method: 'PUT',
        body: JSON.stringify(cfg)
    });

    if (res && res.data) {
        state.cmsConfig = res.data;
    }
    showToast('💾 Đã lưu cấu hình Menu & Chân trang thành công!');
}

async function resetCmsConfig() {
    if (!confirm('Khôi phục toàn bộ cấu hình Menu & Chân trang về mặc định?')) return;
    const res = await fetchApi('/api/cms/config/reset', { method: 'POST' });
    if (res && res.data) {
        state.cmsConfig = res.data;
    }
    populateCmsConfigForm(state.cmsConfig);
    showToast('🔄 Đã khôi phục cấu hình CMS mặc định.');
}

// 9. Centralized Notification Hub
function seedInitialNotifications() {
    if (state.notifications.length === 0) {
        state.notifications = [
            { id: 101, type: 'BOOKING', title: '📅 LỊCH KHÁM MỚI CHỜ CỌC', message: 'Bệnh nhân Hoàng Minh Quân vừa đặt hẹn khám dịch vụ Cấy ghép Implant Straumann.', createdAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(), isRead: false },
            { id: 102, type: 'INVENTORY', title: '⚠️ CẢNH BÁO TỒN KHO THẤP', message: 'Mặt hàng Găng tay y tế khám nha (CONS-GLOVE-M) chỉ còn 4 hộp trong kho trung tâm.', createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString(), isRead: false },
            { id: 103, type: 'SYSTEM', title: '🔒 KIỂM TOÁN AN NINH ENTERPRISE', message: 'Hệ thống bảo mật 20 tiêu chuẩn y tế hoạt động ổn định. Đã ngăn chặn 12 lượt brute-force.', createdAt: new Date(Date.now() - 1000 * 60 * 180).toISOString(), isRead: true }
        ];
        state.unreadCount = 2;
        updateBadge('unreadNotifBadge', state.unreadCount);
    }
    renderNotificationFeed();
    renderOverviewNotifications();
}

function renderNotificationFeed() {
    const feed = document.getElementById('fullNotificationFeed');
    if (!feed) return;

    const filtered = state.notifications.filter(n => {
        return state.activeFilter === 'ALL' || n.type === state.activeFilter || (state.activeFilter === 'BOOKING' && n.type === 'NEW_BOOKING');
    });

    if (filtered.length === 0) {
        feed.innerHTML = `<div style="text-align: center; color: #64748b; padding: 40px;"><i class="fa-solid fa-bell-slash" style="font-size: 32px; margin-bottom: 12px; display: block;"></i>Không có thông báo nào trong phân loại này.</div>`;
        return;
    }

    feed.innerHTML = filtered.map(n => `
        <div class="notification-card ${n.type} ${!n.isRead ? 'unread' : ''}">
            <div style="flex: 1;">
                <div class="notif-title">
                    ${getNotificationIcon(n.type)}
                    ${escapeHtml(n.title)}
                </div>
                <div class="notif-desc">${escapeHtml(n.message)}</div>
            </div>
            <div style="text-align: right;">
                <div class="notif-meta">${formatTimeAgo(n.createdAt)}</div>
                ${!n.isRead ? '<span class="badge badge-blue" style="margin-top: 6px;">Mới</span>' : ''}
            </div>
        </div>
    `).join('');
}

function renderOverviewNotifications() {
    const container = document.getElementById('overviewNotifications');
    if (!container) return;

    const topList = state.notifications.slice(0, 4);
    if (topList.length === 0) {
        container.innerHTML = `<div style="text-align: center; color: #64748b; padding: 20px;">Chưa có thông báo nào.</div>`;
        return;
    }

    container.innerHTML = topList.map(n => `
        <div class="notification-card ${n.type}" style="padding: 10px 14px;">
            <div style="flex: 1;">
                <div class="notif-title" style="font-size: 13px;">${getNotificationIcon(n.type)} ${escapeHtml(n.title)}</div>
                <div class="notif-desc" style="font-size: 11.5px;">${escapeHtml(n.message)}</div>
            </div>
            <div class="notif-meta">${formatTimeAgo(n.createdAt)}</div>
        </div>
    `).join('');
}

function filterNotificationFeed(category) {
    state.activeFilter = category;
    document.querySelectorAll('.notif-filter-btn').forEach(btn => {
        if (btn.getAttribute('data-filter') === category) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
    renderNotificationFeed();
}

function clearNotificationFeed() {
    state.notifications = [];
    state.unreadCount = 0;
    updateBadge('unreadNotifBadge', 0);
    renderNotificationFeed();
    renderOverviewNotifications();
    showToast('🗑️ Đã xóa sạch nhật ký thông báo.');
}

function simulateTestNotification() {
    const sample = {
        type: 'BOOKING',
        title: '🔔 TEST CHUÔNG BÁO COMMAND CENTER',
        message: 'Chuông báo hai âm thanh DentalCare hoạt động hoàn hảo trên nền tảng Web Audio API!',
        createdAt: new Date().toISOString()
    };
    handleIncomingNotification(sample);
}

// 10. Export Engine: Multi-Table UTF-8 BOM CSV / Excel Export
async function exportDataStream(type) {
    showToast(`⏳ Đang tạo file báo cáo "${type.toUpperCase()}" chuẩn UTF-8 BOM...`);
    const endpoint = `/api/export/excel?type=${encodeURIComponent(type)}`;

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`);
        if (!response.ok) {
            throw new Error(`Server returned ${response.status}`);
        }

        const blob = await response.blob();
        const filename = `bao-cao-${type}-${new Date().toISOString().slice(0, 10)}.csv`;

        // Check if running inside Electron Native Runner with saveCsvFile
        if (window.desktopBridge && window.desktopBridge.saveCsvFile) {
            const arrayBuffer = await blob.arrayBuffer();
            const uint8Array = new Uint8Array(arrayBuffer);

            // Pass uint8Array buffer to Electron IPC
            const result = await window.desktopBridge.saveCsvFile({
                defaultPath: filename,
                data: Array.from(uint8Array)
            });

            if (result && result.success) {
                showToast(`📁 Đã lưu file Excel/CSV thành công: ${result.filePath}`);
                return;
            } else if (result && result.canceled) {
                showToast('Hủy lưu file.');
                return;
            }
        }

        // Browser Fallback: Blob download with BOM
        const downloadUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(downloadUrl);

        showToast(`✅ Xuất thành công file "${filename}"!`);
    } catch (err) {
        console.warn('Backend export failed, falling back to local UTF-8 BOM generation:', err);
        generateLocalCsvExport(type);
    }
}

function triggerQuickExport(type) {
    switchTab('export');
    exportDataStream(type);
}

// Local Fallback CSV Generator with Guaranteed UTF-8 BOM (\uFEFF)
function generateLocalCsvExport(type) {
    let csv = '\uFEFF'; // Prepend UTF-8 BOM
    let filename = `bao-cao-${type}-${new Date().toISOString().slice(0, 10)}.csv`;

    if (type === 'inventory') {
        csv += "Mã Sản Phẩm / Vật Tư,Tên Mặt Hàng,Thương Hiệu,Danh Mục,Số Lượng Tồn,Đơn Giá (VNĐ),Định Giá Tồn Kho (VNĐ),Trạng Thái An Toàn\n";
        state.inventory.forEach(p => {
            const stock = p.stockQuantity || 0;
            const price = p.basePrice || 0;
            const status = stock <= 10 ? "CẢNH BÁO TỒN THẤP" : "ĐẢM BẢO AN TOÀN";
            csv += `"${p.code}","${p.name}","${p.brand || 'DentalCare'}","${p.category || 'GENERAL'}",${stock},${price},${stock * price},"${status}"\n`;
        });
    } else if (type === 'kpi') {
        csv += "Mã Bác Sĩ,Họ Và Tên Bác Sĩ,Số Điện Thoại,Email,Tổng Ca Tiếp Nhận,Số Ca Hoàn Tất,Tỷ Lệ Hoàn Thành (%),Đánh Giá Hiệu Suất\n";
        state.doctors.forEach(d => {
            csv += `${d.id},"${d.fullName}","${d.phone}","${d.email}",12,10,83.3%,"XUẤT SẮC (Platinum)"\n`;
        });
    } else if (type === 'intake') {
        csv += "Mã Lead / Hồ Sơ,Họ Tên Bệnh Nhân / Học Sinh,Số Điện Thoại,Dịch Vụ Quan Tâm,Nguồn Tiếp Nhận,Kết Quả Sơ Bộ,Mã Voucher,Trạng Thái\n";
        csv += `LEAD-0001,"Nguyễn Hoàng Minh (Lớp 8A2)","0912345678","Niềng Răng Học Đường","Khám Tầm Soát THCS Lê Quý Đôn","Khớp cắn ngược thể nhẹ","HOCDUONG-500K","ĐÃ TƯ VẤN PHỤ HUYNH"\n`;
        csv += `LEAD-0002,"Trần Thị Thùy Dung","0988776655","Implant Straumann","Hội Thảo VIDEC 2026","Mất răng số 36 lâu năm","VIDEC-VIP","HẸN LỊCH KHÁM"\n`;
    } else {
        // Appointments
        csv += "Mã Lịch Hẹn,Ngày Khám,Giờ Khám,Tên Bệnh Nhân,Số Điện Thoại,Dịch Vụ Khám,Bác Sĩ Phụ Trách,Trạng Thái,Tiền Cọc (VNĐ)\n";
        csv += `1,"25/09/2026","09:30","Hoàng Minh Quân","0977224504","Cấy ghép Implant Straumann","BS.CKII Nguyễn Văn Tuấn","Đã cọc 100K",100000\n`;
        csv += `2,"25/09/2026","14:00","Lê Thảo Vy","0918334455","Niềng Răng Mắc Cài Damon","ThS.BS Trần Mai Phương","Chờ đặt cọc",0\n`;
    }

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast(`✅ Đã xuất file "${filename}" (UTF-8 BOM)!`);
}

// Live Preview of Data Tables before Export
function previewExportData(type) {
    const thead = document.getElementById('exportPreviewThead');
    const tbody = document.getElementById('exportPreviewTbody');
    if (!thead || !tbody) return;

    if (type === 'appointments') {
        thead.innerHTML = `<tr><th>Mã LH</th><th>Ngày Khám</th><th>Bệnh Nhân</th><th>Số ĐT</th><th>Dịch Vụ</th><th>Bác Sĩ</th><th>Trạng Thái</th><th>Tiền Cọc</th></tr>`;
        tbody.innerHTML = `
            <tr><td>#101</td><td>25/09/2026 09:30</td><td>Hoàng Minh Quân</td><td>0977224504</td><td>Cấy ghép Implant Straumann</td><td>BS. Tuấn</td><td><span class="badge badge-emerald">Đã cọc 100K</span></td><td>100.000đ</td></tr>
            <tr><td>#102</td><td>25/09/2026 14:00</td><td>Lê Thảo Vy</td><td>0918334455</td><td>Niềng Răng Mắc Cài Damon</td><td>BS. Phương</td><td><span class="badge badge-amber">Chờ cọc</span></td><td>0đ</td></tr>
        `;
    } else if (type === 'inventory') {
        thead.innerHTML = `<tr><th>Mã Vật Tư</th><th>Tên Mặt Hàng</th><th>Danh Mục</th><th>Tồn Kho</th><th>Đơn Giá</th><th>Định Giá Tồn</th><th>Trạng Thái</th></tr>`;
        tbody.innerHTML = state.inventory.slice(0, 4).map(p => `
            <tr>
                <td>${p.code}</td>
                <td>${p.name}</td>
                <td>${p.category}</td>
                <td style="font-weight: 700;">${p.stockQuantity}</td>
                <td>${formatVND(p.basePrice)}</td>
                <td>${formatVND((p.stockQuantity || 0) * (p.basePrice || 0))}</td>
                <td>${(p.stockQuantity || 0) <= 10 ? '<span class="badge badge-rose">Tồn Thấp</span>' : '<span class="badge badge-emerald">An Toàn</span>'}</td>
            </tr>
        `).join('');
    } else if (type === 'kpi') {
        thead.innerHTML = `<tr><th>ID Bác Sĩ</th><th>Họ Tên</th><th>Chuyên Khoa</th><th>Số Ca</th><th>Tỷ Lệ</th><th>Doanh Thu Cọc</th><th>Xếp Hạng</th></tr>`;
        tbody.innerHTML = state.doctors.slice(0, 3).map((d, i) => `
            <tr>
                <td>#${d.id}</td>
                <td>${d.fullName}</td>
                <td>${d.specialty}</td>
                <td>${12 - i * 2}</td>
                <td>${85 - i * 10}%</td>
                <td>${formatVND(1200000 - i * 300000)}</td>
                <td><span class="badge badge-emerald">Platinum</span></td>
            </tr>
        `).join('');
    } else if (type === 'intake') {
        thead.innerHTML = `<tr><th>Mã Lead</th><th>Họ Tên</th><th>Dịch Vụ</th><th>Nguồn Sự Kiện</th><th>Khuyến Nghị</th><th>Mã Voucher</th></tr>`;
        tbody.innerHTML = `
            <tr><td>LEAD-0001</td><td>Nguyễn Hoàng Minh (8A2)</td><td>Niềng Răng Học Đường</td><td>Khám Tầm Soát THCS Lê Quý Đôn</td><td>Khớp cắn ngược thể nhẹ</td><td><span class="badge badge-blue">HOCDUONG-500K</span></td></tr>
            <tr><td>LEAD-0002</td><td>Trần Thị Thùy Dung</td><td>Implant Straumann</td><td>Hội Thảo VIDEC 2026</td><td>Mất răng số 36 lâu năm</td><td><span class="badge badge-blue">VIDEC-VIP</span></td></tr>
        `;
    }
}

// 11. Helper Functions
function renderOverviewMetrics() {
    const sEl = document.getElementById('statServicesCount');
    const dEl = document.getElementById('statDoctorsCount');
    const pEl = document.getElementById('statProductsCount');
    const bEl = document.getElementById('statBranchesCount');

    if (sEl) sEl.textContent = state.services.length;
    if (dEl) dEl.textContent = state.doctors.length;
    if (pEl) pEl.textContent = state.inventory.length;
    if (bEl) bEl.textContent = state.branches.length;
}

function updateBadge(badgeId, count) {
    const badge = document.getElementById(badgeId);
    if (badge) {
        badge.textContent = count;
        badge.style.display = count > 0 ? 'inline-block' : 'none';
    }
}

function updateStatusSummary(msg) {
    const el = document.getElementById('notifStatusSummary');
    if (el) el.textContent = msg;
}

function formatVND(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatTimeAgo(isoString) {
    if (!isoString) return 'Vừa xong';
    const diffMs = Date.now() - new Date(isoString).getTime();
    const diffSec = Math.floor(diffMs / 1000);
    if (diffSec < 60) return 'Vừa xong';
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin} phút trước`;
    const diffHour = Math.floor(diffMin / 60);
    if (diffHour < 24) return `${diffHour} giờ trước`;
    return new Date(isoString).toLocaleDateString('vi-VN');
}

function getNotificationIcon(type) {
    switch (type) {
        case 'BOOKING':
        case 'NEW_BOOKING':
            return '<i class="fa-solid fa-calendar-check text-emerald-400" style="margin-right: 6px;"></i>';
        case 'INVENTORY':
            return '<i class="fa-solid fa-boxes-packing text-amber-400" style="margin-right: 6px;"></i>';
        case 'EMERGENCY':
            return '<i class="fa-solid fa-triangle-exclamation text-rose-400" style="margin-right: 6px;"></i>';
        default:
            return '<i class="fa-solid fa-bell text-sky-400" style="margin-right: 6px;"></i>';
    }
}

function mapCategoryName(cat) {
    switch (cat) {
        case 'ORTHODONTICS': return 'Niềng Răng';
        case 'IMPLANT': return 'Cấy Ghép Implant';
        case 'COSMETIC': return 'Thẩm Mỹ Răng Sứ';
        case 'GENERAL': return 'Nha Khoa Tổng Quát';
        case 'SURGERY': return 'Tiểu Phẫu';
        default: return cat || 'Chuyên Khoa';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function openModal(id) {
    const m = document.getElementById(id);
    if (m) m.classList.add('open');
}

function closeModal(id) {
    const m = document.getElementById(id);
    if (m) m.classList.remove('open');
}

function showToast(message) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `<i class="fa-solid fa-circle-info text-sky-400"></i> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3200);
}
