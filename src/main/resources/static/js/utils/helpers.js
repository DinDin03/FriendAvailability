/* ===== UTILITY HELPERS ===== */
/*
  This file contains utility functions used throughout the application.
  Think of this as your @Component utility classes in Spring Boot -
  reusable methods that provide common functionality.

  Similar to having utility classes like:
  - StringUtils
  - DateUtils
  - ValidationUtils
  - FileUtils

  These functions are pure (no side effects) and can be easily tested.
*/

/**
 * DOM Manipulation Utilities
 * Similar to having DOM service methods in your frontend architecture
 */
const DOMUtils = {
    /**
     * Safely get element by ID
     * @param {string} id - Element ID
     * @returns {HTMLElement|null} Element or null if not found
     */
    getElementById(id) {
        try {
            return document.getElementById(id);
        } catch (error) {
            errorLog('Error getting element by ID:', error);
            return null;
        }
    },

    /**
     * Safely query selector
     * @param {string} selector - CSS selector
     * @param {HTMLElement} parent - Parent element (defaults to document)
     * @returns {HTMLElement|null} Element or null if not found
     */
    querySelector(selector, parent = document) {
        try {
            return parent.querySelector(selector);
        } catch (error) {
            errorLog('Error with querySelector:', error);
            return null;
        }
    },

    /**
     * Safely query all selector
     * @param {string} selector - CSS selector
     * @param {HTMLElement} parent - Parent element (defaults to document)
     * @returns {NodeList} NodeList of elements
     */
    querySelectorAll(selector, parent = document) {
        try {
            return parent.querySelectorAll(selector);
        } catch (error) {
            errorLog('Error with querySelectorAll:', error);
            return [];
        }
    },

    /**
     * Add event listener with error handling
     * @param {HTMLElement} element - Element to add listener to
     * @param {string} event - Event type
     * @param {Function} handler - Event handler
     * @param {Object} options - Event listener options
     */
    addEventListener(element, event, handler, options = {}) {
        if (!element || typeof handler !== 'function') {
            errorLog('Invalid parameters for addEventListener');
            return;
        }

        try {
            element.addEventListener(event, handler, options);
        } catch (error) {
            errorLog('Error adding event listener:', error);
        }
    },

    /**
     * Remove event listener safely
     * @param {HTMLElement} element - Element to remove listener from
     * @param {string} event - Event type
     * @param {Function} handler - Event handler
     */
    removeEventListener(element, event, handler) {
        if (!element || typeof handler !== 'function') {
            return;
        }

        try {
            element.removeEventListener(event, handler);
        } catch (error) {
            errorLog('Error removing event listener:', error);
        }
    },

    /**
     * Check if element exists and is visible
     * @param {HTMLElement} element - Element to check
     * @returns {boolean} True if element exists and is visible
     */
    isElementVisible(element) {
        if (!element) return false;

        const style = window.getComputedStyle(element);
        return style.display !== 'none' &&
            style.visibility !== 'hidden' &&
            style.opacity !== '0';
    },

    /**
     * Scroll element into view smoothly
     * @param {HTMLElement} element - Element to scroll to
     * @param {Object} options - Scroll options
     */
    scrollIntoView(element, options = {}) {
        if (!element) return;

        const defaultOptions = {
            behavior: 'smooth',
            block: 'start',
            inline: 'nearest'
        };

        try {
            element.scrollIntoView({ ...defaultOptions, ...options });
        } catch (error) {
            // Fallback for older browsers
            element.scrollIntoView();
        }
    }
};

/**
 * String Utilities
 * Similar to StringUtils in Apache Commons or your own utility classes
 */
const StringUtils = {
    /**
     * Check if string is empty or null
     * @param {string} str - String to check
     * @returns {boolean} True if empty or null
     */
    isEmpty(str) {
        return !str || str.trim().length === 0;
    },

    /**
     * Capitalize first letter of string
     * @param {string} str - String to capitalize
     * @returns {string} Capitalized string
     */
    capitalize(str) {
        if (this.isEmpty(str)) return str;
        return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
    },

    /**
     * Convert string to title case
     * @param {string} str - String to convert
     * @returns {string} Title case string
     */
    toTitleCase(str) {
        if (this.isEmpty(str)) return str;

        return str.toLowerCase().split(' ').map(word =>
            this.capitalize(word)
        ).join(' ');
    },

    /**
     * Truncate string with ellipsis
     * @param {string} str - String to truncate
     * @param {number} maxLength - Maximum length
     * @param {string} suffix - Suffix to add (default: '...')
     * @returns {string} Truncated string
     */
    truncate(str, maxLength, suffix = '...') {
        if (this.isEmpty(str) || str.length <= maxLength) return str;
        return str.substring(0, maxLength - suffix.length) + suffix;
    },

    /**
     * Generate random string
     * @param {number} length - Length of string
     * @param {string} chars - Characters to use
     * @returns {string} Random string
     */
    generateRandom(length = 10, chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789') {
        let result = '';
        for (let i = 0; i < length; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    },

    /**
     * Sanitize string for HTML output
     * @param {string} str - String to sanitize
     * @returns {string} Sanitized string
     */
    sanitizeHtml(str) {
        if (this.isEmpty(str)) return str;

        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    },

    /**
     * Extract initials from name
     * @param {string} name - Full name
     * @param {number} maxInitials - Maximum number of initials
     * @returns {string} Initials
     */
    getInitials(name, maxInitials = 2) {
        if (this.isEmpty(name)) return '';

        return name
            .split(' ')
            .filter(word => word.length > 0)
            .slice(0, maxInitials)
            .map(word => word.charAt(0).toUpperCase())
            .join('');
    }
};

/**
 * Date and Time Utilities
 * Similar to having DateTimeUtils in your Spring Boot application
 */
const DateUtils = {
    /**
     * Format date for display
     * @param {Date|string} date - Date to format
     * @param {Object} options - Formatting options
     * @returns {string} Formatted date string
     */
    formatDate(date, options = {}) {
        try {
            const dateObj = date instanceof Date ? date : new Date(date);

            const defaultOptions = {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            };

            return dateObj.toLocaleDateString('en-US', { ...defaultOptions, ...options });
        } catch (error) {
            errorLog('Error formatting date:', error);
            return 'Invalid Date';
        }
    },

    /**
     * Format date and time for display
     * @param {Date|string} date - Date to format
     * @param {Object} options - Formatting options
     * @returns {string} Formatted datetime string
     */
    formatDateTime(date, options = {}) {
        try {
            const dateObj = date instanceof Date ? date : new Date(date);

            const defaultOptions = {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            };

            return dateObj.toLocaleDateString('en-US', { ...defaultOptions, ...options });
        } catch (error) {
            errorLog('Error formatting datetime:', error);
            return 'Invalid Date';
        }
    },

    /**
     * Get relative time (e.g., "2 hours ago")
     * @param {Date|string} date - Date to compare
     * @returns {string} Relative time string
     */
    getRelativeTime(date) {
        try {
            const dateObj = date instanceof Date ? date : new Date(date);
            const now = new Date();
            const diffInSeconds = Math.floor((now - dateObj) / 1000);

            if (diffInSeconds < 60) return 'just now';
            if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} minutes ago`;
            if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hours ago`;
            if (diffInSeconds < 2592000) return `${Math.floor(diffInSeconds / 86400)} days ago`;

            return this.formatDate(dateObj);
        } catch (error) {
            errorLog('Error getting relative time:', error);
            return 'unknown';
        }
    },

    /**
     * Check if date is today
     * @param {Date|string} date - Date to check
     * @returns {boolean} True if date is today
     */
    isToday(date) {
        try {
            const dateObj = date instanceof Date ? date : new Date(date);
            const today = new Date();

            return dateObj.getDate() === today.getDate() &&
                dateObj.getMonth() === today.getMonth() &&
                dateObj.getFullYear() === today.getFullYear();
        } catch (error) {
            return false;
        }
    },

    /**
     * Add days to date
     * @param {Date|string} date - Base date
     * @param {number} days - Number of days to add
     * @returns {Date} New date
     */
    addDays(date, days) {
        try {
            const dateObj = date instanceof Date ? new Date(date) : new Date(date);
            dateObj.setDate(dateObj.getDate() + days);
            return dateObj;
        } catch (error) {
            errorLog('Error adding days to date:', error);
            return new Date();
        }
    }
};

/**
 * Array and Object Utilities
 * Similar to CollectionUtils or custom utility methods
 */
const ObjectUtils = {
    /**
     * Deep clone an object
     * @param {Object} obj - Object to clone
     * @returns {Object} Cloned object
     */
    deepClone(obj) {
        if (obj === null || typeof obj !== 'object') return obj;

        try {
            return JSON.parse(JSON.stringify(obj));
        } catch (error) {
            errorLog('Error deep cloning object:', error);
            return obj;
        }
    },

    /**
     * Check if object is empty
     * @param {Object} obj - Object to check
     * @returns {boolean} True if empty
     */
    isEmpty(obj) {
        if (!obj) return true;
        if (Array.isArray(obj)) return obj.length === 0;
        return Object.keys(obj).length === 0;
    },

    /**
     * Get nested property safely
     * @param {Object} obj - Object to search
     * @param {string} path - Property path (e.g., 'user.profile.name')
     * @param {any} defaultValue - Default value if not found
     * @returns {any} Property value or default
     */
    getNestedProperty(obj, path, defaultValue = undefined) {
        try {
            return path.split('.').reduce((current, key) =>
                current && current[key] !== undefined ? current[key] : defaultValue, obj
            );
        } catch (error) {
            return defaultValue;
        }
    },

    /**
     * Remove duplicates from array
     * @param {Array} array - Array to deduplicate
     * @param {string} key - Key to compare for objects
     * @returns {Array} Deduplicated array
     */
    removeDuplicates(array, key = null) {
        if (!Array.isArray(array)) return [];

        if (key) {
            const seen = new Set();
            return array.filter(item => {
                const value = this.getNestedProperty(item, key);
                if (seen.has(value)) return false;
                seen.add(value);
                return true;
            });
        }

        return [...new Set(array)];
    },

    /**
     * Group array by property
     * @param {Array} array - Array to group
     * @param {string} key - Property to group by
     * @returns {Object} Grouped object
     */
    groupBy(array, key) {
        if (!Array.isArray(array)) return {};

        return array.reduce((groups, item) => {
            const group = this.getNestedProperty(item, key);
            if (!groups[group]) groups[group] = [];
            groups[group].push(item);
            return groups;
        }, {});
    }
};

/**
 * Browser and Device Utilities
 * Similar to having environment detection in your backend
 */
const BrowserUtils = {
    /**
     * Check if browser supports feature
     * @param {string} feature - Feature to check
     * @returns {boolean} True if supported
     */
    supportsFeature(feature) {
        const features = {
            localStorage: typeof Storage !== 'undefined',
            sessionStorage: typeof sessionStorage !== 'undefined',
            webSockets: typeof WebSocket !== 'undefined',
            geolocation: 'geolocation' in navigator,
            notifications: 'Notification' in window,
            serviceWorker: 'serviceWorker' in navigator,
            webRTC: 'RTCPeerConnection' in window
        };

        return features[feature] || false;
    },

    /**
     * Get browser information
     * @returns {Object} Browser info
     */
    getBrowserInfo() {
        const userAgent = navigator.userAgent;

        return {
            userAgent,
            isChrome: userAgent.includes('Chrome'),
            isFirefox: userAgent.includes('Firefox'),
            isSafari: userAgent.includes('Safari') && !userAgent.includes('Chrome'),
            isEdge: userAgent.includes('Edge'),
            isMobile: /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent),
            isTablet: /iPad|Android(?!.*Mobile)/i.test(userAgent),
            isDesktop: !/Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent)
        };
    },

    /**
     * Copy text to clipboard
     * @param {string} text - Text to copy
     * @returns {Promise<boolean>} True if successful
     */
    async copyToClipboard(text) {
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(text);
                return true;
            } else {
                // Fallback for older browsers
                const textArea = document.createElement('textarea');
                textArea.value = text;
                textArea.style.position = 'fixed';
                textArea.style.left = '-999999px';
                textArea.style.top = '-999999px';
                document.body.appendChild(textArea);
                textArea.focus();
                textArea.select();

                const result = document.execCommand('copy');
                document.body.removeChild(textArea);
                return result;
            }
        } catch (error) {
            errorLog('Error copying to clipboard:', error);
            return false;
        }
    }
};

/**
 * Performance and Timing Utilities
 * Similar to having performance monitoring in your backend services
 */
const PerformanceUtils = {
    /**
     * Debounce function execution
     * @param {Function} func - Function to debounce
     * @param {number} wait - Wait time in milliseconds
     * @param {boolean} immediate - Execute immediately on first call
     * @returns {Function} Debounced function
     */
    debounce(func, wait, immediate = false) {
        let timeout;

        return function executedFunction(...args) {
            const later = () => {
                timeout = null;
                if (!immediate) func(...args);
            };

            const callNow = immediate && !timeout;
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);

            if (callNow) func(...args);
        };
    },

    /**
     * Throttle function execution
     * @param {Function} func - Function to throttle
     * @param {number} limit - Time limit in milliseconds
     * @returns {Function} Throttled function
     */
    throttle(func, limit) {
        let lastFunc;
        let lastRan;

        return function(...args) {
            if (!lastRan) {
                func(...args);
                lastRan = Date.now();
            } else {
                clearTimeout(lastFunc);
                lastFunc = setTimeout(() => {
                    if ((Date.now() - lastRan) >= limit) {
                        func(...args);
                        lastRan = Date.now();
                    }
                }, limit - (Date.now() - lastRan));
            }
        };
    },

    /**
     * Measure function execution time
     * @param {Function} func - Function to measure
     * @param {...any} args - Function arguments
     * @returns {Object} Result and execution time
     */
    async measureExecutionTime(func, ...args) {
        const start = performance.now();

        try {
            const result = await func(...args);
            const end = performance.now();

            return {
                result,
                executionTime: end - start,
                success: true
            };
        } catch (error) {
            const end = performance.now();

            return {
                error,
                executionTime: end - start,
                success: false
            };
        }
    }
};

/**
 * Export utilities for use in other modules
 * Similar to having @Component utility beans in Spring Boot
 */
if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment (for testing)
    module.exports = {
        DOMUtils,
        StringUtils,
        DateUtils,
        ObjectUtils,
        BrowserUtils,
        PerformanceUtils
    };
} else {
    // Browser environment - make available globally
    window.LinkUpUtils = {
        DOMUtils,
        StringUtils,
        DateUtils,
        ObjectUtils,
        BrowserUtils,
        PerformanceUtils
    };
}