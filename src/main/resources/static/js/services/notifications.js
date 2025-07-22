const NOTIFICATION_TYPES = {
    SUCCESS: 'success',
    ERROR: 'error',
    WARNING: 'warning',
    INFO: 'info',
    LOADING: 'loading'
};

const NOTIFICATION_POSITIONS = {
    TOP_RIGHT: 'top-right',
    TOP_LEFT: 'top-left',
    TOP_CENTER: 'top-center',
    BOTTOM_RIGHT: 'bottom-right',
    BOTTOM_LEFT: 'bottom-left',
    BOTTOM_CENTER: 'bottom-center'
};

class NotificationManager {
    constructor() {
        this.notifications = new Map();
        this.notificationId = 0;
        this.defaultPosition = NOTIFICATION_POSITIONS.TOP_RIGHT;
        this.defaultDuration = MESSAGE_CONFIG.DISPLAY_DURATION.SUCCESS;
        this.maxNotifications = 5;
        this.container = null;
        this.initializeContainer();
        debugLog('NotificationManager initialized');
    }

    initializeContainer() {
        this.container = DOMUtils.getElementById('messageContainer');
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.id = 'messageContainer';
            this.container.className = 'notification-container';
            document.body.appendChild(this.container);
            debugLog('Created notification container');
        }
        this.applyContainerStyles();
    }

    applyContainerStyles() {
        if (!this.container) return;
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

    show(message, type = NOTIFICATION_TYPES.INFO, options = {}) {
        if (!message || typeof message !== 'string') {
            errorLog('Invalid notification message:', message);
            return null;
        }
        const id = `notification-${++this.notificationId}`;
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
        const notification = {
            id,
            message: StringUtils.sanitizeHtml(message),
            type,
            config,
            timestamp: new Date(),
            element: null
        };
        this.enforceNotificationLimit();
        const element = this.createNotificationElement(notification);
        notification.element = element;
        this.notifications.set(id, notification);
        this.container.appendChild(element);
        requestAnimationFrame(() => {
            element.classList.add('show');
        });
        if (!config.persistent && config.duration > 0) {
            setTimeout(() => {
                this.hide(id);
            }, config.duration);
        }
        debugLog(`Notification shown: ${type} - ${message}`, { id, config });
        return id;
    }

    hide(id) {
        const notification = this.notifications.get(id);
        if (!notification) return;
        const element = notification.element;
        if (!element) return;
        element.classList.remove('show');
        element.classList.add('hide');
        setTimeout(() => {
            if (element.parentNode) {
                element.parentNode.removeChild(element);
            }
            this.notifications.delete(id);
            if (notification.config.onClose) {
                try {
                    notification.config.onClose(notification);
                } catch (error) {
                    errorLog('Error in notification onClose callback:', error);
                }
            }
            debugLog(`Notification hidden: ${id}`);
        }, 300);
    }

    createNotificationElement(notification) {
        const { id, message, type, config } = notification;
        const element = document.createElement('div');
        element.className = `message message-${type} ${config.className}`.trim();
        element.setAttribute('data-notification-id', id);
        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        if (config.icon) {
            const iconElement = document.createElement('i');
            iconElement.className = `fas ${config.icon} message-icon`;
            contentDiv.appendChild(iconElement);
        }
        const textElement = document.createElement('span');
        textElement.className = 'message-text';
        textElement.innerHTML = message;
        contentDiv.appendChild(textElement);
        if (config.closable) {
            const closeButton = document.createElement('button');
            closeButton.className = 'message-close';
            closeButton.innerHTML = '<i class="fas fa-times"></i>';
            closeButton.setAttribute('aria-label', 'Close notification');
            DOMUtils.addEventListener(closeButton, 'click', (e) => {
                e.stopPropagation();
                this.hide(id);
            });
            contentDiv.appendChild(closeButton);
        }
        element.appendChild(contentDiv);
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
        element.style.pointerEvents = 'auto';
        return element;
    }

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
                return 0;
            default:
                return MESSAGE_CONFIG.DISPLAY_DURATION.INFO;
        }
    }

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

    enforceNotificationLimit() {
        const notificationArray = Array.from(this.notifications.values());
        if (notificationArray.length >= this.maxNotifications) {
            notificationArray.sort((a, b) => a.timestamp - b.timestamp);
            const toRemove = notificationArray.slice(0, notificationArray.length - this.maxNotifications + 1);
            toRemove.forEach(notification => {
                this.hide(notification.id);
            });
        }
    }

    clearAll() {
        const notificationIds = Array.from(this.notifications.keys());
        notificationIds.forEach(id => this.hide(id));
        debugLog('All notifications cleared');
    }

    getActiveCount() {
        return this.notifications.size;
    }

    exists(id) {
        return this.notifications.has(id);
    }
}

class NotificationService {
    constructor() {
        this.manager = new NotificationManager();
        this.loadingNotifications = new Set();
        debugLog('NotificationService initialized');
    }

    success(message, options = {}) {
        return this.manager.show(message, NOTIFICATION_TYPES.SUCCESS, {
            ...options,
            icon: options.icon || 'fa-check-circle'
        });
    }

    error(message, options = {}) {
        return this.manager.show(message, NOTIFICATION_TYPES.ERROR, {
            duration: MESSAGE_CONFIG.DISPLAY_DURATION.ERROR,
            ...options,
            icon: options.icon || 'fa-exclamation-circle'
        });
    }

    warning(message, options = {}) {
        return this.manager.show(message, NOTIFICATION_TYPES.WARNING, {
            ...options,
            icon: options.icon || 'fa-exclamation-triangle'
        });
    }

    info(message, options = {}) {
        return this.manager.show(message, NOTIFICATION_TYPES.INFO, {
            ...options,
            icon: options.icon || 'fa-info-circle'
        });
    }

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

    hide(id) {
        this.manager.hide(id);
        this.loadingNotifications.delete(id);
    }

    clearAll() {
        this.manager.clearAll();
        this.loadingNotifications.clear();
    }

    clearLoading() {
        this.loadingNotifications.forEach(id => {
            this.hide(id);
        });
    }

    showApiResult(result, messages = {}) {
        if (result.success) {
            const message = messages.success || 'Operation completed successfully';
            return this.success(message);
        } else {
            const message = messages.error || result.error || 'An error occurred';
            return this.error(message);
        }
    }

    showValidationErrors(errors, title = 'Please correct the following errors:') {
        if (!errors || Object.keys(errors).length === 0) return null;
        const errorList = Object.values(errors).map(error => `• ${error}`).join('<br>');
        const message = `<strong>${title}</strong><br>${errorList}`;
        return this.error(message, { duration: 8000 });
    }

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

const notificationService = new NotificationService();

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

if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        NOTIFICATION_TYPES,
        NOTIFICATION_POSITIONS,
        NotificationManager,
        NotificationService,
        notificationService,
        Notifications
    };
} else {
    window.NotificationService = notificationService;
    window.Notifications = Notifications;
    window.showMessage = Notifications.info;
    window.showSuccessMessage = Notifications.success;
    window.showErrorMessage = Notifications.error;
    window.showWarningMessage = Notifications.warning;
    window.showLoadingMessage = Notifications.loading;
    window.hideLoadingMessage = Notifications.clearLoading;
    window.clearMessages = Notifications.clearAll;
}