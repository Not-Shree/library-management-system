import axios from 'axios';
import { isNativeApp } from '../platform.js';

const TOKEN_KEY = 'lms_token';
const SERVER_KEY = 'lms_api_base';

/**
 * Where the backend API lives.
 * - Browser: VITE_API_BASE_URL, default http://localhost:8080/api
 * - Android app: inside the phone "localhost" is the phone itself, so the app uses
 *   VITE_ANDROID_API_BASE_URL, default http://10.0.2.2:8080/api (the Android emulator's
 *   address for your computer). On a real phone the user can change it from the login screen.
 */
export const DEFAULT_API_BASE = isNativeApp
  ? (import.meta.env.VITE_ANDROID_API_BASE_URL || 'http://10.0.2.2:8080/api')
  : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api');

export function getApiBase() {
  return (isNativeApp && localStorage.getItem(SERVER_KEY)) || DEFAULT_API_BASE;
}

/** One axios instance for the whole app. The JWT is attached to every request. */
const api = axios.create({
  baseURL: getApiBase(),
  // Long enough for a free-tier server to wake up from sleep (about a minute).
  timeout: 90000,
});

/** Android app only: point the app at a different backend (saved on the phone). */
export function setApiBase(url) {
  const clean = (url || '').trim().replace(/\/+$/, '');
  if (clean && clean !== DEFAULT_API_BASE) localStorage.setItem(SERVER_KEY, clean);
  else localStorage.removeItem(SERVER_KEY);
  api.defaults.baseURL = getApiBase();
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// A 401 on any call means the token is missing or expired: log out and go to the login page.
let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

api.interceptors.response.use(
  (res) => res,
  (error) => {
    const isLoginCall = error.config?.url?.includes('/auth/login');
    if (error.response?.status === 401 && !isLoginCall) {
      onUnauthorized(error.response.data?.message);
    }
    return Promise.reject(error);
  }
);

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

/** Turns any API error into one readable sentence (plus field errors for forms). */
export function errorMessage(error) {
  const data = error?.response?.data;
  if (data?.fieldErrors) {
    return Object.entries(data.fieldErrors).map(([f, m]) => `${f}: ${m}`).join('; ');
  }
  if (data?.message) return data.message;
  if (error?.code === 'ECONNABORTED') return 'The server took too long to answer. If it was asleep, try again now.';
  if (error?.code === 'ERR_NETWORK') {
    return isNativeApp
      ? `Cannot reach the server at ${getApiBase()}. Check the server address on the login screen.`
      : 'Cannot reach the server. Check that the backend is running.';
  }
  return error?.message || 'Something went wrong';
}

export function fieldErrors(error) {
  return error?.response?.data?.fieldErrors || {};
}

function blobToBase64(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result).split(',')[1]);
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}

/**
 * Downloads a CSV (or any file) from an authenticated endpoint.
 * Browser: normal file download. Android app: saves to the app cache and opens the
 * share sheet, so the user can save it to Files/Drive or open it in Sheets/Excel.
 */
export async function downloadFile(url, params, fallbackName) {
  const res = await api.get(url, { params, responseType: 'blob' });
  const disposition = res.headers['content-disposition'] || '';
  const match = disposition.match(/filename="?([^"]+)"?/);
  const filename = match ? match[1] : fallbackName;

  if (isNativeApp) {
    // Loaded only inside the app, so the browser build doesn't pay for these plugins.
    const [{ Filesystem, Directory }, { Share }] = await Promise.all([
      import('@capacitor/filesystem'), import('@capacitor/share'),
    ]);
    const saved = await Filesystem.writeFile({ path: filename, data: await blobToBase64(res.data), directory: Directory.Cache });
    await Share.share({ title: filename, files: [saved.uri], dialogTitle: 'Save or open the report' });
    return;
  }

  const link = document.createElement('a');
  link.href = URL.createObjectURL(res.data);
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(link.href);
}

export default api;
