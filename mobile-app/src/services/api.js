import { getApiBaseUrl } from '../config';

let authToken = null;
let currentStaffUser = null;

export const setAuthSession = (token, user) => {
  authToken = token;
  currentStaffUser = user;
};

export const getAuthSession = () => ({ token: authToken, user: currentStaffUser });

export const clearAuthSession = () => {
  authToken = null;
  currentStaffUser = null;
};

export const apiCall = async (endpoint, options = {}) => {
  const baseUrl = getApiBaseUrl();
  const headers = options.headers || {};
  
  if (authToken) {
    headers['Authorization'] = 'Bearer ' + authToken;
  }
  
  if (!headers['Content-Type'] && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(baseUrl + endpoint, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    clearAuthSession();
    throw new Error('Hết phiên đăng nhập. Vui lòng đăng nhập lại.');
  }

  return response;
};

// API Services
export const loginApi = async (username, password) => {
  const res = await apiCall('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  const json = await res.json();
  if (res.ok && json.success) {
    setAuthSession(json.data.token, json.data);
    return json.data;
  }
  throw new Error(json.message || 'Đăng nhập thất bại');
};

export const fetchAppointments = async (preset = 'all', startDate = null, endDate = null, dentistId = null) => {
  let url = '/api/appointments/filter?preset=' + encodeURIComponent(preset);
  if (startDate) url += '&startDate=' + startDate;
  if (endDate) url += '&endDate=' + endDate;
  if (dentistId) url += '&dentistId=' + dentistId;

  const res = await apiCall(url);
  const json = await res.json();
  return json.data || [];
};

export const updateAppointmentStatus = async (id, status) => {
  const res = await apiCall('/api/appointments/' + id + '/status?status=' + status, {
    method: 'PATCH',
  });
  const json = await res.json();
  return json.data;
};

export const fetchMedicalRecords = async () => {
  const res = await apiCall('/api/medical-records');
  const json = await res.json();
  return json.data || [];
};

export const fetchOrthodonticPlans = async () => {
  const res = await apiCall('/api/orthodontics');
  const json = await res.json();
  return json.data || [];
};

export const fetchStaffShifts = async () => {
  const res = await apiCall('/api/shifts');
  const json = await res.json();
  return json.data || [];
};

export const fetchDashboardStats = async () => {
  const res = await apiCall('/api/dashboard/stats');
  const json = await res.json();
  return json.data || {};
};

export const fetchExportCsvText = async (preset = 'all') => {
  const res = await apiCall('/api/appointments/export?preset=' + encodeURIComponent(preset));
  return await res.text();
};

// ==========================================
// Milestone 1: Customer Dental Ecosystem APIs
// ==========================================

export const fetchDentalServices = async (category = null) => {
  const url = category && category !== 'ALL' 
    ? `/api/dental-services?category=${encodeURIComponent(category)}`
    : '/api/dental-services';
  const res = await apiCall(url);
  const json = await res.json();
  return json.data || [];
};

export const fetchDentalProducts = async (category = null) => {
  const url = category && category !== 'ALL'
    ? `/api/dental-products?category=${encodeURIComponent(category)}`
    : '/api/dental-products';
  const res = await apiCall(url);
  const json = await res.json();
  return json.data || [];
};

export const createDentalOrder = async (orderPayload) => {
  const res = await apiCall('/api/dental-orders', {
    method: 'POST',
    body: JSON.stringify(orderPayload),
  });
  const json = await res.json();
  if (res.ok && json.success) {
    return json.data;
  }
  throw new Error(json.message || 'Không thể tạo đơn hàng');
};

export const verifyWarrantyApi = async (query) => {
  const res = await apiCall(`/api/warranties/lookup?query=${encodeURIComponent(query)}`);
  const json = await res.json();
  if (res.ok && json.success) {
    return json.data;
  }
  throw new Error(json.message || 'Không tìm thấy thẻ bảo hành');
};

export const runAiDiagnosticApi = async (payload) => {
  const res = await apiCall('/api/dental-ai/diagnose', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  const json = await res.json();
  if (res.ok && json.success) {
    return json.data;
  }
  throw new Error(json.message || 'Không thể chẩn đoán');
};

export const fetchBranchesApi = async () => {
  const res = await apiCall('/api/branches');
  const json = await res.json();
  return json.data || [];
};

export const fetchNearestBranchApi = async (lat, lon) => {
  const res = await apiCall(`/api/branches/nearest?latitude=${lat}&longitude=${lon}`);
  const json = await res.json();
  if (res.ok && json.success) {
    return json.data;
  }
  throw new Error(json.message || 'Không thể tìm chi nhánh gần nhất');
};

export const fetchForumPostsApi = async (category = null) => {
  const url = category && category !== 'ALL'
    ? `/api/forum/posts?category=${encodeURIComponent(category)}`
    : '/api/forum/posts';
  const res = await apiCall(url);
  const json = await res.json();
  return json.data || [];
};

export const createForumPostApi = async (payload) => {
  const res = await apiCall('/api/forum/posts', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  const json = await res.json();
  if (res.ok && json.success) {
    return json.data;
  }
  throw new Error(json.message || 'Không thể đăng bài');
};

export const likeForumPostApi = async (postId) => {
  const res = await apiCall(`/api/forum/posts/${postId}/like`, { method: 'POST' });
  const json = await res.json();
  return json.data;
};

export const createAppointmentApi = async (bookingData) => {
  const res = await apiCall('/api/appointments', {
    method: 'POST',
    body: JSON.stringify(bookingData),
  });
  const json = await res.json();
  if (res.ok && json.success) {
    return json.data;
  }
  throw new Error(json.message || 'Không thể đặt lịch');
};

// Milestone 2: Staff Attendance & Field Intake APIs
export const checkInApi = async (shiftId, latitude = 10.760624, longitude = 106.587106, networkIp = '192.168.1.50') => {
  const res = await apiCall('/api/attendance/check-in', {
    method: 'POST',
    body: JSON.stringify({ shiftId, latitude, longitude, networkIp }),
  });
  const json = await res.json();
  if (res.ok && json.success) {
    return json.data;
  }
  throw new Error(json.message || 'Chấm công vào thất bại');
};

export const checkOutApi = async (shiftId, notes = 'Check-out từ ứng dụng di động') => {
  const res = await apiCall('/api/attendance/check-out', {
    method: 'POST',
    body: JSON.stringify({ shiftId, notes }),
  });
  const json = await res.json();
  if (res.ok && json.success) {
    return json.data;
  }
  throw new Error(json.message || 'Chấm công ra thất bại');
};

export const fetchAttendanceHistoryApi = async () => {
  const res = await apiCall('/api/attendance/my-history');
  const json = await res.json();
  return json.data || [];
};

export const createFieldIntakeApi = async (leadData) => {
  const res = await apiCall('/api/field-intake', {
    method: 'POST',
    body: JSON.stringify(leadData),
  });
  const json = await res.json();
  if (res.ok && json.success) {
    return json.data;
  }
  throw new Error(json.message || 'Tiếp nhận hồ sơ thất bại');
};

export const fetchFieldIntakeLeadsApi = async (eventName = null) => {
  let url = '/api/field-intake';
  if (eventName) url += '?eventName=' + encodeURIComponent(eventName);
  const res = await apiCall(url);
  const json = await res.json();
  return json.data || [];
};