/**
 * LinkUp Landing Page JavaScript
 * Clean, simple implementation without unnecessary auth checks
 * This is a PUBLIC page - no authentication required!
 */

class LandingPageController {
    constructor() {
        console.log('🎯 LandingPageController created');
        this.initialize();
    }

    /**
     * Initialize landing page functionality
     * Simple and straightforward - no auth needed!
     */
    initialize() {
        try {
            console.log('🚀 Initializing landing page...');

            // Set up UI components
            this.setupEventListeners();
            this.exposeGlobalFunctions();
            this.setupFormValidation();

            console.log('✅ Landing page initialized successfully');

        } catch (error) {
            console.error('❌ Landing page initialization error:', error);
        }
    }

    /**
     * Set up event listeners for UI interactions
     */
    setupEventListeners() {
        console.log('🎧 Setting up event listeners...');

        // Modal close on escape key
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                this.closeAllModals();
            }
        });

        // Modal close on backdrop click
        document.addEventListener('click', (event) => {
            if (event.target.classList.contains('modal')) {
                this.closeModal(event.target.id);
            }
        });

        // Smooth scrolling for anchor links
        document.querySelectorAll('a[href^="#"]').forEach(anchor => {
            anchor.addEventListener('click', (e) => {
                e.preventDefault();
                const targetId = anchor.getAttribute('href').substring(1);
                const targetElement = document.getElementById(targetId);
                if (targetElement) {
                    targetElement.scrollIntoView({ behavior: 'smooth' });
                }
            });
        });

        console.log('✅ Event listeners set up');
    }

    /**
     * Expose functions globally for HTML onclick handlers
     * These are the functions your HTML buttons call
     */
    exposeGlobalFunctions() {
        console.log('🌐 Exposing functions globally...');

        // Modal functions
        window.openLoginModal = () => this.openModal('loginModal');
        window.openSignupModal = () => this.openModal('signupModal');
        window.closeModal = (modalId) => this.closeModal(modalId);
        window.switchToSignup = () => this.switchToSignup();
        window.switchToLogin = () => this.switchToLogin();
        window.showForgotPasswordForm = () => this.showForgotPasswordForm();

        // Auth functions
        window.handleLogin = (event) => this.handleLogin(event);
        window.handleSignup = (event) => this.handleSignup(event);
        window.handleForgotPassword = (event) => this.handleForgotPassword(event);
        window.loginWithGoogle = () => this.loginWithGoogle();

        console.log('✅ Global functions exposed - buttons should work now!');
    }

    /**
     * MODAL MANAGEMENT
     */

    openModal(modalId) {
        console.log(`🔽 Opening modal: ${modalId}`);

        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add('active');
            document.body.style.overflow = 'hidden';

            // Focus first input after modal opens
            setTimeout(() => {
                const firstInput = modal.querySelector('input');
                if (firstInput) {
                    firstInput.focus();
                }
            }, 100);
        } else {
            console.error(`❌ Modal not found: ${modalId}`);
        }
    }

    closeModal(modalId) {
        console.log(`🔼 Closing modal: ${modalId}`);

        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('active');
            document.body.style.overflow = '';

            // Reset form when closing
            const form = modal.querySelector('form');
            if (form) {
                form.reset();
            }
        }
    }

    closeAllModals() {
        const modals = document.querySelectorAll('.modal.active');
        modals.forEach(modal => {
            this.closeModal(modal.id);
        });
    }

    switchToSignup() {
        this.closeModal('loginModal');
        setTimeout(() => this.openModal('signupModal'), 200);
    }

    switchToLogin() {
        this.closeModal('signupModal');
        setTimeout(() => this.openModal('loginModal'), 200);
    }

    showForgotPasswordForm() {
        this.closeModal('loginModal');
        setTimeout(() => this.openModal('forgotPasswordModal'), 200);
    }

    /**
     * FORM HANDLING
     */

    async handleLogin(event) {
        event.preventDefault();
        console.log('🔑 Processing login...');

        // Get form data
        const email = document.getElementById('loginEmail').value.trim();
        const password = document.getElementById('loginPassword').value;

        // Validate input
        if (!email || !password) {
            this.showMessage('Please enter both email and password', 'error');
            return;
        }

        if (!this.isValidEmail(email)) {
            this.showMessage('Please enter a valid email address', 'error');
            return;
        }

        try {
            // Show loading state
            const submitButton = event.target.querySelector('button[type="submit"]');
            const originalText = submitButton.innerHTML;
            submitButton.disabled = true;
            submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Signing in...';

            // Make API call
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({ email, password })
            });

            const data = await response.json();

            if (response.ok) {
                this.showMessage('Login successful! Redirecting...', 'success');
                this.closeModal('loginModal');

                // Redirect to dashboard
                setTimeout(() => {
                    window.location.href = '/pages/dashboard.html';
                }, 1500);
            } else {
                // Handle different error types
                if (data.errorCode === 'EMAIL_NOT_VERIFIED') {
                    this.showMessage('Please verify your email before logging in. Check your inbox!', 'warning');
                } else {
                    this.showMessage(data.message || 'Login failed. Please try again.', 'error');
                }
            }

            // Reset button
            submitButton.disabled = false;
            submitButton.innerHTML = originalText;

        } catch (error) {
            console.error('❌ Login error:', error);
            this.showMessage('Network error. Please try again.', 'error');
        }
    }

    async handleSignup(event) {
        event.preventDefault();
        console.log('📝 Processing signup...');

        // Get form data
        const name = document.getElementById('signupName').value.trim();
        const email = document.getElementById('signupEmail').value.trim();
        const password = document.getElementById('signupPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        // Validate input
        if (!name || !email || !password || !confirmPassword) {
            this.showMessage('Please fill in all fields', 'error');
            return;
        }

        if (name.length < 2) {
            this.showMessage('Name must be at least 2 characters', 'error');
            return;
        }

        if (!this.isValidEmail(email)) {
            this.showMessage('Please enter a valid email address', 'error');
            return;
        }

        if (password.length < 8) {
            this.showMessage('Password must be at least 8 characters', 'error');
            return;
        }

        if (password !== confirmPassword) {
            this.showMessage('Passwords do not match', 'error');
            return;
        }

        try {
            // Show loading state
            const submitButton = event.target.querySelector('button[type="submit"]');
            const originalText = submitButton.innerHTML;
            submitButton.disabled = true;
            submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Creating account...';

            // Make API call
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({ name, email, password })
            });

            const data = await response.json();

            if (response.ok || response.status === 201) {
                this.showMessage('Account created! Check your email for verification.', 'success');
                this.closeModal('signupModal');

                // Redirect to email verification page
                setTimeout(() => {
                    window.location.href = `/email/check-email.html?email=${encodeURIComponent(email)}`;
                }, 1500);
            } else {
                this.showMessage(data.message || 'Registration failed. Please try again.', 'error');
            }

            // Reset button
            submitButton.disabled = false;
            submitButton.innerHTML = originalText;

        } catch (error) {
            console.error('❌ Signup error:', error);
            this.showMessage('Network error. Please try again.', 'error');
        }
    }

    async handleForgotPassword(event) {
        event.preventDefault();
        console.log('🔐 Processing forgot password...');

        const email = document.getElementById('resetEmail').value.trim();

        if (!email) {
            this.showMessage('Please enter your email address', 'error');
            return;
        }

        if (!this.isValidEmail(email)) {
            this.showMessage('Please enter a valid email address', 'error');
            return;
        }

        try {
            const submitButton = event.target.querySelector('button[type="submit"]');
            const originalText = submitButton.innerHTML;
            submitButton.disabled = true;
            submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';

            const response = await fetch('/api/auth/forgot-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ email })
            });

            const data = await response.json();

            if (response.ok) {
                this.showMessage('Password reset email sent! Check your inbox.', 'success');
                this.closeModal('forgotPasswordModal');

                // Redirect to confirmation page
                setTimeout(() => {
                    window.location.href = `/pages/auth/reset-password.html?email=${encodeURIComponent(email)}`;
                }, 1500);
            } else {
                this.showMessage(data.message || 'Failed to send reset email', 'error');
            }

            submitButton.disabled = false;
            submitButton.innerHTML = originalText;

        } catch (error) {
            console.error('❌ Forgot password error:', error);
            this.showMessage('Network error. Please try again.', 'error');
        }
    }

    loginWithGoogle() {
        console.log('🔍 Redirecting to Google OAuth...');
        this.showMessage('Redirecting to Google...', 'info');
        window.location.href = '/oauth2/authorization/google';
    }

    /**
     * UTILITY FUNCTIONS
     */

    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    showMessage(message, type = 'info') {
        // Create message element
        const messageEl = document.createElement('div');
        messageEl.className = `message message-${type}`;
        messageEl.innerHTML = `
            <i class="fas ${this.getMessageIcon(type)}"></i>
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" style="margin-left: auto; background: none; border: none; color: inherit; cursor: pointer;">
                <i class="fas fa-times"></i>
            </button>
        `;

        // Style the message
        Object.assign(messageEl.style, {
            position: 'fixed',
            top: '20px',
            right: '20px',
            padding: '12px 16px',
            borderRadius: '8px',
            color: 'white',
            fontWeight: '500',
            boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
            zIndex: '10000',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            maxWidth: '400px',
            animation: 'slideInRight 0.3s ease'
        });

        // Set background color based on type
        const colors = {
            success: '#00875A',
            error: '#DE350B',
            warning: '#FF8B00',
            info: '#0052CC'
        };
        messageEl.style.backgroundColor = colors[type] || colors.info;

        // Add to page
        document.body.appendChild(messageEl);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (messageEl.parentNode) {
                messageEl.remove();
            }
        }, 5000);
    }

    getMessageIcon(type) {
        const icons = {
            success: 'fa-check-circle',
            error: 'fa-exclamation-circle',
            warning: 'fa-exclamation-triangle',
            info: 'fa-info-circle'
        };
        return icons[type] || icons.info;
    }

    setupFormValidation() {
        console.log('✅ Setting up form validation...');

        // Add real-time validation if needed
        // For now, validation happens on submit
    }
}

/**
 * INITIALIZATION
 * Initialize when DOM is ready - simple and clean!
 */
document.addEventListener('DOMContentLoaded', () => {
    console.log('🌟 DOM loaded, initializing landing page...');

    try {
        window.landingController = new LandingPageController();
        console.log('🎉 Landing page ready!');
    } catch (error) {
        console.error('💥 Failed to initialize landing page:', error);
    }
});

console.log('✅ Landing page script loaded');