class LandingPageController {
    constructor() {
        this.initialize();
    }

    initialize() {
        try {
            this.setupEventListeners();
            this.exposeGlobalFunctions();
            this.setupFormValidation();
        } catch (error) {
            console.error('Landing page initialization error:', error);
        }
    }

    setupEventListeners() {
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                this.closeAllModals();
            }
        });

        document.addEventListener('click', (event) => {
            if (event.target.classList.contains('modal')) {
                this.closeModal(event.target.id);
            }
        });

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
    }

    exposeGlobalFunctions() {
        window.openLoginModal = () => this.openModal('loginModal');
        window.openSignupModal = () => this.openModal('signupModal');
        window.closeModal = (modalId) => this.closeModal(modalId);
        window.switchToSignup = () => this.switchToSignup();
        window.switchToLogin = () => this.switchToLogin();
        window.showForgotPasswordForm = () => this.showForgotPasswordForm();
        window.handleLogin = (event) => this.handleLogin(event);
        window.handleSignup = (event) => this.handleSignup(event);
        window.handleForgotPassword = (event) => this.handleForgotPassword(event);
        window.loginWithGoogle = () => this.loginWithGoogle();
    }

    openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add('active');
            document.body.style.overflow = 'hidden';
            setTimeout(() => {
                const firstInput = modal.querySelector('input');
                if (firstInput) {
                    firstInput.focus();
                }
            }, 100);
        } else {
            console.error(`Modal not found: ${modalId}`);
        }
    }

    closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('active');
            document.body.style.overflow = '';
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

    async handleLogin(event) {
        event.preventDefault();
        const email = document.getElementById('loginEmail').value.trim();
        const password = document.getElementById('loginPassword').value;

        if (!email || !password) {
            this.showMessage('Please enter both email and password', 'error');
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
            submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Signing in...';

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
                setTimeout(() => {
                    window.location.href = '/pages/dashboard.html';
                }, 1500);
            } else {
                if (data.errorCode === 'EMAIL_NOT_VERIFIED') {
                    this.showMessage('Please verify your email before logging in. Check your inbox!', 'warning');
                } else {
                    this.showMessage(data.message || 'Login failed. Please try again.', 'error');
                }
            }

            submitButton.disabled = false;
            submitButton.innerHTML = originalText;

        } catch (error) {
            console.error('Login error:', error);
            this.showMessage('Network error. Please try again.', 'error');
        }
    }

    async handleSignup(event) {
        event.preventDefault();
        const name = document.getElementById('signupName').value.trim();
        const email = document.getElementById('signupEmail').value.trim();
        const password = document.getElementById('signupPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

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
            const submitButton = event.target.querySelector('button[type="submit"]');
            const originalText = submitButton.innerHTML;
            submitButton.disabled = true;
            submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Creating account...';

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
                setTimeout(() => {
                    window.location.href = `/email/check-email.html?email=${encodeURIComponent(email)}`;
                }, 1500);
            } else {
                this.showMessage(data.message || 'Registration failed. Please try again.', 'error');
            }

            submitButton.disabled = false;
            submitButton.innerHTML = originalText;

        } catch (error) {
            console.error('Signup error:', error);
            this.showMessage('Network error. Please try again.', 'error');
        }
    }

    async handleForgotPassword(event) {
        event.preventDefault();
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
                setTimeout(() => {
                    window.location.href = `/pages/auth/reset-password.html?email=${encodeURIComponent(email)}`;
                }, 1500);
            } else {
                this.showMessage(data.message || 'Failed to send reset email', 'error');
            }

            submitButton.disabled = false;
            submitButton.innerHTML = originalText;

        } catch (error) {
            console.error('Forgot password error:', error);
            this.showMessage('Network error. Please try again.', 'error');
        }
    }

    loginWithGoogle() {
        this.showMessage('Redirecting to Google...', 'info');
        window.location.href = '/oauth2/authorization/google';
    }

    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    showMessage(message, type = 'info') {
        const messageEl = document.createElement('div');
        messageEl.className = `message message-${type}`;
        messageEl.innerHTML = `
            <i class="fas ${this.getMessageIcon(type)}"></i>
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" style="margin-left: auto; background: none; border: none; color: inherit; cursor: pointer;">
                <i class="fas fa-times"></i>
            </button>
        `;

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

        const colors = {
            success: '#00875A',
            error: '#DE350B',
            warning: '#FF8B00',
            info: '#0052CC'
        };
        messageEl.style.backgroundColor = colors[type] || colors.info;

        document.body.appendChild(messageEl);

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
    }
}

document.addEventListener('DOMContentLoaded', () => {
    try {
        window.landingController = new LandingPageController();
    } catch (error) {
        console.error('Failed to initialize landing page:', error);
    }
});