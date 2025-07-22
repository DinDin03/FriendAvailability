const API_CONFIG = {
    BASE_URL: '/api',
    VERSION: 'v1',
    TIMEOUTS: {
        DEFAULT: 10000,
        UPLOAD: 30000,
        LONG_RUNNING: 60000
    },
    RATE_LIMITS: {
        LOGIN_ATTEMPTS: 5,
        VERIFICATION_EMAILS: 3,
        PASSWORD_RESET: 3
    },
    ENDPOINTS: {
        AUTH: {
            LOGIN: '/auth/login',
            LOGOUT: '/auth/logout',
            REGISTER: '/auth/register',
            CURRENT_USER: '/auth/current-user',
            REFRESH_TOKEN: '/auth/refresh',
            FORGOT_PASSWORD: '/auth/forgot-password',
            RESET_PASSWORD: '/auth/reset-password',
            VERIFY_EMAIL: '/auth/verify-email',
            RESEND_VERIFICATION: '/auth/resend-verification',
            OAUTH_GOOGLE: '/oauth2/authorization/google'
        },
        USERS: {
            BASE: '/users',
            BY_ID: (id) => `/users/${id}`,
            BY_EMAIL: '/users/by-email',
            PROFILE: '/users/profile',
            UPDATE_PROFILE: '/users/profile',
            DELETE_ACCOUNT: (id) => `/users/${id}`
        },
        FRIENDS: {
            BASE: (userId) => `/friends/${userId}`,
            PENDING: (userId) => `/friends/${userId}/pending`,
            SEND_REQUEST: '/friends/request',
            ACCEPT: (requestId, userId) => `/friends/${requestId}/accept?userId=${userId}`,
            REJECT: (requestId, userId) => `/friends/${requestId}/reject?userId=${userId}`,
            REMOVE: '/friends/remove',
            REMOVE_ALL: (userId) => `/friends/${userId}/all`
        },
        CALENDAR: {
            AVAILABILITY: (userId) => `/calendar/${userId}/availability`,
            EVENTS: (userId) => `/calendar/${userId}/events`,
            CREATE_EVENT: '/calendar/events',
            UPDATE_EVENT: (eventId) => `/calendar/events/${eventId}`,
            DELETE_EVENT: (eventId) => `/calendar/events/${eventId}`,
            SYNC_GOOGLE: '/calendar/sync/google'
        },
        CIRCLES: {
            BASE: (userId) => `/circles/${userId}`,
            CREATE: '/circles',
            UPDATE: (circleId) => `/circles/${circleId}`,
            DELETE: (circleId) => `/circles/${circleId}`,
            ADD_MEMBER: (circleId) => `/circles/${circleId}/members`,
            REMOVE_MEMBER: (circleId, memberId) => `/circles/${circleId}/members/${memberId}`
        },
        NOTIFICATIONS: {
            BASE: (userId) => `/notifications/${userId}`,
            MARK_READ: (notificationId) => `/notifications/${notificationId}/read`,
            MARK_ALL_READ: (userId) => `/notifications/${userId}/read-all`,
            DELETE: (notificationId) => `/notifications/${notificationId}`
        }
    },
    STATUS_CODES: {
        OK: 200,
        CREATED: 201,
        NO_CONTENT: 204,
        BAD_REQUEST: 400,
        UNAUTHORIZED: 401,
        FORBIDDEN: 403,
        NOT_FOUND: 404,
        CONFLICT: 409,
        UNPROCESSABLE_ENTITY: 422,
        TOO_MANY_REQUESTS: 429,
        INTERNAL_SERVER_ERROR: 500,
        SERVICE_UNAVAILABLE: 503
    },
    ERROR_CODES: {
        EMAIL_EXISTS: 'EMAIL_EXISTS',
        EMAIL_NOT_VERIFIED: 'EMAIL_NOT_VERIFIED',
        INVALID_CREDENTIALS: 'INVALID_CREDENTIALS',
        TOKEN_EXPIRED: 'TOKEN_EXPIRED',
        RATE_LIMITED: 'RATE_LIMITED',
        EMAIL_SEND_FAILED: 'EMAIL_SEND_FAILED',
        NETWORK_ERROR: 'NETWORK_ERROR',
        VALIDATION_ERROR: 'VALIDATION_ERROR'
    },
    HEADERS: {
        CONTENT_TYPE: 'Content-Type',
        ACCEPT: 'Accept',
        AUTHORIZATION: 'Authorization',
        X_REQUESTED_WITH: 'X-Requested-With'
    },
    CONTENT_TYPES: {
        JSON: 'application/json',
        FORM_DATA: 'multipart/form-data',
        URL_ENCODED: 'application/x-www-form-urlencoded'
    }
};

const MESSAGE_CONFIG = {
    SUCCESS: {
        LOGIN: 'Login successful! Redirecting...',
        LOGOUT: 'Logged out successfully',
        REGISTER: 'Account created successfully! Please check your email.',
        EMAIL_VERIFIED: 'Email verified successfully!',
        PASSWORD_RESET: 'Password reset successfully!',
        FRIEND_REQUEST_SENT: 'Friend request sent!',
        FRIEND_REQUEST_ACCEPTED: 'Friend request accepted!',
        PROFILE_UPDATED: 'Profile updated successfully'
    },
    ERROR: {
        NETWORK: 'Network error. Please check your connection and try again.',
        UNAUTHORIZED: 'Session expired. Please log in again.',
        VALIDATION: 'Please check your input and try again.',
        EMAIL_EXISTS: 'An account with this email already exists.',
        INVALID_CREDENTIALS: 'Invalid email or password.',
        EMAIL_NOT_VERIFIED: 'Please verify your email before logging in.',
        RATE_LIMITED: 'Too many attempts. Please try again later.',
        GENERIC: 'Something went wrong. Please try again.',
        EMAIL_SEND_FAILED: 'Failed to send email. Please try again.',
        TOKEN_EXPIRED: 'Verification link has expired. Please request a new one.'
    },
    LOADING: {
        LOGIN: 'Signing you in...',
        REGISTER: 'Creating your account...',
        EMAIL_VERIFICATION: 'Verifying your email...',
        PASSWORD_RESET: 'Resetting your password...',
        LOADING_DATA: 'Loading...',
        SAVING: 'Saving...',
        SENDING_EMAIL: 'Sending email...'
    },
    DISPLAY_DURATION: {
        SUCCESS: 5000,
        ERROR: 8000,
        WARNING: 6000,
        INFO: 4000
    }
};

const APP_CONFIG = {
    APP_NAME: 'LinkUp',
    VERSION: '1.0.0',
    STORAGE_KEYS: {
        AUTH_TOKEN: 'linkup_auth_token',
        USER_DATA: 'linkup_user_data',
        THEME_PREFERENCE: 'linkup_theme',
        PENDING_VERIFICATION_EMAIL: 'pendingVerificationEmail',
        VERIFICATION_EXPIRY_TIME: 'verificationExpiryTime',
        RESET_EXPIRY_TIME: 'resetExpiryTime',
        REMEMBER_EMAIL: 'rememberEmail'
    },
    UI: {
        ANIMATION_DURATION: 300,
        MODAL_TRANSITION_DURATION: 300,
        MESSAGE_AUTO_HIDE_DELAY: 5000,
        SCROLL_DEBOUNCE_DELAY: 100,
        SEARCH_DEBOUNCE_DELAY: 300
    },
    VALIDATION: {
        EMAIL_REGEX: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
        PASSWORD_MIN_LENGTH: 8,
        NAME_MIN_LENGTH: 2,
        NAME_MAX_LENGTH: 50,
        EMAIL_MAX_LENGTH: 100
    },
    FEATURES: {
        GOOGLE_AUTH_ENABLED: true,
        EMAIL_VERIFICATION_REQUIRED: true,
        PASSWORD_RESET_ENABLED: true,
        REAL_TIME_NOTIFICATIONS: true,
        CALENDAR_SYNC_ENABLED: true,
        CIRCLES_ENABLED: true
    },
    ENV: {
        DEVELOPMENT: window.location.hostname === 'localhost',
        PRODUCTION: window.location.hostname !== 'localhost'
    }
};

function buildApiUrl(endpoint) {
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint.slice(1) : endpoint;
    return `${API_CONFIG.BASE_URL}/${cleanEndpoint}`;
}

function getDefaultHeaders(additionalHeaders = {}) {
    const headers = {
        [API_CONFIG.HEADERS.CONTENT_TYPE]: API_CONFIG.CONTENT_TYPES.JSON,
        [API_CONFIG.HEADERS.ACCEPT]: API_CONFIG.CONTENT_TYPES.JSON,
        [API_CONFIG.HEADERS.X_REQUESTED_WITH]: 'XMLHttpRequest'
    };
    const authToken = localStorage.getItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
    if (authToken) {
        headers[API_CONFIG.HEADERS.AUTHORIZATION] = `Bearer ${authToken}`;
    }
    return { ...headers, ...additionalHeaders };
}

function isDevelopment() {
    return APP_CONFIG.ENV.DEVELOPMENT;
}

function debugLog(message, data = null) {
    if (isDevelopment()) {
        console.log(`[LinkUp Debug] ${message}`, data || '');
    }
}

function errorLog(message, error = null) {
    console.error(`[LinkUp Error] ${message}`, error || '');
    if (!isDevelopment()) {
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        API_CONFIG,
        MESSAGE_CONFIG,
        APP_CONFIG,
        buildApiUrl,
        getDefaultHeaders,
        isDevelopment,
        debugLog,
        errorLog
    };
} else {
    window.LinkUpConfig = {
        API_CONFIG,
        MESSAGE_CONFIG,
        APP_CONFIG,
        buildApiUrl,
        getDefaultHeaders,
        isDevelopment,
        debugLog,
        errorLog
    };
}