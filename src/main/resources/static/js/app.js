
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

// ================= GOOGLE ANALYTICS 4 & AGRID EVENT TRACKER =================
function trackGaEvent(eventName, params = {}) {
    if (typeof gtag === 'function') {
        gtag('event', eventName, params);
        console.log(`📊 [Google Analytics] Event tracked: ${eventName}`, params);
    }
    if (window.Agrid && typeof window.Agrid.track === 'function') {
        const type = (eventName && eventName.includes('funnel')) ? 'BOOKING_FUNNEL' : 'CLICK';
        window.Agrid.track(type, eventName, params);
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
    initCart();
    loadDentalServices();
    loadDentalProducts();
    loadBranches();
    loadForumPosts();
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

    const staffM2Tabs = ['tab-inventory', 'tab-b2b-orders', 'tab-attendance', 'tab-doctor-kpi', 'tab-field-intake'];

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

        staffM2Tabs.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = 'flex';
        });

        switchTab('itteam');
        return;
    }

    // 1. KHÁCH HÀNG / BỆNH NHÂN (ROLE_PATIENT) -> 100% KHÔNG THẤY DASHBOARD & STAFF TABS
    if (role === 'ROLE_PATIENT') {
        portalTitle.innerHTML = `<i class="fa-solid fa-user text-brand-400"></i> Cổng Chăm Sóc Khách Hàng`;
        if (tabDashboard) tabDashboard.style.display = 'none';
        if (tabShifts) tabShifts.style.display = 'none';
        const tabCoupons = document.getElementById('tab-coupons'); if (tabCoupons) tabCoupons.style.display = 'none';
        if (staffAdminBar) staffAdminBar.classList.add('hidden');

        staffM2Tabs.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = 'none';
        });

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
    // 2. CHỦ PHÒNG & LỄ TÂN (ROLE_OWNER, ROLE_RECEPTIONIST) -> MỞ DASHBOARD & QUẢN LÝ ƯU ĐÃI & STAFF TABS
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

        staffM2Tabs.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = 'flex';
        });

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

        ['tab-inventory', 'tab-attendance', 'tab-doctor-kpi', 'tab-field-intake'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = 'flex';
        });
        const b2bTab = document.getElementById('tab-b2b-orders');
        if (b2bTab) b2bTab.style.display = 'none';

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

        const atnTab = document.getElementById('tab-attendance');
        if (atnTab) atnTab.style.display = 'flex';
        ['tab-inventory', 'tab-b2b-orders', 'tab-doctor-kpi', 'tab-field-intake'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = 'none';
        });

        switchTab('shifts');
    }
}

// Switch Tabs
function switchTab(tabId) {
    const tabs = ['dashboard', 'appointments', 'coupons', 'emr', 'shifts', 'notifications', 'itteam', 'inventory', 'b2b-orders', 'attendance', 'doctor-kpi', 'field-intake'];
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
    if (tabId === 'inventory') loadInventoryMaterials();
    if (tabId === 'b2b-orders') loadB2BOrders();
    if (tabId === 'attendance') loadAttendanceRecords();
    if (tabId === 'doctor-kpi') loadDoctorKpis();
    if (tabId === 'field-intake') loadFieldIntakeLeads();
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

// =========================================================================
// MILESTONE 1: CUSTOMER DENTAL ECOSYSTEM (CATALOG, SHOP, WARRANTY, AI, BRANCHES, FORUM)
// =========================================================================

let cachedServices = [];
let cachedProducts = [];
let cachedBranches = [];
let cachedForumPosts = [];
let currentProductForModal = null;
let selectedPackagingOption = null;
let modalPackagingQty = 1;
let cartItems = [];

// Helper: Currency Formatter
function formatVND(amount) {
    if (amount === undefined || amount === null) return '0 đ';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// -------------------------------------------------------------------------
// 1. DYNAMIC DENTAL SERVICE CATALOG
// -------------------------------------------------------------------------
async function loadDentalServices(category = 'ALL') {
    const grid = document.getElementById('dynamic-services-grid');
    if (!grid) return;
    
    try {
        let url = '/api/dental-services';
        if (category && category !== 'ALL') {
            url += `?category=${encodeURIComponent(category)}`;
        }
        const res = await apiFetch(url);
        if (res.ok && res.data.success) {
            cachedServices = res.data.data || [];
            renderDentalServices(cachedServices);
        }
    } catch (err) {
        console.error('Error loading dental services:', err);
    }
}

function renderDentalServices(services) {
    const grid = document.getElementById('dynamic-services-grid');
    if (!grid) return;

    if (!services || services.length === 0) {
        grid.innerHTML = `
            <div class="col-span-full text-center py-12 text-slate-400 text-xs">
                <i class="fa-solid fa-folder-open text-3xl mb-2 text-slate-300"></i>
                <p>Chưa tìm thấy dịch vụ trong chuyên mục này.</p>
            </div>`;
        return;
    }

    const categoryIcons = {
        'ORTHODONTICS': 'fa-teeth-open',
        'IMPLANT': 'fa-tooth',
        'PORCELAIN_CROWNS': 'fa-wand-magic-sparkles',
        'WHITENING': 'fa-sparkles',
        'WISDOM_TEETH': 'fa-bone',
        'GENERAL': 'fa-shield-halved'
    };

    grid.innerHTML = services.map(svc => {
        const icon = categoryIcons[svc.category] || 'fa-tooth';
        return `
            <div class="card-3d-tilt bg-white p-6 rounded-3xl border border-slate-200/90 hover:border-brand-500 hover:shadow-xl transition-all duration-300 flex flex-col justify-between group">
                <div>
                    <div class="flex items-center justify-between mb-4">
                        <div class="w-12 h-12 rounded-2xl bg-brand-50 text-brand-700 flex items-center justify-center text-xl group-hover:scale-110 group-hover:bg-brand-600 group-hover:text-white transition-all shadow-sm">
                            <i class="fa-solid ${icon}"></i>
                        </div>
                        ${svc.isFeatured ? `
                            <span class="inline-flex items-center gap-1 text-[10px] font-black uppercase tracking-wider bg-rose-50 text-rose-600 border border-rose-200 px-2.5 py-0.5 rounded-full">
                                <i class="fa-solid fa-fire text-rose-500"></i> Nổi Bật
                            </span>` : ''}
                    </div>
                    <span class="text-[10px] font-extrabold uppercase tracking-widest text-slate-600 font-mono">${svc.code || ''}</span>
                    <h3 class="text-base font-extrabold text-slate-900 group-hover:text-brand-700 transition line-clamp-1 mb-2">
                        ${svc.name}
                    </h3>
                    <p class="text-xs text-slate-500 leading-relaxed line-clamp-3 mb-4">
                        ${svc.description || 'Dịch vụ nha khoa chuẩn quốc tế thực hiện bởi Bác sĩ Chuyên khoa II.'}
                    </p>
                </div>

                <div class="pt-4 border-t border-slate-100 flex items-center justify-between">
                    <div>
                        <div class="text-[10px] text-slate-600 font-semibold flex items-center gap-1">
                            <i class="fa-regular fa-clock"></i> ${svc.durationMinutes || 45} phút
                        </div>
                        <div class="text-sm font-black text-brand-700">
                            ${formatVND(svc.price)}
                        </div>
                    </div>
                    <button type="button" onclick="selectServiceForBooking('${svc.name.replace(/'/g, "\\'")}')" class="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold rounded-xl shadow-md transition flex items-center gap-1.5 cursor-pointer">
                        <span>Đặt Lịch</span>
                        <i class="fa-solid fa-arrow-right text-[10px]"></i>
                    </button>
                </div>
            </div>`;
    }).join('');
}

function filterServices(category, btnElement) {
    document.querySelectorAll('#service-catalog-tabs .service-tab-btn').forEach(btn => {
        btn.classList.remove('bg-brand-600', 'text-white', 'shadow-sm');
        btn.classList.add('bg-slate-100', 'text-slate-700');
    });
    if (btnElement) {
        btnElement.classList.remove('bg-slate-100', 'text-slate-700');
        btnElement.classList.add('bg-brand-600', 'text-white', 'shadow-sm');
    } else {
        // Fallback selector by onclick
        const targetBtn = Array.from(document.querySelectorAll('#service-catalog-tabs .service-tab-btn'))
            .find(b => b.getAttribute('onclick') && b.getAttribute('onclick').includes(category));
        if (targetBtn) {
            targetBtn.classList.remove('bg-slate-100', 'text-slate-700');
            targetBtn.classList.add('bg-brand-600', 'text-white', 'shadow-sm');
        }
    }
    loadDentalServices(category);
}

function selectServiceForBooking(serviceName) {
    const bookingSection = document.getElementById('booking-section');
    const serviceSelect = document.getElementById('serviceName');
    if (serviceSelect) {
        let matched = false;
        for (let i = 0; i < serviceSelect.options.length; i++) {
            if (serviceSelect.options[i].value === serviceName || serviceSelect.options[i].text.includes(serviceName)) {
                serviceSelect.selectedIndex = i;
                matched = true;
                break;
            }
        }
        if (!matched) {
            const opt = new Option(serviceName, serviceName, true, true);
            serviceSelect.add(opt);
        }
    }
    if (bookingSection) {
        bookingSection.scrollIntoView({ behavior: 'smooth' });
    }
    showToast(`✓ Đã chọn dịch vụ: ${serviceName}`);
}

// -------------------------------------------------------------------------
// 2. DENTAL CARE SHOPPING & CART WITH PACKAGING DISCOUNTS
// -------------------------------------------------------------------------
function initCart() {
    try {
        const saved = localStorage.getItem('DENTAL_CART');
        cartItems = saved ? JSON.parse(saved) : [];
        updateCartBadge();
    } catch (e) {
        cartItems = [];
    }
}

function updateCartBadge() {
    const badge = document.getElementById('cart-badge-count');
    const totalQty = cartItems.reduce((sum, item) => sum + (item.quantity || 1), 0);
    if (badge) {
        badge.innerText = totalQty;
        badge.classList.toggle('hidden', totalQty === 0);
    }
}

async function loadDentalProducts(category = 'ALL') {
    const grid = document.getElementById('dental-products-grid');
    if (!grid) return;

    try {
        let url = '/api/dental-products';
        if (category && category !== 'ALL') {
            url += `?category=${encodeURIComponent(category)}`;
        }
        const res = await apiFetch(url);
        if (res.ok && res.data.success) {
            cachedProducts = res.data.data || [];
            renderDentalProducts(cachedProducts);
        }
    } catch (err) {
        console.error('Error loading dental products:', err);
    }
}

function renderDentalProducts(products) {
    const grid = document.getElementById('dental-products-grid');
    if (!grid) return;

    if (!products || products.length === 0) {
        grid.innerHTML = `
            <div class="col-span-full text-center py-12 text-slate-400 text-xs">
                <i class="fa-solid fa-box-open text-3xl mb-2 text-slate-300"></i>
                <p>Chưa có sản phẩm trong danh mục này.</p>
            </div>`;
        return;
    }

    grid.innerHTML = products.map(prod => {
        const packagingCount = (prod.packagingOptions || []).length;
        const comboOption = (prod.packagingOptions || []).find(p => p.packagingType === 'COMBO_PACK');
        const comboDiscount = comboOption ? comboOption.discountPercent : 0;

        return `
            <div class="card-3d-tilt bg-white rounded-3xl border border-slate-200/90 overflow-hidden hover:border-emerald-500 hover:shadow-xl transition-all duration-300 flex flex-col justify-between group">
                <div class="relative h-48 bg-slate-100 overflow-hidden">
                    <img loading="lazy" src="${prod.imageUrl || 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=400&auto=format&fit=crop&q=80'}" 
                         alt="${prod.name}" class="w-full h-full object-cover group-hover:scale-105 transition duration-500">
                    <span class="absolute top-3 left-3 bg-slate-900/80 backdrop-blur-xs text-white text-[10px] font-black uppercase px-2.5 py-1 rounded-lg">
                        ${prod.brand || 'DentalCare'}
                    </span>
                    ${comboDiscount > 0 ? `
                        <span class="absolute top-3 right-3 bg-rose-600 text-white text-[10px] font-black uppercase px-2.5 py-1 rounded-full shadow-md">
                            Combo -${comboDiscount}%
                        </span>` : ''}
                </div>

                <div class="p-5 flex-1 flex flex-col justify-between">
                    <div>
                        <div class="flex items-center gap-1 text-amber-500 text-xs mb-1">
                            <i class="fa-solid fa-star"></i>
                            <span class="font-bold text-slate-700">${prod.rating || 5.0}</span>
                            <span class="text-slate-600 text-[10px]">(${prod.reviewsCount || 48} đánh giá)</span>
                        </div>
                        <h3 class="font-extrabold text-slate-900 text-sm group-hover:text-emerald-700 transition line-clamp-2 mb-1.5">
                            ${prod.name}
                        </h3>
                        <p class="text-xs text-slate-500 line-clamp-2 leading-relaxed mb-4">
                            ${prod.description || ''}
                        </p>
                    </div>

                    <div class="pt-3 border-t border-slate-100 flex items-center justify-between">
                        <div>
                            <span class="text-[10px] text-slate-600 font-medium">Giá tiêu chuẩn</span>
                            <div class="text-base font-black text-emerald-700">
                                ${formatVND(prod.basePrice)}
                            </div>
                        </div>
                        <button type="button" onclick="openProductPackagingModal(${prod.id})" class="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-md transition flex items-center gap-1.5 cursor-pointer">
                            <i class="fa-solid fa-cart-plus"></i>
                            <span>Chọn Mua</span>
                        </button>
                    </div>
                </div>
            </div>`;
    }).join('');
}

function filterProducts(category, btnElement) {
    document.querySelectorAll('#product-shop-tabs .prod-tab-btn').forEach(btn => {
        btn.classList.remove('bg-emerald-600', 'text-white', 'shadow-sm');
        btn.classList.add('bg-white', 'text-slate-700');
    });
    if (btnElement) {
        btnElement.classList.remove('bg-white', 'text-slate-700');
        btnElement.classList.add('bg-emerald-600', 'text-white', 'shadow-sm');
    } else {
        const targetBtn = Array.from(document.querySelectorAll('#product-shop-tabs .prod-tab-btn'))
            .find(b => b.getAttribute('onclick') && b.getAttribute('onclick').includes(category));
        if (targetBtn) {
            targetBtn.classList.remove('bg-white', 'text-slate-700');
            targetBtn.classList.add('bg-emerald-600', 'text-white', 'shadow-sm');
        }
    }
    loadDentalProducts(category);
}

// Packaging Options Modal
function openProductPackagingModal(productId) {
    const product = cachedProducts.find(p => p.id === productId);
    if (!product) return;

    currentProductForModal = product;
    modalPackagingQty = 1;
    document.getElementById('pack-modal-brand').innerText = product.brand || 'DentalCare';
    document.getElementById('pack-modal-title').innerText = product.name;
    document.getElementById('pack-modal-desc').innerText = product.description || '';
    document.getElementById('pack-modal-qty').innerText = '1';

    const options = product.packagingOptions && product.packagingOptions.length > 0 
        ? product.packagingOptions 
        : [{ id: 0, packagingType: 'SINGLE_BOX', unitName: 'Hộp Đơn 1 Sản Phẩm', quantityPerUnit: 1, unitPrice: product.basePrice, discountPercent: 0, bonusGifts: 'Miễn phí vận chuyển' }];

    selectedPackagingOption = options[0];

    const list = document.getElementById('pack-options-list');
    list.innerHTML = options.map((opt, idx) => {
        const isSelected = idx === 0;
        return `
            <div onclick="selectPackagingOption(${opt.id}, ${opt.unitPrice})" id="pack-opt-item-${opt.id}" class="pack-opt-card p-3 rounded-2xl border-2 cursor-pointer transition flex items-center justify-between ${isSelected ? 'border-emerald-600 bg-emerald-50/50' : 'border-slate-200 bg-white hover:border-slate-300'}">
                <div class="flex items-center gap-3">
                    <input type="radio" name="packaging_radio" ${isSelected ? 'checked' : ''} class="text-emerald-600 focus:ring-emerald-500">
                    <div>
                        <div class="font-black text-xs text-slate-900 flex items-center gap-2">
                            <span>${opt.unitName}</span>
                            ${opt.discountPercent > 0 ? `<span class="bg-rose-100 text-rose-700 text-[10px] px-2 py-0.5 rounded-full font-black uppercase">-${opt.discountPercent}% Tiết Kiệm</span>` : ''}
                        </div>
                        <div class="text-[11px] text-slate-500 mt-0.5">
                            ${opt.bonusGifts ? `<i class="fa-solid fa-gift text-amber-500 mr-1"></i>Tặng kèm: <b>${opt.bonusGifts}</b>` : 'Quy cách chuẩn nha khoa'}
                        </div>
                    </div>
                </div>
                <div class="text-right">
                    <div class="text-xs font-black text-emerald-700">${formatVND(opt.unitPrice)}</div>
                    <div class="text-[10px] text-slate-600">Đơn vị: ${opt.quantityPerUnit || 1} cái</div>
                </div>
            </div>`;
    }).join('');

    updateModalTotal();
    document.getElementById('product-packaging-modal').classList.remove('hidden');
}

function selectPackagingOption(optionId, price) {
    if (!currentProductForModal) return;
    const opt = (currentProductForModal.packagingOptions || []).find(o => o.id === optionId);
    if (opt) {
        selectedPackagingOption = opt;
    }
    document.querySelectorAll('.pack-opt-card').forEach(card => {
        card.classList.remove('border-emerald-600', 'bg-emerald-50/50');
        card.classList.add('border-slate-200', 'bg-white');
        const radio = card.querySelector('input[type="radio"]');
        if (radio) radio.checked = false;
    });

    const activeCard = document.getElementById(`pack-opt-item-${optionId}`);
    if (activeCard) {
        activeCard.classList.remove('border-slate-200', 'bg-white');
        activeCard.classList.add('border-emerald-600', 'bg-emerald-50/50');
        const radio = activeCard.querySelector('input[type="radio"]');
        if (radio) radio.checked = true;
    }
    updateModalTotal();
}

function adjustModalQty(delta) {
    modalPackagingQty = Math.max(1, modalPackagingQty + delta);
    document.getElementById('pack-modal-qty').innerText = modalPackagingQty;
    updateModalTotal();
}

function updateModalTotal() {
    const unitPrice = selectedPackagingOption ? selectedPackagingOption.unitPrice : (currentProductForModal ? currentProductForModal.basePrice : 0);
    const total = unitPrice * modalPackagingQty;
    document.getElementById('pack-modal-total').innerText = formatVND(total);
}

function closeProductPackagingModal() {
    document.getElementById('product-packaging-modal').classList.add('hidden');
    currentProductForModal = null;
    selectedPackagingOption = null;
}

function confirmAddToCart() {
    if (!currentProductForModal) return;

    const opt = selectedPackagingOption || {
        id: 0,
        packagingType: 'SINGLE_BOX',
        unitName: 'Hộp Đơn Tiêu Chuẩn',
        unitPrice: currentProductForModal.basePrice
    };

    const existingIndex = cartItems.findIndex(item => 
        item.productId === currentProductForModal.id && item.packagingOptionId === opt.id
    );

    if (existingIndex > -1) {
        cartItems[existingIndex].quantity += modalPackagingQty;
    } else {
        cartItems.push({
            productId: currentProductForModal.id,
            productName: currentProductForModal.name,
            productBrand: currentProductForModal.brand,
            productImageUrl: currentProductForModal.imageUrl,
            packagingOptionId: opt.id,
            packagingType: opt.packagingType,
            unitName: opt.unitName,
            price: opt.unitPrice,
            quantity: modalPackagingQty
        });
    }

    localStorage.setItem('DENTAL_CART', JSON.stringify(cartItems));
    updateCartBadge();
    closeProductPackagingModal();
    showToast(`✓ Đã thêm ${modalPackagingQty} "${opt.unitName}" vào giỏ hàng!`);
}

// Cart Drawer Modal
function openCartModal() {
    renderCart();
    document.getElementById('cart-drawer-modal').classList.remove('hidden');
}

function closeCartModal() {
    document.getElementById('cart-drawer-modal').classList.add('hidden');
}

function renderCart() {
    const container = document.getElementById('cart-items-container');
    const itemsCountText = document.getElementById('cart-items-count-text');
    const subtotalText = document.getElementById('cart-subtotal-price');
    const totalText = document.getElementById('cart-total-price');
    const loyaltyPtsText = document.getElementById('cart-loyalty-pts');

    if (currentUser) {
        const nameInput = document.getElementById('order-customer-name');
        const phoneInput = document.getElementById('order-customer-phone');
        if (nameInput && !nameInput.value) nameInput.value = currentUser.fullName || currentUser.username;
        if (phoneInput && !phoneInput.value) phoneInput.value = currentUser.phone || '';
    }

    const totalQty = cartItems.reduce((sum, item) => sum + (item.quantity || 1), 0);
    const subtotal = cartItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const loyaltyPoints = Math.floor(subtotal / 10000);

    if (itemsCountText) itemsCountText.innerText = `${totalQty} sản phẩm`;
    if (subtotalText) subtotalText.innerText = formatVND(subtotal);
    if (totalText) totalText.innerText = formatVND(subtotal);
    if (loyaltyPtsText) loyaltyPtsText.innerText = `+${loyaltyPoints.toLocaleString('vi-VN')}`;

    if (!container) return;

    if (cartItems.length === 0) {
        container.innerHTML = `
            <div class="text-center py-16 text-slate-400 space-y-3">
                <i class="fa-solid fa-cart-shopping text-4xl text-slate-300"></i>
                <div class="text-xs font-bold text-slate-600">Giỏ hàng của bạn đang trống</div>
                <p class="text-[11px] text-slate-400">Hãy dạo xem các sản phẩm chăm sóc răng miệng chính hãng nhé!</p>
            </div>`;
        return;
    }

    container.innerHTML = cartItems.map((item, idx) => `
        <div class="pt-3 pb-3 flex items-center justify-between gap-3">
            <div class="flex items-center gap-3">
                <img src="${item.productImageUrl || 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=100'}" class="w-12 h-12 rounded-xl object-cover border border-slate-200">
                <div>
                    <h4 class="font-extrabold text-xs text-slate-900 line-clamp-1">${item.productName}</h4>
                    <span class="text-[10px] text-emerald-600 font-bold bg-emerald-50 px-2 py-0.5 rounded">${item.unitName}</span>
                    <div class="text-xs font-black text-slate-800 mt-1">${formatVND(item.price)}</div>
                </div>
            </div>
            <div class="flex items-center gap-2">
                <div class="flex items-center border border-slate-200 rounded-lg">
                    <button type="button" onclick="updateCartItemQty(${idx}, -1)" class="w-6 h-6 text-xs text-slate-600 hover:bg-slate-100 rounded-l font-bold">-</button>
                    <span class="w-6 text-center text-xs font-bold text-slate-900">${item.quantity}</span>
                    <button type="button" onclick="updateCartItemQty(${idx}, 1)" class="w-6 h-6 text-xs text-slate-600 hover:bg-slate-100 rounded-r font-bold">+</button>
                </div>
                <button type="button" onclick="removeCartItem(${idx})" class="text-slate-400 hover:text-rose-600 p-1 transition" title="Xóa khỏi giỏ">
                    <i class="fa-solid fa-trash-can text-xs"></i>
                </button>
            </div>
        </div>
    `).join('');
}

function updateCartItemQty(index, delta) {
    if (!cartItems[index]) return;
    cartItems[index].quantity += delta;
    if (cartItems[index].quantity <= 0) {
        cartItems.splice(index, 1);
    }
    localStorage.setItem('DENTAL_CART', JSON.stringify(cartItems));
    updateCartBadge();
    renderCart();
}

function removeCartItem(index) {
    cartItems.splice(index, 1);
    localStorage.setItem('DENTAL_CART', JSON.stringify(cartItems));
    updateCartBadge();
    renderCart();
}

async function submitDentalOrder() {
    if (cartItems.length === 0) {
        showToast('⚠️ Giỏ hàng của bạn đang trống!');
        return;
    }

    const customerName = document.getElementById('order-customer-name').value.trim();
    const customerPhone = document.getElementById('order-customer-phone').value.trim();
    const shippingAddress = document.getElementById('order-customer-address').value.trim();
    const paymentMethod = document.getElementById('order-payment-method').value;

    if (!customerName || !customerPhone || !shippingAddress) {
        showToast('⚠️ Vui lòng điền đầy đủ Tên, Số điện thoại và Địa chỉ giao hàng!');
        return;
    }

    const payload = {
        customerName: customerName,
        customerPhone: customerPhone,
        shippingAddress: shippingAddress,
        paymentMethod: paymentMethod,
        items: cartItems.map(item => ({
            productId: item.productId,
            packagingOptionId: item.packagingOptionId > 0 ? item.packagingOptionId : null,
            quantity: item.quantity
        }))
    };

    const submitBtn = document.getElementById('btn-submit-order');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fa-solid fa-spinner animate-spin"></i> Đang tạo đơn hàng...';
    }

    try {
        const res = await apiFetch('/api/dental-orders', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.ok && res.data.success) {
            const order = res.data.data;
            cartItems = [];
            localStorage.removeItem('DENTAL_CART');
            updateCartBadge();
            closeCartModal();

            showToast(`🎉 Đặt hàng thành công! Mã đơn: ${order.orderCode}. Tích lũy +${order.loyaltyPointsEarned || 0} điểm thưởng!`);
        } else {
            showToast('⚠️ ' + (res.data?.message || 'Không thể khởi tạo đơn hàng. Vui lòng thử lại!'));
        }
    } catch (err) {
        console.error('Submit order error:', err);
        showToast('⚠️ Có lỗi xảy ra trong quá trình đặt hàng!');
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="fa-solid fa-bag-shopping"></i> Đặt Hàng Ngay (1-Click)';
        }
    }
}

// -------------------------------------------------------------------------
// 3. PORCELAIN CROWN WARRANTY & QR VERIFICATION
// -------------------------------------------------------------------------
async function searchWarranty() {
    const input = document.getElementById('warranty-search-input');
    const query = input ? input.value.trim() : '';
    if (!query) {
        showToast('⚠️ Vui lòng nhập Mã thẻ bảo hành hoặc Mã QR!');
        return;
    }

    const resultCard = document.getElementById('warranty-result-card');
    if (!resultCard) return;

    try {
        resultCard.classList.remove('hidden');
        resultCard.innerHTML = `
            <div class="text-center py-6 text-slate-400">
                <i class="fa-solid fa-spinner animate-spin text-2xl text-sky-400 mb-2"></i>
                <div class="text-xs">Đang truy vấn cơ sở dữ liệu phôi sứ chính hãng...</div>
            </div>`;

        const res = await apiFetch(`/api/warranties/lookup?query=${encodeURIComponent(query)}`);
        if (res.ok && res.data.success) {
            const wr = res.data.data;
            const statusBg = wr.status === 'ACTIVE' 
                ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40' 
                : 'bg-rose-500/20 text-rose-300 border-rose-500/40';
            const statusLabel = wr.status === 'ACTIVE' ? 'Đang Hiệu Lực' : 'Hết Hạn / Tạm Dừng';

            resultCard.innerHTML = `
                <div class="p-6 rounded-2xl bg-slate-800/90 border border-sky-500/40 shadow-xl space-y-4">
                    <div class="flex items-center justify-between border-b border-slate-700/80 pb-3">
                        <div class="flex items-center gap-2.5">
                            <i class="fa-solid fa-certificate text-sky-400 text-xl"></i>
                            <div>
                                <span class="text-[10px] text-sky-300 font-extrabold uppercase tracking-widest">Thẻ Bảo Hành Điện Tử</span>
                                <h4 class="text-sm font-black text-white font-mono">${wr.serialCode}</h4>
                            </div>
                        </div>
                        <span class="text-xs font-black px-3 py-1 rounded-full border ${statusBg}">
                            ● ${statusLabel}
                        </span>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                        <div>
                            <span class="text-slate-400 text-[11px]">Chủ sở hữu:</span>
                            <div class="font-bold text-white">${wr.patientName} (${wr.patientPhone})</div>
                        </div>
                        <div>
                            <span class="text-slate-400 text-[11px]">Dòng phôi sứ chính hãng:</span>
                            <div class="font-bold text-amber-300">${wr.crownType} - Màu ${wr.shadeCode || 'A1'}</div>
                        </div>
                        <div>
                            <span class="text-slate-400 text-[11px]">Vị trí răng phục hình:</span>
                            <div class="font-bold text-sky-300">Răng số: ${wr.teethNumbers}</div>
                        </div>
                        <div>
                            <span class="text-slate-400 text-[11px]">Labo chế tác kỹ thuật số:</span>
                            <div class="font-bold text-white">${wr.laboPartner}</div>
                        </div>
                        <div>
                            <span class="text-slate-400 text-[11px]">Ngày phục hình:</span>
                            <div class="font-medium text-slate-300">${wr.issueDate || '2026-01-15'}</div>
                        </div>
                        <div>
                            <span class="text-slate-400 text-[11px]">Thời hạn bảo hành:</span>
                            <div class="font-bold text-emerald-400">${wr.warrantyYears} Năm (Đến ${wr.expiryDate || '2041-01-15'})</div>
                        </div>
                    </div>

                    <div class="pt-3 border-t border-slate-700/80 flex items-center justify-between text-[11px] text-slate-400">
                        <span class="font-mono text-[10px]">QR Code: ${wr.qrVerificationCode}</span>
                        <span class="text-emerald-400 font-bold"><i class="fa-solid fa-circle-check"></i> Xác thực nguồn gốc 100%</span>
                    </div>
                </div>`;
        } else {
            resultCard.innerHTML = `
                <div class="p-4 rounded-xl bg-rose-950/40 border border-rose-800 text-rose-300 text-xs text-center space-y-1">
                    <i class="fa-solid fa-triangle-exclamation text-rose-400 text-lg"></i>
                    <p class="font-bold">Không tìm thấy thông tin thẻ bảo hành với từ khóa "${query}"</p>
                    <p class="text-[11px] text-rose-400">Vui lòng kiểm tra lại mã số in trên thẻ cứng hoặc liên hệ hotline phòng khám để được hỗ trợ.</p>
                </div>`;
        }
    } catch (err) {
        console.error('Warranty search error:', err);
        resultCard.innerHTML = `
            <div class="p-4 rounded-xl bg-rose-950/40 border border-rose-800 text-rose-300 text-xs text-center">
                Không thể kết nối đến máy chủ bảo hành. Vui lòng thử lại sau!
            </div>`;
    }
}

function quickSearchWarranty(code) {
    const input = document.getElementById('warranty-search-input');
    if (input) {
        input.value = code;
        searchWarranty();
    }
}

// -------------------------------------------------------------------------
// 4. AI DENTAL DIAGNOSTIC VISION (9ROUTER GATEWAY & CLINICAL RULES)
// -------------------------------------------------------------------------
function selectAiSample(symptomText, pathologyKey) {
    const input = document.getElementById('ai-symptoms-input');
    if (input) {
        input.value = symptomText;
        input.focus();
    }
}

async function runAiDiagnostic() {
    const symptoms = (document.getElementById('ai-symptoms-input')?.value || '').trim();
    const patientName = (document.getElementById('ai-patient-name')?.value || '').trim();
    const patientPhone = (document.getElementById('ai-patient-phone')?.value || '').trim();

    if (!symptoms) {
        showToast('⚠️ Vui lòng mô tả triệu chứng hoặc chọn mẫu bệnh lý bên dưới!');
        return;
    }

    const placeholder = document.getElementById('ai-diag-placeholder');
    const loading = document.getElementById('ai-diag-loading');
    const result = document.getElementById('ai-diag-result');

    if (placeholder) placeholder.classList.add('hidden');
    if (result) result.classList.add('hidden');
    if (loading) loading.classList.remove('hidden');

    try {
        const payload = {
            patientName: patientName || 'Khách Hàng Trực Tuyến',
            patientPhone: patientPhone || '',
            symptomsDescription: symptoms,
            modelUsed: '9router/gemini-2.5-flash'
        };

        const res = await apiFetch('/api/dental-ai/diagnose', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.ok && res.data.success) {
            const diag = res.data.data;
            const riskColors = {
                'LOW': 'bg-emerald-100 text-emerald-800 border-emerald-300',
                'MEDIUM': 'bg-amber-100 text-amber-800 border-amber-300',
                'HIGH': 'bg-rose-100 text-rose-800 border-rose-300',
                'URGENT': 'bg-red-200 text-red-900 border-red-400 animate-pulse'
            };

            const riskBadge = riskColors[diag.riskLevel] || 'bg-slate-100 text-slate-800';

            result.innerHTML = `
                <div class="space-y-4">
                    <div class="flex items-center justify-between border-b border-slate-200 pb-3">
                        <div class="flex items-center gap-2.5">
                            <div class="w-10 h-10 rounded-xl bg-purple-600 text-white flex items-center justify-center font-bold">
                                <i class="fa-solid fa-stethoscope"></i>
                            </div>
                            <div>
                                <span class="text-[10px] text-purple-700 font-black uppercase">Kết Quả Chẩn Đoán AI</span>
                                <h3 class="text-sm font-black text-slate-900">${diag.pathologyName}</h3>
                            </div>
                        </div>
                        <span class="text-[11px] font-black uppercase px-3 py-1 rounded-full border ${riskBadge}">
                            ${diag.riskLevel}
                        </span>
                    </div>

                    <div class="bg-white p-4 rounded-2xl border border-slate-200 space-y-2 text-xs">
                        <div class="flex items-center justify-between text-slate-600">
                            <span>Độ chuẩn xác lâm sàng:</span>
                            <span class="font-black text-purple-700">${Math.round(diag.confidenceScore * 100)}%</span>
                        </div>
                        <div class="w-full bg-slate-100 rounded-full h-2">
                            <div class="bg-gradient-to-r from-purple-500 to-indigo-600 h-2 rounded-full" style="width: ${Math.round(diag.confidenceScore * 100)}%"></div>
                        </div>
                        <div class="pt-2 text-slate-700 font-medium leading-relaxed">
                            <b>Phát hiện lâm sàng:</b> ${diag.clinicalFindings}
                        </div>
                    </div>

                    <div class="bg-purple-50/70 p-4 rounded-2xl border border-purple-200/80 space-y-2 text-xs">
                        <div class="font-bold text-purple-900 flex items-center gap-1.5">
                            <i class="fa-solid fa-user-doctor text-purple-700"></i> Phác đồ & Lời khuyên điều trị:
                        </div>
                        <p class="text-purple-950 font-medium leading-relaxed">${diag.treatmentAdvice}</p>
                        <div class="flex items-center justify-between pt-2 border-t border-purple-200 text-[11px]">
                            <span class="text-purple-800">Chi phí dự kiến:</span>
                            <span class="font-black text-purple-900">${diag.estimatedCostRange || 'Liên hệ để nhận báo giá'}</span>
                        </div>
                    </div>

                    <div class="flex gap-2">
                        <button type="button" onclick="selectServiceForBooking('${diag.pathologyName.replace(/'/g, "\\'")}')" class="flex-1 py-3 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-700 hover:to-indigo-700 text-white rounded-xl text-xs font-black shadow-md transition flex items-center justify-center gap-1.5 cursor-pointer">
                            <i class="fa-solid fa-calendar-check"></i> Đặt Lịch Khám Ưu Tiên
                        </button>
                    </div>
                </div>`;
            result.classList.remove('hidden');
        } else {
            showToast('⚠️ Không thể phân tích bệnh lý. Vui lòng thử lại!');
            if (placeholder) placeholder.classList.remove('hidden');
        }
    } catch (err) {
        console.error('AI diagnostic error:', err);
        showToast('⚠️ Có lỗi xảy ra khi kết nối máy chủ AI!');
        if (placeholder) placeholder.classList.remove('hidden');
    } finally {
        if (loading) loading.classList.add('hidden');
    }
}

// -------------------------------------------------------------------------
// 5. MULTI-BRANCH DIRECTORY WITH GPS DISTANCE CALCULATION
// -------------------------------------------------------------------------
async function loadBranches() {
    const grid = document.getElementById('branches-grid');
    if (!grid) return;

    try {
        const res = await apiFetch('/api/branches');
        if (res.ok && res.data.success) {
            cachedBranches = res.data.data || [];
            renderBranches(cachedBranches);
        }
    } catch (err) {
        console.error('Error loading branches:', err);
    }
}

function renderBranches(branches, nearestId = null, distanceKm = null) {
    const grid = document.getElementById('branches-grid');
    if (!grid) return;

    if (!branches || branches.length === 0) {
        grid.innerHTML = `<div class="col-span-full text-center py-8 text-slate-400 text-xs">Đang cập nhật danh sách cơ sở...</div>`;
        return;
    }

    grid.innerHTML = branches.map(b => {
        const isNearest = nearestId && b.id === nearestId;
        const facilitiesList = (b.facilities || '').split(',').map(f => f.trim()).filter(Boolean);

        return `
            <div class="card-3d-tilt rounded-3xl p-6 transition-all duration-300 flex flex-col justify-between ${isNearest ? 'bg-gradient-to-b from-emerald-950/80 to-slate-900 border-2 border-emerald-400 shadow-2xl shadow-emerald-500/20' : 'bg-slate-800/80 border border-slate-700/80 hover:border-teal-500'}">
                <div>
                    <div class="flex items-center justify-between mb-3">
                        <span class="text-[10px] font-black uppercase tracking-wider px-2.5 py-0.5 rounded-full ${isNearest ? 'bg-emerald-500 text-slate-950 font-extrabold' : 'bg-slate-700 text-teal-300'}">
                            ${isNearest ? `📍 GẦN BẠN NHẤT (${distanceKm} km)` : b.city}
                        </span>
                        <span class="text-xs font-mono font-bold text-slate-400">${b.branchCode}</span>
                    </div>

                    <h3 class="text-sm font-black text-white mb-2 leading-snug">${b.branchName}</h3>
                    <p class="text-xs text-slate-300 mb-3 flex items-start gap-1.5">
                        <i class="fa-solid fa-location-dot text-teal-400 shrink-0 mt-0.5"></i>
                        <span>${b.address}</span>
                    </p>

                    <div class="space-y-1.5 text-xs text-slate-400 mb-4">
                        <div class="flex items-center gap-1.5">
                            <i class="fa-solid fa-phone text-amber-400 text-[11px]"></i>
                            <a href="tel:${b.phone}" class="hover:text-white font-bold">${b.phone}</a>
                        </div>
                        <div class="flex items-center gap-1.5">
                            <i class="fa-solid fa-clock text-sky-400 text-[11px]"></i>
                            <span>${b.openingHours || '08:00 - 20:00 (Cả T7 & CN)'}</span>
                        </div>
                    </div>

                    <div class="flex flex-wrap gap-1 mb-4">
                        ${facilitiesList.map(f => `
                            <span class="text-[9px] bg-slate-700/60 text-teal-200 px-2 py-0.5 rounded-md border border-slate-600/50">
                                ${f}
                            </span>
                        `).join('')}
                    </div>
                </div>

                <div class="pt-3 border-t border-slate-700/80 flex items-center justify-between">
                    <a href="https://www.google.com/maps?q=${b.latitude},${b.longitude}" target="_blank" rel="noopener noreferrer" class="text-xs font-bold text-teal-300 hover:text-teal-200 flex items-center gap-1">
                        <span>Chỉ Đường</span>
                        <i class="fa-solid fa-arrow-up-right-from-square text-[10px]"></i>
                    </a>
                    <a href="#booking-section" onclick="showLandingPage()" class="px-3 py-1.5 bg-teal-600 hover:bg-teal-500 text-white text-xs font-bold rounded-xl transition">
                        Đặt Lịch Tại Đây
                    </a>
                </div>
            </div>`;
    }).join('');
}

function locateNearestBranch() {
    if (!navigator.geolocation) {
        showToast('⚠️ Trình duyệt của bạn không hỗ trợ định vị GPS.');
        queryNearestBranch(10.760624, 106.587106); // Fallback: HCM Central
        return;
    }

    showToast('📍 Đang xác định vị trí của bạn...');

    navigator.geolocation.getCurrentPosition(
        async (position) => {
            const lat = position.coords.latitude;
            const lon = position.coords.longitude;
            await queryNearestBranch(lat, lon);
        },
        async (err) => {
            console.warn('Geolocation failed, fallback to Ho Chi Minh City coordinates:', err);
            showToast('⚠️ Không thể lấy GPS, áp dụng tọa độ TP. Hồ Chí Minh mặc định.');
            await queryNearestBranch(10.760624, 106.587106);
        },
        { timeout: 8000 }
    );
}

async function queryNearestBranch(lat, lon) {
    try {
        const res = await apiFetch(`/api/branches/nearest?latitude=${lat}&longitude=${lon}`);
        if (res.ok && res.data.success) {
            const data = res.data.data;
            const nearestBranch = data.branch;
            const distanceKm = data.distanceKm;

            showToast(`✓ Cơ sở gần bạn nhất: ${nearestBranch.branchName} (Cách ${distanceKm} km)!`);
            renderBranches(cachedBranches, nearestBranch.id, distanceKm);

            const grid = document.getElementById('branches-grid');
            if (grid) grid.scrollIntoView({ behavior: 'smooth' });
        }
    } catch (err) {
        console.error('Nearest branch query error:', err);
    }
}

// -------------------------------------------------------------------------
// 6. DENTAL COMMUNITY FORUM (POSTS, LIKES, INTERACTIONS)
// -------------------------------------------------------------------------
async function loadForumPosts(category = 'ALL') {
    const container = document.getElementById('forum-posts-container');
    if (!container) return;

    try {
        let url = '/api/forum/posts';
        if (category && category !== 'ALL') {
            url += `?category=${encodeURIComponent(category)}`;
        }
        const res = await apiFetch(url);
        if (res.ok && res.data.success) {
            cachedForumPosts = res.data.data || [];
            renderForumPosts(cachedForumPosts);
        }
    } catch (err) {
        console.error('Error loading forum posts:', err);
    }
}

function renderForumPosts(posts) {
    const container = document.getElementById('forum-posts-container');
    if (!container) return;

    if (!posts || posts.length === 0) {
        container.innerHTML = `
            <div class="col-span-full text-center py-12 text-slate-400 text-xs">
                <i class="fa-solid fa-comments text-3xl mb-2 text-slate-300"></i>
                <p>Chưa có bài viết thảo luận trong chuyên mục này.</p>
            </div>`;
        return;
    }

    const categoryLabels = {
        'EXPERIENCE': { name: 'Kinh Nghiệm Điều Trị', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
        'RECOVERY_TIPS': { name: 'Mẹo Hồi Phục', color: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
        'DENTAL_QA': { name: 'Hỏi Đáp Bác Sĩ', color: 'bg-amber-50 text-amber-700 border-amber-200' }
    };

    container.innerHTML = posts.map(post => {
        const cat = categoryLabels[post.category] || { name: 'Thảo Luận', color: 'bg-slate-50 text-slate-700 border-slate-200' };

        return `
            <div class="bg-white rounded-3xl p-6 border border-slate-200/90 hover:border-amber-500 hover:shadow-xl transition-all duration-300 flex flex-col justify-between">
                <div>
                    <div class="flex items-center justify-between mb-3">
                        <span class="text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full border ${cat.color}">
                            ${cat.name}
                        </span>
                        <span class="text-[11px] text-slate-600 font-semibold"><i class="fa-regular fa-clock"></i> 2 giờ trước</span>
                    </div>

                    <h3 class="text-sm font-black text-slate-900 mb-2 hover:text-amber-700 transition">
                        ${post.title}
                    </h3>
                    <p class="text-xs text-slate-600 leading-relaxed line-clamp-4 mb-4">
                        ${post.content}
                    </p>
                </div>

                <div class="pt-4 border-t border-slate-100 flex items-center justify-between">
                    <div class="flex items-center gap-2">
                        <div class="w-7 h-7 rounded-full bg-slate-900 text-white flex items-center justify-center text-xs font-black">
                            ${(post.authorName || 'B')[0]}
                        </div>
                        <div>
                            <div class="text-xs font-bold text-slate-900">${post.authorName}</div>
                            <div class="text-[10px] text-slate-600 font-medium">Bệnh nhân DentalCare</div>
                        </div>
                    </div>

                    <button type="button" onclick="likeForumPost(${post.id}, this)" class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-rose-50 text-slate-700 hover:text-rose-600 text-xs font-bold transition cursor-pointer">
                        <i class="fa-regular fa-heart text-rose-500"></i>
                        <span class="post-like-count">${post.likesCount || 0}</span>
                    </button>
                </div>
            </div>`;
    }).join('');
}

function filterForumPosts(category, btnElement) {
    document.querySelectorAll('#forum-tabs .forum-tab-btn').forEach(btn => {
        btn.classList.remove('bg-amber-600', 'text-white', 'shadow-sm');
        btn.classList.add('bg-white', 'text-slate-700');
    });
    if (btnElement) {
        btnElement.classList.remove('bg-white', 'text-slate-700');
        btnElement.classList.add('bg-amber-600', 'text-white', 'shadow-sm');
    } else {
        const targetBtn = Array.from(document.querySelectorAll('#forum-tabs .forum-tab-btn'))
            .find(b => b.getAttribute('onclick') && b.getAttribute('onclick').includes(category));
        if (targetBtn) {
            targetBtn.classList.remove('bg-white', 'text-slate-700');
            targetBtn.classList.add('bg-amber-600', 'text-white', 'shadow-sm');
        }
    }
    loadForumPosts(category);
}

function openNewForumPostModal() {
    if (currentUser) {
        const authorInput = document.getElementById('forum-new-author');
        const phoneInput = document.getElementById('forum-new-phone');
        if (authorInput && !authorInput.value) authorInput.value = currentUser.fullName || currentUser.username;
        if (phoneInput && !phoneInput.value) phoneInput.value = currentUser.phone || '';
    }
    document.getElementById('forum-post-modal').classList.remove('hidden');
}

function closeForumPostModal() {
    document.getElementById('forum-post-modal').classList.add('hidden');
}

async function submitForumPost() {
    const author = (document.getElementById('forum-new-author')?.value || '').trim();
    const phone = (document.getElementById('forum-new-phone')?.value || '').trim();
    const category = document.getElementById('forum-new-category')?.value || 'EXPERIENCE';
    const title = (document.getElementById('forum-new-title')?.value || '').trim();
    const content = (document.getElementById('forum-new-content')?.value || '').trim();

    if (!author || !title || !content) {
        showToast('⚠️ Vui lòng điền Họ tên, Tiêu đề và Nội dung bài viết!');
        return;
    }

    try {
        const payload = {
            authorName: author,
            authorPhone: phone,
            category: category,
            title: title,
            content: content
        };

        const res = await apiFetch('/api/forum/posts', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.ok && res.data.success) {
            showToast('✓ Đã đăng bài viết thành công lên Diễn đàn cộng đồng!');
            closeForumPostModal();
            loadForumPosts(category);
        } else {
            showToast('⚠️ ' + (res.data?.message || 'Không thể đăng bài viết!'));
        }
    } catch (err) {
        console.error('Forum post error:', err);
        showToast('⚠️ Có lỗi xảy ra khi gửi bài viết!');
    }
}

async function likeForumPost(postId, btnElement) {
    try {
        const res = await apiFetch(`/api/forum/posts/${postId}/like`, { method: 'POST' });
        if (res.ok && res.data.success) {
            const updated = res.data.data;
            if (btnElement) {
                const countSpan = btnElement.querySelector('.post-like-count');
                if (countSpan) countSpan.innerText = updated.likesCount;
                btnElement.classList.add('text-rose-600', 'bg-rose-50');
                const heartIcon = btnElement.querySelector('i');
                if (heartIcon) {
                    heartIcon.classList.remove('fa-regular');
                    heartIcon.classList.add('fa-solid');
                }
            }
        }
    } catch (err) {
        console.error('Like post error:', err);
    }
}

// ================= MILESTONE 2: STAFF & B2B OPERATIONS =================

// 1. KHO VẬT TƯ & KHÍ CỤ NHA KHOA
async function loadInventoryMaterials() {
    const tbody = document.getElementById('inventory-table-body');
    if (!tbody) return;
    try {
        const catFilter = document.getElementById('filter-inventory-category')?.value || '';
        let url = '/api/dental-materials';
        if (catFilter) url += `?category=${encodeURIComponent(catFilter)}`;
        
        const res = await apiFetch(url);
        if (res.ok && res.data && res.data.data) {
            const list = res.data.data;
            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="8" class="text-center py-6 text-slate-400">Không có vật tư nào trong danh mục.</td></tr>`;
                return;
            }
            tbody.innerHTML = list.map(m => {
                const isLow = m.stockQuantity <= (m.minSafetyStock || 0);
                const stockBadge = isLow
                    ? `<span class="bg-rose-950 text-rose-300 border border-rose-800 px-2 py-0.5 rounded font-black text-[11px] animate-pulse">Cảnh báo: ${m.stockQuantity}</span>`
                    : `<span class="text-emerald-400 font-bold">${m.stockQuantity}</span>`;
                const priceFormatted = (m.unitPrice || 0).toLocaleString('vi-VN') + ' đ';
                return `
                    <tr class="hover:bg-slate-700/40 transition">
                        <td class="py-3 px-4 font-mono font-bold text-amber-300">${m.materialCode || ('MAT-' + m.id)}</td>
                        <td class="py-3 px-4 font-bold text-white">${m.materialName || ''}</td>
                        <td class="py-3 px-4"><span class="bg-slate-900 text-slate-300 px-2 py-0.5 rounded text-[11px] font-bold border border-slate-700">${m.category || ''}</span></td>
                        <td class="py-3 px-4 text-slate-300">${m.manufacturer || '-'}</td>
                        <td class="py-3 px-4 text-center text-slate-300 font-bold">${m.unit || 'Cái'}</td>
                        <td class="py-3 px-4 text-right">${stockBadge}</td>
                        <td class="py-3 px-4 text-right font-mono text-slate-300">${priceFormatted}</td>
                        <td class="py-3 px-4 text-center">
                            <button onclick="handleQuickStockAdjust(${m.id}, ${m.stockQuantity})" class="px-2.5 py-1 bg-amber-600 hover:bg-amber-700 text-white rounded-lg text-xs font-bold transition">
                                <i class="fa-solid fa-arrows-up-down mr-1"></i> Điều Chỉnh
                            </button>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch (e) {
        console.error('loadInventoryMaterials error:', e);
        if (tbody) tbody.innerHTML = `<tr><td colspan="8" class="text-center py-6 text-rose-400">Lỗi khi tải kho vật tư.</td></tr>`;
    }
}

async function handleQuickStockAdjust(materialId, currentStock) {
    const input = prompt(`Điều chỉnh tồn kho (Hiện tại: ${currentStock}). Nhập số lượng thay đổi (dương để nhập kho, âm để xuất kho):`, "10");
    if (input === null) return;
    const delta = parseInt(input, 10);
    if (isNaN(delta) || delta === 0) {
        alert('Số lượng nhập không hợp lệ!');
        return;
    }
    const reason = prompt("Lý do điều chỉnh tồn kho:", delta > 0 ? "Nhập bổ sung từ nhà phân phối" : "Xuất tiêu hao phòng khám");
    if (!reason) return;

    try {
        const res = await apiFetch(`/api/dental-materials/${materialId}/stock`, {
            method: 'POST',
            body: JSON.stringify({ quantityChange: delta, reason: reason })
        });
        if (res.ok && res.data.success) {
            showToast(`✓ Đã điều chỉnh tồn kho thành công! Tồn kho mới: ${res.data.data.stockQuantity}`);
            loadInventoryMaterials();
        } else {
            alert(res.data?.message || 'Không thể điều chỉnh tồn kho!');
        }
    } catch (e) {
        console.error('Adjust stock error:', e);
    }
}

// 2. ĐƠN HÀNG VẬT TƯ B2B (ĐẠI LÝ CẤP 2)
async function loadB2BOrders() {
    const tbody = document.getElementById('b2b-orders-table-body');
    if (!tbody) return;
    try {
        const res = await apiFetch('/api/material-orders');
        if (res.ok && res.data && res.data.data) {
            const orders = res.data.data;
            if (orders.length === 0) {
                tbody.innerHTML = `<tr><td colspan="7" class="text-center py-6 text-slate-400">Chưa có đơn đặt hàng B2B nào.</td></tr>`;
                return;
            }
            tbody.innerHTML = orders.map(ord => {
                const totalFormatted = (ord.totalAmount || 0).toLocaleString('vi-VN');
                let statusBadge = `<span class="bg-amber-950 text-amber-300 border border-amber-800 px-2 py-0.5 rounded font-bold text-[11px]">${ord.status}</span>`;
                if (ord.status === 'APPROVED') statusBadge = `<span class="bg-emerald-950 text-emerald-300 border border-emerald-800 px-2 py-0.5 rounded font-bold text-[11px]">ĐÃ DUYỆT</span>`;
                if (ord.status === 'REJECTED') statusBadge = `<span class="bg-rose-950 text-rose-300 border border-rose-800 px-2 py-0.5 rounded font-bold text-[11px]">TỪ CHỐI</span>`;

                const agentName = ord.agent ? (ord.agent.agentName || ord.agent.name || ord.agent.code) : 'Đại lý #1';
                const dateStr = ord.orderDate ? new Date(ord.orderDate).toLocaleDateString('vi-VN') : '-';

                let actionBtns = '-';
                if (ord.status === 'PENDING') {
                    actionBtns = `
                        <div class="flex items-center justify-center gap-1.5">
                            <button onclick="handleApproveB2BOrder(${ord.id})" class="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold transition">
                                Duyệt
                            </button>
                            <button onclick="handleRejectB2BOrder(${ord.id})" class="px-2.5 py-1 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-bold transition">
                                Từ chối
                            </button>
                        </div>
                    `;
                }

                return `
                    <tr class="hover:bg-slate-700/40 transition">
                        <td class="py-3 px-4 font-mono font-bold text-sky-400">${ord.orderCode || ('ORD-' + ord.id)}</td>
                        <td class="py-3 px-4 font-bold text-white">${agentName}</td>
                        <td class="py-3 px-4 text-slate-300">${dateStr}</td>
                        <td class="py-3 px-4 text-right font-mono font-bold text-white">${totalFormatted} đ</td>
                        <td class="py-3 px-4 text-center">${statusBadge}</td>
                        <td class="py-3 px-4 text-slate-300 text-xs">${ord.notes || ord.rejectionReason || '-'}</td>
                        <td class="py-3 px-4 text-center">${actionBtns}</td>
                    </tr>
                `;
            }).join('');
        }
    } catch (e) {
        console.error('loadB2BOrders error:', e);
        if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="text-center py-6 text-rose-400">Lỗi khi tải đơn đặt hàng B2B.</td></tr>`;
    }
}

async function handleApproveB2BOrder(orderId) {
    if (!confirm('Xác nhận duyệt xuất kho cho đơn đặt hàng B2B này?')) return;
    try {
        const res = await apiFetch(`/api/material-orders/${orderId}/status`, {
            method: 'PUT',
            body: JSON.stringify({ status: 'APPROVED', notes: 'Quản trị viên phê duyệt xuất kho' })
        });
        if (res.ok && res.data.success) {
            showToast('✓ Phê duyệt đơn hàng thành công! Kho đã được trừ tự động.');
            loadB2BOrders();
            loadInventoryMaterials();
        } else {
            alert(res.data?.message || 'Lỗi khi phê duyệt đơn hàng!');
        }
    } catch (e) {
        console.error('Approve order error:', e);
    }
}

async function handleRejectB2BOrder(orderId) {
    const reason = prompt('Nhập lý do từ chối đơn hàng:', 'Vượt hạn mức tín dụng công nợ');
    if (!reason) return;
    try {
        const res = await apiFetch(`/api/material-orders/${orderId}/status`, {
            method: 'PUT',
            body: JSON.stringify({ status: 'REJECTED', notes: reason })
        });
        if (res.ok && res.data.success) {
            showToast('✓ Đã từ chối đơn đặt hàng.');
            loadB2BOrders();
        } else {
            alert(res.data?.message || 'Lỗi khi từ chối đơn hàng!');
        }
    } catch (e) {
        console.error('Reject order error:', e);
    }
}

// 3. CHẤM CÔNG NHÂN SỰ ĐỊNH VỊ GPS
async function loadAttendanceRecords() {
    const tbody = document.getElementById('attendance-table-body');
    if (!tbody) return;
    try {
        const res = await apiFetch('/api/attendance');
        if (res.ok && res.data && res.data.data) {
            const list = res.data.data;
            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" class="text-center py-6 text-slate-400">Chưa có nhật ký chấm công hôm nay.</td></tr>`;
                return;
            }
            tbody.innerHTML = list.map(a => {
                const staffName = a.staff ? (a.staff.fullName || a.staff.username) : 'Nhân sự';
                const shiftInfo = a.shift ? (a.shift.shiftType || 'Ca trực') : 'Ca sáng';
                const inTime = a.checkInTime ? new Date(a.checkInTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '-';
                const outTime = a.checkOutTime ? new Date(a.checkOutTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '<span class="text-slate-500">Đang trực</span>';
                
                let statusBadge = `<span class="bg-emerald-950 text-emerald-300 border border-emerald-800 px-2 py-0.5 rounded font-bold text-[11px]">ĐÚNG GIỜ</span>`;
                if (a.status === 'LATE') statusBadge = `<span class="bg-amber-950 text-amber-300 border border-amber-800 px-2 py-0.5 rounded font-bold text-[11px]">ĐI MUỘN</span>`;
                if (a.status === 'CHECKED_OUT') statusBadge = `<span class="bg-sky-950 text-sky-300 border border-sky-800 px-2 py-0.5 rounded font-bold text-[11px]">ĐÃ RA CA</span>`;

                const gpsBadge = a.gpsVerified 
                    ? `<span class="text-emerald-400 font-bold"><i class="fa-solid fa-circle-check"></i> Hợp lệ</span>` 
                    : `<span class="text-slate-500"><i class="fa-solid fa-circle-xmark"></i> Không</span>`;

                return `
                    <tr class="hover:bg-slate-700/40 transition">
                        <td class="py-2.5 px-3 font-bold text-white">${staffName}</td>
                        <td class="py-2.5 px-3 text-slate-300">${shiftInfo}</td>
                        <td class="py-2.5 px-3 font-mono text-emerald-400 font-bold">${inTime}</td>
                        <td class="py-2.5 px-3 font-mono text-slate-300">${outTime}</td>
                        <td class="py-2.5 px-3 text-center">${gpsBadge}</td>
                        <td class="py-2.5 px-3 text-center">${statusBadge}</td>
                    </tr>
                `;
            }).join('');
        }
    } catch (e) {
        console.error('loadAttendanceRecords error:', e);
        if (tbody) tbody.innerHTML = `<tr><td colspan="6" class="text-center py-6 text-rose-400">Lỗi khi tải nhật ký chấm công.</td></tr>`;
    }
}

async function handleQuickCheckIn(shiftId = 1) {
    try {
        const payload = {
            shiftId: shiftId,
            latitude: 10.760624,
            longitude: 106.587106,
            ipAddress: '192.168.1.15'
        };
        const res = await apiFetch('/api/attendance/check-in', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        if (res.ok && res.data.success) {
            showToast('✓ Chấm công vào ca thành công! Tọa độ GPS hợp lệ.');
            loadAttendanceRecords();
        } else {
            alert(res.data?.message || 'Chấm công thất bại!');
        }
    } catch (e) {
        console.error('Check-in error:', e);
    }
}

async function handleQuickCheckOut(shiftId = 1) {
    try {
        const payload = {
            shiftId: shiftId,
            notes: 'Kết thúc ca trực lâm sàng'
        };
        const res = await apiFetch('/api/attendance/check-out', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        if (res.ok && res.data.success) {
            showToast('✓ Chấm công ra ca thành công!');
            loadAttendanceRecords();
        } else {
            alert(res.data?.message || 'Chấm công ra ca thất bại!');
        }
    } catch (e) {
        console.error('Check-out error:', e);
    }
}

// 4. KPI & NĂNG SUẤT BÁC SĨ NHA KHOA
async function loadDoctorKpis() {
    const tbody = document.getElementById('doctor-kpi-table-body');
    const cards = document.getElementById('doctor-kpi-cards');
    if (!tbody) return;
    try {
        const res = await apiFetch('/api/staff-kpi/doctors');
        if (res.ok && res.data && res.data.data) {
            const list = res.data.data;
            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="8" class="text-center py-6 text-slate-400">Chưa có dữ liệu KPI bác sĩ.</td></tr>`;
                return;
            }

            if (cards) {
                cards.innerHTML = list.slice(0, 3).map((kpi, idx) => {
                    const docName = kpi.doctorName || ('Bác sĩ #' + (kpi.doctorId || (idx + 1)));
                    const rev = (kpi.totalRevenue || 0).toLocaleString('vi-VN');
                    const badgeColor = idx === 0 ? 'text-amber-400 border-amber-600 bg-amber-950/40' : 'text-sky-400 border-sky-600 bg-sky-950/40';
                    return `
                        <div class="p-4 rounded-2xl border ${badgeColor} space-y-2">
                            <div class="flex justify-between items-center">
                                <span class="font-extrabold text-sm text-white">${docName}</span>
                                <span class="text-xs font-black px-2 py-0.5 rounded bg-slate-900 border border-slate-700">Hạng #${idx + 1}</span>
                            </div>
                            <div class="text-2xl font-black text-white">${kpi.kpiScore || 0} <span class="text-xs text-slate-400 font-normal">điểm</span></div>
                            <div class="flex justify-between text-xs text-slate-300">
                                <span>Doanh số: <strong class="text-emerald-400">${rev} đ</strong></span>
                                <span>Chuyển đổi: <strong class="text-yellow-300">${kpi.conversionRate || 0}%</strong></span>
                            </div>
                        </div>
                    `;
                }).join('');
            }

            tbody.innerHTML = list.map(kpi => {
                const docName = kpi.doctorName || ('Bác sĩ #' + (kpi.doctorId || ''));
                const rev = (kpi.totalRevenue || 0).toLocaleString('vi-VN') + ' đ';
                return `
                    <tr class="hover:bg-slate-700/40 transition">
                        <td class="py-3 px-4 font-bold text-white">${docName}</td>
                        <td class="py-3 px-4 text-center font-bold text-slate-300">${kpi.totalConsultations || 0}</td>
                        <td class="py-3 px-4 text-center font-mono text-purple-400 font-bold">${kpi.orthoCases || 0}</td>
                        <td class="py-3 px-4 text-center font-mono text-teal-400 font-bold">${kpi.implantCases || 0}</td>
                        <td class="py-3 px-4 text-center font-mono text-slate-300">${kpi.generalCases || 0}</td>
                        <td class="py-3 px-4 text-right font-mono font-bold text-emerald-400">${rev}</td>
                        <td class="py-3 px-4 text-center font-bold text-yellow-400">${kpi.conversionRate || 0}%</td>
                        <td class="py-3 px-4 text-center">
                            <span class="bg-yellow-950 text-yellow-300 border border-yellow-800 px-2 py-0.5 rounded font-black text-xs">
                                ${kpi.kpiScore || 0}
                            </span>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch (e) {
        console.error('loadDoctorKpis error:', e);
        if (tbody) tbody.innerHTML = `<tr><td colspan="8" class="text-center py-6 text-rose-400">Lỗi khi tải bảng đánh giá KPI.</td></tr>`;
    }
}

// 5. TIẾP NHẬN BỆNH NHÂN HIỆN TRƯỜNG & HỌC ĐƯỜNG
async function loadFieldIntakeLeads() {
    const tbody = document.getElementById('field-intake-table-body');
    if (!tbody) return;
    try {
        const res = await apiFetch('/api/field-intake');
        if (res.ok && res.data && res.data.data) {
            const leads = res.data.data;
            if (leads.length === 0) {
                tbody.innerHTML = `<tr><td colspan="7" class="text-center py-6 text-slate-400">Chưa có hồ sơ tiếp nhận hiện trường nào.</td></tr>`;
                return;
            }
            tbody.innerHTML = leads.map(l => {
                let statusBadge = `<span class="bg-purple-950 text-purple-300 border border-purple-800 px-2 py-0.5 rounded font-bold text-[11px]">${l.status}</span>`;
                if (l.status === 'CONVERTED') statusBadge = `<span class="bg-emerald-950 text-emerald-300 border border-emerald-800 px-2 py-0.5 rounded font-bold text-[11px]">ĐÃ ĐẾN KHÁM</span>`;
                if (l.status === 'CONTACTED') statusBadge = `<span class="bg-sky-950 text-sky-300 border border-sky-800 px-2 py-0.5 rounded font-bold text-[11px]">ĐÃ TƯ VẤN</span>`;

                const patientDisplay = l.studentClass ? `${l.patientName} (${l.studentClass})` : l.patientName;

                return `
                    <tr class="hover:bg-slate-700/40 transition">
                        <td class="py-2.5 px-3 font-mono font-bold text-purple-300">${l.leadCode || ('INT-' + l.id)}</td>
                        <td class="py-2.5 px-3 font-bold text-white">${patientDisplay}</td>
                        <td class="py-2.5 px-3 font-mono text-slate-300">${l.phone || '-'}</td>
                        <td class="py-2.5 px-3 text-slate-400 text-xs">${l.eventName || '-'}</td>
                        <td class="py-2.5 px-3 text-slate-200 text-xs">${l.screeningFindings || '-'}</td>
                        <td class="py-2.5 px-3 text-center font-mono text-yellow-300 text-xs font-bold">${l.voucherCode || '-'}</td>
                        <td class="py-2.5 px-3 text-center">${statusBadge}</td>
                    </tr>
                `;
            }).join('');
        }
    } catch (e) {
        console.error('loadFieldIntakeLeads error:', e);
        if (tbody) tbody.innerHTML = `<tr><td colspan="7" class="text-center py-6 text-rose-400">Lỗi khi tải danh sách hồ sơ hiện trường.</td></tr>`;
    }
}

async function handleQuickCreateLead(event) {
    if (event) event.preventDefault();
    const eventName = document.getElementById('intake-event-name')?.value || '';
    const patientName = document.getElementById('intake-patient-name')?.value || '';
    const phone = document.getElementById('intake-phone')?.value || '';
    const studentClass = document.getElementById('intake-class')?.value || '';
    const findings = document.getElementById('intake-findings')?.value || '';
    const recommendation = document.getElementById('intake-recommendation')?.value || '';
    const voucher = document.getElementById('intake-voucher')?.value || '';

    if (!patientName || !phone) {
        alert('Vui lòng điền họ tên và số điện thoại!');
        return;
    }

    try {
        const payload = {
            eventName: eventName,
            patientName: patientName,
            phone: phone,
            studentClass: studentClass,
            screeningFindings: findings,
            recommendation: recommendation,
            voucherCode: voucher,
            eventType: 'SCHOOL_OUTREACH'
        };

        const res = await apiFetch('/api/field-intake', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.ok && res.data.success) {
            showToast('✓ Tiếp nhận hồ sơ khám hiện trường thành công!');
            document.getElementById('intake-patient-name').value = '';
            document.getElementById('intake-phone').value = '';
            document.getElementById('intake-findings').value = '';
            loadFieldIntakeLeads();
        } else {
            alert(res.data?.message || 'Lỗi khi lưu hồ sơ tiếp nhận!');
        }
    } catch (e) {
        console.error('Create lead error:', e);
    }
}

