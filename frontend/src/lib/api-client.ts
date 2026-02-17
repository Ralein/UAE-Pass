import axios from 'axios';

/**
 * Secure Axios API client.
 * 
 * SECURITY:
 * - withCredentials: true → sends HttpOnly cookies (no localStorage tokens)
 * - CSRF token read from XSRF-TOKEN cookie (double-submit cookie pattern)
 * - Global error interceptor for 401/403/429
 * - No secrets stored in frontend
 */
const apiClient = axios.create({
    baseURL: '/api/v1',
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 15000,
});

// Request interceptor: attach CSRF token from cookie
apiClient.interceptors.request.use((config) => {
    // Read XSRF-TOKEN from cookie (set by Spring Security CookieCsrfTokenRepository)
    const csrfToken = getCookie('XSRF-TOKEN');
    if (csrfToken) {
        config.headers['X-XSRF-TOKEN'] = csrfToken;
    }

    // Add request ID for correlation
    config.headers['X-Request-ID'] = generateRequestId();

    return config;
});

// Response interceptor: global error handling
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    // Session expired — redirect to login
                    if (typeof window !== 'undefined') {
                        window.location.href = '/signup';
                    }
                    break;
                case 429:
                    // Rate limited
                    console.warn('Rate limited. Please wait before retrying.');
                    break;
                case 403:
                    console.warn('Access forbidden.');
                    break;
            }
        }
        return Promise.reject(error);
    }
);

function getCookie(name: string): string | null {
    if (typeof document === 'undefined') return null;
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? decodeURIComponent(match[2]) : null;
}

function generateRequestId(): string {
    return 'req_' + Date.now().toString(36) + '_' + Math.random().toString(36).substring(2, 8);
}

// ===================================
// API Methods
// ===================================

export interface RegistrationStartData {
    emiratesId: string;
    fullName: string;
    email: string;
    phone: string;
    gender: string;
}

export interface OtpVerifyData {
    userId: string;
    otpCode: string;
    channel: 'SMS' | 'EMAIL';
}

export interface PinCreateData {
    userId: string;
    pin: string;
    pinConfirm: string;
}

export const registrationApi = {
    start: (data: RegistrationStartData) =>
        apiClient.post('/registration/start', data),

    getStatus: (userId: string) =>
        apiClient.get(`/registration/status/${userId}`),
};

export const otpApi = {
    send: (userId: string, channel: 'SMS' | 'EMAIL') =>
        apiClient.post('/otp/send', { userId, channel }),

    verify: (data: OtpVerifyData) =>
        apiClient.post('/otp/verify', data),
};

export const pinApi = {
    create: (data: PinCreateData) =>
        apiClient.post('/pin/create', data),
};

export default apiClient;
