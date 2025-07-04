/* ===== API SERVICE ===== */
/*
  This file contains all HTTP communication logic and API calls.
  Think of this as your @Service classes in Spring Boot that use
  @RestTemplate, @WebClient, or @FeignClient to communicate with other services.

  This centralizes all API communication, handles:
  - HTTP requests and responses
  - Error handling and retry logic
  - Authentication headers
  - Request/response interceptors
  - Timeout management
*/

/**
 * HTTP Client Class
 * Similar to RestTemplate or WebClient in Spring Boot
 */
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

    /**
     * Add request interceptor
     * Similar to ClientHttpRequestInterceptor in Spring Boot
     *
     * @param {Function} interceptor - Request interceptor function
     */
    addRequestInterceptor(interceptor) {
        this.interceptors.request.push(interceptor);
    }

    /**
     * Add response interceptor
     * Similar to ResponseEntityExceptionHandler in Spring Boot
     *
     * @param {Function} interceptor - Response interceptor function
     */
    addResponseInterceptor(interceptor) {
        this.interceptors.response.push(interceptor);
    }

    /**
     * Add error interceptor
     * Similar to @ControllerAdvice error handling
     *
     * @param {Function} interceptor - Error interceptor function
     */
    addErrorInterceptor(interceptor) {
        this.interceptors.error.push(interceptor);
    }

    /**
     * Build full URL
     * @param {string} endpoint - API endpoint
     * @returns {string} Full URL
     */
    buildUrl(endpoint) {
        const cleanEndpoint = endpoint.startsWith('/') ? endpoint.slice(1) : endpoint;
        return `${this.baseURL}/${cleanEndpoint}`;
    }

    /**
     * Apply request interceptors
     * @param {Object} config - Request configuration
     * @returns {Object} Modified configuration
     */
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

    /**
     * Apply response interceptors
     * @param {Response} response - Fetch response
     * @returns {Response} Modified response
     */
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

    /**
     * Apply error interceptors
     * @param {Error} error - Error object
     * @returns {Error} Modified error
     */
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

    /**
     * Make HTTP request
     * Similar to RestTemplate.exchange() in Spring Boot
     *
     * @param {string} endpoint - API endpoint
     * @param {Object} options - Request options
     * @returns {Promise<Object>} API response
     */
    async request(endpoint, options = {}) {
        const startTime = performance.now();

        try {
            // Default configuration
            const defaultConfig = {
                method: 'GET',
                headers: getDefaultHeaders(),
                credentials: 'include',
                timeout: this.defaultTimeout
            };

            // Merge configurations
            let config = { ...defaultConfig, ...options };
            config.headers = { ...defaultConfig.headers, ...options.headers };

            // Apply request interceptors
            config = await this.applyRequestInterceptors(config);

            // Build full URL
            const url = this.buildUrl(endpoint);

            debugLog(`${config.method} ${url}`, config);

            // Create AbortController for timeout
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), config.timeout);

            config.signal = controller.signal;

            // Make the request
            let response = await fetch(url, config);
            clearTimeout(timeoutId);

            // Apply response interceptors
            response = await this.applyResponseInterceptors(response);

            // Parse response
            const result = await this.parseResponse(response);

            const duration = performance.now() - startTime;
            debugLog(`${config.method} ${url} completed in ${duration.toFixed(2)}ms`);

            return result;

        } catch (error) {
            const duration = performance.now() - startTime;
            errorLog(`${options.method || 'GET'} ${endpoint} failed after ${duration.toFixed(2)}ms:`, error);

            // Apply error interceptors
            const modifiedError = await this.applyErrorInterceptors(error);
            throw modifiedError;
        }
    }

    /**
     * Parse response based on content type
     * @param {Response} response - Fetch response
     * @returns {Promise<any>} Parsed response data
     */
    async parseResponse(response) {
        const contentType = response.headers.get('content-type');

        // Handle different response types
        if (!response.ok) {
            const error = new Error(`HTTP ${response.status}: ${response.statusText}`);
            error.status = response.status;
            error.statusText = response.statusText;

            // Try to parse error body
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

        // Parse successful response
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        } else if (contentType && contentType.includes('text/')) {
            return await response.text();
        } else {
            return response;
        }
    }

    /**
     * GET request
     * Similar to RestTemplate.getForObject()
     */
    async get(endpoint, params = {}, options = {}) {
        const queryString = new URLSearchParams(params).toString();
        const url = queryString ? `${endpoint}?${queryString}` : endpoint;

        return this.request(url, {
            method: 'GET',
            ...options
        });
    }

    /**
     * POST request
     * Similar to RestTemplate.postForObject()
     */
    async post(endpoint, data = null, options = {}) {
        return this.request(endpoint, {
            method: 'POST',
            body: data ? JSON.stringify(data) : null,
            ...options
        });
    }

    /**
     * PUT request
     * Similar to RestTemplate.put()
     */
    async put(endpoint, data = null, options = {}) {
        return this.request(endpoint, {
            method: 'PUT',
            body: data ? JSON.stringify(data) : null,
            ...options
        });
    }

    /**
     * PATCH request
     * Similar to RestTemplate.patchForObject()
     */
    async patch(endpoint, data = null, options = {}) {
        return this.request(endpoint, {
            method: 'PATCH',
            body: data ? JSON.stringify(data) : null,
            ...options
        });
    }

    /**
     * DELETE request
     * Similar to RestTemplate.delete()
     */
    async delete(endpoint, options = {}) {
        return this.request(endpoint, {
            method: 'DELETE',
            ...options
        });
    }

    /**
     * Upload file
     * Similar to multipart file upload in Spring Boot
     */
    async upload(endpoint, file, options = {}) {
        const formData = new FormData();
        formData.append('file', file);

        // Add additional form fields if provided
        if (options.fields) {
            for (const [key, value] of Object.entries(options.fields)) {
                formData.append(key, value);
            }
        }

        return this.request(endpoint, {
            method: 'POST',
            body: formData,
            headers: {
                // Don't set Content-Type for FormData - browser will set it with boundary
                ...options.headers
            },
            timeout: API_CONFIG.TIMEOUTS.UPLOAD,
            ...options
        });
    }
}

/**
 * API Service Classes
 * Similar to @Service classes in Spring Boot
 */

/**
 * Authentication Service
 * Similar to AuthService in Spring Boot
 */
class AuthService {
    constructor(httpClient) {
        this.http = httpClient;
    }

    /**
     * User login
     * @param {Object} credentials - Login credentials
     * @returns {Promise<Object>} Login response
     */
    async login(credentials) {
        try {
            const response = await this.http.post(API_CONFIG.ENDPOINTS.AUTH.LOGIN, credentials);

            // Store auth token if provided
            if (response.token) {
                localStorage.setItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN, response.token);
            }

            // Store user data
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

    /**
     * User registration
     * @param {Object} userData - Registration data
     * @returns {Promise<Object>} Registration response
     */
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

    /**
     * User logout
     * @returns {Promise<Object>} Logout response
     */
    async logout() {
        try {
            await this.http.post(API_CONFIG.ENDPOINTS.AUTH.LOGOUT);

            // Clear stored data
            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.USER_DATA);

            debugLog('Logout successful');
            return { success: true };

        } catch (error) {
            errorLog('Logout failed:', error);
            // Clear data anyway
            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.USER_DATA);

            return { success: true }; // Consider logout successful even if server call fails
        }
    }

    /**
     * Get current user
     * @returns {Promise<Object>} Current user data
     */
    async getCurrentUser() {
        try {
            const response = await this.http.get(API_CONFIG.ENDPOINTS.AUTH.CURRENT_USER);

            // Update stored user data
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

    /**
     * Forgot password
     * @param {string} email - Email address
     * @returns {Promise<Object>} Response
     */
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

    /**
     * Reset password
     * @param {Object} resetData - Reset password data
     * @returns {Promise<Object>} Response
     */
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

    /**
     * Resend verification email
     * @param {string} email - Email address
     * @returns {Promise<Object>} Response
     */
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

/**
 * User Service
 * Similar to UserService in Spring Boot
 */
class UserService {
    constructor(httpClient) {
        this.http = httpClient;
    }

    /**
     * Get user by ID
     * @param {number} userId - User ID
     * @returns {Promise<Object>} User data
     */
    async getUserById(userId) {
        try {
            const response = await this.http.get(API_CONFIG.ENDPOINTS.USERS.BY_ID(userId));
            return { success: true, data: response };
        } catch (error) {
            errorLog('Get user by ID failed:', error);
            return { success: false, error: error.data?.message || 'Failed to get user' };
        }
    }

    /**
     * Get user by email
     * @param {string} email - Email address
     * @returns {Promise<Object>} User data
     */
    async getUserByEmail(email) {
        try {
            const response = await this.http.get(API_CONFIG.ENDPOINTS.USERS.BY_EMAIL, { email });
            return { success: true, data: response };
        } catch (error) {
            errorLog('Get user by email failed:', error);
            return { success: false, error: error.data?.message || 'User not found' };
        }
    }

    /**
     * Update user profile
     * @param {Object} profileData - Profile data
     * @returns {Promise<Object>} Updated user data
     */
    async updateProfile(profileData) {
        try {
            const response = await this.http.put(API_CONFIG.ENDPOINTS.USERS.UPDATE_PROFILE, profileData);

            // Update stored user data
            localStorage.setItem(APP_CONFIG.STORAGE_KEYS.USER_DATA, JSON.stringify(response));

            return { success: true, data: response };
        } catch (error) {
            errorLog('Update profile failed:', error);
            return { success: false, error: error.data?.message || 'Failed to update profile' };
        }
    }

    /**
     * Delete user account
     * @param {number} userId - User ID
     * @returns {Promise<Object>} Response
     */
    async deleteAccount(userId) {
        try {
            await this.http.delete(API_CONFIG.ENDPOINTS.USERS.DELETE_ACCOUNT(userId));

            // Clear stored data
            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.USER_DATA);

            return { success: true };
        } catch (error) {
            errorLog('Delete account failed:', error);
            return { success: false, error: error.data?.message || 'Failed to delete account' };
        }
    }
}

/**
 * Friends Service
 * Similar to FriendsService in Spring Boot
 */
class FriendsService {
    constructor(httpClient) {
        this.http = httpClient;
    }

    /**
     * Get user's friends
     * @param {number} userId - User ID
     * @returns {Promise<Object>} Friends list
     */
    async getFriends(userId) {
        try {
            const response = await this.http.get(API_CONFIG.ENDPOINTS.FRIENDS.BASE(userId));
            return { success: true, data: response };
        } catch (error) {
            errorLog('Get friends failed:', error);
            return { success: false, error: error.data?.message || 'Failed to get friends' };
        }
    }

    /**
     * Get pending friend requests
     * @param {number} userId - User ID
     * @returns {Promise<Object>} Pending requests
     */
    async getPendingRequests(userId) {
        try {
            const response = await this.http.get(API_CONFIG.ENDPOINTS.FRIENDS.PENDING(userId));
            return { success: true, data: response };
        } catch (error) {
            errorLog('Get pending requests failed:', error);
            return { success: false, error: error.data?.message || 'Failed to get pending requests' };
        }
    }

    /**
     * Send friend request
     * @param {number} fromUserId - Sender user ID
     * @param {number} toUserId - Recipient user ID
     * @returns {Promise<Object>} Response
     */
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

    /**
     * Accept friend request
     * @param {number} requestId - Request ID
     * @param {number} userId - User ID
     * @returns {Promise<Object>} Response
     */
    async acceptFriendRequest(requestId, userId) {
        try {
            const response = await this.http.put(API_CONFIG.ENDPOINTS.FRIENDS.ACCEPT(requestId, userId));
            return { success: true, data: response };
        } catch (error) {
            errorLog('Accept friend request failed:', error);
            return { success: false, error: error.data?.message || 'Failed to accept friend request' };
        }
    }

    /**
     * Reject friend request
     * @param {number} requestId - Request ID
     * @param {number} userId - User ID
     * @returns {Promise<Object>} Response
     */
    async rejectFriendRequest(requestId, userId) {
        try {
            const response = await this.http.put(API_CONFIG.ENDPOINTS.FRIENDS.REJECT(requestId, userId));
            return { success: true, data: response };
        } catch (error) {
            errorLog('Reject friend request failed:', error);
            return { success: false, error: error.data?.message || 'Failed to reject friend request' };
        }
    }

    /**
     * Remove friendship
     * @param {number} userId1 - First user ID
     * @param {number} userId2 - Second user ID
     * @returns {Promise<Object>} Response
     */
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

/**
 * API Service Factory
 * Similar to @Configuration and @Bean in Spring Boot
 */
class ApiServiceFactory {
    constructor() {
        this.httpClient = new HttpClient();
        this.setupInterceptors();

        // Initialize services
        this.auth = new AuthService(this.httpClient);
        this.users = new UserService(this.httpClient);
        this.friends = new FriendsService(this.httpClient);
    }

    /**
     * Setup HTTP interceptors
     * Similar to WebMvcConfigurer in Spring Boot
     */
    setupInterceptors() {
        // Request interceptor for authentication
        this.httpClient.addRequestInterceptor((config) => {
            const token = localStorage.getItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            if (token) {
                config.headers = config.headers || {};
                config.headers[API_CONFIG.HEADERS.AUTHORIZATION] = `Bearer ${token}`;
            }
            return config;
        });

        // Response interceptor for token refresh
        this.httpClient.addResponseInterceptor(async (response) => {
            // Handle token refresh if needed
            const newToken = response.headers.get('X-New-Token');
            if (newToken) {
                localStorage.setItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN, newToken);
            }
            return response;
        });

        // Error interceptor for global error handling
        this.httpClient.addErrorInterceptor(async (error) => {
            if (error.status === API_CONFIG.STATUS_CODES.UNAUTHORIZED) {
                // Clear auth data and redirect to login
                localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.AUTH_TOKEN);
                localStorage.removeItem(APP_CONFIG.STORAGE_KEYS.USER_DATA);

                // Only redirect if not already on login page
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

/**
 * Create and export API service instance
 * Similar to having a singleton @Service in Spring Boot
 */
const apiService = new ApiServiceFactory();

/**
 * Export API services
 */
if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment (for testing)
    module.exports = {
        HttpClient,
        AuthService,
        UserService,
        FriendsService,
        ApiServiceFactory,
        apiService
    };
} else {
    // Browser environment - make available globally
    window.ApiService = apiService;
}