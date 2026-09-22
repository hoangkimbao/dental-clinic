import React, { useState, useEffect } from 'react';
import { registerRootComponent } from 'expo';
import {
  StyleSheet,
  Text,
  View,
  TextInput,
  TouchableOpacity,
  FlatList,
  ActivityIndicator,
  Modal,
  ScrollView,
  SafeAreaView,
  StatusBar,
  Alert,
  Share,
} from 'react-native';
import {
  loginApi,
  fetchAppointments,
  updateAppointmentStatus,
  fetchMedicalRecords,
  fetchOrthodonticPlans,
  fetchStaffShifts,
  fetchDashboardStats,
  fetchExportCsvText,
  setAuthSession,
  clearAuthSession,
  fetchDentalServices,
  fetchDentalProducts,
  createDentalOrder,
  verifyWarrantyApi,
  runAiDiagnosticApi,
  createAppointmentApi,
  checkInApi,
  checkOutApi,
  fetchAttendanceHistoryApi,
  createFieldIntakeApi,
  fetchFieldIntakeLeadsApi,
} from './src/services/api';
import { getApiBaseUrl, setApiBaseUrl, DEFAULT_SERVER_IP } from './src/config';

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [currentTab, setCurrentTab] = useState('appointments'); // appointments, emr, shifts, stats

  // Dual-role Patient Mode states
  const [isPatientMode, setIsPatientMode] = useState(false);
  const [patientTab, setPatientTab] = useState('services'); // services, booking, shop, warranty, ai
  const [patientServices, setPatientServices] = useState([]);
  const [serviceCategory, setServiceCategory] = useState('ALL');
  const [patientProducts, setPatientProducts] = useState([]);
  const [productCategory, setProductCategory] = useState('ALL');
  const [mobileCart, setMobileCart] = useState([]);
  const [showCartModal, setShowCartModal] = useState(false);

  // Booking states
  const [bookName, setBookName] = useState('Nguyễn Văn An');
  const [bookPhone, setBookPhone] = useState('0912345678');
  const [bookService, setBookService] = useState('Niềng Răng Chỉnh Nha 3D');
  const [bookTime, setBookTime] = useState('09:00');
  const [bookNotes, setBookNotes] = useState('');

  // Warranty states
  const [warrantyCode, setWarrantyCode] = useState('DC-WR-2026-88992');
  const [warrantyResult, setWarrantyResult] = useState(null);
  const [searchingWarranty, setSearchingWarranty] = useState(false);

  // AI Diagnostic states
  const [aiSymptoms, setAiSymptoms] = useState('Đau nhức góc hàm dưới, răng khôn mọc lệch đâm vào răng số 7');
  const [aiResult, setAiResult] = useState(null);
  const [runningAi, setRunningAi] = useState(false);

  // Login form states
  const [username, setUsername] = useState('letan');
  const [password, setPassword] = useState('123');
  const [loading, setLoading] = useState(false);

  // IP settings modal
  const [showIpModal, setShowIpModal] = useState(false);
  const [serverIp, setServerIp] = useState(getApiBaseUrl());

  // Appointments state
  const [appointments, setAppointments] = useState([]);
  const [appointmentPreset, setAppointmentPreset] = useState('all');
  const [loadingAppts, setLoadingAppts] = useState(false);

  // EMR & Ortho state
  const [records, setRecords] = useState([]);
  const [orthoPlans, setOrthoPlans] = useState([]);

  // Shifts & Stats
  const [shifts, setShifts] = useState([]);
  const [stats, setStats] = useState({});

  // Milestone 2: Staff Attendance & Field Intake states
  const [attendances, setAttendances] = useState([]);
  const [fieldLeads, setFieldLeads] = useState([]);
  const [intakeEventName, setIntakeEventName] = useState('Chương Trình Nụ Cười Học Đường 2026');
  const [intakePatientName, setIntakePatientName] = useState('Trần Minh Quân');
  const [intakePhone, setIntakePhone] = useState('0908112233');
  const [intakeClass, setIntakeClass] = useState('7A2');
  const [intakeFindings, setIntakeFindings] = useState('Sâu răng hàm số 46, khớp cắn hở nhẹ');
  const [intakeRecommendation, setIntakeRecommendation] = useState('Hàn răng sâu composite, khám chỉnh nha');
  const [intakeVoucher, setIntakeVoucher] = useState('HOCDUONG100K');
  const [actionLoading, setActionLoading] = useState(false);

  const handleQuickStaffCheckIn = async (shiftId) => {
    setActionLoading(true);
    try {
      const res = await checkInApi(shiftId || 1);
      Alert.alert('Chấm Công Thành Công 🎉', `Trạng thái: ${res.status}\nThời gian: ${new Date(res.checkInTime).toLocaleTimeString('vi-VN')}\nXác thực GPS: ${res.isGpsVerified ? 'Hợp lệ ✓' : 'Không có'}`);
      loadAttendanceHistory();
    } catch (err) {
      Alert.alert('Lỗi chấm công', err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleQuickStaffCheckOut = async (shiftId) => {
    setActionLoading(true);
    try {
      const res = await checkOutApi(shiftId || 1, 'Bàn giao ca thành công từ Mobile App');
      Alert.alert('Check-out Thành Công', `Đã ghi nhận giờ kết thúc ca làm việc lúc: ${new Date(res.checkOutTime).toLocaleTimeString('vi-VN')}`);
      loadAttendanceHistory();
    } catch (err) {
      Alert.alert('Lỗi check-out', err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const loadAttendanceHistory = async () => {
    try {
      const data = await fetchAttendanceHistoryApi();
      setAttendances(data);
    } catch (e) {
      console.log('Error loading attendance:', e);
    }
  };

  const loadFieldIntakeData = async () => {
    try {
      const data = await fetchFieldIntakeLeadsApi();
      setFieldLeads(data);
    } catch (e) {
      console.log('Error loading field leads:', e);
    }
  };

  const handleSaveFieldLead = async () => {
    if (!intakePatientName || !intakePhone) {
      Alert.alert('Thông báo', 'Vui lòng nhập Họ tên và Số điện thoại!');
      return;
    }
    setActionLoading(true);
    try {
      const payload = {
        eventName: intakeEventName,
        eventType: 'SCHOOL_SCREENING',
        patientFullName: intakePatientName,
        studentClass: intakeClass,
        phone: intakePhone,
        screeningFindings: intakeFindings,
        recommendation: intakeRecommendation,
        voucherCode: intakeVoucher,
      };
      const res = await createFieldIntakeApi(payload);
      Alert.alert('Tiếp Nhận Thành Công 📋', `Mã hồ sơ: ${res.leadCode}\nBệnh nhân: ${res.patientFullName || res.patientName}\nTrạng thái: ${res.leadStatus || res.status}`);
      loadFieldIntakeData();
    } catch (err) {
      Alert.alert('Lỗi tiếp nhận', err.message);
    } finally {
      setActionLoading(false);
    }
  };

  // Export CSV Modal
  const [showCsvModal, setShowCsvModal] = useState(false);
  const [csvContent, setCsvContent] = useState('');

  const handleLogin = async (overrideUser, overridePass) => {
    const userToUse = overrideUser || username;
    const passToUse = overridePass || password;

    if (!userToUse || !passToUse) {
      Alert.alert('Thông báo', 'Vui lòng nhập tên đăng nhập và mật khẩu!');
      return;
    }

    setLoading(true);
    try {
      const data = await loginApi(userToUse, passToUse);
      setCurrentUser(data);
      setIsLoggedIn(true);
      if (data.role === 'ROLE_PATIENT') {
        setIsPatientMode(true);
        loadPatientServices('ALL');
        loadPatientProducts('ALL');
      } else {
        setIsPatientMode(false);
        loadAppData(data);
      }
    } catch (err) {
      Alert.alert('Lỗi đăng nhập', err.message || 'Không thể kết nối đến máy chủ. Kiểm tra lại IP máy chủ!');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    clearAuthSession();
    setIsLoggedIn(false);
    setIsPatientMode(false);
    setCurrentUser(null);
    setAppointments([]);
    setMobileCart([]);
    setWarrantyResult(null);
    setAiResult(null);
  };

  const loadPatientServices = async (category = 'ALL') => {
    try {
      const data = await fetchDentalServices(category);
      setPatientServices(data);
    } catch (e) {
      console.log('Error loading patient services:', e);
    }
  };

  const loadPatientProducts = async (category = 'ALL') => {
    try {
      const data = await fetchDentalProducts(category);
      setPatientProducts(data);
    } catch (e) {
      console.log('Error loading patient products:', e);
    }
  };

  const handleAddToCart = (product, isCombo = false) => {
    const comboOption = (product.packagingOptions || []).find(p => p.packagingType === 'COMBO_PACK');
    const singleOption = (product.packagingOptions || []).find(p => p.packagingType === 'SINGLE_BOX');
    const chosen = isCombo && comboOption ? comboOption : singleOption;
    const price = chosen ? chosen.unitPrice : product.basePrice;
    const unitName = chosen ? chosen.unitName : 'Hộp Đơn';

    setMobileCart(prev => [
      ...prev,
      {
        productId: product.id,
        productName: product.name,
        unitName,
        price,
        packagingOptionId: chosen ? chosen.id : null,
        quantity: 1,
      }
    ]);
    Alert.alert('Thành công', `Đã thêm "${product.name} (${unitName})" vào giỏ hàng!`);
  };

  const handleCheckoutOrder = async () => {
    if (mobileCart.length === 0) {
      Alert.alert('Thông báo', 'Giỏ hàng của bạn đang trống!');
      return;
    }
    setLoading(true);
    try {
      const payload = {
        customerName: bookName || 'Bệnh Nhân DentalCare',
        customerPhone: bookPhone || '0912345678',
        shippingAddress: '36/9/12/7 Nguyễn Triệu Luật, P. Bình Tân, TP.HCM',
        paymentMethod: 'COD',
        items: mobileCart.map(i => ({
          productId: i.productId,
          packagingOptionId: i.packagingOptionId,
          quantity: i.quantity,
        })),
      };
      const order = await createDentalOrder(payload);
      setMobileCart([]);
      setShowCartModal(false);
      Alert.alert(
        'Đặt Hàng Thành Công 🎉',
        `Mã đơn: ${order.orderCode}\nTổng tiền: ${order.totalAmount?.toLocaleString('vi-VN')} đ\nTích lũy: +${order.loyaltyPointsEarned} điểm thưởng Loyalty!`
      );
    } catch (err) {
      Alert.alert('Lỗi đặt hàng', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSearchWarranty = async (codeOverride) => {
    const query = codeOverride || warrantyCode;
    if (!query) {
      Alert.alert('Thông báo', 'Vui lòng nhập mã bảo hành hoặc quét mã QR!');
      return;
    }
    setSearchingWarranty(true);
    try {
      const data = await verifyWarrantyApi(query);
      setWarrantyResult(data);
    } catch (err) {
      Alert.alert('Tra cứu thất bại', err.message || 'Không tìm thấy thẻ bảo hành');
      setWarrantyResult(null);
    } finally {
      setSearchingWarranty(false);
    }
  };

  const handleRunAiDiagnostic = async (symptomsOverride) => {
    const s = symptomsOverride || aiSymptoms;
    if (!s) {
      Alert.alert('Thông báo', 'Vui lòng mô tả triệu chứng gặp phải!');
      return;
    }
    setRunningAi(true);
    try {
      const payload = {
        patientName: bookName || 'Bệnh Nhân Khám Phá',
        patientPhone: bookPhone || '0912345678',
        symptomsDescription: s,
        modelUsed: '9router/gemini-2.5-flash',
      };
      const res = await runAiDiagnosticApi(payload);
      setAiResult(res);
    } catch (err) {
      Alert.alert('Lỗi AI Diagnostic', err.message);
    } finally {
      setRunningAi(false);
    }
  };

  const handlePatientBooking = async () => {
    if (!bookName || !bookPhone || !bookService) {
      Alert.alert('Thông báo', 'Vui lòng nhập Họ tên, Số điện thoại và Dịch vụ!');
      return;
    }
    setLoading(true);
    try {
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      const apptTime = tomorrow.toISOString().slice(0, 10) + 'T' + bookTime + ':00';
      const bookingData = {
        patientName: bookName,
        patientPhone: bookPhone,
        serviceName: bookService,
        appointmentTime: apptTime,
        notes: bookNotes,
      };
      const res = await createAppointmentApi(bookingData);
      Alert.alert(
        'Đặt Lịch Thành Công 🎉',
        `Mã lịch hẹn: #${res.id}\nDịch vụ: ${res.serviceName}\nKhung giờ: ${bookTime} ngày mai.\nCọc 100K giữ chỗ đã được áp dụng!`
      );
    } catch (err) {
      Alert.alert('Lỗi đặt lịch', err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadAppData = async (user) => {
    loadAppointmentsData(appointmentPreset, user);
    try {
      const [emrData, orthoData, shiftData, statData] = await Promise.allSettled([
        fetchMedicalRecords(),
        fetchOrthodonticPlans(),
        fetchStaffShifts(),
        fetchDashboardStats(),
      ]);
      if (emrData.status === 'fulfilled') setRecords(emrData.value);
      if (orthoData.status === 'fulfilled') setOrthoPlans(orthoData.value);
      if (shiftData.status === 'fulfilled') setShifts(shiftData.value);
      if (statData.status === 'fulfilled') setStats(statData.value);
    } catch (e) {
      console.log('Load app data error:', e);
    }
  };

  const loadAppointmentsData = async (preset, user = currentUser) => {
    setLoadingAppts(true);
    try {
      const dentistId = (user && user.role === 'ROLE_DENTIST') ? user.id : null;
      const data = await fetchAppointments(preset, null, null, dentistId);
      setAppointments(data);
    } catch (err) {
      console.log('Fetch appointments error:', err);
    } finally {
      setLoadingAppts(false);
    }
  };

  const handleFilterChange = (preset) => {
    setAppointmentPreset(preset);
    loadAppointmentsData(preset);
  };

  const handleStatusUpdate = async (id, newStatus) => {
    try {
      await updateAppointmentStatus(id, newStatus);
      Alert.alert('Thành công', 'Đã cập nhật trạng thái lịch hẹn!');
      loadAppointmentsData(appointmentPreset);
    } catch (err) {
      Alert.alert('Lỗi', 'Không thể cập nhật trạng thái');
    }
  };

  const handleExportCsv = async () => {
    try {
      setLoading(true);
      const csv = await fetchExportCsvText(appointmentPreset);
      setCsvContent(csv);
      setShowCsvModal(true);
    } catch (err) {
      Alert.alert('Lỗi', 'Không thể xuất file CSV: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleShareCsv = async () => {
    try {
      await Share.share({
        message: csvContent,
        title: 'Lịch Khám DentalCare (' + appointmentPreset + ')',
      });
    } catch (error) {
      Alert.alert('Lỗi', 'Không thể chia sẻ file');
    }
  };

  const formatDateTime = (dtStr) => {
    if (!dtStr) return '--:--';
    const d = new Date(dtStr);
    const hh = d.getHours().toString().padStart(2, '0');
    const mm = d.getMinutes().toString().padStart(2, '0');
    const dd = d.getDate().toString().padStart(2, '0');
    const MM = (d.getMonth() + 1).toString().padStart(2, '0');
    return hh + ':' + mm + ' - ' + dd + '/' + MM;
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'DEPOSIT_PAID':
        return { label: 'ĐÃ CỌC 100K', bg: '#065f46', text: '#34d399' };
      case 'CONFIRMED':
        return { label: 'ĐÃ XÁC NHẬN', bg: '#0369a1', text: '#38bdf8' };
      case 'COMPLETED':
        return { label: 'HOÀN TẤT', bg: '#1e293b', text: '#94a3b8' };
      default:
        return { label: 'CHỜ CỌC', bg: '#854d0e', text: '#fde047' };
    }
  };

  // ================= 1. LOGIN SCREEN =================
  if (!isLoggedIn) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" backgroundColor="#0f172a" />
        <ScrollView contentContainerStyle={styles.loginScroll}>
          {/* Header Branding */}
          <View style={styles.loginHeader}>
            <View style={styles.logoBadge}>
              <Text style={styles.logoText}>🦷</Text>
            </View>
            <Text style={styles.brandTitle}>DentalCare Staff</Text>
            <Text style={styles.brandSubtitle}>Ứng Dụng Quản Lý Nội Bộ Phòng Khám</Text>
          </View>

          {/* Login Card */}
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Đăng Nhập Nhân Sự</Text>

            <Text style={styles.inputLabel}>Tên đăng nhập / Số điện thoại</Text>
            <TextInput
              style={styles.input}
              value={username}
              onChangeText={setUsername}
              placeholder="Nhập tên đăng nhập (VD: letan, bs_tuan)..."
              placeholderTextColor="#64748b"
              autoCapitalize="none"
            />

            <Text style={styles.inputLabel}>Mật khẩu</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              secureTextEntry
              placeholder="Mật khẩu (mặc định: 123)..."
              placeholderTextColor="#64748b"
            />

            <TouchableOpacity
              style={styles.btnPrimary}
              onPress={() => handleLogin()}
              disabled={loading}
            >
              {loading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.btnPrimaryText}>Đăng Nhập Vào Hệ Thống</Text>
              )}
            </TouchableOpacity>

            {/* Fast Login Demo Buttons */}
            <Text style={styles.quickLoginTitle}>⚡ Đăng nhập nhanh để test:</Text>
            <View style={styles.quickLoginRow}>
              <TouchableOpacity
                style={styles.btnQuick}
                onPress={() => { setUsername('letan'); setPassword('123'); handleLogin('letan', '123'); }}
              >
                <Text style={styles.btnQuickText}>👩‍💼 Lễ Tân</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.btnQuick}
                onPress={() => { setUsername('bs_tuan'); setPassword('123'); handleLogin('bs_tuan', '123'); }}
              >
                <Text style={styles.btnQuickText}>👨‍⚕️ Bác Sĩ</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.btnQuick}
                onPress={() => { setUsername('owner'); setPassword('123'); handleLogin('owner', '123'); }}
              >
                <Text style={styles.btnQuickText}>👑 Chủ Phòng</Text>
              </TouchableOpacity>
            </View>

            {/* Patient Mode Entry */}
            <View style={{ marginTop: 14, paddingTop: 14, borderTopWidth: 1, borderTopColor: '#334155' }}>
              <TouchableOpacity
                style={styles.btnPatientMode}
                onPress={() => {
                  setIsPatientMode(true);
                  setIsLoggedIn(true);
                  setCurrentUser({
                    username: 'patient_guest',
                    fullName: 'Khách Hàng Khám Phá',
                    role: 'ROLE_PATIENT',
                  });
                  loadPatientServices('ALL');
                  loadPatientProducts('ALL');
                }}
              >
                <Text style={styles.btnPatientModeText}>🌟 Trải Nghiệm Khách Hàng / Bệnh Nhân</Text>
                <Text style={styles.btnPatientModeSub}>Xem dịch vụ, đặt lịch, mua sắm & chẩn đoán AI</Text>
              </TouchableOpacity>
            </View>

            {/* Server IP Config Link */}
            <TouchableOpacity
              style={styles.serverConfigLink}
              onPress={() => setShowIpModal(true)}
            >
              <Text style={styles.serverConfigText}>⚙️ Server IP: {getApiBaseUrl()}</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>

        {/* Modal Change IP */}
        <Modal visible={showIpModal} transparent animationType="fade">
          <View style={styles.modalOverlay}>
            <View style={styles.modalBox}>
              <Text style={styles.modalTitle}>Cấu Hình Server Backend</Text>
              <Text style={styles.modalSub}>Nhập địa chỉ IPv4 máy tính chạy Spring Boot (cùng Wifi):</Text>
              <TextInput
                style={styles.input}
                value={serverIp}
                onChangeText={setServerIp}
                placeholder="192.168.1.x"
                placeholderTextColor="#64748b"
              />
              <View style={styles.modalBtnRow}>
                <TouchableOpacity
                  style={[styles.btnSmall, { backgroundColor: '#334155' }]}
                  onPress={() => setShowIpModal(false)}
                >
                  <Text style={styles.btnSmallText}>Đóng</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.btnSmall, { backgroundColor: '#0284c7' }]}
                  onPress={() => {
                    setApiBaseUrl(serverIp);
                    setShowIpModal(false);
                    Alert.alert('Thành công', 'Đã cập nhật Server: ' + getApiBaseUrl());
                  }}
                >
                  <Text style={styles.btnSmallText}>Lưu IP</Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
        </Modal>
      </SafeAreaView>
  // ================= 1.5. PATIENT ECOSYSTEM WORKSPACE =================
  if (isPatientMode) {
    const totalCartQty = mobileCart.reduce((sum, i) => sum + i.quantity, 0);
    const cartTotalAmount = mobileCart.reduce((sum, i) => sum + (i.price * i.quantity), 0);

    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" backgroundColor="#042f2e" />

        {/* Top Patient Header */}
        <View style={[styles.topBar, { backgroundColor: '#042f2e' }]}>
          <View>
            <Text style={styles.topBarTitle}>DentalCare Luxury</Text>
            <Text style={[styles.topBarSubtitle, { color: '#34d399' }]}>
              {currentUser?.fullName || 'Khách Hàng Trực Tuyến'} • 🌟 Gold VIP (1.250 Pts)
            </Text>
          </View>
          <TouchableOpacity style={styles.btnLogout} onPress={handleLogout}>
            <Text style={styles.btnLogoutText}>Đổi vai trò</Text>
          </TouchableOpacity>
        </View>

        {/* 5 Patient Tabs Navigation */}
        <View style={[styles.tabBar, { backgroundColor: '#022c22' }]}>
          {[
            { key: 'services', label: '✨ Dịch Vụ' },
            { key: 'booking', label: '📅 Đặt Lịch' },
            { key: 'shop', label: '🛍️ Mua Sắm' },
            { key: 'warranty', label: '🛡️ Bảo Hành' },
            { key: 'ai', label: '🤖 Bác Sĩ AI' },
          ].map(t => (
            <TouchableOpacity
              key={t.key}
              style={[
                styles.tabItem,
                patientTab === t.key && { borderBottomColor: '#10b981', borderBottomWidth: 3 },
              ]}
              onPress={() => {
                setPatientTab(t.key);
                if (t.key === 'services' && patientServices.length === 0) loadPatientServices('ALL');
                if (t.key === 'shop' && patientProducts.length === 0) loadPatientProducts('ALL');
              }}
            >
              <Text
                style={[
                  styles.tabText,
                  patientTab === t.key && { color: '#34d399', fontWeight: '800' },
                ]}
              >
                {t.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* PATIENT TAB 1: DỊCH VỤ NHA KHOA */}
        {patientTab === 'services' && (
          <View style={styles.tabContent}>
            {/* Category Filter Pills */}
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterScroll}>
              {[
                { key: 'ALL', label: 'Tất Cả' },
                { key: 'ORTHODONTICS', label: 'Niềng Răng 3D' },
                { key: 'IMPLANT', label: 'Cấy Ghép Implant' },
                { key: 'PORCELAIN_CROWNS', label: 'Răng Sứ Thẩm Mỹ' },
                { key: 'WHITENING', label: 'Tẩy Trắng Răng' },
                { key: 'WISDOM_TEETH', label: 'Nhổ Răng Khôn' },
                { key: 'GENERAL', label: 'Tổng Quát' },
              ].map(c => (
                <TouchableOpacity
                  key={c.key}
                  style={[
                    styles.filterPill,
                    serviceCategory === c.key && { backgroundColor: '#059669', borderColor: '#10b981' },
                  ]}
                  onPress={() => {
                    setServiceCategory(c.key);
                    loadPatientServices(c.key);
                  }}
                >
                  <Text
                    style={[
                      styles.filterPillText,
                      serviceCategory === c.key && { color: '#ffffff', fontWeight: '800' },
                    ]}
                  >
                    {c.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>

            <FlatList
              data={patientServices}
              keyExtractor={item => item.id.toString()}
              contentContainerStyle={{ padding: 14, paddingBottom: 60 }}
              ListEmptyComponent={
                <Text style={styles.emptyText}>Đang tải danh mục dịch vụ nha khoa...</Text>
              }
              renderItem={({ item }) => (
                <View style={styles.patientCard}>
                  <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                    <Text style={{ fontSize: 11, color: '#38bdf8', fontWeight: '700', fontFamily: 'monospace' }}>
                      {item.code}
                    </Text>
                    {item.isFeatured && (
                      <View style={{ backgroundColor: 'rgba(244,63,94,0.2)', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 8 }}>
                        <Text style={{ color: '#fb7185', fontSize: 10, fontWeight: '800' }}>🔥 Nổi Bật</Text>
                      </View>
                    )}
                  </View>

                  <Text style={styles.patientCardTitle}>{item.name}</Text>
                  <Text style={styles.patientCardDesc}>{item.description}</Text>

                  <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingTop: 10, borderTopWidth: 1, borderTopColor: '#334155' }}>
                    <View>
                      <Text style={{ fontSize: 11, color: '#94a3b8' }}>⏰ {item.durationMinutes || 45} phút</Text>
                      <Text style={styles.patientPriceText}>{item.price?.toLocaleString('vi-VN')} đ</Text>
                    </View>
                    <TouchableOpacity
                      style={styles.btnBookingSmall}
                      onPress={() => {
                        setBookService(item.name);
                        setPatientTab('booking');
                      }}
                    >
                      <Text style={styles.btnBookingSmallText}>Đặt Lịch Khám →</Text>
                    </TouchableOpacity>
                  </View>
                </View>
              )}
            />
          </View>
        )}

        {/* PATIENT TAB 2: ĐẶT LỊCH KHÁM & CỌC 100K */}
        {patientTab === 'booking' && (
          <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 60 }}>
            <View style={[styles.card, { backgroundColor: '#1e293b' }]}>
              <Text style={styles.cardTitle}>Đặt Lịch Khám & Cọc 100K</Text>
              <Text style={{ fontSize: 12, color: '#94a3b8', marginBottom: 14 }}>
                Nhận lịch khám ưu tiên với bác sĩ chuyên khoa & giảm ngay 500K khi cọc online.
              </Text>

              <Text style={styles.inputLabel}>Họ và tên *</Text>
              <TextInput
                style={styles.input}
                value={bookName}
                onChangeText={setBookName}
                placeholder="Nhập họ và tên..."
                placeholderTextColor="#64748b"
              />

              <Text style={styles.inputLabel}>Số điện thoại *</Text>
              <TextInput
                style={styles.input}
                value={bookPhone}
                onChangeText={setBookPhone}
                placeholder="Nhập số điện thoại..."
                placeholderTextColor="#64748b"
                keyboardType="phone-pad"
              />

              <Text style={styles.inputLabel}>Dịch vụ thăm khám *</Text>
              <TextInput
                style={styles.input}
                value={bookService}
                onChangeText={setBookService}
                placeholder="VD: Niềng Răng 3D, Cấy Implant..."
                placeholderTextColor="#64748b"
              />

              <Text style={styles.inputLabel}>Chọn khung giờ khám ngày mai:</Text>
              <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 12 }}>
                {['08:30', '10:00', '14:00', '16:00', '17:30', '19:00'].map(slot => (
                  <TouchableOpacity
                    key={slot}
                    style={{
                      paddingHorizontal: 12,
                      paddingVertical: 7,
                      borderRadius: 10,
                      backgroundColor: bookTime === slot ? '#059669' : '#334155',
                    }}
                    onPress={() => setBookTime(slot)}
                  >
                    <Text style={{ color: '#ffffff', fontSize: 11, fontWeight: '700' }}>{slot}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              <Text style={styles.inputLabel}>Ghi chú tình trạng răng</Text>
              <TextInput
                style={[styles.input, { height: 60 }]}
                value={bookNotes}
                onChangeText={setBookNotes}
                multiline
                placeholder="Mô tả cảm giác hoặc yêu cầu riêng..."
                placeholderTextColor="#64748b"
              />

              <TouchableOpacity
                style={[styles.btnPrimary, { backgroundColor: '#059669' }]}
                onPress={handlePatientBooking}
                disabled={loading}
              >
                {loading ? (
                  <ActivityIndicator color="#fff" />
                ) : (
                  <Text style={styles.btnPrimaryText}>✓ Xác Nhận Đặt Lịch & Cọc 100K</Text>
                )}
              </TouchableOpacity>
            </View>
          </ScrollView>
        )}

        {/* PATIENT TAB 3: MUA SẮM SẢN PHẨM RĂNG MIỆNG */}
        {patientTab === 'shop' && (
          <View style={styles.tabContent}>
            {/* Product Category Filter Pills */}
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterScroll}>
              {[
                { key: 'ALL', label: 'Tất Cả' },
                { key: 'BRUSH', label: 'Bàn Chải Điện' },
                { key: 'FLOSSER', label: 'Máy Tăm Nước' },
                { key: 'TOOTHPASTE', label: 'Kem Đánh Răng' },
                { key: 'FLOSS', label: 'Chỉ Nha Khoa' },
                { key: 'RETAINER', label: 'Máng Duy Trì' },
              ].map(c => (
                <TouchableOpacity
                  key={c.key}
                  style={[
                    styles.filterPill,
                    productCategory === c.key && { backgroundColor: '#059669', borderColor: '#10b981' },
                  ]}
                  onPress={() => {
                    setProductCategory(c.key);
                    loadPatientProducts(c.key);
                  }}
                >
                  <Text
                    style={[
                      styles.filterPillText,
                      productCategory === c.key && { color: '#ffffff', fontWeight: '800' },
                    ]}
                  >
                    {c.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>

            <FlatList
              data={patientProducts}
              keyExtractor={item => item.id.toString()}
              contentContainerStyle={{ padding: 14, paddingBottom: 80 }}
              ListEmptyComponent={
                <Text style={styles.emptyText}>Đang tải sản phẩm chăm sóc răng miệng...</Text>
              }
              renderItem={({ item }) => (
                <View style={styles.patientCard}>
                  <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 }}>
                    <Text style={{ fontSize: 11, color: '#38bdf8', fontWeight: '700' }}>{item.brand}</Text>
                    <Text style={{ fontSize: 11, color: '#fbbf24', fontWeight: '700' }}>⭐ {item.rating || 5.0}</Text>
                  </View>

                  <Text style={styles.patientCardTitle}>{item.name}</Text>
                  <Text style={styles.patientCardDesc}>{item.description}</Text>

                  <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingTop: 10, borderTopWidth: 1, borderTopColor: '#334155' }}>
                    <View>
                      <Text style={{ fontSize: 10, color: '#94a3b8' }}>Giá cơ bản</Text>
                      <Text style={styles.patientPriceText}>{item.basePrice?.toLocaleString('vi-VN')} đ</Text>
                    </View>
                    <View style={{ flexDirection: 'row', gap: 6 }}>
                      <TouchableOpacity
                        style={[styles.btnBookingSmall, { backgroundColor: '#334155' }]}
                        onPress={() => handleAddToCart(item, false)}
                      >
                        <Text style={styles.btnBookingSmallText}>Hộp Đơn</Text>
                      </TouchableOpacity>
                      <TouchableOpacity
                        style={styles.btnBookingSmall}
                        onPress={() => handleAddToCart(item, true)}
                      >
                        <Text style={styles.btnBookingSmallText}>Combo (-15%)</Text>
                      </TouchableOpacity>
                    </View>
                  </View>
                </View>
              )}
            />

            {/* Floating Cart Button */}
            {totalCartQty > 0 && (
              <TouchableOpacity
                style={styles.cartFloatingBtn}
                onPress={() => setShowCartModal(true)}
              >
                <Text style={styles.cartFloatingText}>🛒 Giỏ Hàng</Text>
                <View style={styles.cartBadge}>
                  <Text style={styles.cartBadgeText}>{totalCartQty}</Text>
                </View>
              </TouchableOpacity>
            )}
          </View>
        )}

        {/* PATIENT TAB 4: BẢO HÀNH RĂNG SỨ & QR */}
        {patientTab === 'warranty' && (
          <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 60 }}>
            <View style={[styles.card, { backgroundColor: '#1e293b' }]}>
              <Text style={styles.cardTitle}>Xác Thực Thẻ Bảo Hành Răng Sứ</Text>
              <Text style={{ fontSize: 12, color: '#94a3b8', marginBottom: 12 }}>
                Nhập số serial hoặc mã QR in trên thẻ bảo hành Lava Plus, Cercon HT, Emax chính hãng.
              </Text>

              <TextInput
                style={styles.input}
                value={warrantyCode}
                onChangeText={setWarrantyCode}
                placeholder="Nhập mã thẻ bảo hành..."
                placeholderTextColor="#64748b"
              />

              <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 14 }}>
                <Text style={{ fontSize: 11, color: '#94a3b8', width: '100%' }}>Mã thử nghiệm:</Text>
                <TouchableOpacity
                  style={{ padding: 6, backgroundColor: '#0f172a', borderRadius: 8, borderWidth: 1, borderColor: '#334155' }}
                  onPress={() => { setWarrantyCode('DC-WR-2026-88992'); handleSearchWarranty('DC-WR-2026-88992'); }}
                >
                  <Text style={{ color: '#38bdf8', fontSize: 10, fontFamily: 'monospace' }}>DC-WR-2026-88992 (Lava)</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={{ padding: 6, backgroundColor: '#0f172a', borderRadius: 8, borderWidth: 1, borderColor: '#334155' }}
                  onPress={() => { setWarrantyCode('DENTALCARE-CERCON-77123'); handleSearchWarranty('DENTALCARE-CERCON-77123'); }}
                >
                  <Text style={{ color: '#fbbf24', fontSize: 10, fontFamily: 'monospace' }}>CERCON-77123 (QR)</Text>
                </TouchableOpacity>
              </View>

              <TouchableOpacity
                style={[styles.btnPrimary, { backgroundColor: '#0284c7' }]}
                onPress={() => handleSearchWarranty()}
                disabled={searchingWarranty}
              >
                {searchingWarranty ? (
                  <ActivityIndicator color="#fff" />
                ) : (
                  <Text style={styles.btnPrimaryText}>🔍 Tra Cứu Thẻ Bảo Hành</Text>
                )}
              </TouchableOpacity>

              {/* Warranty Result Card */}
              {warrantyResult && (
                <View style={styles.warrantyCertificateCard}>
                  <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                    <Text style={{ fontSize: 13, fontWeight: '900', color: '#38bdf8', fontFamily: 'monospace' }}>
                      {warrantyResult.serialCode}
                    </Text>
                    <View style={{ backgroundColor: 'rgba(16,185,129,0.2)', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 8 }}>
                      <Text style={{ color: '#34d399', fontSize: 10, fontWeight: '800' }}>● ĐANG HIỆU LỰC</Text>
                    </View>
                  </View>

                  <Text style={{ fontSize: 12, color: '#cbd5e1', marginBottom: 4 }}>
                    👤 Khách hàng: <Text style={{ color: '#fff', fontWeight: '700' }}>{warrantyResult.patientName}</Text>
                  </Text>
                  <Text style={{ fontSize: 12, color: '#cbd5e1', marginBottom: 4 }}>
                    🦷 Phôi sứ: <Text style={{ color: '#fde047', fontWeight: '700' }}>{warrantyResult.crownType} (Màu {warrantyResult.shadeCode || 'A1'})</Text>
                  </Text>
                  <Text style={{ fontSize: 12, color: '#cbd5e1', marginBottom: 4 }}>
                    📍 Vị trí răng: <Text style={{ color: '#38bdf8', fontWeight: '700' }}>Răng số {warrantyResult.teethNumbers}</Text>
                  </Text>
                  <Text style={{ fontSize: 12, color: '#cbd5e1', marginBottom: 4 }}>
                    🏭 Labo sản xuất: <Text style={{ color: '#fff', fontWeight: '700' }}>{warrantyResult.laboPartner}</Text>
                  </Text>
                  <Text style={{ fontSize: 12, color: '#cbd5e1', marginBottom: 4 }}>
                    🛡️ Thời hạn: <Text style={{ color: '#34d399', fontWeight: '700' }}>{warrantyResult.warrantyYears} Năm (Đến {warrantyResult.expiryDate || '2041-01-15'})</Text>
                  </Text>
                  <Text style={{ fontSize: 10, color: '#94a3b8', marginTop: 6, fontStyle: 'italic' }}>
                    ✓ QR Code: {warrantyResult.qrVerificationCode}
                  </Text>
                </View>
              )}
            </View>
          </ScrollView>
        )}

        {/* PATIENT TAB 5: AI BÁC SĨ CHẨN ĐOÁN */}
        {patientTab === 'ai' && (
          <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 60 }}>
            <View style={[styles.card, { backgroundColor: '#1e293b' }]}>
              <Text style={styles.cardTitle}>Bác Sĩ AI Chẩn Đoán Răng Miệng</Text>
              <Text style={{ fontSize: 12, color: '#94a3b8', marginBottom: 12 }}>
                Ứng dụng trí tuệ nhân tạo (9Router Gateway & bộ luật lâm sàng) phát hiện sớm bệnh lý răng miệng.
              </Text>

              <Text style={styles.inputLabel}>Mô tả triệu chứng / Cảm giác khó chịu:</Text>
              <TextInput
                style={[styles.input, { height: 75 }]}
                value={aiSymptoms}
                onChangeText={setAiSymptoms}
                multiline
                placeholder="Nhập triệu chứng..."
                placeholderTextColor="#64748b"
              />

              <Text style={{ fontSize: 11, color: '#94a3b8', marginBottom: 6 }}>Hoặc chọn mẫu triệu chứng:</Text>
              <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 14 }}>
                {[
                  { label: '⚡ Răng Khôn Lệch', text: 'Đau nhức góc hàm dưới, răng khôn mọc lệch đâm vào răng số 7' },
                  { label: '🦷 Sâu Men Răng', text: 'Răng hàm trên có lỗ đen sâu kẽ, ê buốt khi ăn đồ ngọt' },
                  { label: '🪨 Vôi Răng Ố Vàng', text: 'Chân răng cửa dưới bám nhiều vôi ố vàng xỉn màu' },
                  { label: '🩸 Viêm Nướu Lợi', text: 'Lợi bị sưng đỏ, dễ chảy máu khi chải răng nhẹ' },
                  { label: '✨ Răng Khỏe', text: 'Răng sáng bóng không đau, muốn kiểm tra tổng quát' },
                ].map((s, idx) => (
                  <TouchableOpacity
                    key={idx}
                    style={{ padding: 6, backgroundColor: '#0f172a', borderRadius: 8, borderWidth: 1, borderColor: '#334155' }}
                    onPress={() => {
                      setAiSymptoms(s.text);
                      handleRunAiDiagnostic(s.text);
                    }}
                  >
                    <Text style={{ color: '#c084fc', fontSize: 10, fontWeight: '700' }}>{s.label}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              <TouchableOpacity
                style={[styles.btnPrimary, { backgroundColor: '#7c3aed' }]}
                onPress={() => handleRunAiDiagnostic()}
                disabled={runningAi}
              >
                {runningAi ? (
                  <ActivityIndicator color="#fff" />
                ) : (
                  <Text style={styles.btnPrimaryText}>🤖 Bác Sĩ AI Tiến Hành Chẩn Đoán</Text>
                )}
              </TouchableOpacity>

              {/* AI Diagnostic Result */}
              {aiResult && (
                <View style={[styles.warrantyCertificateCard, { borderColor: '#a855f7', marginTop: 14 }]}>
                  <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                    <Text style={{ fontSize: 14, fontWeight: '900', color: '#ffffff' }}>
                      {aiResult.pathologyName}
                    </Text>
                    <View style={[
                      styles.aiRiskPill,
                      {
                        backgroundColor: aiResult.riskLevel === 'LOW' ? '#065f46' : (aiResult.riskLevel === 'MEDIUM' ? '#854d0e' : '#991b1b')
                      }
                    ]}>
                      <Text style={[styles.aiRiskPillText, { color: '#ffffff' }]}>{aiResult.riskLevel}</Text>
                    </View>
                  </View>

                  <Text style={{ fontSize: 11, color: '#c084fc', fontWeight: '700', marginBottom: 6 }}>
                    🎯 Độ tin cậy lâm sàng: {Math.round((aiResult.confidenceScore || 0.95) * 100)}%
                  </Text>
                  <Text style={{ fontSize: 12, color: '#cbd5e1', marginBottom: 6, lineHeight: 18 }}>
                    <b>Phát hiện:</b> {aiResult.clinicalFindings}
                  </Text>
                  <Text style={{ fontSize: 12, color: '#a7f3d0', marginBottom: 6, lineHeight: 18 }}>
                    <b>Lời khuyên:</b> {aiResult.treatmentAdvice}
                  </Text>
                  <Text style={{ fontSize: 12, color: '#fde047', fontWeight: '800' }}>
                    Chi phí dự kiến: {aiResult.estimatedCostRange || 'Liên hệ phòng khám'}
                  </Text>

                  <TouchableOpacity
                    style={[styles.btnBookingSmall, { marginTop: 12, backgroundColor: '#7c3aed', alignItems: 'center' }]}
                    onPress={() => {
                      setBookService(aiResult.pathologyName);
                      setPatientTab('booking');
                    }}
                  >
                    <Text style={styles.btnBookingSmallText}>Đặt Lịch Điều Trị Ngay →</Text>
                  </TouchableOpacity>
                </View>
              )}
            </View>
          </ScrollView>
        )}

        {/* CART DRAWER MODAL */}
        <Modal visible={showCartModal} transparent animationType="slide">
          <View style={styles.modalOverlay}>
            <View style={[styles.modalBox, { maxHeight: '80%' }]}>
              <Text style={styles.modalTitle}>Giỏ Hàng Của Bạn</Text>
              <Text style={styles.modalSub}>Các sản phẩm chăm sóc răng miệng chính hãng đã chọn:</Text>

              <FlatList
                data={mobileCart}
                keyExtractor={(item, index) => index.toString()}
                renderItem={({ item }) => (
                  <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: '#334155' }}>
                    <View style={{ flex: 1 }}>
                      <Text style={{ fontSize: 13, fontWeight: '700', color: '#fff' }}>{item.productName}</Text>
                      <Text style={{ fontSize: 11, color: '#34d399' }}>{item.unitName}</Text>
                    </View>
                    <Text style={{ fontSize: 13, fontWeight: '800', color: '#38bdf8' }}>
                      {item.price?.toLocaleString('vi-VN')} đ
                    </Text>
                  </View>
                )}
              />

              <View style={{ marginTop: 12, paddingTop: 10, borderTopWidth: 1, borderTopColor: '#334155' }}>
                <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 }}>
                  <Text style={{ color: '#94a3b8', fontSize: 12 }}>Tổng tiền hàng:</Text>
                  <Text style={{ color: '#34d399', fontSize: 14, fontWeight: '900' }}>
                    {cartTotalAmount.toLocaleString('vi-VN')} đ
                  </Text>
                </View>
                <Text style={{ color: '#fbbf24', fontSize: 11, marginBottom: 12 }}>
                  🎁 Tích lũy dự kiến: +{Math.floor(cartTotalAmount / 10000)} điểm thưởng Loyalty!
                </Text>
              </View>

              <View style={styles.modalBtnRow}>
                <TouchableOpacity
                  style={[styles.btnSmall, { backgroundColor: '#334155' }]}
                  onPress={() => setShowCartModal(false)}
                >
                  <Text style={styles.btnSmallText}>Đóng</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.btnSmall, { backgroundColor: '#059669' }]}
                  onPress={handleCheckoutOrder}
                >
                  <Text style={styles.btnSmallText}>Đặt Hàng (COD)</Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
        </Modal>
      </SafeAreaView>
    );
  }

  // ================= 2. MAIN APP WORKSPACE =================
  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#0b1329" />

      {/* Top Staff Header */}
      <View style={styles.topBar}>
        <View>
          <Text style={styles.topBarTitle}>DentalCare Clinic</Text>
          <Text style={styles.topBarSubtitle}>
            {currentUser?.fullName} ({currentUser?.role?.replace('ROLE_', '')})
          </Text>
        </View>
        <TouchableOpacity style={styles.btnLogout} onPress={handleLogout}>
          <Text style={styles.btnLogoutText}>Đăng xuất</Text>
        </TouchableOpacity>
      </View>

      {/* Main Tabs Navigation */}
      <View style={styles.tabBar}>
        <TouchableOpacity
          style={[styles.tabItem, currentTab === 'appointments' && styles.tabItemActive]}
          onPress={() => setCurrentTab('appointments')}
        >
          <Text style={[styles.tabText, currentTab === 'appointments' && styles.tabTextActive]}>
            📅 Lịch Khám
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.tabItem, currentTab === 'emr' && styles.tabItemActive]}
          onPress={() => setCurrentTab('emr')}
        >
          <Text style={[styles.tabText, currentTab === 'emr' && styles.tabTextActive]}>
            🩺 Bệnh Án
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.tabItem, currentTab === 'shifts' && styles.tabItemActive]}
          onPress={() => setCurrentTab('shifts')}
        >
          <Text style={[styles.tabText, currentTab === 'shifts' && styles.tabTextActive]}>
            ⏱️ Ca Trực
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.tabItem, currentTab === 'attendance' && styles.tabItemActive]}
          onPress={() => {
            setCurrentTab('attendance');
            loadAttendanceHistory();
          }}
        >
          <Text style={[styles.tabText, currentTab === 'attendance' && styles.tabTextActive]}>
            📍 Chấm Công
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.tabItem, currentTab === 'field_intake' && styles.tabItemActive]}
          onPress={() => {
            setCurrentTab('field_intake');
            loadFieldIntakeData();
          }}
        >
          <Text style={[styles.tabText, currentTab === 'field_intake' && styles.tabTextActive]}>
            📋 Hiện Trường
          </Text>
        </TouchableOpacity>

        {currentUser?.role === 'ROLE_OWNER' && (
          <TouchableOpacity
            style={[styles.tabItem, currentTab === 'stats' && styles.tabItemActive]}
            onPress={() => setCurrentTab('stats')}
          >
            <Text style={[styles.tabText, currentTab === 'stats' && styles.tabTextActive]}>
              📊 Quản Trị
            </Text>
          </TouchableOpacity>
        )}
      </View>

      {/* TAB CONTENT: 1. APPOINTMENTS */}
      {currentTab === 'appointments' && (
        <View style={styles.tabContent}>
          {/* Action Row: Export CSV & Count */}
          <View style={styles.actionRow}>
            <View style={styles.badgeCount}>
              <Text style={styles.badgeCountText}>
                {appointments.length} Lịch Hẹn
              </Text>
            </View>

            <TouchableOpacity style={styles.btnExport} onPress={handleExportCsv}>
              <Text style={styles.btnExportText}>📥 Xuất Excel (.CSV)</Text>
            </TouchableOpacity>
          </View>

          {/* Preset Filter Buttons */}
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterScroll}>
            {[
              { key: 'all', label: 'Tất Cả' },
              { key: 'today', label: '📍 Hôm Nay' },
              { key: 'tomorrow', label: '⏩ Ngày Mai' },
              { key: 'next7days', label: '🗓️ 7 Ngày Tới' },
              { key: 'future', label: '🔮 Tương Lai' },
            ].map((item) => (
              <TouchableOpacity
                key={item.key}
                style={[
                  styles.filterPill,
                  appointmentPreset === item.key && styles.filterPillActive,
                ]}
                onPress={() => handleFilterChange(item.key)}
              >
                <Text
                  style={[
                    styles.filterPillText,
                    appointmentPreset === item.key && styles.filterPillTextActive,
                  ]}
                >
                  {item.label}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>

          {/* Appointments List */}
          {loadingAppts ? (
            <ActivityIndicator size="large" color="#0284c7" style={{ marginTop: 40 }} />
          ) : (
            <FlatList
              data={appointments}
              keyExtractor={(item) => item.id.toString()}
              contentContainerStyle={styles.listContainer}
              ListEmptyComponent={
                <Text style={styles.emptyText}>Không tìm thấy lịch khám nào theo bộ lọc</Text>
              }
              renderItem={({ item }) => {
                const badge = getStatusBadge(item.status);
                return (
                  <View style={styles.apptCard}>
                    <View style={styles.apptHeader}>
                      <Text style={styles.apptId}>#{item.id}</Text>
                      <View style={[styles.statusBadge, { backgroundColor: badge.bg }]}>
                        <Text style={[styles.statusBadgeText, { color: badge.text }]}>
                          {badge.label}
                        </Text>
                      </View>
                    </View>

                    <Text style={styles.patientName}>{item.patientName}</Text>
                    <Text style={styles.patientPhone}>📞 {item.patientPhone || 'Không có SĐT'}</Text>
                    
                    <View style={styles.divider} />

                    <Text style={styles.serviceName}>🦷 {item.serviceName}</Text>
                    <Text style={styles.dentistName}>
                      👨‍⚕️ {item.dentist ? item.dentist.fullName : 'Chưa chỉ định bác sĩ'}
                    </Text>
                    <Text style={styles.apptTime}>
                      ⏰ {formatDateTime(item.appointmentTime)}
                    </Text>

                    {item.notes ? (
                      <Text style={styles.apptNotes}>📝 {item.notes}</Text>
                    ) : null}

                    {/* Quick Action Buttons for Staff */}
                    <View style={styles.cardActionRow}>
                      {item.status === 'DEPOSIT_PAID' && (
                        <TouchableOpacity
                          style={styles.btnCardConfirm}
                          onPress={() => handleStatusUpdate(item.id, 'CONFIRMED')}
                        >
                          <Text style={styles.btnCardText}>Xác nhận đón tiếp</Text>
                        </TouchableOpacity>
                      )}

                      {item.status === 'CONFIRMED' && (
                        <TouchableOpacity
                          style={styles.btnCardComplete}
                          onPress={() => handleStatusUpdate(item.id, 'COMPLETED')}
                        >
                          <Text style={styles.btnCardText}>Hoàn tất ca khám</Text>
                        </TouchableOpacity>
                      )}
                    </View>
                  </View>
                );
              }}
            />
          )}
        </View>
      )}

      {/* TAB CONTENT: 2. EMR & ORTHO */}
      {currentTab === 'emr' && (
        <ScrollView style={styles.tabContent} contentContainerStyle={styles.listContainer}>
          <Text style={styles.sectionHeader}>📋 Hồ Sơ Bệnh Án Điện Tử (EMR)</Text>
          {records.map((r) => (
            <View key={r.id} style={styles.emrCard}>
              <Text style={styles.patientName}>Bệnh nhân: {r.patient?.fullName}</Text>
              <Text style={styles.emrDiagnosis}>🩺 Chẩn đoán: {r.diagnosis}</Text>
              <Text style={styles.emrDetail}>💉 Điều trị: {r.treatmentDone}</Text>
              <Text style={styles.emrDetail}>💊 Đơn thuốc: {r.prescription}</Text>
            </View>
          ))}

          <Text style={[styles.sectionHeader, { marginTop: 20 }]}>🦷 Phác Đồ Chỉnh Nha (Niềng Răng)</Text>
          {orthoPlans.map((o) => (
            <View key={o.id} style={styles.emrCard}>
              <Text style={styles.patientName}>{o.patient?.fullName}</Text>
              <Text style={styles.emrDetail}>Loại mắc cài: {o.bracketType}</Text>
              <Text style={styles.emrDetail}>Giai đoạn: {o.currentStage}</Text>
              <Text style={styles.emrDetail}>Tiến trình: {o.completedMonths}/{o.totalEstimatedMonths} tháng</Text>
              <Text style={styles.emrDiagnosis}>Ghi chú bác sĩ: {o.doctorNotes}</Text>
            </View>
          ))}
        </ScrollView>
      )}

      {/* TAB CONTENT: 3. SHIFTS */}
      {currentTab === 'shifts' && (
        <ScrollView style={styles.tabContent} contentContainerStyle={styles.listContainer}>
          <Text style={styles.sectionHeader}>⏱️ Phân Ca Làm Việc Hôm Nay</Text>
          {shifts.map((s) => (
            <View key={s.id} style={styles.emrCard}>
              <Text style={styles.patientName}>{s.staff?.fullName}</Text>
              <Text style={styles.emrDetail}>Ca trực: {s.shiftType}</Text>
              <Text style={styles.emrDetail}>Khu vực: {s.roomOrChair || s.roleTitle || 'Phòng khám'}</Text>
              <Text style={styles.emrDiagnosis}>Nhiệm vụ: {s.dutyDescription || s.notes || 'Ca trực tiêu chuẩn'}</Text>
            </View>
          ))}
        </ScrollView>
      )}

      {/* TAB CONTENT: ATTENDANCE (CHẤM CÔNG GPS & IP) */}
      {currentTab === 'attendance' && (
        <ScrollView style={styles.tabContent} contentContainerStyle={styles.listContainer}>
          <Text style={styles.sectionHeader}>📍 Chấm Công Nhân Sự Thời Gian Thực</Text>
          <View style={[styles.card, { backgroundColor: '#1e293b', marginBottom: 16 }]}>
            <Text style={styles.cardTitle}>Xác Nhận Có Mặt Tại Chi Nhánh</Text>
            <Text style={{ fontSize: 12, color: '#94a3b8', marginBottom: 14 }}>
              Định vị GPS tọa độ phòng khám (10.760624, 106.587106) & xác thực IP mạng nội bộ.
            </Text>

            <View style={{ flexDirection: 'row', gap: 10, marginBottom: 12 }}>
              <TouchableOpacity
                style={[styles.btnPrimary, { flex: 1, backgroundColor: '#059669' }]}
                onPress={() => handleQuickStaffCheckIn(1)}
                disabled={actionLoading}
              >
                <Text style={styles.btnPrimaryText}>🟢 Check-in Vào</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.btnPrimary, { flex: 1, backgroundColor: '#dc2626' }]}
                onPress={() => handleQuickStaffCheckOut(1)}
                disabled={actionLoading}
              >
                <Text style={styles.btnPrimaryText}>🔴 Check-out Ra</Text>
              </TouchableOpacity>
            </View>
            <Text style={{ fontSize: 11, color: '#38bdf8', textAlign: 'center' }}>
              ✓ Tự động xác thực GPS & chống gian lận chấm công
            </Text>
          </View>

          <Text style={[styles.sectionHeader, { marginTop: 12 }]}>📜 Lịch Sử Chấm Công Gần Đây</Text>
          {attendances.length === 0 ? (
            <Text style={styles.emptyText}>Chưa có bản ghi chấm công nào hôm nay</Text>
          ) : (
            attendances.map((a) => (
              <View key={a.id} style={styles.emrCard}>
                <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text style={styles.patientName}>Ca Trực #{a.shift?.id || a.id}</Text>
                  <View style={{ backgroundColor: a.status === 'ON_TIME' ? '#065f46' : '#991b1b', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 8 }}>
                    <Text style={{ color: '#fff', fontSize: 10, fontWeight: '800' }}>{a.status}</Text>
                  </View>
                </View>
                <Text style={styles.emrDetail}>Vào: {a.checkInTime ? new Date(a.checkInTime).toLocaleString('vi-VN') : '--'}</Text>
                <Text style={styles.emrDetail}>Ra: {a.checkOutTime ? new Date(a.checkOutTime).toLocaleString('vi-VN') : 'Đang làm việc'}</Text>
                <Text style={styles.emrDiagnosis}>GPS: {a.isGpsVerified ? '✓ Đã xác thực tọa độ' : 'Chưa định vị'}</Text>
              </View>
            ))
          )}
        </ScrollView>
      )}

      {/* TAB CONTENT: FIELD INTAKE (KHÁM HIỆN TRƯỜNG & HỌC ĐƯỜNG) */}
      {currentTab === 'field_intake' && (
        <ScrollView style={styles.tabContent} contentContainerStyle={styles.listContainer}>
          <Text style={styles.sectionHeader}>📋 Tiếp Nhận Bệnh Nhân Hiện Trường</Text>
          <View style={[styles.card, { backgroundColor: '#1e293b', marginBottom: 16 }]}>
            <Text style={styles.cardTitle}>Phiếu Khám Sơ Bộ Tại Sự Kiện</Text>
            
            <Text style={styles.inputLabel}>Tên sự kiện / Trường học:</Text>
            <TextInput
              style={styles.input}
              value={intakeEventName}
              onChangeText={setIntakeEventName}
              placeholder="VD: THCS Lê Quý Đôn"
              placeholderTextColor="#64748b"
            />

            <Text style={styles.inputLabel}>Họ và tên bệnh nhân / học sinh *:</Text>
            <TextInput
              style={styles.input}
              value={intakePatientName}
              onChangeText={setIntakePatientName}
              placeholder="VD: Trần Minh Quân"
              placeholderTextColor="#64748b"
            />

            <Text style={styles.inputLabel}>Số điện thoại liên hệ *:</Text>
            <TextInput
              style={styles.input}
              value={intakePhone}
              onChangeText={setIntakePhone}
              keyboardType="phone-pad"
              placeholder="VD: 0908112233"
              placeholderTextColor="#64748b"
            />

            <Text style={styles.inputLabel}>Lớp / Khối:</Text>
            <TextInput
              style={styles.input}
              value={intakeClass}
              onChangeText={setIntakeClass}
              placeholder="VD: 7A2"
              placeholderTextColor="#64748b"
            />

            <Text style={styles.inputLabel}>Kết quả tầm soát sơ bộ:</Text>
            <TextInput
              style={[styles.input, { height: 60 }]}
              value={intakeFindings}
              onChangeText={setIntakeFindings}
              multiline
              placeholder="VD: Sâu răng hàm số 46, khớp cắn hở..."
              placeholderTextColor="#64748b"
            />

            <Text style={styles.inputLabel}>Khuyến nghị điều trị:</Text>
            <TextInput
              style={styles.input}
              value={intakeRecommendation}
              onChangeText={setIntakeRecommendation}
              placeholder="VD: Cần hàn răng sâu, khám chỉnh nha"
              placeholderTextColor="#64748b"
            />

            <Text style={styles.inputLabel}>Mã voucher tặng kèm:</Text>
            <TextInput
              style={styles.input}
              value={intakeVoucher}
              onChangeText={setIntakeVoucher}
              placeholder="HOCDUONG100K"
              placeholderTextColor="#64748b"
            />

            <TouchableOpacity
              style={[styles.btnPrimary, { backgroundColor: '#7c3aed' }]}
              onPress={handleSaveFieldLead}
              disabled={actionLoading}
            >
              {actionLoading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.btnPrimaryText}>💾 Lưu Hồ Sơ Hiện Trường</Text>
              )}
            </TouchableOpacity>
          </View>

          <Text style={[styles.sectionHeader, { marginTop: 8 }]}>📂 Danh Sách Hồ Sơ Đã Thu Thập</Text>
          {fieldLeads.length === 0 ? (
            <Text style={styles.emptyText}>Chưa có hồ sơ hiện trường nào</Text>
          ) : (
            fieldLeads.map((l) => (
              <View key={l.id} style={styles.emrCard}>
                <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text style={styles.patientName}>{l.patientFullName || l.patientName}</Text>
                  <View style={{ backgroundColor: '#4338ca', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 8 }}>
                    <Text style={{ color: '#fff', fontSize: 10, fontWeight: '800' }}>{l.leadStatus || l.status}</Text>
                  </View>
                </View>
                <Text style={styles.emrDetail}>📞 SĐT: {l.phone}</Text>
                <Text style={styles.emrDetail}>📍 Sự kiện: {l.eventName}</Text>
                <Text style={styles.emrDiagnosis}>Kết quả: {l.screeningFindings || l.initialComplaint}</Text>
                {l.voucherCode && (
                  <Text style={{ fontSize: 11, color: '#f59e0b', fontWeight: 'bold', marginTop: 4 }}>
                    🎟️ Voucher: {l.voucherCode}
                  </Text>
                )}
              </View>
            ))
          )}
        </ScrollView>
      )}

      {/* TAB CONTENT: 4. OWNER STATS */}
      {currentTab === 'stats' && (
        <ScrollView style={styles.tabContent} contentContainerStyle={styles.listContainer}>
          <Text style={styles.sectionHeader}>📊 Thống Kê Điều Hành Doanh Thu</Text>
          <View style={styles.statBox}>
            <Text style={styles.statLabel}>Doanh Thu Tổng</Text>
            <Text style={styles.statValue}>
              {stats.totalRevenue ? stats.totalRevenue.toLocaleString() + ' đ' : '0 đ'}
            </Text>
          </View>
          <View style={styles.statBox}>
            <Text style={styles.statLabel}>Tổng Ca Khám Đã Đặt</Text>
            <Text style={styles.statValue}>{stats.totalAppointments || 0}</Text>
          </View>
          <View style={styles.statBox}>
            <Text style={styles.statLabel}>Ca Đã Cọc 100K</Text>
            <Text style={styles.statValue}>{stats.depositPaidCount || 0}</Text>
          </View>
        </ScrollView>
      )}

      {/* MODAL PREVIEW & SHARE CSV */}
      <Modal visible={showCsvModal} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={[styles.modalBox, { maxHeight: '80%' }]}>
            <Text style={styles.modalTitle}>📄 Xem Trước File Excel/CSV</Text>
            <Text style={styles.modalSub}>Dữ liệu xuất lịch khám ({appointmentPreset}):</Text>

            <ScrollView style={styles.csvPreviewBox}>
              <Text style={styles.csvText}>{csvContent}</Text>
            </ScrollView>

            <View style={styles.modalBtnRow}>
              <TouchableOpacity
                style={[styles.btnSmall, { backgroundColor: '#334155' }]}
                onPress={() => setShowCsvModal(false)}
              >
                <Text style={styles.btnSmallText}>Đóng</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.btnSmall, { backgroundColor: '#059669' }]}
                onPress={handleShareCsv}
              >
                <Text style={styles.btnSmallText}>📤 Chia Sẻ / Tải Về</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
  },
  loginScroll: {
    padding: 20,
    justifyContent: 'center',
    minHeight: '100%',
  },
  loginHeader: {
    alignItems: 'center',
    marginBottom: 28,
  },
  logoBadge: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#0284c7',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  logoText: {
    fontSize: 32,
  },
  brandTitle: {
    fontSize: 24,
    fontWeight: '900',
    color: '#ffffff',
    letterSpacing: 0.5,
  },
  brandSubtitle: {
    fontSize: 13,
    color: '#94a3b8',
    marginTop: 4,
  },
  card: {
    backgroundColor: '#1e293b',
    borderRadius: 24,
    padding: 20,
    borderWidth: 1,
    borderColor: '#334155',
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: '#ffffff',
    marginBottom: 16,
  },
  inputLabel: {
    fontSize: 12,
    fontWeight: '700',
    color: '#cbd5e1',
    marginBottom: 6,
    textTransform: 'uppercase',
  },
  input: {
    backgroundColor: '#0f172a',
    borderWidth: 1,
    borderColor: '#334155',
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 14,
    color: '#ffffff',
    marginBottom: 14,
  },
  btnPrimary: {
    backgroundColor: '#0284c7',
    borderRadius: 14,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 4,
  },
  btnPrimaryText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '800',
  },
  quickLoginTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: '#94a3b8',
    marginTop: 20,
    marginBottom: 10,
  },
  quickLoginRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 8,
  },
  btnQuick: {
    flex: 1,
    backgroundColor: '#334155',
    paddingVertical: 10,
    borderRadius: 10,
    alignItems: 'center',
  },
  btnQuickText: {
    color: '#e2e8f0',
    fontSize: 12,
    fontWeight: '700',
  },
  serverConfigLink: {
    marginTop: 18,
    alignItems: 'center',
  },
  serverConfigText: {
    color: '#38bdf8',
    fontSize: 11,
    fontFamily: 'monospace',
  },
  topBar: {
    backgroundColor: '#0b1329',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#1e293b',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  topBarTitle: {
    fontSize: 16,
    fontWeight: '900',
    color: '#38bdf8',
  },
  topBarSubtitle: {
    fontSize: 11,
    color: '#94a3b8',
    marginTop: 2,
  },
  btnLogout: {
    backgroundColor: '#334155',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
  },
  btnLogoutText: {
    color: '#f87171',
    fontSize: 11,
    fontWeight: '700',
  },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: '#0f172a',
    borderBottomWidth: 1,
    borderBottomColor: '#1e293b',
  },
  tabItem: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  tabItemActive: {
    borderBottomColor: '#38bdf8',
  },
  tabText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#64748b',
  },
  tabTextActive: {
    color: '#ffffff',
  },
  tabContent: {
    flex: 1,
  },
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 6,
  },
  badgeCount: {
    backgroundColor: '#0369a1',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  badgeCountText: {
    color: '#ffffff',
    fontSize: 11,
    fontWeight: '800',
  },
  btnExport: {
    backgroundColor: '#059669',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 10,
  },
  btnExportText: {
    color: '#ffffff',
    fontSize: 11,
    fontWeight: '800',
  },
  filterScroll: {
    paddingHorizontal: 16,
    marginVertical: 8,
    maxHeight: 38,
  },
  filterPill: {
    backgroundColor: '#1e293b',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 10,
    marginRight: 6,
    borderWidth: 1,
    borderColor: '#334155',
  },
  filterPillActive: {
    backgroundColor: '#0284c7',
    borderColor: '#38bdf8',
  },
  filterPillText: {
    color: '#94a3b8',
    fontSize: 11,
    fontWeight: '700',
  },
  filterPillTextActive: {
    color: '#ffffff',
  },
  listContainer: {
    padding: 16,
    gap: 12,
  },
  apptCard: {
    backgroundColor: '#1e293b',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: '#334155',
  },
  apptHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  apptId: {
    fontSize: 12,
    fontWeight: '900',
    color: '#38bdf8',
    fontFamily: 'monospace',
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 8,
  },
  statusBadgeText: {
    fontSize: 9,
    fontWeight: '900',
  },
  patientName: {
    fontSize: 15,
    fontWeight: '800',
    color: '#ffffff',
  },
  patientPhone: {
    fontSize: 12,
    color: '#94a3b8',
    marginTop: 2,
  },
  divider: {
    height: 1,
    backgroundColor: '#334155',
    marginVertical: 10,
  },
  serviceName: {
    fontSize: 13,
    fontWeight: '700',
    color: '#e2e8f0',
  },
  dentistName: {
    fontSize: 12,
    color: '#cbd5e1',
    marginTop: 4,
  },
  apptTime: {
    fontSize: 12,
    color: '#38bdf8',
    marginTop: 4,
    fontWeight: '700',
  },
  apptNotes: {
    fontSize: 11,
    color: '#94a3b8',
    marginTop: 6,
    fontStyle: 'italic',
  },
  cardActionRow: {
    marginTop: 12,
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
  btnCardConfirm: {
    backgroundColor: '#0284c7',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
  },
  btnCardComplete: {
    backgroundColor: '#059669',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
  },
  btnCardText: {
    color: '#ffffff',
    fontSize: 11,
    fontWeight: '700',
  },
  emptyText: {
    textAlign: 'center',
    color: '#64748b',
    marginTop: 30,
    fontSize: 13,
  },
  sectionHeader: {
    fontSize: 14,
    fontWeight: '800',
    color: '#38bdf8',
    marginBottom: 8,
    textTransform: 'uppercase',
  },
  emrCard: {
    backgroundColor: '#1e293b',
    padding: 14,
    borderRadius: 14,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#334155',
  },
  emrDiagnosis: {
    fontSize: 12,
    fontWeight: '700',
    color: '#fde047',
    marginTop: 4,
  },
  emrDetail: {
    fontSize: 12,
    color: '#cbd5e1',
    marginTop: 3,
  },
  statBox: {
    backgroundColor: '#1e293b',
    padding: 16,
    borderRadius: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#334155',
  },
  statLabel: {
    fontSize: 12,
    color: '#94a3b8',
    fontWeight: '700',
  },
  statValue: {
    fontSize: 22,
    fontWeight: '900',
    color: '#38bdf8',
    marginTop: 4,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalBox: {
    backgroundColor: '#1e293b',
    borderRadius: 20,
    padding: 20,
    width: '100%',
    borderWidth: 1,
    borderColor: '#334155',
  },
  modalTitle: {
    fontSize: 16,
    fontWeight: '800',
    color: '#ffffff',
    marginBottom: 4,
  },
  modalSub: {
    fontSize: 12,
    color: '#94a3b8',
    marginBottom: 14,
  },
  modalBtnRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 8,
    marginTop: 12,
  },
  btnSmall: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 10,
  },
  btnSmallText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '700',
  },
  csvPreviewBox: {
    maxHeight: 200,
    backgroundColor: '#0f172a',
    borderRadius: 10,
    padding: 10,
  },
  csvText: {
    color: '#cbd5e1',
    fontFamily: 'monospace',
    fontSize: 10,
  },
  // Patient Ecosystem Styles
  btnPatientMode: {
    backgroundColor: '#064e3b',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#059669',
  },
  btnPatientModeText: {
    color: '#34d399',
    fontSize: 13,
    fontWeight: '800',
  },
  btnPatientModeSub: {
    color: '#a7f3d0',
    fontSize: 10,
    marginTop: 2,
  },
  patientCard: {
    backgroundColor: '#1e293b',
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#334155',
  },
  patientCardTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: '#ffffff',
    marginBottom: 4,
  },
  patientCardDesc: {
    fontSize: 12,
    color: '#94a3b8',
    lineHeight: 18,
    marginBottom: 10,
  },
  patientPriceText: {
    fontSize: 15,
    fontWeight: '900',
    color: '#34d399',
  },
  btnBookingSmall: {
    backgroundColor: '#059669',
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 10,
  },
  btnBookingSmallText: {
    color: '#ffffff',
    fontSize: 11,
    fontWeight: '800',
  },
  cartFloatingBtn: {
    position: 'absolute',
    bottom: 20,
    right: 20,
    backgroundColor: '#059669',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 25,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    shadowColor: '#059669',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 8,
    elevation: 8,
  },
  cartFloatingText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '800',
  },
  cartBadge: {
    backgroundColor: '#ef4444',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 10,
  },
  cartBadgeText: {
    color: '#ffffff',
    fontSize: 10,
    fontWeight: '900',
  },
  warrantyCertificateCard: {
    backgroundColor: '#0f172a',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1.5,
    borderColor: '#38bdf8',
    marginTop: 12,
  },
  aiRiskPill: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    alignSelf: 'flex-start',
  },
  aiRiskPillText: {
    fontSize: 10,
    fontWeight: '900',
    textTransform: 'uppercase',
  },
});

registerRootComponent(App);