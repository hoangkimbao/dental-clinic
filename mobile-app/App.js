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
} from './src/services/api';
import { getApiBaseUrl, setApiBaseUrl, DEFAULT_SERVER_IP } from './src/config';

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [currentTab, setCurrentTab] = useState('appointments'); // appointments, emr, shifts, stats

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
      loadAppData(data);
    } catch (err) {
      Alert.alert('Lỗi đăng nhập', err.message || 'Không thể kết nối đến máy chủ. Kiểm tra lại IP máy chủ!');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    clearAuthSession();
    setIsLoggedIn(false);
    setCurrentUser(null);
    setAppointments([]);
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
              <Text style={styles.emrDetail}>Khu vực: {s.roomOrChair}</Text>
              <Text style={styles.emrDiagnosis}>Nhiệm vụ: {s.dutyDescription}</Text>
            </View>
          ))}
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
});

registerRootComponent(App);