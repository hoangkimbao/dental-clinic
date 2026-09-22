
// ================= DYNAMIC ON-DEMAND PORTAL DEPENDENCY LOADER =================
function loadScriptAsync(src) {
    return new Promise((resolve) => {
        if (document.querySelector(`script[src="${src}"]`)) return resolve();
        const script = document.createElement('script');
        script.src = src;
        script.onload = resolve;
        script.onerror = () => { console.warn('Could not load script:', src); resolve(); };
        document.head.appendChild(script);
    });
}

let portalDependenciesLoaded = false;
async function ensurePortalLibraries() {
    if (portalDependenciesLoaded) return;
    try {
        await Promise.all([
            loadScriptAsync('https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.6.1/sockjs.min.js'),
            loadScriptAsync('https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js'),
            loadScriptAsync('https://cdn.jsdelivr.net/npm/chart.js')
        ]);
        portalDependenciesLoaded = true;
    } catch (e) {
        console.warn('Dynamic load error:', e);
    }
}

// DentalCare Application JavaScript (Enterprise JWT Interceptor & Standard ApiResponse)

let currentUser = null;
let lastCreatedAppointmentId = null;
let stompClient = null;
let isPortalOpen = false;
let revenueChart = null;
let servicePieChart = null;

// ================= GOOGLE ANALYTICS 4 EVENT TRACKER =================
function trackGaEvent(eventName, params = {}) {
    if (typeof gtag === 'function') {
        gtag('event', eventName, params);
        console.log(`📊 [Google Analytics] Event tracked: ${eventName}`, params);
    }
}

// ================= JWT HTTP INTERCEPTOR =================
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

document.addEventListener('DOMContentLoaded', () => {
    // Check session: Ưu tiên sessionStorage (phiên làm việc), fallback sang localStorage (nếu có Ghi nhớ)
    let saved = sessionStorage.getItem('DENTAL_USER');
    let sessionType = 'Session (Tự hủy khi đóng tab)';
    if (!saved) {
        saved = localStorage.getItem('DENTAL_USER');
        sessionType = 'Ghi nhớ dài hạn (24h)';
    }

    if (saved) {
        try {
            currentUser = JSON.parse(saved);
            updateAuthUI();
            console.log(`%c[DentalCare Security] Phiên đăng nhập hoạt động: ${currentUser.username} (${sessionType})`, 'color: #0ea5e9; font-weight: bold;');
        } catch (e) {
            localStorage.removeItem('DENTAL_USER');
            sessionStorage.removeItem('DENTAL_USER');
        }
    }

    // Set default datetime to tomorrow 9:00 AM
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(9, 0, 0, 0);
    const timeInput = document.getElementById('appointmentTime');
    if (timeInput) {
        timeInput.value = tomorrow.toISOString().slice(0, 16);
    }

    // Connect WebSocket
    // WebSocket is now loaded on-demand for staff only
    startFlashSaleCountdown();
    loadDentistsDropdown();
});

// ================= UNIVERSAL AUTH MODAL (LOGIN / REGISTER) =================
function openAuthModal(mode = 'login') {
    document.getElementById('auth-modal').classList.remove('hidden');
    toggleAuthMode(mode);
}

function closeAuthModal() {
    document.getElementById('auth-modal').classList.add('hidden');
}

function toggleAuthMode(mode) {
    const isLogin = mode === 'login';
    document.getElementById('universal-login-form').classList.toggle('hidden', !isLogin);
    document.getElementById('customer-register-form').classList.toggle('hidden', isLogin);

    document.getElementById('auth-modal-title').innerText = isLogin ? 'Đăng Nhập DentalCare' : 'Đăng Ký Tài Khoản Khách Hàng';
    document.getElementById('auth-modal-sub').innerText = isLogin 
        ? 'Dành cho Khách Hàng & Nhân Viên Phòng Khám' 
        : 'Theo dõi tiến trình khám & lộ trình niềng răng';
}

// 1 Ô ĐĂNG NHẬP DUY NHẤT -> TỰ ĐỘNG NHẬN DIỆN ROLE & NHẬN CHUẨN JWT TOKEN
async function handleUniversalLogin(e) {
    e.preventDefault();
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const res = await apiFetch('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });

        if (res.ok && res.data.success) {
            currentUser = res.data.data;
            const rememberMe = document.getElementById('rememberMeCheckbox') ? document.getElementById('rememberMeCheckbox').checked : false;
            
            if (rememberMe) {
                // Persistent: Lưu localStorage (giữ đăng nhập 24h khi đóng mở lại browser)
                localStorage.setItem('DENTAL_USER', JSON.stringify(currentUser));
                sessionStorage.removeItem('DENTAL_USER');
                showToast(`Đăng nhập thành công (Đã bật Ghi nhớ 24h)! Xin chào, ${currentUser.fullName}`);
            } else {
                // Session: Lưu sessionStorage (Tự động xóa phiên ngay khi người dùng đóng tab/trình duyệt để bảo mật)
                sessionStorage.setItem('DENTAL_USER', JSON.stringify(currentUser));
                localStorage.removeItem('DENTAL_USER');
                showToast(`Đăng nhập phiên làm việc an toàn! Xin chào, ${currentUser.fullName}`);
            }

            closeAuthModal();
            updateAuthUI();
            togglePortal(true);
        } else {
            alert(res.data.message || 'Tên đăng nhập hoặc mật khẩu không chính xác!');
        }
    } catch (err) {
        // Caught by interceptor
    }
}

// ĐĂNG KÝ KHÁCH HÀNG MỚI (100% ROLE_PATIENT)
async function handleCustomerRegister(e) {
    e.preventDefault();
    const payload = {
        fullName: document.getElementById('regFullName').value,
        phone: document.getElementById('regPhone').value,
        email: document.getElementById('regEmail').value,
        username: document.getElementById('regUsername').value,
        password: document.getElementById('regPassword').value
    };

    try {
        const res = await apiFetch('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.ok && res.data.success) {
            currentUser = res.data.data;
            localStorage.setItem('DENTAL_USER', JSON.stringify(currentUser));
            closeAuthModal();
            updateAuthUI();
            showToast(`Tạo tài khoản thành công! Xin chào, ${currentUser.fullName}`);
            togglePortal(true);
        } else {
            alert(res.data.message || 'Đăng ký thất bại!');
        }
    } catch (err) {
        // Caught
    }
}

// CẤP TÀI KHOẢN NHÂN VIÊN (CHỈ CHỦ & LỄ TÂN)
function openStaffCreateModal() {
    document.getElementById('staff-create-modal').classList.remove('hidden');
}

function closeStaffCreateModal() {
    document.getElementById('staff-create-modal').classList.add('hidden');
}

async function handleCreateStaffSubmit(e) {
    e.preventDefault();
    const payload = {
        fullName: document.getElementById('staffFullName').value,
        phone: document.getElementById('staffPhone').value,
        role: document.getElementById('staffRole').value,
        username: document.getElementById('staffUsername').value,
        password: document.getElementById('staffPassword').value
    };

    try {
        const res = await apiFetch('/api/staff/create', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.ok && res.data.success) {
            showToast(`Đã cấp tài khoản thành công cho: ${payload.fullName}!`);
            closeStaffCreateModal();
            document.getElementById('create-staff-form').reset();
            loadShifts();
        } else {
            alert(res.data.message || 'Lỗi cấp tài khoản!');
        }
    } catch (err) {
        // Caught
    }
}

function handleLogout() {
    const floatContact = document.getElementById('floating-contact-widget');
    if (floatContact) floatContact.classList.remove('hidden');
    currentUser = null;
    localStorage.removeItem('DENTAL_USER');
    sessionStorage.removeItem('DENTAL_USER');

    // Xóa sạch thông tin trên form đặt lịch khi đăng xuất
    const nameInput = document.getElementById('patientName');
    const phoneInput = document.getElementById('patientPhone');
    const emailInput = document.getElementById('patientEmail');
    if (nameInput) nameInput.value = '';
    if (phoneInput) phoneInput.value = '';
    if (emailInput) emailInput.value = '';

    updateAuthUI();
    showLandingPage();
    showToast('Đã đăng xuất an toàn!');
}

function updateAuthUI() {
    const unauth = document.getElementById('unauth-actions');
    const auth = document.getElementById('auth-actions');
    const autofillBanner = document.getElementById('booking-autofill-banner');
    const autofillName = document.getElementById('autofill-user-name');
    const autofillPhone = document.getElementById('autofill-user-phone');

    const nameInput = document.getElementById('patientName');
    const phoneInput = document.getElementById('patientPhone');
    const emailInput = document.getElementById('patientEmail');

    if (currentUser) {
        unauth.classList.add('hidden');
        auth.classList.remove('hidden');
        document.getElementById('nav-user-name').innerText = currentUser.fullName;
        document.getElementById('nav-user-role').innerText = getRoleShortBadge(currentUser.role);
        
        const badgeEl = document.getElementById('portal-user-badge');
        if (badgeEl) badgeEl.innerText = `${currentUser.fullName} (${getRoleBadge(currentUser.role)})`;

        // TỰ ĐỘNG ĐIỀN THÔNG TIN TÀI KHOẢN VÀO FORM ĐẶT LỊCH KHÁM
        if (nameInput) nameInput.value = currentUser.fullName || '';
        if (phoneInput) phoneInput.value = currentUser.phone || '';
        if (emailInput && currentUser.email) emailInput.value = currentUser.email || '';

        // Hiển thị banner thông báo Autofill thân thiện
        if (autofillBanner && autofillName && autofillPhone) {
            autofillBanner.classList.remove('hidden');
            autofillName.innerText = currentUser.fullName;
            autofillPhone.innerText = currentUser.phone || 'Chưa cập nhật';
        }
    } else {
        unauth.classList.remove('hidden');
        auth.classList.add('hidden');
        if (autofillBanner) autofillBanner.classList.add('hidden');
    }
}

function getRoleShortBadge(role) {
    switch (role) {
        case 'ROLE_ADMIN': return 'IT Admin';
        case 'ROLE_OWNER': return 'Chủ Phòng';
        case 'ROLE_RECEPTIONIST': return 'Lễ Tân';
        case 'ROLE_DENTIST': return 'Nha Sĩ';
        case 'ROLE_ASSISTANT': return 'Phụ Tá';
        case 'ROLE_CLEANER': return 'Tạp Vụ';
        case 'ROLE_PATIENT': return 'Bệnh Nhân';
        default: return role;
    }
}

function getRoleBadge(role) {
    switch (role) {
        case 'ROLE_ADMIN': return '🛠️ IT Admin';
        case 'ROLE_OWNER': return '👑 Chủ Phòng';
        case 'ROLE_RECEPTIONIST': return '🛎️ Lễ Tân';
        case 'ROLE_DENTIST': return '🩺 Nha Sĩ';
        case 'ROLE_ASSISTANT': return '🧤 Phụ Tá';
        case 'ROLE_CLEANER': return '🧹 Tạp Vụ';
        case 'ROLE_PATIENT': return '👤 Khách Hàng';
        default: return role;
    }
}

// ================= RBAC DYNAMIC WORKSPACE =================
function togglePortal(forceOpen = null) {
    if (!currentUser) {
        openAuthModal('login');
        return;
    }

    isPortalOpen = forceOpen !== null ? forceOpen : !isPortalOpen;
    const landing = document.getElementById('public-landing-page');
    const portal = document.getElementById('management-portal');
    const btnText = document.getElementById('portal-btn-text');

    const floatContact = document.getElementById('floating-contact-widget');
    if (isPortalOpen) {
        if (!stompClient && currentUser) connectWebSocket();
        landing.classList.add('hidden');
        portal.classList.remove('hidden');
        if (floatContact) floatContact.classList.add('hidden');
        btnText.innerText = 'Trang Chủ';
        renderDynamicRoleView();
    } else {
        landing.classList.remove('hidden');
        portal.classList.add('hidden');
        if (floatContact) floatContact.classList.remove('hidden');
        btnText.innerText = (currentUser.role === 'ROLE_PATIENT') ? 'Hồ Sơ Của Tôi' : 'Khu Vực Làm Việc';
    }
}

function showLandingPage() {
    if (isPortalOpen) {
        togglePortal(false);
    }
}

function renderDynamicRoleView() {
    /* hide-float-in-role-view */
    const floatContact = document.getElementById('floating-contact-widget');
    if (floatContact) floatContact.classList.add('hidden');
    if (!currentUser) return;
    const role = currentUser.role;

    const tabDashboard = document.getElementById('tab-dashboard');
    const tabAppointments = document.getElementById('tab-appointments');
    const labelAppointments = document.getElementById('label-tab-appointments');
    const tabEMR = document.getElementById('tab-emr');
    const labelEMR = document.getElementById('label-tab-emr');
    const tabShifts = document.getElementById('tab-shifts');
    const labelShifts = document.getElementById('label-tab-shifts');
    const staffAdminBar = document.getElementById('staff-admin-bar');
    const portalTitle = document.getElementById('portal-title-text');

    const tabItTeam = document.getElementById('tab-itteam');
    if (tabItTeam) {
        tabItTeam.style.display = ['ROLE_OWNER', 'ROLE_ADMIN'].includes(role) ? 'flex' : 'none';
    }

    // 0. IT ADMIN / QUẢN TRỊ VIÊN (ROLE_ADMIN) -> TRUY CẬP ĐẶC VỤ IT VÀ QUẢN TRỊ TOÀN DIỆN
    if (role === 'ROLE_ADMIN') {
        portalTitle.innerHTML = `<i class="fa-solid fa-terminal text-teal-400"></i> IT Command Center &amp; Quản Trị Hệ Thống`;
        if (tabDashboard) tabDashboard.style.display = 'flex';
        if (tabAppointments) {
            tabAppointments.style.display = 'flex';
            labelAppointments.innerText = 'Quản Lý Lịch Hẹn Toàn Phòng';
        }
        const tabCoupons = document.getElementById('tab-coupons');
        if (tabCoupons) tabCoupons.style.display = 'flex';
        if (tabEMR) {
            tabEMR.style.display = 'flex';
            labelEMR.innerText = 'Bệnh Án EMR &amp; Chỉnh Nha';
        }
        if (tabShifts) {
            tabShifts.style.display = 'flex';
            labelShifts.innerText = 'Phân Ca &amp; Cấp Tài Khoản Nhân Viên';
        }
        if (staffAdminBar) staffAdminBar.classList.remove('hidden');

        const revCard = document.getElementById('card-revenue');
        if (revCard) revCard.style.display = 'flex';

        switchTab('itteam');
        return;
    }

    // 1. KHÁCH HÀNG / BỆNH NHÂN (ROLE_PATIENT) -> 100% KHÔNG THẤY DASHBOARD
    if (role === 'ROLE_PATIENT') {
        portalTitle.innerHTML = `<i class="fa-solid fa-user text-brand-400"></i> Cổng Chăm Sóc Khách Hàng`;
        if (tabDashboard) tabDashboard.style.display = 'none';
        if (tabShifts) tabShifts.style.display = 'none';
        const tabCoupons = document.getElementById('tab-coupons'); if (tabCoupons) tabCoupons.style.display = 'none';
        if (staffAdminBar) staffAdminBar.classList.add('hidden');

        if (tabAppointments) {
            tabAppointments.style.display = 'flex';
            labelAppointments.innerText = 'Lịch Hẹn Của Tôi';
        }
        if (tabEMR) {
            tabEMR.style.display = 'flex';
            labelEMR.innerText = 'Bệnh Án & Tiến Trình Niềng Răng';
        }
        switchTab('appointments');
    } 
    // 2. CHỦ PHÒNG & LỄ TÂN (ROLE_OWNER, ROLE_RECEPTIONIST) -> MỞ DASHBOARD & QUẢN LÝ ƯU ĐÃI
    else if (['ROLE_OWNER', 'ROLE_RECEPTIONIST'].includes(role)) {
        portalTitle.innerHTML = `<i class="fa-solid fa-gauge-high text-brand-400"></i> Khu Quản Trị &amp; Điều Phối`;
        if (tabDashboard) tabDashboard.style.display = 'flex';
        if (tabAppointments) {
            tabAppointments.style.display = 'flex';
            labelAppointments.innerText = 'Quản Lý Lịch Hẹn Toàn Phòng';
        }
        const tabCoupons = document.getElementById('tab-coupons');
        if (tabCoupons) tabCoupons.style.display = 'flex';
        if (tabEMR) {
            tabEMR.style.display = 'flex';
            labelEMR.innerText = 'Bệnh Án EMR &amp; Chỉnh Nha';
        }
        if (tabShifts) {
            tabShifts.style.display = 'flex';
            labelShifts.innerText = 'Phân Ca &amp; Cấp Tài Khoản Nhân Viên';
        }
        if (staffAdminBar) staffAdminBar.classList.remove('hidden');

        // Doanh thu chỉ riêng Chủ phòng xem
        const revCard = document.getElementById('card-revenue');
        if (revCard) revCard.style.display = (role === 'ROLE_OWNER') ? 'flex' : 'none';

        switchTab('dashboard');
    }
    // 3. NHA SĨ (ROLE_DENTIST) -> KHÔNG VÀO DASHBOARD
    else if (role === 'ROLE_DENTIST') {
        portalTitle.innerHTML = `<i class="fa-solid fa-stethoscope text-brand-400"></i> Không Gian Làm Việc Bác Sĩ`;
        if (tabDashboard) tabDashboard.style.display = 'none';
        if (tabShifts) tabShifts.style.display = 'none';
        const tabCoupons = document.getElementById('tab-coupons'); if (tabCoupons) tabCoupons.style.display = 'none';
        if (staffAdminBar) staffAdminBar.classList.add('hidden');

        if (tabAppointments) {
            tabAppointments.style.display = 'flex';
            labelAppointments.innerText = 'Lịch Khám Được Phân Công';
        }
        if (tabEMR) {
            tabEMR.style.display = 'flex';
            labelEMR.innerText = 'Hồ Sơ Bệnh Án EMR Điều Trị';
        }
        switchTab('appointments');
    }
    // 4. PHỤ TÁ & TẠP VỤ -> KHÔNG VÀO DASHBOARD
    else {
        portalTitle.innerHTML = `<i class="fa-solid fa-clipboard-check text-brand-400"></i> Lịch Trực &amp; Nhiệm Vụ Ca`;
        if (tabDashboard) tabDashboard.style.display = 'none';
        if (tabAppointments) tabAppointments.style.display = 'none';
        if (tabEMR) tabEMR.style.display = 'none';
        if (staffAdminBar) staffAdminBar.classList.add('hidden');

        if (tabShifts) {
            tabShifts.style.display = 'flex';
            labelShifts.innerText = 'Ca Trực Phân Công';
        }
        switchTab('shifts');
    }
}

// Switch Tabs
function switchTab(tabId) {
    const tabs = ['dashboard', 'appointments', 'coupons', 'emr', 'shifts', 'notifications', 'itteam'];
    tabs.forEach(t => {
        const sec = document.getElementById(`section-${t}`);
        const btn = document.getElementById(`tab-${t}`);
        if (sec) sec.classList.toggle('hidden', t !== tabId);
        if (btn) {
            if (t === tabId) {
                btn.className = (t === 'itteam') 
                    ? "tab-btn py-3.5 px-4 border-b-2 border-teal-400 text-white flex items-center gap-2" 
                    : "tab-btn py-3.5 px-4 border-b-2 border-brand-400 text-white flex items-center gap-2";
            } else {
                btn.className = "tab-btn py-3.5 px-4 border-b-2 border-transparent text-slate-400 hover:text-white flex items-center gap-2";
            }
        }
    });

    if (tabId === 'dashboard') { loadDashboardStats(); initCharts(); }
    if (tabId === 'appointments') loadAppointments();
    if (tabId === 'emr') { loadEMR(); loadOrthoPlans(); }
    if (tabId === 'shifts') loadShifts();
    if (tabId === 'coupons') loadDashboardCoupons();
    if (tabId === 'notifications') loadNotifications();
    if (tabId === 'itteam' && typeof initItTeamCommandCenter === 'function') initItTeamCommandCenter();
}

// ================= CHART.JS ANALYTICS INITIALIZER =================
async function initCharts() {
    await ensurePortalLibraries();
    if (typeof Chart === 'undefined') {
        console.warn('Chart.js not available yet.');
        return;
    }
    setTimeout(() => {
        const revCtx = document.getElementById('revenueChart');
        if (revCtx) {
            if (revenueChart) revenueChart.destroy();
            revenueChart = new Chart(revCtx, {
                type: 'line',
                data: {
                    labels: ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'],
                    datasets: [
                        {
                            label: 'Doanh Thu Thực Tế (Triệu VNĐ)',
                            data: [12.5, 18.0, 15.2, 22.0, 28.5, 35.0, 31.2],
                            borderColor: '#0ea5e9',
                            backgroundColor: 'rgba(14, 165, 233, 0.1)',
                            fill: true,
                            tension: 0.4,
                            borderWidth: 3
                        },
                        {
                            label: 'Lịch Khám Mới',
                            data: [5, 8, 6, 9, 12, 16, 14],
                            borderColor: '#10b981',
                            backgroundColor: 'transparent',
                            borderDash: [5, 5],
                            tension: 0.4,
                            borderWidth: 2
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { labels: { color: '#94a3b8', font: { size: 11 } } } },
                    scales: {
                        x: { grid: { color: 'rgba(51, 65, 85, 0.5)' }, ticks: { color: '#94a3b8' } },
                        y: { grid: { color: 'rgba(51, 65, 85, 0.5)' }, ticks: { color: '#94a3b8' } }
                    }
                }
            });
        }

        const pieCtx = document.getElementById('servicePieChart');
        if (pieCtx) {
            if (servicePieChart) servicePieChart.destroy();
            servicePieChart = new Chart(pieCtx, {
                type: 'doughnut',
                data: {
                    labels: ['Niềng Răng 3D', 'Implant Straumann', 'Răng Sứ Emax', 'Nhổ Răng Khôn', 'Tẩy Trắng & Khám'],
                    datasets: [{
                        data: [40, 25, 18, 12, 5],
                        backgroundColor: ['#0ea5e9', '#6366f1', '#f59e0b', '#10b981', '#f43f5e'],
                        borderWidth: 0
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { position: 'bottom', labels: { color: '#94a3b8', font: { size: 10 } } } }
                }
            });
        }
    }, 100);
}

// ================= WEBSOCKET REALTIME (STAFF ONLY) =================
async function connectWebSocket() {
    if (!currentUser || currentUser.role === 'ROLE_PATIENT') return;
    await ensurePortalLibraries();
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
    try {
        const socket = new SockJS('/ws-dental');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;
        stompClient.connect({}, () => {
            stompClient.subscribe('/topic/notifications', (message) => {
                const notif = JSON.parse(message.body);
                handleIncomingNotification(notif);
            });
        }, () => {
            if (currentUser && isPortalOpen) {
                setTimeout(connectWebSocket, 15000);
            }
        });
    } catch (e) {
        // Silent fallback
    }
}

function handleIncomingNotification(notif) {
    showToast(`🔔 ${notif.title}: ${notif.message}`);
    if (isPortalOpen) {
        loadNotifications();
        loadAppointments();
        loadDashboardStats();
    }
}

// ================= BOOKING & DEPOSIT =================
async function handleBookingSubmit(e) {
    e.preventDefault();
    const payload = {
        patientName: document.getElementById('patientName').value,
        patientPhone: document.getElementById('patientPhone').value,
        patientEmail: document.getElementById('patientEmail').value,
        serviceName: document.getElementById('serviceName').value,
        dentistId: document.getElementById('dentistId').value,
        appointmentTime: document.getElementById('appointmentTime').value,
        notes: document.getElementById('notes').value,
        couponCode: document.getElementById('bookingCouponCode') ? document.getElementById('bookingCouponCode').value.trim() : ''
    };

    try {
        const res = await apiFetch('/api/appointments/book', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.ok && res.data.success) {
            const bookingResult = res.data.data;
            const appt = bookingResult.appointment || bookingResult;
            lastCreatedAppointmentId = appt.id;

            document.getElementById('qr-placeholder').classList.add('hidden');
            document.getElementById('qr-active').classList.remove('hidden');

            // Ghi nhận sự kiện đặt lịch vào Google Analytics
            trackGaEvent('book_appointment', {
                service_name: payload.serviceName,
                dentist_id: payload.dentistId,
                coupon_code: payload.couponCode || 'NONE',
                appointment_id: appt.id
            });

            // NẾU TỰ ĐỘNG TẠO TÀI KHOẢN MỚI -> TỰ ĐỘNG ĐĂNG NHẬP CHO KHÁCH HÀNG!
            if (bookingResult.newAccountCreated && bookingResult.authInfo) {
                currentUser = bookingResult.authInfo;
                localStorage.setItem('DENTAL_USER', JSON.stringify(currentUser));
                updateAuthUI();
                
                alert(`🎉 ĐẶT LỊCH KHÁM THÀNH CÔNG!\\n\\n✨ DentalCare đã TỰ ĐỘNG KÍCH HOẠT TÀI KHOẢN cho bạn:\\n👤 Tên đăng nhập: ${bookingResult.generatedUsername} (Số điện thoại của bạn)\\n🔑 Mật khẩu ban đầu: ${bookingResult.generatedPassword}\\n\\nBạn đã được tự động đăng nhập và có thể quét mã QR bên cạnh để cọc 100K giữ chỗ!`);
            } else {
                showToast(`✅ Đã đặt lịch #${appt.id}! Quét mã cọc 100k giữ slot ngay.`);
            }
        } else {
            alert(res.data.message || 'Lỗi đặt lịch!');
        }
    } catch (err) {
        // Caught
    }
}

async function simulateDepositPayment() {
    if (!lastCreatedAppointmentId) {
        alert('Vui lòng tạo lịch hẹn trước!');
        return;
    }

    try {
        const res = await apiFetch(`/api/appointments/${lastCreatedAppointmentId}/pay-deposit?method=QR_VNPAY`, {
            method: 'POST'
        });

        if (res.ok && res.data.success) {
            const data = res.data.data;
            showToast(`🎉 Cọc giữ chỗ thành công! Mã GD: ${data.transactionCode}`);
            
            // Ghi nhận sự kiện thanh toán cọc thành công vào Google Analytics
            trackGaEvent('purchase', {
                transaction_id: data.transactionCode,
                value: 100000,
                currency: 'VND',
                items: [{ item_name: 'Cọc Giữ Chỗ Nha Khoa', price: 100000, quantity: 1 }]
            });

            document.getElementById('qr-active').innerHTML = `
                <div class="p-4 bg-emerald-50 text-emerald-800 rounded-xl font-bold text-xs space-y-1">
                    <i class="fa-solid fa-circle-check text-emerald-600 text-2xl mb-1"></i>
                    <div>ĐÃ CỌC THÀNH CÔNG 100.000đ</div>
                    <div class="text-[10px] text-emerald-600 font-mono">Mã GD: ${data.transactionCode}</div>
                    <div class="text-[10px] text-slate-500">Đã gửi thông báo tức thì tới Bác Sĩ & Lễ Tân!</div>
                </div>
            `;
        }
    } catch (err) {
        // Caught
    }
}

// ================= APPOINTMENT FILTERS & EXPORT =================
let currentAppointmentPreset = 'all';
let filterStartDate = null;
let filterEndDate = null;

function setAppointmentFilter(preset) {
    currentAppointmentPreset = preset;
    filterStartDate = null;
    filterEndDate = null;

    const startInput = document.getElementById('filter-start-date');
    const endInput = document.getElementById('filter-end-date');
    if (startInput) startInput.value = '';
    if (endInput) endInput.value = '';

    // Update button active styles
    const presets = ['all', 'today', 'tomorrow', 'next7days', 'future'];
    presets.forEach(p => {
        const btn = document.getElementById(`btn-filter-${p}`);
        if (btn) {
            if (p === preset) {
                btn.className = "px-3 py-1.5 rounded-lg bg-brand-600 text-white font-bold transition shadow-sm";
            } else {
                btn.className = "px-3 py-1.5 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 transition";
            }
        }
    });

    loadAppointments();
}

function applyCustomDateFilter() {
    const start = document.getElementById('filter-start-date').value;
    const end = document.getElementById('filter-end-date').value;

    if (!start && !end) {
        showToast('⚠️ Vui lòng chọn ít nhất ngày bắt đầu hoặc ngày kết thúc!');
        return;
    }

    currentAppointmentPreset = 'custom';
    filterStartDate = start || null;
    filterEndDate = end || null;

    // Reset preset buttons highlight
    const presets = ['all', 'today', 'tomorrow', 'next7days', 'future'];
    presets.forEach(p => {
        const btn = document.getElementById(`btn-filter-${p}`);
        if (btn) btn.className = "px-3 py-1.5 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 transition";
    });

    loadAppointments();
}

async function exportAppointmentsCsv() {
    if (!currentUser) {
        showToast('⚠️ Vui lòng đăng nhập để xuất dữ liệu!');
        return;
    }

    try {
        let url = `/api/appointments/export?preset=${encodeURIComponent(currentAppointmentPreset)}`;
        if (filterStartDate) url += `&startDate=${filterStartDate}`;
        if (filterEndDate) url += `&endDate=${filterEndDate}`;
        if (currentUser.role === 'ROLE_DENTIST') {
            url += `&dentistId=${currentUser.id}`;
        }

        showToast('⏳ Đang chuẩn bị file Excel/CSV...');

        const headers = {};
        if (currentUser.token) {
            headers['Authorization'] = `Bearer ${currentUser.token}`;
        }

        const res = await fetch(url, { headers });
        if (!res.ok) {
            throw new Error(`Lỗi tải file: ${res.status}`);
        }

        const blob = await res.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = downloadUrl;
        
        const timestamp = new Date().toISOString().slice(0, 10);
        a.download = `lich-kham-dentalcare-${currentAppointmentPreset}-${timestamp}.csv`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        a.remove();

        showToast('✅ Đã xuất file Excel (.CSV) thành công!');
    } catch (err) {
        console.error('Export error:', err);
        showToast('❌ Không thể xuất file, vui lòng thử lại!');
    }
}

// ================= LOAD DATA (AUTOMATIC ROLE DATA ISOLATION) =================
async function loadAppointments() {
    try {
        let url = '/api/appointments';

        if (currentUser && currentUser.role === 'ROLE_PATIENT' && currentUser.phone) {
            url += `?phone=${encodeURIComponent(currentUser.phone)}`;
        } else {
            // Dành cho Staff (Lễ tân, Bác sĩ, Chủ phòng) -> Sử dụng bộ lọc thời gian
            url = `/api/appointments/filter?preset=${encodeURIComponent(currentAppointmentPreset)}`;
            if (filterStartDate) url += `&startDate=${filterStartDate}`;
            if (filterEndDate) url += `&endDate=${filterEndDate}`;
            if (currentUser && currentUser.role === 'ROLE_DENTIST') {
                url += `&dentistId=${currentUser.id}`;
            }
        }

        const res = await apiFetch(url);
        const appointments = (res.ok && res.data.success) ? res.data.data : [];
        const tbody = document.getElementById('appointment-table-body');
        const countBadge = document.getElementById('appointment-count-badge');
        
        if (countBadge) {
            countBadge.innerText = appointments.length;
        }

        if (!tbody) return;

        if (appointments.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-8 text-slate-400 font-medium">Không tìm thấy lịch hẹn nào theo điều kiện lọc đã chọn</td></tr>`;
            return;
        }

        tbody.innerHTML = appointments.map(a => `
            <tr class="hover:bg-slate-800/80 transition font-medium">
                <td class="py-3.5 px-4 font-mono font-bold text-brand-400">#${a.id}</td>
                <td class="py-3.5 px-4">
                    <div class="font-bold text-white">${a.patientName}</div>
                    <div class="text-slate-400 text-[11px] font-mono">${a.patientPhone || '---'}</div>
                </td>
                <td class="py-3.5 px-4 font-semibold text-slate-200">${a.serviceName}</td>
                <td class="py-3.5 px-4 text-slate-300">${a.dentist ? a.dentist.fullName : '<span class="text-slate-500 italic">Chưa chỉ định</span>'}</td>
                <td class="py-3.5 px-4 text-slate-300 font-mono text-[11px]">${formatDate(a.appointmentTime)}</td>
                <td class="py-3.5 px-4">
                    ${getStatusBadge(a.status)}
                </td>
                <td class="py-3.5 px-4 text-right space-x-1">
                    ${(currentUser && ['ROLE_OWNER', 'ROLE_RECEPTIONIST'].includes(currentUser.role) && a.status === 'DEPOSIT_PAID') ? `
                        <button onclick="updateStatus(${a.id}, 'CONFIRMED')" class="bg-brand-600 hover:bg-brand-700 text-white px-3 py-1 rounded text-[11px] font-bold shadow-sm transition hover:scale-105 active:scale-95 cursor-pointer">
                            <i class="fa-solid fa-check mr-1"></i>Xác nhận
                        </button>
                    ` : ''}
                    ${(currentUser && ['ROLE_OWNER', 'ROLE_RECEPTIONIST'].includes(currentUser.role) && a.status === 'PENDING') ? `
                        <button onclick="updateStatus(${a.id}, 'CONFIRMED')" title="Thu cọc / Xác nhận tại quầy" class="bg-amber-600/80 hover:bg-amber-600 text-white px-2.5 py-1 rounded text-[11px] font-bold shadow-sm transition cursor-pointer">
                            <i class="fa-solid fa-money-bill-wave mr-1"></i>Thu cọc & Xác nhận
                        </button>
                    ` : ''}
                    ${(currentUser && ['ROLE_OWNER', 'ROLE_DENTIST'].includes(currentUser.role) && a.status === 'CONFIRMED') ? `
                        <button onclick="updateStatus(${a.id}, 'COMPLETED')" class="bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-1 rounded text-[11px] font-bold shadow-sm transition hover:scale-105 active:scale-95 cursor-pointer">
                            <i class="fa-solid fa-stethoscope mr-1"></i>Khám xong
                        </button>
                    ` : ''}
                    ${(currentUser && ['ROLE_RECEPTIONIST'].includes(currentUser.role) && a.status === 'CONFIRMED') ? `
                        <span class="inline-flex items-center gap-1 text-[11px] font-bold text-sky-400 bg-sky-500/10 px-2 py-0.5 rounded border border-sky-500/20">
                            <i class="fa-solid fa-circle-check"></i> Đã đón tiếp
                        </span>
                    ` : ''}
                    ${(a.status === 'COMPLETED') ? `
                        <span class="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                            <i class="fa-solid fa-circle-check"></i> Hoàn tất
                        </span>
                    ` : ''}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error(e);
    }
}

function getStatusBadge(status) {
    switch (status) {
        case 'DEPOSIT_PAID':
            return `<span class="bg-emerald-500/20 text-emerald-300 font-bold px-2 py-0.5 rounded-full text-[11px] border border-emerald-500/30">Đã Cọc (100k)</span>`;
        case 'PENDING':
            return `<span class="bg-amber-500/20 text-amber-300 font-bold px-2 py-0.5 rounded-full text-[11px] border border-amber-500/30">Chờ Cọc</span>`;
        case 'CONFIRMED':
            return `<span class="bg-sky-500/20 text-sky-300 font-bold px-2 py-0.5 rounded-full text-[11px] border border-sky-500/30">Đã Check-in</span>`;
        case 'COMPLETED':
            return `<span class="bg-slate-700 text-slate-300 font-bold px-2 py-0.5 rounded-full text-[11px]">Hoàn Tất</span>`;
        default:
            return status;
    }
}

async function updateStatus(id, status) {
    try {
        const res = await apiFetch(`/api/appointments/${id}/status?status=${status}`, { method: 'PATCH' });
        if (res && res.ok) {
            const mapNames = {
                'CONFIRMED': 'Đã Xác Nhận / Check-in',
                'COMPLETED': 'Đã Hoàn Tất Khám',
                'DEPOSIT_PAID': 'Đã Thu Cọc (100k)',
                'CANCELLED': 'Đã Hủy Lịch'
            };
            showToast(`✅ Thành công: Lịch #${id} chuyển sang: ${mapNames[status] || status}`);
            await loadAppointments();
            await loadDashboardStats();
        } else {
            const errMsg = res?.data?.message || 'Không thể cập nhật trạng thái lịch hẹn';
            showToast(`❌ Lỗi: ${errMsg}`);
        }
    } catch (err) {
        console.error('Error updating status:', err);
        showToast('❌ Lỗi kết nối khi cập nhật trạng thái!');
    }
}

async function loadDashboardStats() {
    try {
        const res = await apiFetch('/api/dashboard/stats');
        if (res.ok && res.data.success) {
            const data = res.data.data;
            const revEl = document.getElementById('stat-revenue');
            if (revEl) revEl.innerText = Number(data.totalRevenueVND).toLocaleString('vi-VN') + ' đ';
            const appEl = document.getElementById('stat-appointments');
            if (appEl) appEl.innerText = data.totalAppointments;
            const depEl = document.getElementById('stat-deposit-count');
            if (depEl) depEl.innerText = data.depositPaidCount;
            const ortEl = document.getElementById('stat-ortho');
            if (ortEl) ortEl.innerText = data.activeOrthoPlans + ' ca';
            const staEl = document.getElementById('stat-staff');
            if (staEl) staEl.innerText = data.totalStaff + ' nhân sự';
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadEMR() {
    try {
        let url = '/api/medical-records';
        if (currentUser && currentUser.role === 'ROLE_PATIENT' && currentUser.phone) {
            url += `?phone=${encodeURIComponent(currentUser.phone)}`;
        }
        const res = await apiFetch(url);
        const records = (res.ok && res.data.success) ? res.data.data : [];
        const container = document.getElementById('emr-list');
        if (!container) return;

        if (records.length === 0) {
            container.innerHTML = `<div class="text-xs text-slate-400 text-center py-4">Không có hồ sơ bệnh án nào.</div>`;
            return;
        }

        container.innerHTML = records.map(r => `
            <div class="p-4 rounded-2xl bg-slate-900 border border-slate-700/80 space-y-1.5">
                <div class="flex justify-between items-center">
                    <span class="font-extrabold text-white text-xs">${r.patientName} (${r.patientPhone})</span>
                    <span class="text-[10px] text-slate-400 font-mono">${r.recordDate}</span>
                </div>
                <div class="text-[11px] text-slate-300"><b class="text-brand-400">Chẩn đoán:</b> ${r.diagnosis}</div>
                <div class="text-[11px] text-slate-300"><b class="text-brand-400">Điều trị:</b> ${r.treatmentDone}</div>
                <div class="text-[10px] text-slate-400"><b class="text-slate-200">Bác sĩ:</b> ${r.dentist ? r.dentist.fullName : 'Nha sĩ'}</div>
            </div>
        `).join('');
    } catch (e) {
        console.error(e);
    }
}

async function loadOrthoPlans() {
    try {
        let url = '/api/orthodontic-plans';
        if (currentUser && currentUser.role === 'ROLE_PATIENT' && currentUser.phone) {
            url += `?phone=${encodeURIComponent(currentUser.phone)}`;
        }
        const res = await apiFetch(url);
        const plans = (res.ok && res.data.success) ? res.data.data : [];
        const container = document.getElementById('ortho-list');
        if (!container) return;

        if (plans.length === 0) {
            container.innerHTML = `<div class="text-xs text-slate-400 text-center py-4">Chưa có kế hoạch chỉnh nha nào.</div>`;
            return;
        }

        container.innerHTML = plans.map(p => `
            <div class="p-4 rounded-2xl bg-slate-900 border border-indigo-500/30 space-y-1.5">
                <div class="flex justify-between items-center">
                    <span class="font-extrabold text-indigo-300 text-xs">${p.patientName}</span>
                    <span class="bg-indigo-600 text-white text-[9px] font-black px-2 py-0.5 rounded-full">${p.currentStage}</span>
                </div>
                <div class="text-[11px] text-slate-300"><b class="text-indigo-400">Khí cụ:</b> ${p.bracketType}</div>
                <div class="text-[11px] text-slate-300"><b class="text-indigo-400">Lịch siết răng tiếp:</b> <span class="text-rose-400 font-bold font-mono">${p.nextAdjustmentDate || 'Chưa xếp'}</span></div>
                <div class="text-[10px] text-slate-400 bg-slate-950 p-2.5 rounded-xl border border-slate-800">
                    <b class="text-indigo-300">Ghi chú bác sĩ:</b> ${p.doctorNotes}
                </div>
            </div>
        `).join('');
    } catch (e) {
        console.error(e);
    }
}

async function loadShifts() {
    try {
        const res = await apiFetch('/api/shifts');
        const shifts = (res.ok && res.data.success) ? res.data.data : [];
        const container = document.getElementById('shifts-list');
        if (!container) return;

        container.innerHTML = shifts.map(s => `
            <div class="p-4 rounded-2xl bg-slate-900 border border-slate-700 space-y-1.5">
                <div class="flex justify-between items-start">
                    <div>
                        <div class="font-bold text-white text-xs">${s.staff ? s.staff.fullName : 'Nhân sự'}</div>
                        <div class="text-[10px] font-semibold text-brand-400">${s.roleTitle}</div>
                    </div>
                    <span class="bg-slate-800 text-slate-300 text-[9px] font-bold px-2 py-0.5 rounded border border-slate-700">${s.shiftType}</span>
                </div>
                <div class="text-[11px] text-slate-400">${s.notes}</div>
                <div class="text-[10px] text-slate-500 font-mono">${s.shiftDate}</div>
            </div>
        `).join('');
    } catch (e) {
        console.error(e);
    }
}

async function loadNotifications() {
    try {
        const role = currentUser ? currentUser.role : 'ALL';
        const res = await apiFetch(`/api/notifications?role=${role}`);
        const notifs = (res.ok && res.data.success) ? res.data.data : [];
        const container = document.getElementById('notifications-list');
        const badge = document.getElementById('notif-badge');
        if (badge) badge.innerText = notifs.length;
        if (!container) return;

        container.innerHTML = notifs.map(n => `
            <div class="p-4 rounded-2xl bg-slate-900 border border-slate-700 shadow-sm flex items-start space-x-3">
                <div class="w-8 h-8 rounded-xl bg-brand-500/20 text-brand-400 flex items-center justify-center text-xs font-bold mt-0.5">
                    <i class="fa-solid fa-bell"></i>
                </div>
                <div class="flex-1">
                    <div class="flex justify-between items-center">
                        <span class="font-bold text-white text-xs">${n.title}</span>
                        <span class="text-[10px] text-slate-400 font-mono">${formatDate(n.createdAt)}</span>
                    </div>
                    <p class="text-xs text-slate-300 mt-1">${n.message}</p>
                </div>
            </div>
        `).join('');
    } catch (e) {
        console.error(e);
    }
}

function showToast(msg) {
    let container = document.getElementById('toast-notification-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-notification-container';
        container.className = 'fixed bottom-6 right-6 z-[9999] flex flex-col-reverse gap-2.5 pointer-events-none max-w-md w-full sm:w-auto px-4 sm:px-0';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = 'pointer-events-auto bg-slate-950/95 text-white text-xs font-bold px-4 py-3 rounded-2xl shadow-2xl flex items-center justify-between gap-3 border border-slate-700/80 backdrop-blur-md transition-all duration-300 transform translate-y-2 opacity-0';
    toast.innerHTML = `
        <div class="flex items-center gap-2.5">
            <i class="fa-solid fa-circle-check text-brand-400 text-sm"></i>
            <span class="leading-snug">${msg}</span>
        </div>
        <button onclick="this.parentElement.remove()" class="text-slate-500 hover:text-white transition ml-2 cursor-pointer">
            <i class="fa-solid fa-xmark"></i>
        </button>
    `;
    container.appendChild(toast);
    requestAnimationFrame(() => {
        toast.classList.remove('translate-y-2', 'opacity-0');
        toast.classList.add('translate-y-0', 'opacity-100');
    });
    setTimeout(() => {
        toast.classList.add('opacity-0', 'translate-y-2');
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}

function formatDate(dtStr) {
    if (!dtStr) return '';
    try {
        const d = new Date(dtStr);
        return d.toLocaleDateString('vi-VN') + ' ' + d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
        return dtStr;
    }
}

// ================= DYNAMIC DENTIST DROPDOWN LOADER =================
async function loadDentistsDropdown() {
    try {
        const res = await apiFetch('/api/dentists');
        if (res.ok && res.data.success) {
            const selectEl = document.getElementById('dentistId');
            if (!selectEl) return;
            const dentists = res.data.data;
            if (dentists.length === 0) {
                selectEl.innerHTML = '<option value="">Phòng khám phân công bác sĩ phù hợp</option>';
                return;
            }
            selectEl.innerHTML = dentists.map(d => `
                <option value="${d.id}">${d.fullName}</option>
            `).join('');
        }
    } catch (e) {
        console.error('Error loading dentists:', e);
    }
}

// ================= PROMOTIONS & COUPON MANAGEMENT =================
let appliedCouponData = null;

function applyCouponToForm(code) {
    showLandingPage();
    const input = document.getElementById('bookingCouponCode');
    if (input) {
        input.value = code;
        validateBookingCoupon();
    }
    const bookingSec = document.getElementById('booking-section');
    if (bookingSec) {
        bookingSec.scrollIntoView({ behavior: 'smooth' });
    }
}

async function validateBookingCoupon() {
    const input = document.getElementById('bookingCouponCode');
    const msgEl = document.getElementById('coupon-status-msg');
    if (!input || !msgEl) return;

    const code = input.value.trim();
    if (!code) {
        msgEl.className = 'mt-2 p-2.5 rounded-xl text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200 flex items-center gap-2';
        msgEl.innerHTML = '<i class="fa-solid fa-circle-exclamation"></i> Vui lòng nhập mã ưu đãi!';
        msgEl.classList.remove('hidden');
        return;
    }

    try {
        const res = await apiFetch('/api/coupons/validate', {
            method: 'POST',
            body: JSON.stringify({ code: code })
        });

        if (res.ok && res.data.success) {
            appliedCouponData = res.data.data;
            msgEl.className = 'mt-2 p-2.5 rounded-xl text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-300 flex items-center gap-2';
            msgEl.innerHTML = `<i class="fa-solid fa-circle-check text-emerald-600 text-sm"></i> <div><b>${appliedCouponData.title}</b>: <span class="text-rose-600 font-extrabold">${appliedCouponData.discountDescription}</span> (Đã kích hoạt)</div>`;
            msgEl.classList.remove('hidden');
            showToast(`🎉 Áp dụng thành công mã voucher: ${code}!`);
        } else {
            appliedCouponData = null;
            msgEl.className = 'mt-2 p-2.5 rounded-xl text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200 flex items-center gap-2';
            msgEl.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> ${res.data.message || 'Mã ưu đãi không hợp lệ!'}`;
            msgEl.classList.remove('hidden');
        }
    } catch (err) {
        appliedCouponData = null;
        msgEl.className = 'mt-2 p-2.5 rounded-xl text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200 flex items-center gap-2';
        msgEl.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> Mã voucher không tồn tại hoặc đã hết hạn!';
        msgEl.classList.remove('hidden');
    }
}

// Flash Sale Countdown Timer
function startFlashSaleCountdown() {
    let totalSeconds = 3 * 24 * 3600 + 14 * 3600 + 28 * 60 + 45; // 3 days, 14 hours...
    setInterval(() => {
        if (totalSeconds <= 0) totalSeconds = 4 * 24 * 3600;
        totalSeconds--;
        const days = Math.floor(totalSeconds / (3600 * 24));
        const hours = Math.floor((totalSeconds % (3600 * 24)) / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;

        const dEl = document.getElementById('timer-days');
        const hEl = document.getElementById('timer-hours');
        const mEl = document.getElementById('timer-minutes');
        const sEl = document.getElementById('timer-seconds');

        if (dEl) dEl.innerText = String(days).padStart(2, '0');
        if (hEl) hEl.innerText = String(hours).padStart(2, '0');
        if (mEl) mEl.innerText = String(minutes).padStart(2, '0');
        if (sEl) sEl.innerText = String(seconds).padStart(2, '0');
    }, 1000);
}

// ================= FORGOT PASSWORD DIALOG =================
async function promptForgotPassword() {
    const iden = prompt("Nhập Tên đăng nhập hoặc Số điện thoại của bạn để nhận mã OTP khôi phục mật khẩu:");
    if (!iden || !iden.trim()) return;

    try {
        const res = await apiFetch('/api/auth/forgot-password', {
            method: 'POST',
            body: JSON.stringify({ identifier: iden.trim() })
        });

        if (res.ok && res.data.success) {
            alert(`✅ ${res.data.message}`);
        } else {
            alert(`❌ ${res.data.message || 'Không tìm thấy tài khoản!'}`);
        }
    } catch (e) {
        alert("❌ Đã có lỗi xảy ra khi gửi yêu cầu!");
    }
}

// ================= RECEPTIONIST & OWNER COUPON MANAGEMENT =================
function openCouponCreateModal() {
    const modal = document.getElementById('coupon-create-modal');
    if (modal) modal.classList.remove('hidden');
}

function closeCouponCreateModal() {
    const modal = document.getElementById('coupon-create-modal');
    if (modal) modal.classList.add('hidden');
}

async function handleCreateCouponSubmit(e) {
    e.preventDefault();
    const code = document.getElementById('couponCodeInput').value.trim().toUpperCase();
    const title = document.getElementById('couponTitleInput').value.trim();
    const discountType = document.getElementById('couponDiscountType').value;
    const discountValue = parseFloat(document.getElementById('couponDiscountValue').value);
    const applicableService = document.getElementById('couponServiceInput') ? document.getElementById('couponServiceInput').value : 'ALL';
    const validDays = parseInt(document.getElementById('couponValidDays').value) || 30;
    const description = document.getElementById('couponDescInput') ? document.getElementById('couponDescInput').value.trim() : '';

    const payload = {
        code: code,
        title: title,
        discountType: discountType,
        discountValue: discountValue,
        applicableService: applicableService,
        validDays: validDays,
        description: description
    };

    try {
        const res = await apiFetch('/api/coupons', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.ok && res.data.success) {
            showToast(`✅ Đã tạo thành công mã ưu đãi: ${payload.code}!`);
            closeCouponCreateModal();
            const form = document.getElementById('create-coupon-form');
            if (form) form.reset();
            loadDashboardCoupons();
        } else {
            alert(res.data.message || 'Lỗi khi tạo mã ưu đãi!');
        }
    } catch (err) {
        alert('Lỗi kết nối khi tạo mã ưu đãi!');
    }
}

async function loadDashboardCoupons() {
    try {
        const res = await apiFetch('/api/coupons/active');
        const coupons = (res.ok && res.data.success) ? res.data.data : [];
        const tbody = document.getElementById('coupons-table-body');
        if (!tbody) return;

        if (coupons.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-6 text-slate-400">Chưa có mã ưu đãi nào đang kích hoạt</td></tr>`;
            return;
        }

        tbody.innerHTML = coupons.map(c => `
            <tr class="hover:bg-slate-800/80 transition font-medium">
                <td class="py-3.5 px-4 font-mono font-black text-rose-400 text-sm">${c.code}</td>
                <td class="py-3.5 px-4">
                    <div class="font-bold text-white">${c.title}</div>
                    <div class="text-slate-400 text-[10px]">${c.description || 'Không có mô tả'}</div>
                </td>
                <td class="py-3.5 px-4 font-extrabold text-gold-400">
                    ${c.discountType === 'FIXED_AMOUNT' ? Number(c.discountValue).toLocaleString('vi-VN') + ' đ' : c.discountValue + '%'}
                </td>
                <td class="py-3.5 px-4">
                    <span class="bg-slate-700/80 text-slate-200 px-2 py-0.5 rounded text-[10px] font-bold">${c.applicableService}</span>
                </td>
                <td class="py-3.5 px-4 text-slate-400 font-mono">${c.validUntil || 'Vô thời hạn'}</td>
                <td class="py-3.5 px-4">
                    <span class="bg-emerald-500/20 text-emerald-300 font-bold px-2 py-0.5 rounded-full text-[10px] border border-emerald-500/30">Đang Bật</span>
                </td>
                <td class="py-3.5 px-4 text-right">
                    <button onclick="deleteCoupon(${c.id}, '${c.code}')" class="text-rose-400 hover:text-rose-300 font-bold text-xs p-1.5 hover:bg-rose-500/20 rounded-lg transition cursor-pointer" title="Xóa mã">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Error loading dashboard coupons:', e);
    }
}

async function deleteCoupon(id, code) {
    if (!confirm(`Bạn có chắc muốn hủy kích hoạt mã ưu đãi "${code}"?`)) return;
    try {
        const res = await apiFetch(`/api/coupons/${id}`, { method: 'DELETE' });
        if (res.ok && res.data.success) {
            showToast(`Đã xóa mã ưu đãi ${code}!`);
            loadDashboardCoupons();
        }
    } catch (e) {
        console.error(e);
    }
}

// ================= MOBILE NAVIGATION MENU TOGGLE =================
function toggleMobileMenu() {
    const drawer = document.getElementById('mobile-menu-drawer');
    const icon = document.getElementById('mobile-menu-icon');
    if (!drawer) return;

    const isHidden = drawer.classList.contains('hidden');
    drawer.classList.toggle('hidden', !isHidden);
    if (icon) {
        icon.className = isHidden ? 'fa-solid fa-xmark text-lg text-rose-500' : 'fa-solid fa-bars text-lg';
    }
}

function closeMobileMenu() {
    showLandingPage();
    const drawer = document.getElementById('mobile-menu-drawer');
    const icon = document.getElementById('mobile-menu-icon');
    if (drawer) drawer.classList.add('hidden');
    if (icon) icon.className = 'fa-solid fa-bars text-lg';
}

// ================= BOOKINGCARE-STYLE TIME SLOTS SELECTOR =================
function selectTimeSlot(timeStr, btnElement) {
    const apptInput = document.getElementById('appointmentTime');
    if (!apptInput) return;

    // Get selected date or default to today
    let selectedDate = new Date();
    if (apptInput.value) {
        const parsed = new Date(apptInput.value);
        if (!isNaN(parsed.getTime())) {
            selectedDate = parsed;
        }
    }

    const yyyy = selectedDate.getFullYear();
    const mm = String(selectedDate.getMonth() + 1).padStart(2, '0');
    const dd = String(selectedDate.getDate()).padStart(2, '0');
    apptInput.value = `${yyyy}-${mm}-${dd}T${timeStr}`;

    // Highlight active time slot button
    document.querySelectorAll('.time-slot-btn').forEach(btn => {
        btn.classList.remove('bg-brand-600', 'text-white', 'border-brand-600', 'shadow-md');
        btn.classList.add('bg-white', 'text-slate-800', 'border-slate-200');
    });

    if (btnElement) {
        btnElement.classList.remove('bg-white', 'text-slate-800', 'border-slate-200');
        btnElement.classList.add('bg-brand-600', 'text-white', 'border-brand-600', 'shadow-md');
    }

    showToast(`⏰ Đã chọn khung giờ khám: ${timeStr} ngày ${dd}/${mm}`);
    trackGaEvent('select_time_slot', { slot: timeStr, date: `${yyyy}-${mm}-${dd}` });
}

// ================= FAQ ACCORDION TOGGLER =================
function toggleFaq(faqId) {
    const content = document.getElementById(`content-${faqId}`);
    const icon = document.getElementById(`icon-${faqId}`);
    if (!content || !icon) return;

    const isClosed = content.classList.contains('hidden');
    
    // Close other FAQs
    document.querySelectorAll('[id^="content-faq-"]').forEach(el => el.classList.add('hidden'));
    document.querySelectorAll('[id^="icon-faq-"]').forEach(el => el.classList.remove('rotate-180', 'text-brand-600'));

    if (isClosed) {
        content.classList.remove('hidden');
        icon.classList.add('rotate-180', 'text-brand-600');
        trackGaEvent('view_faq_answer', { faq_id: faqId });
    }
}

// ================= MEDICAL HANDBOOK ARTICLE READER =================
const HANDBOOK_ARTICLES = {
    'nieng_rang': {
        category: 'Chỉnh Nha Chuyên Sâu',
        title: 'Bảng Giá Niềng Răng 2026: Chi Tiết Từng Loại & Lộ Trình Trả Góp 0%',
        content: `
            <div class="space-y-4">
                <p class="text-sm font-semibold text-slate-800 leading-relaxed">
                    Niềng răng (chỉnh nha) là giải pháp hàng đầu giúp khắc phục tình trạng răng hô, móm, khấp khểnh, thưa hoặc sai lệch khớp cắn, mang lại nụ cười chuẩn tỉ lệ vàng và khuôn mặt cân đối.
                </p>
                
                <h4 class="text-sm font-black text-slate-900 border-l-4 border-brand-500 pl-3">1. Phân biệt các phương pháp niềng răng phổ biến:</h4>
                <div class="space-y-2">
                    <div class="p-3 bg-slate-50 rounded-xl border border-slate-200">
                        <b class="text-brand-700">● Mắc cài kim loại tự buộc Damon Q2 (25 Triệu):</b> Rút ngắn 4-6 tháng so với mắc cài thường, lực siết êm ái, hạn chế nhổ răng tối đa.
                    </div>
                    <div class="p-3 bg-slate-50 rounded-xl border border-slate-200">
                        <b class="text-indigo-700">● Mắc cài sứ thẩm mỹ (38 Triệu):</b> Màu sắc tiệp màu răng thật 85%, độ bền cao, tính thẩm mỹ vượt trội khi giao tiếp.
                    </div>
                    <div class="p-3 bg-slate-50 rounded-xl border border-slate-200">
                        <b class="text-emerald-700">● Khay trong suốt Invisalign Mỹ (65 Triệu):</b> Vô hình 100%, tháo lắp linh hoạt khi ăn uống và vệ sinh, thấy trước kết quả 3D ClinCheck qua từng giai đoạn.
                    </div>
                </div>

                <h4 class="text-sm font-black text-slate-900 border-l-4 border-brand-500 pl-3">2. Chính sách hỗ trợ trả góp 0% tại DentalCare:</h4>
                <p>Khách hàng chỉ cần trả trước <b>30% chi phí</b> ban đầu khi gắn mắc cài. 70% còn lại được chia đều đóng theo từng lần tái khám hàng tháng (khoảng <b>1.200.000đ - 1.800.000đ / tháng</b>) mà không chịu thêm bất kỳ khoản lãi suất nào.</p>
            </div>
        `
    },
    'implant': {
        category: 'Trồng Răng Kỹ Thuật Số',
        title: 'Trồng Răng Implant Có Đau Không? Quy Trình 4 Bước Chuẩn Bộ Y Tế',
        content: `
            <div class="space-y-4">
                <p class="text-sm font-semibold text-slate-800 leading-relaxed">
                    Cấy ghép Implant là kỹ thuật phục hình răng mất hoàn hảo nhất hiện nay, thay thế cả chân răng và thân răng đã mất, ngăn ngừa tuyệt đối tình trạng tiêu xương hàm và hóp má.
                </p>
                <h4 class="text-sm font-black text-slate-900 border-l-4 border-amber-500 pl-3">1. Cấy ghép Implant có đau như lời đồn không?</h4>
                <p>Với công nghệ định vị 3D vi phẫu kết hợp thuốc tê chuyên dụng thế hệ mới, <b>bệnh nhân hoàn toàn không có cảm giác đau đớn trong suốt quá trình cấy trụ</b>. Thời gian cấy 1 trụ chỉ kéo dài từ 15 đến 20 phút - nhanh hơn cả một ca nhổ răng thông thường.</p>
                
                <h4 class="text-sm font-black text-slate-900 border-l-4 border-amber-500 pl-3">2. Quy trình 4 bước chuẩn Bộ Y Tế tại DentalCare:</h4>
                <ul class="list-disc pl-5 space-y-1.5">
                    <li><b>Bước 1:</b> Chụp phim CT Cone Beam 3D khảo sát mật độ xương và lập kế hoạch phục hình kỹ thuật số.</li>
                    <li><b>Bước 2:</b> Cấy trụ Implant chính hãng Thụy Sĩ / Hàn Quốc vào xương hàm trong phòng phẫu thuật áp lực dương vô trùng 1 chiều.</li>
                    <li><b>Bước 3:</b> Gắn răng tạm thẩm mỹ để đảm bảo ăn nhai và giao tiếp bình thường.</li>
                    <li><b>Bước 4:</b> Lắp mão răng sứ nguyên khối Zirconia sau khi trụ tích hợp xương hoàn tất.</li>
                </ul>
            </div>
        `
    },
    'rang_khon': {
        category: 'Tiểu Phẫu Không Đau',
        title: 'Nhổ Răng Khôn Sóng Siêu Âm Piezotome: Giảm Sưng Đau 80%, Lành Thương 24h',
        content: `
            <div class="space-y-4">
                <p class="text-sm font-semibold text-slate-800 leading-relaxed">
                    Răng khôn (răng số 8) mọc lệch, mọc ngầm là nguyên nhân hàng đầu gây sâu răng số 7, viêm lợi trùm, tiêu xương hàm và xô lệch toàn bộ hàm răng.
                </p>
                <h4 class="text-sm font-black text-slate-900 border-l-4 border-indigo-500 pl-3">Vì sao nên chọn công nghệ sóng siêu âm Piezotome?</h4>
                <div class="space-y-2">
                    <p>Khác với phương pháp truyền thống dùng kìm và búa đục gây sang chấn mạnh, máy siêu âm Piezotome chỉ tác động lên mô cứng quanh chân răng, bảo vệ an toàn 100% mô mềm và dây thần kinh hàm dưới.</p>
                    <div class="p-3 bg-indigo-50 text-indigo-900 rounded-xl border border-indigo-200">
                        <b>✓ Ưu điểm vượt trội:</b> Không sưng má, không đau buốt, thời gian nhổ chỉ 10-15 phút và có thể ăn cháo nhẹ ngay sau 2 giờ.
                    </div>
                </div>
            </div>
        `
    },
    'rang_su': {
        category: 'Thẩm Mỹ Nụ Cười',
        title: 'Bọc Răng Sứ & Dán Sứ Veneer Không Mài Răng: Giải Pháp Nào Phù Hợp Cho Bạn?',
        content: `
            <div class="space-y-4">
                <p class="text-sm font-semibold text-slate-800 leading-relaxed">
                    Sở hữu hàm răng trắng sáng đều đặn giúp bạn tự tin tỏa sáng trong công việc và cuộc sống. Dưới đây là cách phân biệt 2 phương pháp thẩm mỹ răng sứ hot nhất hiện nay:
                </p>
                <h4 class="text-sm font-black text-slate-900 border-l-4 border-rose-500 pl-3">So sánh Bọc sứ vs Dán sứ Veneer:</h4>
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                    <div class="p-3.5 bg-slate-50 rounded-xl border border-slate-200">
                        <b class="text-rose-700 block mb-1 text-sm">Bọc Răng Sứ Toàn Phần</b>
                        <p>Phù hợp cho răng đã chữa tủy, răng mẻ vỡ lớn hoặc nhiễm màu kháng sinh nặng. Độ chịu lực gấp 5-7 lần răng thật.</p>
                    </div>
                    <div class="p-3.5 bg-slate-50 rounded-xl border border-slate-200">
                        <b class="text-brand-700 block mb-1 text-sm">Dán Sứ Veneer Siêu Mỏng</b>
                        <p>Chỉ mài siêu mỏng bề mặt 0.2 - 0.5mm (hoặc không mài), bảo tồn 99% tủy và răng gốc tự nhiên. Màu sắc trong bóng tự nhiên như ngọc trai.</p>
                    </div>
                </div>
            </div>
        `
    }
};

function openHandbookArticle(articleKey) {
    const data = HANDBOOK_ARTICLES[articleKey];
    if (!data) return;

    document.getElementById('article-modal-category').innerText = data.category;
    document.getElementById('article-modal-title').innerText = data.title;
    document.getElementById('article-modal-body').innerHTML = data.content;
    document.getElementById('handbook-article-modal').classList.remove('hidden');

    trackGaEvent('read_handbook_article', { article_id: articleKey, title: data.title });
}

function closeHandbookArticle() {
    const modal = document.getElementById('handbook-article-modal');
    if (modal) modal.classList.add('hidden');
}


// =========================================================================
// 9ROUTER AI BLOG WRITER & DYNAMIC HANDBOOK INTEGRATION
// =========================================================================
let loadedArticlesCache = [];

function openAiWriterModal() {
    const modal = document.getElementById('ai-writer-modal');
    if (modal) {
        modal.classList.remove('hidden');
        const topicInput = document.getElementById('ai-topic-input');
        if (topicInput && !topicInput.value) {
            topicInput.value = 'Bọc Răng Sứ Có Bị Hôi Miệng Không? 5 Sai Lầm Phổ Biến & Giải Pháp';
        }
    }
}

function closeAiWriterModal() {
    const modal = document.getElementById('ai-writer-modal');
    if (modal) modal.classList.add('hidden');
}

function setAiTopic(topic, category) {
    const input = document.getElementById('ai-topic-input');
    const catSelect = document.getElementById('ai-category-select');
    if (input) input.value = topic;
    if (catSelect && category) catSelect.value = category;
}

async function submitAiArticle(publishImmediately) {
    const topicInput = document.getElementById('ai-topic-input');
    const catSelect = document.getElementById('ai-category-select');
    const docSelect = document.getElementById('ai-doctor-select');
    const loadingBox = document.getElementById('ai-loading-box');
    const resultBox = document.getElementById('ai-result-box');
    const previewBox = document.getElementById('ai-result-preview');
    const publishBtn = document.getElementById('ai-publish-btn');

    const topic = topicInput ? topicInput.value.trim() : '';
    if (!topic) {
        alert('Vui lòng nhập chủ đề bài viết cần AI soạn thảo!');
        return;
    }

    const category = catSelect ? catSelect.value : 'IMPLANT';
    const doctorName = docSelect ? docSelect.value : 'BS.CKII Trần Văn Thắng';

    if (loadingBox) loadingBox.classList.remove('hidden');
    if (resultBox) resultBox.classList.add('hidden');
    if (publishBtn) {
        publishBtn.disabled = true;
        publishBtn.classList.add('opacity-50', 'cursor-not-allowed');
    }

    try {
        const endpoint = publishImmediately ? '/api/articles/ai-generate-and-publish' : '/api/articles/ai-generate';
        const res = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ topic, category, doctorName })
        });

        if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || 'Lỗi khi gọi API 9Router');
        }

        const article = await res.json();

        if (loadingBox) loadingBox.classList.add('hidden');
        if (resultBox) resultBox.classList.remove('hidden');

        if (previewBox) {
            previewBox.innerHTML = `
                <div class="font-black text-slate-900 text-sm mb-1">${article.title}</div>
                <div class="text-[11px] text-slate-500 mb-2 flex items-center gap-3">
                    <span><i class="fa-solid fa-user-doctor text-purple-600"></i> ${article.authorName || doctorName}</span>
                    <span><i class="fa-regular fa-clock text-amber-500"></i> ${article.readTime || '5 phút đọc'}</span>
                    <span class="text-emerald-600 font-bold"><i class="fa-solid fa-check"></i> Đã kích hoạt SEO URL: /${article.slug}</span>
                </div>
                <p class="text-[11px] text-slate-600 line-clamp-2 italic">${article.summary || ''}</p>
                <div class="pt-2 flex items-center gap-2">
                    <button type="button" onclick="openHandbookArticleByData('${article.slug}')" class="px-3 py-1 bg-purple-600 text-white rounded-lg font-bold text-[11px] hover:bg-purple-700 cursor-pointer">
                        Xem Toàn Bộ Bài Viết ➔
                    </button>
                </div>
            `;
        }

        // Refresh the articles list on the homepage
        await loadDynamicArticles();

    } catch (err) {
        console.error('AI generate error:', err);
        if (loadingBox) loadingBox.classList.add('hidden');
        alert('Lỗi trong quá trình tạo bài viết AI: ' + (err.message || 'Không thể kết nối đến AI'));
    } finally {
        if (publishBtn) {
            publishBtn.disabled = false;
            publishBtn.classList.remove('opacity-50', 'cursor-not-allowed');
        }
    }
}

async function loadDynamicArticles() {
    const grid = document.getElementById('handbook-articles-grid');
    if (!grid) return;

    try {
        const res = await fetch('/api/articles');
        if (!res.ok) return;
        const articles = await res.json();
        if (!articles || articles.length === 0) return;

        loadedArticlesCache = articles;

        const categoryLabels = {
            'NIENG_RANG': { text: 'Chỉnh Nha', bg: 'bg-brand-700' },
            'IMPLANT': { text: 'Implant', bg: 'bg-indigo-600' },
            'RANG_SU': { text: 'Thẩm Mỹ', bg: 'bg-rose-600' },
            'RANG_KHON': { text: 'Tiểu Phẫu', bg: 'bg-amber-600' },
            'TONG_QUAT': { text: 'Tổng Quát', bg: 'bg-emerald-600' }
        };

        const defaultThumbs = [
            'https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?w=500&auto=format&fit=crop&q=80',
            'https://images.unsplash.com/photo-1606811841689-23dfddce3e95?w=500&auto=format&fit=crop&q=80',
            'https://images.unsplash.com/photo-1629909613654-28e377c37b09?w=500&auto=format&fit=crop&q=80',
            'https://images.unsplash.com/photo-1571772996211-2f02c9727629?w=500&auto=format&fit=crop&q=80',
            'https://images.unsplash.com/photo-1598256989800-fe5f95da9787?w=500&auto=format&fit=crop&q=80'
        ];

        grid.innerHTML = articles.slice(0, 8).map((art, idx) => {
            const catBadge = categoryLabels[art.category] || { text: art.category || 'Nha Khoa', bg: 'bg-brand-600' };
            const thumb = defaultThumbs[idx % defaultThumbs.length];
            return `
                <div class="bg-white rounded-3xl p-5 border border-slate-200 hover:border-brand-500 hover:shadow-xl transition-all flex flex-col justify-between group cursor-pointer" onclick="openHandbookArticleByData('${art.slug}')">
                    <div>
                        <div class="h-40 rounded-2xl overflow-hidden mb-4 relative">
                            <img src="${thumb}" class="w-full h-full object-cover group-hover:scale-105 transition duration-500" alt="${art.title}">
                            <span class="absolute top-2.5 left-2.5 ${catBadge.bg} text-white text-[9px] font-black px-2.5 py-1 rounded-full uppercase">${catBadge.text}</span>
                        </div>
                        <h3 class="font-extrabold text-slate-900 text-sm mb-2 group-hover:text-brand-600 transition line-clamp-2">
                            ${art.title}
                        </h3>
                        <p class="text-[11px] text-slate-500 line-clamp-3 leading-relaxed mb-4">
                            ${art.summary || 'Tổng hợp kiến thức y khoa chuyên sâu từ bác sĩ chuyên khoa răng hàm mặt.'}
                        </p>
                    </div>
                    <div class="flex items-center justify-between pt-3 border-t border-slate-100 text-[11px] text-brand-700 font-bold">
                        <span>Đọc tiếp ➔</span>
                        <span class="text-slate-600 font-medium"><i class="fa-regular fa-clock"></i> ${art.readTime || '5 phút đọc'}</span>
                    </div>
                </div>
            `;
        }).join('');

    } catch (err) {
        console.warn('Could not load dynamic articles:', err);
    }
}

function openHandbookArticleByData(slug) {
    const article = loadedArticlesCache.find(a => a.slug === slug);
    if (!article) {
        // Try to fetch by slug directly
        fetch('/api/articles/' + slug)
            .then(r => r.json())
            .then(data => {
                if (data && data.title) showArticleInModal(data);
            })
            .catch(e => console.error(e));
        return;
    }
    showArticleInModal(article);
}

function showArticleInModal(article) {
    const catEl = document.getElementById('article-modal-category');
    const titleEl = document.getElementById('article-modal-title');
    const bodyEl = document.getElementById('article-modal-body');
    const modal = document.getElementById('handbook-article-modal');

    if (catEl) catEl.innerText = article.category || 'Cẩm Nang Y Khoa';
    if (titleEl) titleEl.innerText = article.title;
    if (bodyEl) bodyEl.innerHTML = article.content;
    if (modal) modal.classList.remove('hidden');
}

// Initialize dynamic articles on page load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        loadDynamicArticles();
        init3DAnimations();
    });
} else {
    loadDynamicArticles();
    init3DAnimations();
}

// ================= 3D ANIMATION & INTERACTION ENGINE (GSAP POWERED) =================
function init3DAnimations() {
    // 1. Interactive 3D Card Tilt on Mouse Move
    const tiltElements = document.querySelectorAll('.card-3d-tilt');
    tiltElements.forEach(card => {
        let isHovered = false;

        card.addEventListener('mouseenter', () => {
            isHovered = true;
        });

        card.addEventListener('mousemove', (e) => {
            if (!isHovered) return;
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left; // x position within the element
            const y = e.clientY - rect.top;  // y position within the element
            
            const centerX = rect.width / 2;
            const centerY = rect.height / 2;

            // Calculate tilt angle: max 12 degrees
            const rotateX = ((y - centerY) / centerY) * -10;
            const rotateY = ((x - centerX) / centerX) * 10;

            if (window.gsap) {
                gsap.to(card, {
                    rotationX: rotateX,
                    rotationY: rotateY,
                    transformPerspective: 1000,
                    scale: 1.02,
                    duration: 0.25,
                    ease: "power1.out",
                    overwrite: "auto"
                });
            } else {
                card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.02, 1.02, 1.02)`;
            }
        });

        card.addEventListener('mouseleave', () => {
            isHovered = false;
            if (window.gsap) {
                gsap.to(card, {
                    rotationX: 0,
                    rotationY: 0,
                    scale: 1,
                    duration: 0.6,
                    ease: "power2.out",
                    overwrite: "auto"
                });
            } else {
                card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
            }
        });
    });

    // 2. Parallax Floating Animations for Hero 3D Badges & Elements via GSAP
    if (window.gsap) {
        // Subtle floating pulse for 3D tooth icon badge
        gsap.to(".fa-tooth", {
            rotation: 8,
            yoyo: true,
            repeat: -1,
            duration: 2.5,
            ease: "sine.inOut"
        });

        // Entrance animation for Hero Cards if visible
        const heroDoctorCard = document.querySelector('.perspective-1000 .card-3d-tilt');
        if (heroDoctorCard) {
            gsap.from(heroDoctorCard, {
                opacity: 0,
                y: 35,
                rotationX: 12,
                duration: 1.1,
                ease: "power3.out"
            });
        }
    }
}
