/* ===== MODAL COMPONENT SYSTEM ===== */
/*
  This file handles modal/dialog component functionality.
  Think of this as your UI component system in Spring Boot - similar to
  how you might have reusable JSP fragments, Thymeleaf templates, or
  React components that can be composed together.

  This provides:
  - Modal lifecycle management (open, close, destroy)
  - Keyboard navigation and accessibility
  - Multiple modal support with z-index management
  - Event handling and callbacks
  - Form integration
  - Responsive behavior

  Similar to having reusable UI components in your view layer
*/

/**
 * Modal Configuration Constants
 */
const MODAL_CONFIG = {
    ANIMATION_DURATION: 300,
    BACKDROP_CLICK_CLOSE: true,
    KEYBOARD_CLOSE: true,
    FOCUS_TRAP: true,
    SCROLL_LOCK: true,
    Z_INDEX_BASE: 2000,
    Z_INDEX_INCREMENT: 10
};

/**
 * Modal Event Types
 * Similar to component lifecycle events
 */
const MODAL_EVENTS = {
    BEFORE_OPEN: 'beforeOpen',
    OPENED: 'opened',
    BEFORE_CLOSE: 'beforeClose',
    CLOSED: 'closed',
    BACKDROP_CLICK: 'backdropClick',
    ESCAPE_KEY: 'escapeKey'
};

/**
 * Modal Manager Class
 * Manages multiple modals and their interactions
 * Similar to a component registry in frontend frameworks
 */
class ModalManager {
    constructor() {
        this.activeModals = new Map(); // Currently active modals
        this.modalStack = []; // Stack for managing multiple modals
        this.currentZIndex = MODAL_CONFIG.Z_INDEX_BASE;
        this.bodyScrollPosition = 0;

        // Setup global event listeners
        this.setupGlobalEventListeners();

        debugLog('ModalManager initialized');
    }

    /**
     * Setup global event listeners
     * Similar to setting up global interceptors
     */
    setupGlobalEventListeners() {
        // Escape key handler
        DOMUtils.addEventListener(document, 'keydown', (e) => {
            if (e.key === 'Escape' && MODAL_CONFIG.KEYBOARD_CLOSE) {
                this.handleEscapeKey(e);
            }
        });

        // Handle browser back button
        DOMUtils.addEventListener(window, 'popstate', () => {
            if (this.modalStack.length > 0) {
                const topModal = this.getTopModal();
                if (topModal) {
                    this.close(topModal.id);
                }
            }
        });
    }

    /**
     * Register a modal
     * @param {Modal} modal - Modal instance
     */
    register(modal) {
        this.activeModals.set(modal.id, modal);
        debugLog(`Modal registered: ${modal.id}`);
    }

    /**
     * Unregister a modal
     * @param {string} modalId - Modal ID
     */
    unregister(modalId) {
        this.activeModals.delete(modalId);
        this.modalStack = this.modalStack.filter(id => id !== modalId);
        debugLog(`Modal unregistered: ${modalId}`);
    }

    /**
     * Open a modal
     * @param {string} modalId - Modal ID
     * @param {Object} options - Open options
     */
    open(modalId, options = {}) {
        const modal = this.activeModals.get(modalId);
        if (!modal) {
            errorLog(`Modal not found: ${modalId}`);
            return false;
        }

        // Check if modal is already open
        if (modal.isOpen) {
            debugLog(`Modal already open: ${modalId}`);
            return true;
        }

        try {
            // Emit before open event
            const beforeOpenEvent = modal.emit(MODAL_EVENTS.BEFORE_OPEN, { modal, options });
            if (beforeOpenEvent.defaultPrevented) {
                debugLog(`Modal open cancelled: ${modalId}`);
                return false;
            }

            // Add to modal stack
            this.modalStack.push(modalId);

            // Set z-index
            this.currentZIndex += MODAL_CONFIG.Z_INDEX_INCREMENT;
            modal.setZIndex(this.currentZIndex);

            // Lock body scroll if this is the first modal
            if (this.modalStack.length === 1 && MODAL_CONFIG.SCROLL_LOCK) {
                this.lockBodyScroll();
            }

            // Open the modal
            modal.open(options);

            debugLog(`Modal opened: ${modalId}`);
            return true;
        } catch (error) {
            errorLog(`Error opening modal ${modalId}:`, error);
            return false;
        }
    }

    /**
     * Close a modal
     * @param {string} modalId - Modal ID
     * @param {Object} result - Close result data
     */
    close(modalId, result = null) {
        const modal = this.activeModals.get(modalId);
        if (!modal) {
            errorLog(`Modal not found: ${modalId}`);
            return false;
        }

        if (!modal.isOpen) {
            debugLog(`Modal already closed: ${modalId}`);
            return true;
        }

        try {
            // Emit before close event
            const beforeCloseEvent = modal.emit(MODAL_EVENTS.BEFORE_CLOSE, { modal, result });
            if (beforeCloseEvent.defaultPrevented) {
                debugLog(`Modal close cancelled: ${modalId}`);
                return false;
            }

            // Remove from modal stack
            this.modalStack = this.modalStack.filter(id => id !== modalId);

            // Close the modal
            modal.close(result);

            // Unlock body scroll if this was the last modal
            if (this.modalStack.length === 0 && MODAL_CONFIG.SCROLL_LOCK) {
                this.unlockBodyScroll();
            }

            debugLog(`Modal closed: ${modalId}`);
            return true;
        } catch (error) {
            errorLog(`Error closing modal ${modalId}:`, error);
            return false;
        }
    }

    /**
     * Close all modals
     */
    closeAll() {
        const modalIds = [...this.modalStack];
        modalIds.forEach(modalId => {
            this.close(modalId);
        });
    }

    /**
     * Get the top (most recent) modal
     * @returns {Modal|null} Top modal instance
     */
    getTopModal() {
        if (this.modalStack.length === 0) return null;
        const topModalId = this.modalStack[this.modalStack.length - 1];
        return this.activeModals.get(topModalId);
    }

    /**
     * Handle escape key press
     * @param {KeyboardEvent} event - Keyboard event
     */
    handleEscapeKey(event) {
        const topModal = this.getTopModal();
        if (topModal && topModal.config.closeOnEscape) {
            event.preventDefault();
            topModal.emit(MODAL_EVENTS.ESCAPE_KEY, { modal: topModal });
            this.close(topModal.id);
        }
    }

    /**
     * Lock body scroll
     */
    lockBodyScroll() {
        this.bodyScrollPosition = window.pageYOffset;
        document.body.style.overflow = 'hidden';
        document.body.style.position = 'fixed';
        document.body.style.top = `-${this.bodyScrollPosition}px`;
        document.body.style.width = '100%';
    }

    /**
     * Unlock body scroll
     */
    unlockBodyScroll() {
        document.body.style.removeProperty('overflow');
        document.body.style.removeProperty('position');
        document.body.style.removeProperty('top');
        document.body.style.removeProperty('width');
        window.scrollTo(0, this.bodyScrollPosition);
    }

    /**
     * Get modal statistics
     * @returns {Object} Modal statistics
     */
    getStats() {
        return {
            totalRegistered: this.activeModals.size,
            openModals: this.modalStack.length,
            topModalId: this.modalStack[this.modalStack.length - 1] || null
        };
    }
}

/**
 * Modal Class
 * Individual modal component
 * Similar to a UI component class in React or Vue
 */
class Modal {
    constructor(id, config = {}) {
        this.id = id;
        this.isOpen = false;
        this.element = null;
        this.backdrop = null;
        this.eventListeners = new Map();
        this.focusableElements = [];
        this.previousFocus = null;

        // Default configuration
        this.config = {
            closeOnEscape: true,
            closeOnBackdrop: MODAL_CONFIG.BACKDROP_CLICK_CLOSE,
            focusTrap: MODAL_CONFIG.FOCUS_TRAP,
            animation: true,
            animationDuration: MODAL_CONFIG.ANIMATION_DURATION,
            className: '',
            size: 'medium', // small, medium, large, fullscreen
            ...config
        };

        // Find or create modal element
        this.initialize();

        // Register with modal manager
        modalManager.register(this);

        debugLog(`Modal created: ${id}`, this.config);
    }

    /**
     * Initialize modal element
     */
    initialize() {
        // Try to find existing modal element
        this.element = DOMUtils.getElementById(this.id);

        if (!this.element) {
            // Create modal element if it doesn't exist
            this.createElement();
        }

        // Setup modal structure
        this.setupModalStructure();

        // Setup event listeners
        this.setupEventListeners();
    }

    /**
     * Create modal element
     */
    createElement() {
        this.element = document.createElement('div');
        this.element.id = this.id;
        this.element.className = 'modal';
        this.element.setAttribute('role', 'dialog');
        this.element.setAttribute('aria-modal', 'true');
        this.element.setAttribute('aria-hidden', 'true');

        document.body.appendChild(this.element);
    }

    /**
     * Setup modal structure
     */
    setupModalStructure() {
        if (!this.element) return;

        // Add size class
        this.element.classList.add(`modal-${this.config.size}`);

        // Add custom class if provided
        if (this.config.className) {
            this.element.classList.add(this.config.className);
        }

        // Ensure modal has proper structure
        if (!this.element.querySelector('.modal-content')) {
            const content = document.createElement('div');
            content.className = 'modal-content';

            // Move existing content into modal-content
            while (this.element.firstChild) {
                content.appendChild(this.element.firstChild);
            }

            this.element.appendChild(content);
        }

        // Find focusable elements
        this.updateFocusableElements();
    }

    /**
     * Setup event listeners
     */
    setupEventListeners() {
        if (!this.element) return;

        // Backdrop click handler
        DOMUtils.addEventListener(this.element, 'click', (e) => {
            if (e.target === this.element && this.config.closeOnBackdrop) {
                this.emit(MODAL_EVENTS.BACKDROP_CLICK, { modal: this });
                modalManager.close(this.id);
            }
        });

        // Close button handler
        const closeButtons = this.element.querySelectorAll('[data-modal-close]');
        closeButtons.forEach(button => {
            DOMUtils.addEventListener(button, 'click', (e) => {
                e.preventDefault();
                modalManager.close(this.id);
            });
        });

        // Form submission handler
        const forms = this.element.querySelectorAll('form');
        forms.forEach(form => {
            DOMUtils.addEventListener(form, 'submit', (e) => {
                this.handleFormSubmit(e, form);
            });
        });

        // Focus trap handler
        if (this.config.focusTrap) {
            DOMUtils.addEventListener(this.element, 'keydown', (e) => {
                this.handleFocusTrap(e);
            });
        }
    }

    /**
     * Update list of focusable elements
     */
    updateFocusableElements() {
        if (!this.element) return;

        const focusableSelectors = [
            'button:not([disabled])',
            'input:not([disabled])',
            'select:not([disabled])',
            'textarea:not([disabled])',
            'a[href]',
            '[tabindex]:not([tabindex="-1"])'
        ].join(', ');

        this.focusableElements = Array.from(
            this.element.querySelectorAll(focusableSelectors)
        );
    }

    /**
     * Handle focus trap
     * @param {KeyboardEvent} event - Keyboard event
     */
    handleFocusTrap(event) {
        if (event.key !== 'Tab') return;

        if (this.focusableElements.length === 0) {
            event.preventDefault();
            return;
        }

        const firstElement = this.focusableElements[0];
        const lastElement = this.focusableElements[this.focusableElements.length - 1];

        if (event.shiftKey) {
            // Shift + Tab (backwards)
            if (document.activeElement === firstElement) {
                event.preventDefault();
                lastElement.focus();
            }
        } else {
            // Tab (forwards)
            if (document.activeElement === lastElement) {
                event.preventDefault();
                firstElement.focus();
            }
        }
    }

    /**
     * Handle form submission
     * @param {Event} event - Submit event
     * @param {HTMLFormElement} form - Form element
     */
    handleFormSubmit(event, form) {
        // Emit form submit event
        const submitEvent = this.emit('formSubmit', {
            modal: this,
            form,
            formData: new FormData(form)
        });

        // If event not handled, prevent default
        if (!submitEvent.defaultPrevented) {
            event.preventDefault();
        }
    }

    /**
     * Open modal
     * @param {Object} options - Open options
     */
    open(options = {}) {
        if (this.isOpen) return;

        // Store previous focus
        this.previousFocus = document.activeElement;

        // Show modal
        this.element.style.display = 'flex';
        this.element.setAttribute('aria-hidden', 'false');

        // Update focusable elements
        this.updateFocusableElements();

        // Apply animation
        if (this.config.animation) {
            requestAnimationFrame(() => {
                this.element.classList.add('active');
            });
        } else {
            this.element.classList.add('active');
        }

        // Set focus to first focusable element
        setTimeout(() => {
            if (this.focusableElements.length > 0) {
                this.focusableElements[0].focus();
            }
        }, this.config.animation ? this.config.animationDuration : 0);

        this.isOpen = true;

        // Emit opened event
        setTimeout(() => {
            this.emit(MODAL_EVENTS.OPENED, { modal: this, options });
        }, this.config.animation ? this.config.animationDuration : 0);
    }

    /**
     * Close modal
     * @param {any} result - Close result
     */
    close(result = null) {
        if (!this.isOpen) return;

        // Remove active class
        this.element.classList.remove('active');

        // Hide modal after animation
        const hideModal = () => {
            this.element.style.display = 'none';
            this.element.setAttribute('aria-hidden', 'true');

            // Restore previous focus
            if (this.previousFocus && DOMUtils.isElementVisible(this.previousFocus)) {
                this.previousFocus.focus();
            }

            this.isOpen = false;

            // Emit closed event
            this.emit(MODAL_EVENTS.CLOSED, { modal: this, result });
        };

        if (this.config.animation) {
            setTimeout(hideModal, this.config.animationDuration);
        } else {
            hideModal();
        }
    }

    /**
     * Set modal z-index
     * @param {number} zIndex - Z-index value
     */
    setZIndex(zIndex) {
        if (this.element) {
            this.element.style.zIndex = zIndex;
        }
    }

    /**
     * Set modal content
     * @param {string|HTMLElement} content - Modal content
     */
    setContent(content) {
        const contentElement = this.element.querySelector('.modal-content');
        if (!contentElement) return;

        if (typeof content === 'string') {
            contentElement.innerHTML = content;
        } else if (content instanceof HTMLElement) {
            contentElement.innerHTML = '';
            contentElement.appendChild(content);
        }

        // Update focusable elements after content change
        this.updateFocusableElements();
    }

    /**
     * Set modal title
     * @param {string} title - Modal title
     */
    setTitle(title) {
        let titleElement = this.element.querySelector('.modal-title');
        if (!titleElement) {
            // Create title element if it doesn't exist
            titleElement = document.createElement('h2');
            titleElement.className = 'modal-title';

            const contentElement = this.element.querySelector('.modal-content');
            if (contentElement) {
                contentElement.insertBefore(titleElement, contentElement.firstChild);
            }
        }

        titleElement.textContent = title;
        this.element.setAttribute('aria-labelledby', titleElement.id || 'modal-title');
    }

    /**
     * Add event listener
     * @param {string} event - Event name
     * @param {Function} handler - Event handler
     */
    on(event, handler) {
        if (!this.eventListeners.has(event)) {
            this.eventListeners.set(event, []);
        }
        this.eventListeners.get(event).push(handler);
    }

    /**
     * Remove event listener
     * @param {string} event - Event name
     * @param {Function} handler - Event handler
     */
    off(event, handler) {
        if (this.eventListeners.has(event)) {
            const handlers = this.eventListeners.get(event);
            const index = handlers.indexOf(handler);
            if (index > -1) {
                handlers.splice(index, 1);
            }
        }
    }

    /**
     * Emit event
     * @param {string} event - Event name
     * @param {Object} data - Event data
     * @returns {Object} Event object
     */
    emit(event, data = {}) {
        const eventObj = {
            type: event,
            defaultPrevented: false,
            preventDefault: function() { this.defaultPrevented = true; },
            ...data
        };

        if (this.eventListeners.has(event)) {
            this.eventListeners.get(event).forEach(handler => {
                try {
                    handler(eventObj);
                } catch (error) {
                    errorLog(`Error in modal event handler for ${event}:`, error);
                }
            });
        }

        return eventObj;
    }

    /**
     * Destroy modal
     */
    destroy() {
        // Close modal if open
        if (this.isOpen) {
            this.close();
        }

        // Remove from DOM
        if (this.element && this.element.parentNode) {
            this.element.parentNode.removeChild(this.element);
        }

        // Unregister from modal manager
        modalManager.unregister(this.id);

        // Clear references
        this.element = null;
        this.eventListeners.clear();
        this.focusableElements = [];

        debugLog(`Modal destroyed: ${this.id}`);
    }
}

/**
 * Modal Factory
 * Convenience functions for creating common modal types
 * Similar to factory methods in Spring Boot
 */
class ModalFactory {
    /**
     * Create confirmation modal
     * @param {Object} options - Confirmation options
     * @returns {Promise} Resolves with user choice
     */
    static confirm(options = {}) {
        const {
            title = 'Confirm Action',
            message = 'Are you sure?',
            confirmText = 'Confirm',
            cancelText = 'Cancel',
            confirmClass = 'btn-primary',
            cancelClass = 'btn-outline'
        } = options;

        return new Promise((resolve) => {
            const modalId = `confirm-modal-${Date.now()}`;
            const content = `
        <div class="modal-header">
          <h2 class="modal-title">${StringUtils.sanitizeHtml(title)}</h2>
        </div>
        <div class="modal-body">
          <p>${StringUtils.sanitizeHtml(message)}</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn ${cancelClass}" data-modal-close>
            ${StringUtils.sanitizeHtml(cancelText)}
          </button>
          <button type="button" class="btn ${confirmClass}" data-action="confirm">
            ${StringUtils.sanitizeHtml(confirmText)}
          </button>
        </div>
      `;

            const modal = new Modal(modalId, { size: 'small' });
            modal.setContent(content);

            // Handle confirm button
            const confirmBtn = modal.element.querySelector('[data-action="confirm"]');
            DOMUtils.addEventListener(confirmBtn, 'click', () => {
                resolve(true);
                modalManager.close(modalId);
            });

            // Handle close events (cancel)
            modal.on(MODAL_EVENTS.CLOSED, () => {
                resolve(false);
                setTimeout(() => modal.destroy(), 100);
            });

            modalManager.open(modalId);
        });
    }

    /**
     * Create alert modal
     * @param {Object} options - Alert options
     * @returns {Promise} Resolves when dismissed
     */
    static alert(options = {}) {
        const {
            title = 'Alert',
            message = '',
            buttonText = 'OK',
            type = 'info'
        } = options;

        return new Promise((resolve) => {
            const modalId = `alert-modal-${Date.now()}`;
            const icon = {
                success: 'fa-check-circle',
                error: 'fa-exclamation-circle',
                warning: 'fa-exclamation-triangle',
                info: 'fa-info-circle'
            }[type] || 'fa-info-circle';

            const content = `
        <div class="modal-header">
          <h2 class="modal-title">
            <i class="fas ${icon}" style="margin-right: 8px; color: var(--${type === 'error' ? 'error' : type === 'warning' ? 'warning' : type === 'success' ? 'success' : 'primary-blue'});"></i>
            ${StringUtils.sanitizeHtml(title)}
          </h2>
        </div>
        <div class="modal-body">
          <p>${StringUtils.sanitizeHtml(message)}</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" data-modal-close>
            ${StringUtils.sanitizeHtml(buttonText)}
          </button>
        </div>
      `;

            const modal = new Modal(modalId, { size: 'small' });
            modal.setContent(content);

            modal.on(MODAL_EVENTS.CLOSED, () => {
                resolve();
                setTimeout(() => modal.destroy(), 100);
            });

            modalManager.open(modalId);
        });
    }

    /**
     * Create loading modal
     * @param {Object} options - Loading options
     * @returns {Object} Loading modal controller
     */
    static loading(options = {}) {
        const {
            title = 'Loading...',
            message = 'Please wait...',
            showProgress = false
        } = options;

        const modalId = `loading-modal-${Date.now()}`;
        const progressHTML = showProgress ?
            '<div class="progress-bar"><div class="progress-fill" style="width: 0%"></div></div>' :
            '<div class="spinner"></div>';

        const content = `
      <div class="modal-body" style="text-align: center; padding: 2rem;">
        <h3 style="margin-bottom: 1rem;">${StringUtils.sanitizeHtml(title)}</h3>
        ${progressHTML}
        <p style="margin-top: 1rem; color: var(--gray-700);">${StringUtils.sanitizeHtml(message)}</p>
      </div>
    `;

        const modal = new Modal(modalId, {
            size: 'small',
            closeOnEscape: false,
            closeOnBackdrop: false
        });
        modal.setContent(content);

        modalManager.open(modalId);

        return {
            update: (newMessage, progress = null) => {
                const messageEl = modal.element.querySelector('p');
                if (messageEl) {
                    messageEl.textContent = newMessage;
                }

                if (progress !== null && showProgress) {
                    const progressFill = modal.element.querySelector('.progress-fill');
                    if (progressFill) {
                        progressFill.style.width = `${Math.max(0, Math.min(100, progress))}%`;
                    }
                }
            },
            close: () => {
                modalManager.close(modalId);
                setTimeout(() => modal.destroy(), 100);
            }
        };
    }
}

/**
 * Create global modal manager instance
 * Similar to having a singleton service in Spring Boot
 */
const modalManager = new ModalManager();

/**
 * Convenience functions for global use
 */
const Modals = {
    // Core modal functions
    open: (modalId, options) => modalManager.open(modalId, options),
    close: (modalId, result) => modalManager.close(modalId, result),
    closeAll: () => modalManager.closeAll(),

    // Modal creation
    create: (id, config) => new Modal(id, config),

    // Factory methods
    confirm: (options) => ModalFactory.confirm(options),
    alert: (options) => ModalFactory.alert(options),
    loading: (options) => ModalFactory.loading(options),

    // Utility functions
    isOpen: (modalId) => {
        const modal = modalManager.activeModals.get(modalId);
        return modal ? modal.isOpen : false;
    },
    getStats: () => modalManager.getStats()
};

/**
 * Export modal system
 */
if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment (for testing)
    module.exports = {
        MODAL_CONFIG,
        MODAL_EVENTS,
        ModalManager,
        Modal,
        ModalFactory,
        modalManager,
        Modals
    };
} else {
    // Browser environment - make available globally
    window.ModalManager = modalManager;
    window.Modal = Modal;
    window.Modals = Modals;

    // Legacy support for existing modal functions
    window.openModal = Modals.open;
    window.closeModal = Modals.close;
    window.openLoginModal = () => Modals.open('loginModal');
    window.openSignupModal = () => Modals.open('signupModal');
}