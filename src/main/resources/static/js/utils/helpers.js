const DOMUtils = {
    getElementById(id) {
        try {
            return document.getElementById(id);
        } catch (error) {
            errorLog('Error getting element by ID:', error);
            return null;
        }
    },

    querySelector(selector, parent = document) {
        try {
            return parent.querySelector(selector);
        } catch (error) {
            errorLog('Error with querySelector:', error);
            return null;
        }
    },

    querySelectorAll(selector, parent = document) {
        try {
            return parent.querySelectorAll(selector);
        } catch (error) {
            errorLog('Error with querySelectorAll:', error);
            return [];
        }
    },

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

    isElementVisible(element) {
        if (!element) return false;

        const style = window.getComputedStyle(element);
        return style.display !== 'none' &&
            style.visibility !== 'hidden' &&
            style.opacity !== '0';
    },

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
            element.scrollIntoView();
        }
    }
};

const StringUtils = {
    isEmpty(str) {
        return !str || str.trim().length === 0;
    },

    capitalize(str) {
        if (this.isEmpty(str)) return str;
        return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
    },

    toTitleCase(str) {
        if (this.isEmpty(str)) return str;

        return str.toLowerCase().split(' ').map(word =>
            this.capitalize(word)
        ).join(' ');
    },

    truncate(str, maxLength, suffix = '...') {
        if (this.isEmpty(str) || str.length <= maxLength) return str;
        return str.substring(0, maxLength - suffix.length) + suffix;
    },

    generateRandom(length = 10, chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789') {
        let result = '';
        for (let i = 0; i < length; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    },

    sanitizeHtml(str) {
        if (this.isEmpty(str)) return str;

        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    },

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

const DateUtils = {
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

const ObjectUtils = {
    deepClone(obj) {
        if (obj === null || typeof obj !== 'object') return obj;

        try {
            return JSON.parse(JSON.stringify(obj));
        } catch (error) {
            errorLog('Error deep cloning object:', error);
            return obj;
        }
    },

    isEmpty(obj) {
        if (!obj) return true;
        if (Array.isArray(obj)) return obj.length === 0;
        return Object.keys(obj).length === 0;
    },

    getNestedProperty(obj, path, defaultValue = undefined) {
        try {
            return path.split('.').reduce((current, key) =>
                current && current[key] !== undefined ? current[key] : defaultValue, obj
            );
        } catch (error) {
            return defaultValue;
        }
    },

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

const BrowserUtils = {
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

    async copyToClipboard(text) {
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(text);
                return true;
            } else {
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

const PerformanceUtils = {
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

if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        DOMUtils,
        StringUtils,
        DateUtils,
        ObjectUtils,
        BrowserUtils,
        PerformanceUtils
    };
} else {
    window.LinkUpUtils = {
        DOMUtils,
        StringUtils,
        DateUtils,
        ObjectUtils,
        BrowserUtils,
        PerformanceUtils
    };
}