// Config for DentalCare Staff App
export const OFFICIAL_DOMAIN_URL = 'https://nhakhoadentalcare.id.vn';
export const DEFAULT_SERVER_IP = '192.168.1.16';
export const DEFAULT_PORT = '8080';

// Default to official custom domain
let currentBaseUrl = OFFICIAL_DOMAIN_URL;

export const getApiBaseUrl = () => currentBaseUrl;

export const setApiBaseUrl = (input, port = '8080') => {
  const clean = input.trim();
  if (clean.startsWith('http://') || clean.startsWith('https://')) {
    currentBaseUrl = clean.replace(/\/$/, '');
  } else {
    currentBaseUrl = 'http://' + clean.replace(/\/$/, '') + ':' + port;
  }
  return currentBaseUrl;
};