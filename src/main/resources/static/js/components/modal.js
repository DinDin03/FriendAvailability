const MODAL_CONFIG = {
    ANIMATION_DURATION: 300,
    BACKDROP_CLICK_CLOSE: true,
    KEYBOARD_CLOSE: true,
    FOCUS_TRAP: true,
    SCROLL_LOCK: true,
    Z_INDEX_BASE: 2000,
    Z_INDEX_INCREMENT: 10
};

const MODAL_EVENTS = {
    BEFORE_OPEN: 'beforeOpen',
    OPENED: 'opened',
    BEFORE_CLOSE: 'beforeClose',
    CLOSED: 'closed',
    BACKDROP_CLICK: 'backdropClick',
    ESCAPE_KEY: 'escapeKey'
};

class ModalManager {
    constructor() {
        this.activeModals = new Map();
        this.modalStack = [];
        this.currentZIndex = MODAL_CONFIG.Z_INDEX_BASE;
        this.bodyScrollPosition = 0;

        this.setupGlobalEventListeners();

        debugLog('ModalManager initialized');
    }

    setupGlobalEventListeners() {
        DOMUtils.addEventListener(document, 'keydown', (e) => {
            if (e.key === 'Escape' && MODAL_CONFIG.KEYBOARD_CLOSE) {
                this.handleEscapeKey(e);
            }
        });

        DOMUtils.addEventListener(window, 'popstate', () => {
            if (this.modalStack.length > 0) {
                const topModal = this.getTopModal();
                if (topModal) {
                    this.close(topModal.id);
                }
            }
        });
    }

    register(modal) {
        this.activeModals.set(modal.id, modal);
        debugLog(`Modal registered: ${modal.id}`);
    }

    unregister(modalId) {
        this.activeModals.delete(modalId);
        this.modalStack = this.modalStack.filter(id => id !== modalId);
        debugLog(`Modal unregistered: ${modalId}`);
    }

    open(modalId, options = {}) {
        const modal = this.activeModals.get(modalId);
        if (!modal) {
            errorLog(`Modal not found: ${modalId}`);
            return false;
        }

        if (modal.isOpen) {
            debugLog(`Modal already open: ${modalId}`);
            return true;
        }

        try {
            const beforeOpenEvent = modal.emit(MODAL_EVENTS.BEFORE_OPEN, { modal, options });
            if (beforeOpenEvent.defaultPrevented) {
                debugLog(`Modal open cancelled: ${modalId}`);
                return false;
            }

            this.modalStack.push(modalId);

            this.currentZIndex += MODAL_CONFIG.Z_INDEX_INCREMENT;
            modal.setZIndex(this.currentZIndex);

            if (this.modalStack.length === 1 && MODAL_CONFIG.SCROLL_LOCK) {
                this.lockBodyScroll();
            }

            modal.open(options);

            debugLog(`Modal opened: ${modalId}`);
            return true;
        } catch (error) {
            errorLog(`Error opening modal ${modalId}:`, error);
            return false;
        }
    }

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
            const beforeCloseEvent = modal.emit(MODAL_EVENTS.BEFORE_CLOSE, { modal, result });
            if (beforeCloseEvent.defaultPrevented) {
                debugLog(`Modal close cancelled: ${modalId}`);
                return false;
            }

            this.modalStack = this.modalStack.filter(id => id !== modalId);

            modal.close(result);

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

    closeAll() {
        const modalIds = [...this.modalStack];
        modalIds.forEach(modalId => {
            this.close(modalId);
        });
    }

    getTopModal() {
        if (this.modalStack.length === 0) return null;
        const topModalId = this.modalStack[this.modalStack.length - 1];
        return this.activeModals.get(topModalId);
    }

    handleEscapeKey(event) {
        const topModal = this.getTopModal();
        if (topModal && topModal.config.closeOnEscape) {
            event.preventDefault();
            topModal.emit(MODAL_EVENTS.ESCAPE_KEY, { modal: topModal });
            this.close(topModal.id);
        }
    }

    lockBodyScroll() {
        this.bodyScrollPosition = window.pageYOffset;
        document.body.style.overflow = 'hidden';
        document.body.style.position = 'fixed';
        document.body.style.top = `-${this.bodyScrollPosition}px`;
        document.body.style.width = '100%';
    }

    unlockBodyScroll() {
        document.body.style.removeProperty('overflow');
        document.body.style.removeProperty('position');
        document.body.style.removeProperty('top');
        document.body.style.removeProperty('width');
        window.scrollTo(0, this.bodyScrollPosition);
    }

    getStats() {
        return {
            totalRegistered: this.activeModals.size,
            openModals: this.modalStack.length,
            topModalId: this.modalStack[this.modalStack.length - 1] || null
        };
    }
}

class Modal {
    constructor(id, config = {}) {
        this.id = id;
        this.isOpen = false;
        this.element = null;
        this.backdrop = null;
        this.eventListeners = new Map();
        this.focusableElements = [];
        this.previousFocus = null;

        this.config = {
            closeOnEscape: true,
            closeOnBackdrop: MODAL_CONFIG.BACKDROP_CLICK_CLOSE,
            focusTrap: MODAL_CONFIG.FOCUS_TRAP,
            animation: true,
            animationDuration: MODAL_CONFIG.ANIMATION_DURATION,
            className: '',
            size: 'medium',
            ...config
        };

        this.initialize();

        modalManager.register(this);

        debugLog(`Modal created: ${id}`, this.config);
    }

    initialize() {
        this.element = DOMUtils.getElementById(this.id);

        if (!this.element) {
            this.createElement();
        }

        this.setupModalStructure();

        this.setupEventListeners();
    }

    createElement() {
        this.element = document.createElement('div');
        this.element.id = this.id;
        this.element.className = 'modal';
        this.element.setAttribute('role', 'dialog');
        this.element.setAttribute('aria-modal', 'true');
        this.element.setAttribute('aria-hidden', 'true');

        document.body.appendChild(this.element);
    }

    setupModalStructure() {
        if (!this.element) return;

        this.element.classList.add(`modal-${this.config.size}`);

        if (this.config.className) {
            this.element.classList.add(this.config.className);
        }

        if (!this.element.querySelector('.modal-content')) {
            const content = document.createElement('div');
            content.className = 'modal-content';

            while (this.element.firstChild) {
                content.appendChild(this.element.firstChild);
            }

            this.element.appendChild(content);
        }

        this.updateFocusableElements();
    }

    setupEventListeners() {
        if (!this.element) return;

        DOMUtils.addEventListener(this.element, 'click', (e) => {
            if (e.target === this.element && this.config.closeOnBackdrop) {
                this.emit(MODAL_EVENTS.BACKDROP_CLICK, { modal: this });
                modalManager.close(this.id);
            }
        });

        const closeButtons = this.element.querySelectorAll('[data-modal-close]');
        closeButtons.forEach(button => {
            DOMUtils.addEventListener(button, 'click', (e) => {
                e.preventDefault();
                modalManager.close(this.id);
            });
        });

        const forms = this.element.querySelectorAll('form');
        forms.forEach(form => {
            DOMUtils.addEventListener(form, 'submit', (e) => {
                this.handleFormSubmit(e, form);
            });
        });

        if (this.config.focusTrap) {
            DOMUtils.addEventListener(this.element, 'keydown', (e) => {
                this.handleFocusTrap(e);
            });
        }
    }

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

    handleFocusTrap(event) {
        if (event.key !== 'Tab') return;

        if (this.focusableElements.length === 0) {
            event.preventDefault();
            return;
        }

        const firstElement = this.focusableElements[0];
        const lastElement = this.focusableElements[this.focusableElements.length - 1];

        if (event.shiftKey) {
            if (document.activeElement === firstElement) {
                event.preventDefault();
                lastElement.focus();
            }
        } else {
            if (document.activeElement === lastElement) {
                event.preventDefault();
                firstElement.focus();
            }
        }
    }

    handleFormSubmit(event, form) {
        const submitEvent = this.emit('formSubmit', {
            modal: this,
            form,
            formData: new FormData(form)
        });

        if (!submitEvent.defaultPrevented) {
            event.preventDefault();
        }
    }

    open(options = {}) {
        if (this.isOpen) return;

        this.previousFocus = document.activeElement;

        this.element.style.display = 'flex';
        this.element.setAttribute('aria-hidden', 'false');

        this.updateFocusableElements();

        if (this.config.animation) {
            requestAnimationFrame(() => {
                this.element.classList.add('active');
            });
        } else {
            this.element.classList.add('active');
        }

        setTimeout(() => {
            if (this.focusableElements.length > 0) {
                this.focusableElements[0].focus();
            }
        }, this.config.animation ? this.config.animationDuration : 0);

        this.isOpen = true;

        setTimeout(() => {
            this.emit(MODAL_EVENTS.OPENED, { modal: this, options });
        }, this.config.animation ? this.config.animationDuration : 0);
    }

    close(result = null) {
        if (!this.isOpen) return;

        this.element.classList.remove('active');

        const hideModal = () => {
            this.element.style.display = 'none';
            this.element.setAttribute('aria-hidden', 'true');

            if (this.previousFocus && DOMUtils.isElementVisible(this.previousFocus)) {
                this.previousFocus.focus();
            }

            this.isOpen = false;

            this.emit(MODAL_EVENTS.CLOSED, { modal: this, result });
        };

        if (this.config.animation) {
            setTimeout(hideModal, this.config.animationDuration);
        } else {
            hideModal();
        }
    }

    setZIndex(zIndex) {
        if (this.element) {
            this.element.style.zIndex = zIndex;
        }
    }

    setContent(content) {
        const contentElement = this.element.querySelector('.modal-content');
        if (!contentElement) return;

        if (typeof content === 'string') {
            contentElement.innerHTML = content;
        } else if (content instanceof HTMLElement) {
            contentElement.innerHTML = '';
            contentElement.appendChild(content);
        }

        this.updateFocusableElements();
    }

    setTitle(title) {
        let titleElement = this.element.querySelector('.modal-title');
        if (!titleElement) {
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

    on(event, handler) {
        if (!this.eventListeners.has(event)) {
            this.eventListeners.set(event, []);
        }
        this.eventListeners.get(event).push(handler);
    }

    off(event, handler) {
        if (this.eventListeners.has(event)) {
            const handlers = this.eventListeners.get(event);
            const index = handlers.indexOf(handler);
            if (index > -1) {
                handlers.splice(index, 1);
            }
        }
    }

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

    destroy() {
        if (this.isOpen) {
            this.close();
        }

        if (this.element && this.element.parentNode) {
            this.element.parentNode.removeChild(this.element);
        }

        modalManager.unregister(this.id);

        this.element = null;
        this.eventListeners.clear();
        this.focusableElements = [];

        debugLog(`Modal destroyed: ${this.id}`);
    }
}

class ModalFactory {
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

            const confirmBtn = modal.element.querySelector('[data-action="confirm"]');
            DOMUtils.addEventListener(confirmBtn, 'click', () => {
                resolve(true);
                modalManager.close(modalId);
            });

            modal.on(MODAL_EVENTS.CLOSED, () => {
                resolve(false);
                setTimeout(() => modal.destroy(), 100);
            });

            modalManager.open(modalId);
        });
    }

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

const modalManager = new ModalManager();

const Modals = {
    open: (modalId, options) => modalManager.open(modalId, options),
    close: (modalId, result) => modalManager.close(modalId, result),
    closeAll: () => modalManager.closeAll(),
    create: (id, config) => new Modal(id, config),
    confirm: (options) => ModalFactory.confirm(options),
    alert: (options) => ModalFactory.alert(options),
    loading: (options) => ModalFactory.loading(options),
    isOpen: (modalId) => {
        const modal = modalManager.activeModals.get(modalId);
        return modal ? modal.isOpen : false;
    },
    getStats: () => modalManager.getStats()
};

if (typeof module !== 'undefined' && module.exports) {
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
    window.ModalManager = modalManager;
    window.Modal = Modal;
    window.Modals = Modals;
    window.openModal = Modals.open;
    window.closeModal = Modals.close;
    window.openLoginModal = () => Modals.open('loginModal');
    window.openSignupModal = () => Modals.open('signupModal');
}