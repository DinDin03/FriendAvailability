/* ===== NOTIFICATIONS SERVICE ===== */
/*
  This file handles user notifications and feedback messages.
  Think of this as your logging and user feedback system in Spring Boot -
  similar to how you might use @Slf4j for logging and flash messages
  for user feedback.

  This centralizes all user notifications including:
  - Success/error/warning/info messages
  - Toast notifications
  - Modal alerts
  - Progress indicators
  - Loading states

  Similar to having a centralized logging and notification service
*/

/**
 * Notification Types
 * Similar to log levels in Spring Boot (DEBUG, INFO, WARN, ERROR)
 */
const NOTIFICATION_TYPES = {
    SUCCESS: 'success',
    ERROR: 'error',
    WARNING: 'warning',
    INFO: 'info',
    LOADING: 'loading'
};

/**
 * Notification Positions
 * Where notifications appear on screen
 */
const NOTIFICATION_POSITIONS = {
    TOP_RIGHT: 'top-right',
    TOP_LEFT: 'top-left',
    TOP_CENTER: 'top-center',
    BOTTOM_RIGHT: 'bottom-right',
    BOTTOM_LEFT: 'bottom-left',
    BOTTOM_CENTER: 'bottom-center'
};

/**
 * Notification Manager
 * Similar to a centralized logging service in Spring Boot
 */
class NotificationManager {
    constructor() {
        this.notifications = new Map(); // Active notifications
        this.notificationId = 0; // Unique ID counter
        this.defaultPosition = NOTIFICATION_POSITIONS.TOP_RIGHT;
        this.defaultDuration = MESSAGE_CONFIG.DISPLAY_DURATION.SUCCESS;
        this.maxNotifications = 5; // Maximum concurrent notifications
        this.container = null;

        // Initialize notification container
        this.initializeContainer();

        debugLog('NotificationManager initialized');
    }

    /**
     * Initialize notification container
     * Creates the DOM container for notifications
     */
    initializeContainer() {
        // Check if container already exists
        this.container = DOMUtils.getElementById('messageContainer');

        if (!this.container) {
            // Create container if it doesn't exist
            this.container = document.createElement('div');
            this.container.id = 'messageContainer';
            this.container.className = 'notification-container';

            // Add to body
            document.body.appendChild(this.container);

            debugLog('Created notification container');
        }

        // Ensure container has proper styling
        this.applyContainerStyles();
    }

    /**
     * Apply styles to notification container
     */
    applyContainerStyles() {
        if (!this.container) return;

        // Apply CSS styles programmatically
        Object.assign(this.container.style, {
            position: 'fixed',
            top: '80px',
            right: '20px',
            zIndex: '9999',
            maxWidth: '400px',
            pointerEvents: 'none',
            display: 'block',
            visibility: 'visible',
            opacity: '1'
        });
    }

    /**
     * Show notification
     * Similar to adding a log entry or flash message
     *
     * @param {string} message - Notification message
     * @param {string} type - Notification type
     * @param {Object} options - Additional options
     * @returns {string} Notification ID
     */
    show(message, type = NOTIFICATION_TYPES.INFO, options = {}) {
        // Validate inputs
        if (!message || typeof message !== 'string') {
            errorLog('Invalid notification message:', message);
            return null;
        }

        // Generate unique ID
        const id = `notification-${++this.notificationId}`;

        // Default options
        const defaultOptions = {
            duration: this.getDurationByType(type),
            position: this.defaultPosition,
            closable: true,
            persistent: false,
            icon: this.getIconByType(type),
            className: '',
            onClick: null,
            onClose: null
        };

        const config = { ...defaultOptions, ...options };

        // Create notification object
        const notification = {
            id,
            message: StringUtils.sanitizeHtml(message),
            type,
            config,
            timestamp: new Date(),
            element: null
        };

        // Check notification limits
        this.enforceNotificationLimit();

        // Create and show notification element
        const element = this.createNotificationElement(notification);
        notification.element = element;

        // Store notification
        this.notifications.set(id, notification);

        // Add to container
        this.container.appendChild(element);

        // Trigger show animation
        requestAnimationFrame(() => {
            element.classList.add('show');
        });

        // Auto-hide if not persistent
        if (!config.persistent && config.duration > 0) {
            setTimeout(() => {
                this.hide(id);
            }, config.duration);
        }

        debugLog(`Notification shown: ${type} - ${message}`, { id, config });

        return id;
    }

    /**
     * Hide notification
     * @param {string} id - Notification ID
     */
    hide(id) {
        const notification = this.notifications.get(id);
        if (!notification) return;

        const element = notification.element;
        if (!element) return;

        // Remove show class to trigger hide animation
        element.classList.remove('show');
        element.classList.add('hide');

        // Remove from DOM after animation
        setTimeout(() => {
            if (element.parentNode) {
                element.parentNode.removeChild(element);
            }

            // Remove from notifications map
            this.notifications.delete(id);

            // Call onClose callback if provided
            if (notification.config.onClose) {
                try {
                    notification.config.onClose(notification);
                } catch (error) {
                    errorLog('Error in notification onClose callback:', error);
                }
            }

            debugLog(`Notification hidden: ${id}`);
        }, 300); // Match CSS animation duration
    }

    /**
     * Create notification DOM element
     * @param {Object} notification - Notification object
     * @returns {HTMLElement} Notification element
     */
    createNotificationElement(notification) {
        const { id, message, type, config } = notification;

        // Create main container
        const element = document.createElement('div');
        element.className = `message message-${type} ${config.className}`.trim();
        element.setAttribute('data-notification-id', id);

        // Create content container
        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';

        // Add icon if specified
        if (config.icon) {
            const iconElement = document.createElement('i');
            iconElement.className = `fas ${config.icon} message-icon`;
            contentDiv.appendChild(iconElement);
        }

        // Add message text
        const textElement = document.createElement('span');
        textElement.className = 'message-text';
        textElement.innerHTML = message;
        contentDiv.appendChild(textElement);

        // Add close button if closable
        if (config.closable) {
            const closeButton = document.createElement('button');
            closeButton.className = 'message-close';
            closeButton.innerHTML = '<i class="fas fa-times"></i>';
            closeButton.setAttribute('aria-label', 'Close notification');

            // Add click handler
            DOMUtils.addEventListener(closeButton, 'click', (e) => {
                e.stopPropagation();
                this.hide(id);
            });

            contentDiv.appendChild(closeButton);
        }

        element.appendChild(contentDiv);

        // Add click handler for entire notification if specified
        if (config.onClick) {
            element.style.cursor = 'pointer';
            DOMUtils.addEventListener(element, 'click', () => {
                try {
                    config.onClick(notification);
                } catch (error) {
                    errorLog('Error in notification onClick callback:', error);
                }
            });
        }

        // Make notification interactive
        element.style.pointerEvents = 'auto';

        return element;
    }

    /**
     * Get default duration by notification type
     * @param {string} type - Notification type
     * @returns {number} Duration in milliseconds
     */
    getDurationByType(type) {
        switch (type) {
            case NOTIFICATION_TYPES.SUCCESS:
                return MESSAGE_CONFIG.DISPLAY_DURATION.SUCCESS;
            case NOTIFICATION_TYPES.ERROR:
                return MESSAGE_CONFIG.DISPLAY_DURATION.ERROR;
            case NOTIFICATION_TYPES.WARNING:
                return MESSAGE_CONFIG.DISPLAY_DURATION.WARNING;
            case NOTIFICATION_TYPES.INFO:
                return MESSAGE_CONFIG.DISPLAY_DURATION.INFO;
            case NOTIFICATION_TYPES.LOADING:
                return 0; // Loading notifications are persistent by default
            default:
                return MESSAGE_CONFIG.DISPLAY_DURATION.INFO;
        }
    }

    /**
     * Get default icon by notification type
     * @param {string} type - Notification type
     * @returns {string} Font Awesome icon class
     */
    getIconByType(type) {
        switch (type) {
            case NOTIFICATION_TYPES.SUCCESS:
                return 'fa-check-circle';
            case NOTIFICATION_TYPES.ERROR:
                return 'fa-exclamation-circle';
            case NOTIFICATION_TYPES.WARNING:
                return 'fa-exclamation-triangle';
            case NOTIFICATION_TYPES.INFO:
                return 'fa-info-circle';
            case NOTIFICATION_TYPES.LOADING:
                return 'fa-spinner fa-spin';
            default:
                return 'fa-info-circle';
        }
    }

    /**
     * Enforce notification limit
     * Removes oldest notifications if limit exceeded
     */
    enforceNotificationLimit() {
        const notificationArray = Array.from(this.notifications.values());

        if (notificationArray.length >= this.maxNotifications) {
            // Sort by timestamp (oldest first)
            notificationArray.sort((a, b) => a.timestamp - b.timestamp);

            // Remove oldest notifications
            const toRemove = notificationArray.slice(0, notificationArray.length - this.maxNotifications + 1);
            toRemove.forEach(notification => {
                this.hide(notification.id);
            });
        }
    }

    /**
     * Clear all notifications
     */
    clearAll() {
        const notificationIds = Array.from(this.notifications.keys());
        notificationIds.forEach(id => this.hide(id));

        debugLog('All notifications cleared');
    }

    /**
     * Get active notifications count
     * @returns {number} Number of active notifications
     */
    getActiveCount() {
        return this.notifications.size;
    }

    /**
     * Check if notification exists
     * @param {string} id - Notification ID
     * @returns {boolean} True if notification exists
     */
    exists(id) {
        return this.notifications.has(id);
    }
}

/**
 * Notification Service
 * High-level API for showing different types of notifications
 * Similar to having service methods for different log levels
 */
class NotificationService {
    constructor() {
        this.manager = new NotificationManager();
        this.loadingNotifications = new Set(); // Track loading notifications

        debugLog('NotificationService initialized');
    }

    /**
     * Show success notification
     * Similar to log.info() in Spring Boot
     * @param {string} message - Success message
     * @param {Object} options - Additional options
     * @returns {string} Notification ID
     */
    success(message, options = {}) {
        return this.manager.show(message, NOTIFICATION_TYPES.SUCCESS, {
            ...options,
            icon: options.icon || 'fa-check-circle'
        });
    }

    /**
     * Show error notification
     * Similar to log.error() in Spring Boot
     * @param {string} message - Error message
     * @param {Object} options - Additional options
     * @returns {string} Notification ID
     */
    error(message, options = {}) {
        return this.manager.show(message, NOTIFICATION_TYPES.ERROR, {
            duration: MESSAGE_CONFIG.DISPLAY_DURATION.ERROR,
            ...options,
            icon: options.icon || 'fa-exclamation-circle'
        });
    }

    /**
     * Show warning notification
     * Similar to log.warn() in Spring Boot
     * @param {string} message - Warning message
     * @param {Object} options - Additional options
     * @returns {string} Notification ID
     */
    warning(message, options = {}) {
        return this.manager.show(message, NOTIFICATION_TYPES.WARNING, {
            ...options,
            icon: options.icon || 'fa-exclamation-triangle'
        });
    }

    /**
     * Show info notification
     * Similar to log.info() in Spring Boot
     * @param {string} message - Info message
     * @param {Object} options - Additional options
     * @returns {string} Notification ID
     */
    info(message, options = {}) {
        return this.manager.show(message, NOTIFICATION_TYPES.INFO, {
            ...options,
            icon: options.icon || 'fa-info-circle'
        });
    }

    /**
     * Show loading notification
     * @param {string} message - Loading message
     * @param {Object} options - Additional options
     * @returns {string} Notification ID
     */
    loading(message, options = {}) {
        const id = this.manager.show(message, NOTIFICATION_TYPES.LOADING, {
            persistent: true,
            closable: false,
            ...options,
            icon: options.icon || 'fa-spinner fa-spin'
        });

        this.loadingNotifications.add(id);
        return id;
    }

    /**
     * Hide specific notification
     * @param {string} id - Notification ID
     */
    hide(id) {
        this.manager.hide(id);
        this.loadingNotifications.delete(id);
    }

    /**
     * Clear all notifications
     */
    clearAll() {
        this.manager.clearAll();
        this.loadingNotifications.clear();
    }

    /**
     * Clear all loading notifications
     */
    clearLoading() {
        this.loadingNotifications.forEach(id => {
            this.hide(id);
        });
    }

    /**
     * Show API result notification
     * Automatically shows success or error based on API response
     * @param {Object} result - API result object
     * @param {Object} messages - Custom messages
     * @returns {string|null} Notification ID
     */
    showApiResult(result, messages = {}) {
        if (result.success) {
            const message = messages.success || 'Operation completed successfully';
            return this.success(message);
        } else {
            const message = messages.error || result.error || 'An error occurred';
            return this.error(message);
        }
    }

    /**
     * Show form validation errors
     * @param {Object} errors - Validation errors object
     * @param {string} title - Error title
     * @returns {string} Notification ID
     */
    showValidationErrors(errors, title = 'Please correct the following errors:') {
        if (!errors || Object.keys(errors).length === 0) return null;

        const errorList = Object.values(errors).map(error => `• ${error}`).join('<br>');
        const message = `<strong>${title}</strong><br>${errorList}`;

        return this.error(message, { duration: 8000 });
    }

    /**
     * Show progress notification with updates
     * @param {string} initialMessage - Initial message
     * @param {Object} options - Additional options
     * @returns {Object} Progress notification controller
     */
    showProgress(initialMessage, options = {}) {
        const id = this.loading(initialMessage, options);

        return {
            id,
            update: (message) => {
                const notification = this.manager.notifications.get(id);
                if (notification && notification.element) {
                    const textElement = notification.element.querySelector('.message-text');
                    if (textElement) {
                        textElement.innerHTML = StringUtils.sanitizeHtml(message);
                    }
                }
            },
            complete: (message = 'Complete!') => {
                this.hide(id);
                return this.success(message);
            },
            error: (message = 'An error occurred') => {
                this.hide(id);
                return this.error(message);
            }
        };
    }

    /**
     * Show confirmation dialog (using notifications)
     * @param {string} message - Confirmation message
     * @param {Function} onConfirm - Confirm callback
     * @param {Function} onCancel - Cancel callback
     * @returns {string} Notification ID
     */
    confirm(message, onConfirm, onCancel = null) {
        const confirmMessage = `
      ${message}
      <div style="margin-top: 12px; display: flex; gap: 8px; justify-content: flex-end;">
        <button class="btn btn-sm btn-primary confirm-yes">Yes</button>
        <button class="btn btn-sm btn-outline confirm-no">No</button>
      </div>
    `;

        const id = this.manager.show(confirmMessage, NOTIFICATION_TYPES.WARNING, {
            persistent: true,
            closable: false,
            duration: 0
        });

        // Add event listeners after element is created
        setTimeout(() => {
            const notification = this.manager.notifications.get(id);
            if (notification && notification.element) {
                const yesBtn = notification.element.querySelector('.confirm-yes');
                const noBtn = notification.element.querySelector('.confirm-no');

                if (yesBtn) {
                    DOMUtils.addEventListener(yesBtn, 'click', () => {
                        this.hide(id);
                        if (onConfirm) onConfirm();
                    });
                }

                if (noBtn) {
                    DOMUtils.addEventListener(noBtn, 'click', () => {
                        this.hide(id);
                        if (onCancel) onCancel();
                    });
                }
            }
        }, 100);

        return id;
    }

    /**
     * Get notification statistics
     * @returns {Object} Statistics object
     */
    getStats() {
        return {
            total: this.manager.getActiveCount(),
            loading: this.loadingNotifications.size,
            byType: Array.from(this.manager.notifications.values()).reduce((acc, notification) => {
                acc[notification.type] = (acc[notification.type] || 0) + 1;
                return acc;
            }, {})
        };
    }
}

/**
 * Create and export notification service instance
 * Similar to having a singleton @Service in Spring Boot
 */
const notificationService = new NotificationService();

/**
 * Convenience functions for global use
 * Similar to having static utility methods
 */
let Notifications = {
    success: (message, options) => notificationService.success(message, options),
    error: (message, options) => notificationService.error(message, options),
    warning: (message, options) => notificationService.warning(message, options),
    info: (message, options) => notificationService.info(message, options),
    loading: (message, options) => notificationService.loading(message, options),
    hide: (id) => notificationService.hide(id),
    clearAll: () => notificationService.clearAll(),
    clearLoading: () => notificationService.clearLoading(),
    showApiResult: (result, messages) => notificationService.showApiResult(result, messages),
    showValidationErrors: (errors, title) => notificationService.showValidationErrors(errors, title),
    showProgress: (message, options) => notificationService.showProgress(message, options),
    confirm: (message, onConfirm, onCancel) => notificationService.confirm(message, onConfirm, onCancel)
};

/**
 * Export notification service
 */
if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment (for testing)
    module.exports = {
        NOTIFICATION_TYPES,
        NOTIFICATION_POSITIONS,
        NotificationManager,
        NotificationService,
        notificationService,
        Notifications
    };
} else {
    // Browser environment - make available globally
    window.NotificationService = notificationService;
    window.Notifications = Notifications;

    // Legacy support for existing showMessage functions
    window.showMessage = Notifications.info;
    window.showSuccessMessage = Notifications.success;
    window.showErrorMessage = Notifications.error;
    window.showWarningMessage = Notifications.warning;
    window.showLoadingMessage = Notifications.loading;
    window.hideLoadingMessage = Notifications.clearLoading;
    window.clearMessages = Notifications.clearAll;
}