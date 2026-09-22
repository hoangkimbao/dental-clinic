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