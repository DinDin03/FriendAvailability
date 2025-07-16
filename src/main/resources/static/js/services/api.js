class HttpClient {
    constructor(baseURL = API_CONFIG.BASE_URL) {
        this.baseURL = baseURL;
        this.defaultTimeout = API_CONFIG.TIMEOUTS.DEFAULT;
        this.interceptors = {
            request: [],
            response: [],
            error: []
        };

        debugLog('HttpClient initialized with baseURL:', baseURL);
    }

    addRequestInterceptor(interceptor) {
        this.interceptors.request.push(interceptor);
    }

    addResponseInterceptor(interceptor) {
        this.interceptors.response.push(interceptor);
    }

    addErrorInterceptor(interceptor) {
        this.interceptors.error.push(interceptor);
    }

    buildUrl(endpoint) {
        const cleanEndpoint = endpoint.startsWith('/') ? endpoint.slice(1) : endpoint;
        return `${this.baseURL}/${cleanEndpoint}`;
    }

    async applyRequestInterceptors(config) {
        let modifiedConfig = { ...config };

        for (const interceptor of this.interceptors.request) {
            try {
                modifiedConfig = await interceptor(modifiedConfig);
            } catch (error) {
                errorLog('Request interceptor error:', error);
            }
        }

        return modifiedConfig;
    }

    async applyResponseInterceptors(response) {
        let modifiedResponse = response;

        for (const interceptor of this.interceptors.response) {
            try {
                modifiedResponse = await interceptor(modifiedResponse);
            } catch (error) {
                errorLog('Response interceptor error:', error);
            }
        }

        return modifiedResponse;
    }

    async applyErrorInterceptors(error) {
        let modifiedError = error;

        for (const interceptor of this.interceptors.error) {
            try {
                modifiedError = await interceptor(modifiedError);
            } catch (interceptorError) {
                errorLog('Error interceptor error:', interceptorError);
            }
        }

        return modifiedError;
    }

    async request(endpoint, options = {}) {
        const startTime = performance.now();

        try {
            const defaultConfig = {
                method: 'GET',
                headers: getDefaultHeaders(),
                credentials: 'include',
                timeout: this.defaultTimeout
            };

            let config = { ...defaultConfig, ...options };
            config.headers = { ...defaultConfig.headers, ...options.headers };

            config = await this.applyRequestInterceptors(config);

            const url = this.buildUrl(endpoint);

            debugLog(`${config.method} ${url}`, config);

            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), config.timeout);

            config.signal = controller.signal;

            let response = await fetch(url, config);
            clearTimeout(timeoutId);

            response = await this.applyResponseInterceptors(response);

            const result = await this.parseResponse(response);

            const duration = performance.now() - startTime;
            debugLog(`${config.method} ${url} completed in ${duration.toFixed(2)}ms`);

            return result;

        } catch (error) {
            const duration = performance.now() - startTime;
            errorLog(`${options.method || 'GET'} ${endpoint} failed after ${duration.toFixed(2)}ms:`, error);

            const modifiedError = await this.applyErrorInterceptors(error);
            throw modifiedError;
        }
    }

    async parseResponse(response) {
        const contentType = response.headers.get('content-type');

        if (!response.ok) {
            const error = new Error(`HTTP ${response.status}: ${response.statusText}`);
            error.status = response.status;
            error.statusText = response.statusText;

            try {
                if (contentType && contentType.includes('application/json')) {
                    error.data = await response.json();
                } else {
                    error.data = await response.text();
                }
            } catch (parseError) {
                errorLog('Error parsing error response:', parseError);
            }

            throw error;
        }

        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        } else if (contentType && contentType.includes('text/')) {
            return await response.text();
        } else {
            return response;
        }
    }

    async get(endpoint, params = {}, options = {}) {
        const queryString = new URLSearchParams(params).toString();
        const url = queryString ? `${endpoint}?${queryString}` : endpoint;

        return this.request(url, {
            method: 'GET',
            ...options
        });
    }

    async post(endpoint, data = null, options = {}) {
        return this.request(endpoint, {
            method: 'POST',
            body: data ? JSON.stringify(data) : null,
            ...options
        });
    }

    async put(endpoint, data = null, options = {}) {
        return this.request(endpoint, {
            method: 'PUT',
            body: data ? JSON.stringify(data) : null,
            ...options
        });
    }

    async patch(endpoint, data = null, options = {}) {
        return this.request(endpoint, {
            method: 'PATCH',
            body: data ? JSON.stringify(data) : null,
            ...options
        });
    }

    async delete(endpoint, options = {}) {
        return this.request(endpoint, {
            method: 'DELETE',
            ...options
        });
    }

    async upload(endpoint, file, options = {}) {
        const formData = new FormData();
        formData.append('file', file);

        if (options.fields) {
            for (const [key, value] of Object.entries(options.fields)) {
                formData.append(key, value);
            }
        }

        return this.request(endpoint, {
            method: 'POST',
            body: formData,
            headers: {
                ...options.headers
            },
            timeout: API_CONFIG.TIMEOUTS.UPLOAD,
            ...options
        });
    }
}

class AuthService {
    constructor(httpClient) {
        this.http = httpClient;
    }

    async login(credentials) {
        try {
            const response = await this.http.post(API_CONFIG.ENDPOINTS.AUTH.LOGIN, credentials);

            if (response.token) {
                localStorage.setItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN, response.token);
            }

            if (response.user) {
                localStorage.setItem(APP_CONFIG.STORAGE_KEYS.USER_DATA, JSON.stringify(response.user));
            }

            debugLog('Login successful:', response);
            return { success: true, data: response };

        } catch (error) {
            errorLog('Login failed:', error);
            let errorMessage = 'Login failed';
            let errorCode = 'UNKNOWN_ERROR';
            if (error.data) {
                if (typeof error.data === 'string') {
                    try {
                        const parsed = JSON.parse(error.data);
                        errorMessage = parsed.message || errorMessage;
                        errorCode = parsed.errorCode || errorCode;
                    } catch {
                        errorMessage = error.data;
                    }
                } else if (typeof error.data === 'object') {
                    errorMessage = error.data.message || errorMessage;
                    errorCode = error.data.errorCode || errorCode;
                }
            } else if (error.status === 400) {
                errorMessage = 'Invalid input. Please check your email and password.';
                errorCode = 'VALIDATION_ERROR';
            }
            return {
                success: false,
                error: errorMessage,
                errorCode: errorCode
            };
        }
    }

    async register(userData) {
        try {
            const response = await this.http.post(API_CONFIG.ENDPOINTS.AUTH.REGISTER, userData);

            debugLog('Registration successful:', response);
            return { success: true, data: response };

        } catch (error) {
            errorLog('Registration failed:', error);
            return {
                success: false,
                error: error.data?.message || 'Registration failed',
                errorCode: error.data?.errorCode || 'UNKNOWN_ERROR'
            };
        }
    }

    async logout() {
        try {
            await this.http.post(API_CONFIG.ENDPOINTS.AUTH.LOGOUT);

            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.USER_DATA);

            debugLog('Logout successful');
            return { success: true };

        } catch (error) {
            errorLog('Logout failed:', error);
            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.USER_DATA);

            return { success: true };
        }
    }

    async getCurrentUser() {
        try {
            const response = await this.http.get(API_CONFIG.ENDPOINTS.AUTH.CURRENT_USER);

            localStorage.setItem(APP_CONFIG.STORAGE_KEYS.USER_DATA, JSON.stringify(response));

            return { success: true, data: response };

        } catch (error) {
            errorLog('Get current user failed:', error);
            return {
                success: false,
                error: error.data?.message || 'Failed to get user data'
            };
        }
    }

    async forgotPassword(email) {
        try {
            const response = await this.http.post(API_CONFIG.ENDPOINTS.AUTH.FORGOT_PASSWORD, { email });

            return { success: true, data: response };

        } catch (error) {
            errorLog('Forgot password failed:', error);
            return {
                success: false,
                error: error.data?.message || 'Failed to send reset email',
                errorCode: error.data?.errorCode || 'UNKNOWN_ERROR'
            };
        }
    }

    async resetPassword(resetData) {
        try {
            const response = await this.http.post(API_CONFIG.ENDPOINTS.AUTH.RESET_PASSWORD, resetData);

            return { success: true, data: response };

        } catch (error) {
            errorLog('Reset password failed:', error);
            return {
                success: false,
                error: error.data?.message || 'Failed to reset password',
                errorCode: error.data?.errorCode || 'UNKNOWN_ERROR'
            };
        }
    }

    async resendVerification(email) {
        try {
            const response = await this.http.post(API_CONFIG.ENDPOINTS.AUTH.RESEND_VERIFICATION, { email });

            return { success: true, data: response };

        } catch (error) {
            errorLog('Resend verification failed:', error);
            return {
                success: false,
                error: error.data?.message || 'Failed to resend verification email',
                errorCode: error.data?.errorCode || 'UNKNOWN_ERROR'
            };
        }
    }
}

class UserService {
    constructor(httpClient) {
        this.http = httpClient;
    }

    async getUserById(userId) {
        try {
            const response = await this.http.get(API_CONFIG.ENDPOINTS.USERS.BY_ID(userId));
            return { success: true, data: response };
        } catch (error) {
            errorLog('Get user by ID failed:', error);
            return { success: false, error: error.data?.message || 'Failed to get user' };
        }
    }

    async getUserByEmail(email) {
        try {
            const response = await this.http.get(API_CONFIG.ENDPOINTS.USERS.BY_EMAIL, { email });
            return { success: true, data: response };
        } catch (error) {
            errorLog('Get user by email failed:', error);
            return { success: false, error: error.data?.message || 'User not found' };
        }
    }

    async updateProfile(profileData) {
        try {
            const response = await this.http.put(API_CONFIG.ENDPOINTS.USERS.UPDATE_PROFILE, profileData);

            localStorage.setItem(APP_CONFIG.STORAGE_KEYS.USER_DATA, JSON.stringify(response));

            return { success: true, data: response };
        } catch (error) {
            errorLog('Update profile failed:', error);
            return { success: false, error: error.data?.message || 'Failed to update profile' };
        }
    }

    async deleteAccount(userId) {
        try {
            await this.http.delete(API_CONFIG.ENDPOINTS.USERS.DELETE_ACCOUNT(userId));

            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.USER_DATA);

            return { success: true };
        } catch (error) {
            errorLog('Delete account failed:', error);
            return { success: false, error: error.data?.message || 'Failed to delete account' };
        }
    }
}

class FriendsService {
    constructor(httpClient) {
        this.http = httpClient;
    }

    async getFriends(userId) {
        try {
            const response = await this.http.get(API_CONFIG.ENDPOINTS.FRIENDS.BASE(userId));
            return { success: true, data: response };
        } catch (error) {
            errorLog('Get friends failed:', error);
            return { success: false, error: error.data?.message || 'Failed to get friends' };
        }
    }

    async getPendingRequests(userId) {
        try {
            const response = await this.http.get(API_CONFIG.ENDPOINTS.FRIENDS.PENDING(userId));
            return { success: true, data: response };
        } catch (error) {
            errorLog('Get pending requests failed:', error);
            return { success: false, error: error.data?.message || 'Failed to get pending requests' };
        }
    }

    async sendFriendRequest(fromUserId, toUserId) {
        try {
            const response = await this.http.post(
                `${API_CONFIG.ENDPOINTS.FRIENDS.SEND_REQUEST}?fromUserId=${fromUserId}&toUserId=${toUserId}`
            );
            return { success: true, data: response };
        } catch (error) {
            errorLog('Send friend request failed:', error);
            return { success: false, error: error.data?.message || 'Failed to send friend request' };
        }
    }

    async acceptFriendRequest(requestId, userId) {
        try {
            const response = await this.http.put(API_CONFIG.ENDPOINTS.FRIENDS.ACCEPT(requestId, userId));
            return { success: true, data: response };
        } catch (error) {
            errorLog('Accept friend request failed:', error);
            return { success: false, error: error.data?.message || 'Failed to accept friend request' };
        }
    }

    async rejectFriendRequest(requestId, userId) {
        try {
            const response = await this.http.put(API_CONFIG.ENDPOINTS.FRIENDS.REJECT(requestId, userId));
            return { success: true, data: response };
        } catch (error) {
            errorLog('Reject friend request failed:', error);
            return { success: false, error: error.data?.message || 'Failed to reject friend request' };
        }
    }

    async removeFriendship(userId1, userId2) {
        try {
            const response = await this.http.delete(
                `${API_CONFIG.ENDPOINTS.FRIENDS.REMOVE}?userId1=${userId1}&userId2=${userId2}`
            );
            return { success: true, data: response };
        } catch (error) {
            errorLog('Remove friendship failed:', error);
            return { success: false, error: error.data?.message || 'Failed to remove friendship' };
        }
    }
}

class ApiServiceFactory {
    constructor() {
        this.httpClient = new HttpClient();
        this.setupInterceptors();

        this.auth = new AuthService(this.httpClient);
        this.users = new UserService(this.httpClient);
        this.friends = new FriendsService(this.httpClient);
    }

    setupInterceptors() {
        this.httpClient.addRequestInterceptor((config) => {
            const token = localStorage.getItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            if (token) {
                config.headers = config.headers || {};
                config.headers[API_CONFIG.HEADERS.AUTHORIZATION] = `Bearer ${token}`;
            }
            return config;
        });

        this.httpClient.addResponseInterceptor(async (response) => {
            const newToken = response.headers.get('X-New-Token');
            if (newToken) {
                localStorage.setItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN, newToken);
            }
            return response;
        });

        this.httpClient.addErrorInterceptor(async (error) => {
            if (error.status === API_CONFIG.STATUS_CODES.UNAUTHORIZED) {
                localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
                localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.USER_DATA);

                if (!window.location.pathname.includes('login') &&
                    !window.location.pathname.includes('index.html') &&
                    window.location.pathname !== '/') {
                    window.location.href = '/';
                }
            }
            return error;
        });
    }
}

const apiService = new ApiServiceFactory();

if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        HttpClient,
        AuthService,
        UserService,
        FriendsService,
        ApiServiceFactory,
        apiService
    };
} else {
    window.ApiService = apiService;
}