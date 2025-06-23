/**
 * Shared JavaScript Utilities
 * Common functions used across all pages in the LinkUp application
 *
 * This file contains:
 * - Authentication utilities
 * - UI helper functions
 * - API request utilities
 * - Error handling
 * - Loading states
 * - Message display
 */

/**
 * GLOBAL CONFIGURATION
 */
const APP_CONFIG = {
    API_BASE_URL: '/api',
    MESSAGES_TIMEOUT: 5000, // 5 seconds
    LOADING_MIN_TIME: 500,  // Minimum loading time for better UX
    DEFAULT_AVATAR: 'https://via.placeholder.com/40?text=👤'
};

/**
 * AUTHENTICATION UTILITIES
 * Handle user authentication state and redirects
 */

/**
 * Check if user is currently authenticated
 * This calls your backend to verify session validity
 */
async function checkUserAuthentication() {
    try {
        console.log('Checking user authentication...');

        const response = await fetch(`${APP_CONFIG.API_BASE_URL}/auth/current-user`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json',
                'Cache-Control': 'no-cache'
            }
        });

        if (response.ok) {
            const user = await response.json();
            console.log('User is authenticated:', user.name);
            return user;
        } else {
            console.log('User is not authenticated');
            return null;
        }
    } catch (error) {
        console.error('Authentication check failed:', error);
        return null;
    }
}

/**
 * Get current user info (alternative method using your existing endpoint)
 */
async function getCurrentUser() {
    try {
        // Try to get user by checking if we can access protected endpoint
        const response = await fetch(`${APP_CONFIG.API_BASE_URL}/users`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (response.ok) {
            // If we can access users endpoint, we're authenticated
            // For now, we'll need to determine current user differently
            // This is a limitation we'll address later
            return { authenticated: true };
        } else {
            return null;
        }
    } catch (error) {
        console.error('Failed to get current user:', error);
        return null;
    }
}

/**
 * Redirect to login page if not authenticated
 */
function requireAuthentication() {
    return new Promise((resolve, reject) => {
        checkUserAuthentication()
            .then(user => {
                if (user) {
                    resolve(user);
                } else {
                    console.log('Authentication required - redirecting to login');
                    window.location.href = '/index.html';
                    reject(new Error('Authentication required'));
                }
            })
            .catch(error => {
                console.error('Authentication check failed:', error);
                window.location.href = '/index.html';
                reject(error);
            });
    });
}

/**
 * Logout user and redirect to home page
 */
async function logout() {
    try {
        console.log('Logging out user...');

        // Call logout endpoint
        await fetch('/logout', {
            method: 'POST',
            credentials: 'include'
        });

        console.log('User logged out successfully');

        // Redirect to home page
        window.location.href = '/index.html';

    } catch (error) {
        console.error('Logout failed:', error);
        // Still redirect even if logout call fails
        window.location.href = '/index.html';
    }
}

/**
 * UI UTILITY FUNCTIONS
 * Handle loading states, messages, and UI updates
 */

/**
 * Show loading overlay with message
 */
function showLoadingOverlay(message = 'Loading...') {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        const messageElement = overlay.querySelector('p');
        if (messageElement) {
            messageElement.textContent = message;
        }
        overlay.style.display = 'flex';
        console.log('Loading overlay shown:', message);
    }
}

/**
 * Hide loading overlay
 */
function hideLoadingOverlay() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.style.display = 'none';
        console.log('Loading overlay hidden');
    }
}

/**
 * Show success or error message to user
 */
function showMessage(message, type = 'info', duration = APP_CONFIG.MESSAGES_TIMEOUT) {
    console.log(`Showing ${type} message:`, message);

    // Remove any existing messages
    removeExistingMessages();

    // Create message element
    const messageDiv = document.createElement('div');
    messageDiv.className = `message message-${type}`;
    messageDiv.innerHTML = `
        <div class="message-content">
            <span class="message-icon">${getMessageIcon(type)}</span>
            <span class="message-text">${message}</span>
            <button class="message-close" onclick="this.parentElement.parentElement.remove()">×</button>
        </div>
    `;

    // Add to container or create one
    let container = document.getElementById('messageContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'messageContainer';
        container.className = 'message-container';
        document.body.appendChild(container);
    }

    container.appendChild(messageDiv);

    // Auto-remove after duration
    if (duration > 0) {
        setTimeout(() => {
            if (messageDiv.parentElement) {
                messageDiv.remove();
            }
        }, duration);
    }

    // Add animation
    requestAnimationFrame(() => {
        messageDiv.classList.add('message-show');
    });
}

/**
 * Remove all existing messages
 */
function removeExistingMessages() {
    const existingMessages = document.querySelectorAll('.message');
    existingMessages.forEach(message => message.remove());
}

/**
 * Get appropriate icon for message type
 */
function getMessageIcon(type) {
    switch (type) {
        case 'success': return '✅';
        case 'error': return '❌';
        case 'warning': return '⚠️';
        case 'info':
        default: return 'ℹ️';
    }
}

/**
 * Show loading state for specific element
 */
function showElementLoading(elementId, loadingText = 'Loading...') {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = `<div class="loading">${loadingText}</div>`;
    }
}

/**
 * Show error state for specific element
 */
function showElementError(elementId, errorMessage = 'Failed to load data') {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = `<div class="error">${errorMessage}</div>`;
    }
}

/**
 * Show empty state for specific element
 */
function showElementEmpty(elementId, emptyMessage = 'No data available') {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = `<div class="empty-state">${emptyMessage}</div>`;
    }
}

/**
 * API UTILITY FUNCTIONS
 * Handle common API request patterns
 */

/**
 * Make authenticated API request
 */
async function makeApiRequest(endpoint, options = {}) {
    const defaultOptions = {
        credentials: 'include',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        }
    };

    const requestOptions = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers
        }
    };

    const url = endpoint.startsWith('http') ? endpoint : `${APP_CONFIG.API_BASE_URL}${endpoint}`;

    console.log(`Making API request: ${requestOptions.method || 'GET'} ${url}`);

    try {
        const response = await fetch(url, requestOptions);

        // Handle different response types
        if (response.ok) {
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const data = await response.json();
                console.log(`API request successful:`, data);
                return data;
            } else {
                const text = await response.text();
                console.log(`API request successful (text):`, text);
                return text;
            }
        } else {
            const errorText = await response.text();
            const error = new Error(errorText || `HTTP ${response.status}`);
            error.status = response.status;
            throw error;
        }
    } catch (error) {
        console.error(`API request failed:`, error);
        throw error;
    }
}

/**
 * GET request helper
 */
async function apiGet(endpoint) {
    return makeApiRequest(endpoint, { method: 'GET' });
}

/**
 * POST request helper
 */
async function apiPost(endpoint, data = null) {
    const options = { method: 'POST' };
    if (data) {
        options.body = JSON.stringify(data);
    }
    return makeApiRequest(endpoint, options);
}

/**
 * PUT request helper
 */
async function apiPut(endpoint, data = null) {
    const options = { method: 'PUT' };
    if (data) {
        options.body = JSON.stringify(data);
    }
    return makeApiRequest(endpoint, options);
}

/**
 * DELETE request helper
 */
async function apiDelete(endpoint) {
    return makeApiRequest(endpoint, { method: 'DELETE' });
}

/**
 * UTILITY FUNCTIONS
 * General purpose helper functions
 */

/**
 * Format date for display
 */
function formatDate(dateString, options = {}) {
    const defaultOptions = {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    };

    const formatOptions = { ...defaultOptions, ...options };

    try {
        return new Date(dateString).toLocaleDateString('en-US', formatOptions);
    } catch (error) {
        console.error('Date formatting error:', error);
        return 'Invalid date';
    }
}

/**
 * Format time for display
 */
function formatTime(dateString) {
    try {
        return new Date(dateString).toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (error) {
        console.error('Time formatting error:', error);
        return 'Invalid time';
    }
}

/**
 * Get relative time (e.g., "2 hours ago")
 */
function getRelativeTime(dateString) {
    try {
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMins / 60);
        const diffDays = Math.floor(diffHours / 24);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
        if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;

        return formatDate(dateString, { month: 'short', day: 'numeric' });
    } catch (error) {
        console.error('Relative time error:', error);
        return 'Unknown time';
    }
}

/**
 * Debounce function to limit rapid function calls
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Sanitize HTML to prevent XSS attacks
 */
function sanitizeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Get user avatar URL with fallback
 */
function getUserAvatar(user, size = 40) {
    if (user && user.profilePictureUrl) {
        return user.profilePictureUrl;
    }
    return `${APP_CONFIG.DEFAULT_AVATAR}&size=${size}x${size}`;
}

/**
 * Copy text to clipboard
 */
async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        showMessage('Copied to clipboard!', 'success');
        return true;
    } catch (error) {
        console.error('Failed to copy to clipboard:', error);
        showMessage('Failed to copy to clipboard', 'error');
        return false;
    }
}

/**
 * NAVIGATION UTILITIES
 * Handle page navigation and URL parameters
 */

/**
 * Get URL parameter value
 */
function getUrlParameter(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

/**
 * Set URL parameter without page reload
 */
function setUrlParameter(param, value) {
    const url = new URL(window.location);
    url.searchParams.set(param, value);
    window.history.pushState({}, '', url);
}

/**
 * Navigate to page with parameters
 */
function navigateToPage(page, params = {}) {
    const url = new URL(page, window.location.origin);
    Object.keys(params).forEach(key => {
        url.searchParams.set(key, params[key]);
    });
    window.location.href = url.toString();
}

/**
 * INITIALIZATION HELPERS
 * Common initialization tasks
 */

/**
 * Initialize common page elements
 */
function initializeCommonElements() {
    // Add global error handler
    window.addEventListener('error', function(event) {
        console.error('Global error:', event.error);
        showMessage('An unexpected error occurred', 'error');
    });

    // Add global unhandled promise rejection handler
    window.addEventListener('unhandledrejection', function(event) {
        console.error('Unhandled promise rejection:', event.reason);
        showMessage('An unexpected error occurred', 'error');
        event.preventDefault();
    });

    console.log('Common elements initialized');
}

/**
 * Initialize page on DOM content loaded
 */
function initializePage(pageInitFunction) {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            initializeCommonElements();
            pageInitFunction();
        });
    } else {
        initializeCommonElements();
        pageInitFunction();
    }
}

/**
 * EXPORT FUNCTIONS
 * Make functions available globally
 */

// Authentication functions
window.checkUserAuthentication = checkUserAuthentication;
window.getCurrentUser = getCurrentUser;
window.requireAuthentication = requireAuthentication;
window.logout = logout;

// UI functions
window.showLoadingOverlay = showLoadingOverlay;
window.hideLoadingOverlay = hideLoadingOverlay;
window.showMessage = showMessage;
window.showElementLoading = showElementLoading;
window.showElementError = showElementError;
window.showElementEmpty = showElementEmpty;

// API functions
window.makeApiRequest = makeApiRequest;
window.apiGet = apiGet;
window.apiPost = apiPost;
window.apiPut = apiPut;
window.apiDelete = apiDelete;

// Utility functions
window.formatDate = formatDate;
window.formatTime = formatTime;
window.getRelativeTime = getRelativeTime;
window.debounce = debounce;
window.sanitizeHtml = sanitizeHtml;
window.getUserAvatar = getUserAvatar;
window.copyToClipboard = copyToClipboard;

// Navigation functions
window.getUrlParameter = getUrlParameter;
window.setUrlParameter = setUrlParameter;
window.navigateToPage = navigateToPage;

// Initialization functions
window.initializeCommonElements = initializeCommonElements;
window.initializePage = initializePage;

console.log('Shared utilities loaded successfully');