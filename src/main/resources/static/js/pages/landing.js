class LandingPageController {
    constructor() {
        this.currentModal = null;
        this.isLoading = false;
        this.initialize();
        debugLog('LandingPageController initialized');
    }

    async initialize() {
        try {
            const authStatus = await AuthService.checkAuthStatus();
            if (authStatus.isAuthenticated) {
                debugLog('User already authenticated, redirecting to dashboard');
                window.location.href = '/pages/dashboard.html';
                return;
            }
            this.setupEventListeners();
            this.setupFormValidation();
            this.setupScrollAnimations();
            this.setupNavigationEffects();
            debugLog('Landing page initialized successfully');
        } catch (error) {
            errorLog('Error initializing landing page:', error);
            Notifications.error('Failed to initialize page. Please refresh and try again.');
        }
    }

    setupEventListeners() {
        const loginButtons = DOMUtils.querySelectorAll('[data-action="login"]');
        loginButtons.forEach(button => {
            DOMUtils.addEventListener(button, 'click', (e) => {
                e.preventDefault();
                this.openLoginModal();
            });
        });

        const signupButtons = DOMUtils.querySelectorAll('[data-action="signup"]');
        signupButtons.forEach(button => {
            DOMUtils.addEventListener(button, 'click', (e) => {
                e.preventDefault();
                this.openSignupModal();
            });
        });

        const googleButtons = DOMUtils.querySelectorAll('[data-action="google-auth"]');
        googleButtons.forEach(button => {
            DOMUtils.addEventListener(button, 'click', (e) => {
                e.preventDefault();
                this.handleGoogleAuth();
            });
        });

        const navLinks = DOMUtils.querySelectorAll('a[href^="#"]');
        navLinks.forEach(link => {
            DOMUtils.addEventListener(link, 'click', (e) => {
                e.preventDefault();
                this.handleSmoothScroll(link.getAttribute('href'));
            });
        });

        DOMUtils.addEventListener(window, 'scroll',
            PerformanceUtils.throttle(() => {
                this.handleHeaderScroll();
            }, 16)
        );

        debugLog('Event listeners setup completed');
    }

    setupFormValidation() {
        FormValidator.registerRules('loginForm', {
            email: [
                (value) => FieldValidators.required(value, 'Email'),
                (value) => FieldValidators.email(value)
            ],
            password: [
                (value) => FieldValidators.required(value, 'Password')
            ]
        });

        FormValidator.registerRules('signupForm', {
            name: [
                (value) => FieldValidators.required(value, 'Full name'),
                (value) => FieldValidators.length(value, 2, 50, 'Full name')
            ],
            email: [
                (value) => FieldValidators.required(value, 'Email'),
                (value) => FieldValidators.email(value)
            ],
            password: [
                (value) => FieldValidators.required(value, 'Password'),
                (value) => FieldValidators.password(value)
            ],
            confirmPassword: [
                (value) => FieldValidators.required(value, 'Password confirmation')
            ]
        });

        FormValidator.setupRealTimeValidation('loginForm', {
            validateOnBlur: true,
            debounceTime: 300
        });

        FormValidator.setupRealTimeValidation('signupForm', {
            validateOnBlur: true,
            debounceTime: 300
        });

        debugLog('Form validation setup completed');
    }

    setupScrollAnimations() {
        const observerOptions = {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        };

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('visible');
                }
            });
        }, observerOptions);

        const animatedElements = DOMUtils.querySelectorAll('.fade-in-up, .fade-in-left, .fade-in-right');
        animatedElements.forEach(element => {
            observer.observe(element);
        });

        debugLog('Scroll animations setup completed');
    }

    setupNavigationEffects() {
        const navToggle = DOMUtils.querySelector('.nav-toggle');
        const navLinks = DOMUtils.querySelector('.nav-links');

        if (navToggle && navLinks) {
            DOMUtils.addEventListener(navToggle, 'click', () => {
                navToggle.classList.toggle('active');
                navLinks.classList.toggle('active');
            });
        }

        debugLog('Navigation effects setup completed');
    }

    openLoginModal() {
        try {
            this.currentModal = 'loginModal';
            this.resetForm('loginForm');
            Modals.open('loginModal');
            debugLog('Login modal opened');
        } catch (error) {
            errorLog('Error opening login modal:', error);
            Notifications.error('Unable to open login form. Please try again.');
        }
    }

    openSignupModal() {
        try {
            this.currentModal = 'signupModal';
            this.resetForm('signupForm');
            Modals.open('signupModal');
            debugLog('Signup modal opened');
        } catch (error) {
            errorLog('Error opening signup modal:', error);
            Notifications.error('Unable to open signup form. Please try again.');
        }
    }

    async handleLogin(event) {
        event.preventDefault();

        if (this.isLoading) {
            debugLog('Login already in progress');
            return;
        }

        let loadingId = null;
        let submitButton = null;
        let originalButtonText = '';

        try {
            const validationResult = FormValidator.validateForm('loginForm');
            if (!validationResult.isValid) {
                Notifications.showValidationErrors(validationResult.errors);
                return;
            }

            const formData = {
                email: DOMUtils.getElementById('loginEmail')?.value?.trim(),
                password: DOMUtils.getElementById('loginPassword')?.value
            };

            this.isLoading = true;
            loadingId = Notifications.loading('Signing you in...');

            submitButton = event.target.querySelector('button[type="submit"]');
            if (submitButton) {
                originalButtonText = submitButton.innerHTML;
                submitButton.disabled = true;
                submitButton.innerHTML = 'Sign In...';
            }

            const result = await AuthService.login(formData);

            if (result.success) {
                Notifications.hide(loadingId);
                if (submitButton) {
                    submitButton.disabled = false;
                    submitButton.innerHTML = originalButtonText;
                }
                Notifications.success('Login successful! Redirecting...');
                Modals.close('loginModal');

                setTimeout(() => {
                    window.location.href = result.redirectUrl || '/pages/dashboard.html';
                }, 1500);
            } else {
                this.handleLoginError(result);
                const passwordField = DOMUtils.getElementById('loginPassword');
                if (passwordField) {
                    passwordField.value = '';
                }
            }

        } catch (error) {
            errorLog('Login error:', error);

            if (error.message && (error.message.includes('fetch') ||
                error.message.includes('Network') ||
                error.name === 'TypeError')) {
                Notifications.error('Network error. Please check your connection and try again.');
            } else {
                Notifications.error('An unexpected error occurred. Please try again.');
            }

            const passwordField = DOMUtils.getElementById('loginPassword');
            if (passwordField) {
                passwordField.value = '';
            }

        } finally {
            this.isLoading = false;
            if (loadingId) {
                Notifications.hide(loadingId);
            }
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.innerHTML = originalButtonText;
            }
        }
    }

    handleLoginError(result) {
        this.isLoading = false;
        const loginForm = document.getElementById('loginForm');
        const submitButton = loginForm ? loginForm.querySelector('button[type="submit"]') : null;
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.innerHTML = 'Sign In';
        }

        switch (result.errorCode) {
            case 'EMAIL_NOT_VERIFIED':
                this.showEmailNotVerifiedDialog(result.email ||
                    DOMUtils.getElementById('loginEmail')?.value);
                break;

            case 'ACCOUNT_LOCKED':
                Notifications.error(result.error || 'Account is locked. Please contact support.',
                    { duration: 10000 });
                break;

            case 'INVALID_CREDENTIALS':
                Notifications.error('Invalid email or password. Please check your credentials and try again.');
                break;

            case 'RATE_LIMITED':
                Notifications.error('Too many login attempts. Please wait before trying again.');
                break;

            case 'USER_NOT_FOUND':
                Notifications.error('No account found with this email address.');
                break;

            case 'NETWORK_ERROR':
                Notifications.error('Network error. Please check your connection and try again.');
                break;

            default:
                const errorMessage = result.error || 'Login failed. Please try again.';
                Notifications.error(errorMessage);
                errorLog('Unhandled login error:', result);
        }
    }

    async showEmailNotVerifiedDialog(email) {
        const confirmed = await Modals.confirm({
            title: 'Email Not Verified',
            message: `Your email (${email}) hasn't been verified yet. Would you like us to resend the verification email?`,
            confirmText: 'Resend Email',
            cancelText: 'Cancel'
        });

        if (confirmed && email) {
            await this.resendVerificationEmail(email);
        }
    }

    async handleSignup(event) {
        event.preventDefault();

        if (this.isLoading) {
            debugLog('Signup already in progress');
            return;
        }

        try {
            const validationResult = FormValidator.validateForm('signupForm');
            if (!validationResult.isValid) {
                Notifications.showValidationErrors(validationResult.errors);
                return;
            }

            const formData = {
                name: DOMUtils.getElementById('signupName')?.value?.trim(),
                email: DOMUtils.getElementById('signupEmail')?.value?.trim(),
                password: DOMUtils.getElementById('signupPassword')?.value,
                confirmPassword: DOMUtils.getElementById('confirmPassword')?.value
            };

            if (formData.password !== formData.confirmPassword) {
                Notifications.error('Passwords do not match.');
                return;
            }

            this.isLoading = true;
            const loadingId = Notifications.loading('Creating your account...');

            const result = await AuthService.register(formData);

            Notifications.hide(loadingId);
            this.isLoading = false;

            if (result.success) {
                Modals.close('signupModal');
                window.location.href = `/email/check-email.html?email=${encodeURIComponent(formData.email)}`;
            } else {
                this.handleSignupError(result, formData.email);
            }
        } catch (error) {
            this.isLoading = false;
            errorLog('Signup error:', error);
            Notifications.error('An unexpected error occurred. Please try again.');
        }
    }

    handleSignupError(result, email) {
        switch (result.errorCode) {
            case 'EMAIL_EXISTS':
                Notifications.error('An account with this email already exists. Try logging in instead.');
                break;

            case 'EMAIL_SEND_FAILED':
                Notifications.warning('Account created but verification email failed to send. Please try logging in.');
                break;

            case 'RATE_LIMITED':
                Notifications.error('Too many signup attempts. Please wait before trying again.');
                break;

            default:
                Notifications.error(result.error || 'Signup failed. Please try again.');
        }
    }

    handleGoogleAuth() {
        try {
            debugLog('Initiating Google OAuth');
            Notifications.loading('Redirecting to Google...');
            window.location.href = API_CONFIG.ENDPOINTS.AUTH.OAUTH_GOOGLE;
        } catch (error) {
            errorLog('Google auth error:', error);
            Notifications.error('Unable to connect to Google. Please try again.');
        }
    }

    async handleForgotPassword() {
        const email = DOMUtils.getElementById('loginEmail')?.value?.trim();

        if (!email) {
            Notifications.error('Please enter your email address first.');
            return;
        }

        if (!FieldValidators.email(email).isValid) {
            Notifications.error('Please enter a valid email address.');
            return;
        }

        try {
            const loadingId = Notifications.loading('Sending reset email...');
            const result = await ApiService.auth.forgotPassword(email);
            Notifications.hide(loadingId);

            if (result.success) {
                Modals.close('loginModal');
                window.location.href = `/pages/auth/reset-password.html?email=${encodeURIComponent(email)}`;
            } else {
                Notifications.error(result.error || 'Failed to send reset email. Please try again.');
            }
        } catch (error) {
            errorLog('Forgot password error:', error);
            Notifications.error('An error occurred. Please try again.');
        }
    }

    async resendVerificationEmail(email) {
        try {
            const loadingId = Notifications.loading('Sending verification email...');
            const result = await ApiService.auth.resendVerification(email);
            Notifications.hide(loadingId);

            if (result.success) {
                Notifications.success('Verification email sent! Please check your inbox.');
            } else {
                Notifications.error(result.error || 'Failed to send verification email.');
            }
        } catch (error) {
            errorLog('Resend verification error:', error);
            Notifications.error('An error occurred. Please try again.');
        }
    }

    handleSmoothScroll(target) {
        const targetElement = DOMUtils.querySelector(target);
        if (targetElement) {
            DOMUtils.scrollIntoView(targetElement, {
                behavior: 'smooth',
                block: 'start'
            });
        }
    }

    handleHeaderScroll() {
        const header = DOMUtils.querySelector('.header');
        if (header) {
            if (window.scrollY > 50) {
                header.classList.add('scrolled');
            } else {
                header.classList.remove('scrolled');
            }
        }
    }

    resetForm(formId) {
        const form = DOMUtils.getElementById(formId);
        if (form) {
            form.reset();
            const errorElements = form.querySelectorAll('.form-error');
            errorElements.forEach(element => {
                element.style.display = 'none';
            });
            const inputElements = form.querySelectorAll('.form-input');
            inputElements.forEach(element => {
                element.classList.remove('error', 'success');
            });
        }
    }

    switchToSignup() {
        Modals.close('loginModal');
        setTimeout(() => {
            this.openSignupModal();
        }, 200);
    }

    switchToLogin() {
        Modals.close('signupModal');
        setTimeout(() => {
            this.openLoginModal();
        }, 200);
    }

    showForgotPasswordForm() {
        Modals.close('loginModal');
        setTimeout(() => {
            this.handleForgotPassword();
        }, 200);
    }
}

class LandingPageService {
    constructor() {
        this.controller = null;
        this.initialized = false;
    }

    async initialize() {
        if (this.initialized) {
            debugLog('Landing page already initialized');
            return;
        }

        try {
            this.controller = new LandingPageController();
            this.setupFormHandlers();
            this.setupModalHandlers();
            this.initialized = true;
            debugLog('Landing page service initialized');
        } catch (error) {
            errorLog('Failed to initialize landing page service:', error);
            throw error;
        }
    }

    setupFormHandlers() {
        const loginForm = DOMUtils.getElementById('loginForm');
        if (loginForm) {
            DOMUtils.addEventListener(loginForm, 'submit', (e) => {
                this.controller.handleLogin(e);
            });
        }

        const signupForm = DOMUtils.getElementById('signupForm');
        if (signupForm) {
            DOMUtils.addEventListener(signupForm, 'submit', (e) => {
                this.controller.handleSignup(e);
            });
        }
    }

    setupModalHandlers() {
        window.switchToSignup = () => this.controller.switchToSignup();
        window.switchToLogin = () => this.controller.switchToLogin();
        window.showForgotPasswordForm = () => this.controller.showForgotPasswordForm();
        window.loginWithGoogle = () => this.controller.handleGoogleAuth();
        window.openLoginModal = () => this.controller.openLoginModal();
        window.openSignupModal = () => this.controller.openSignupModal();
    }

    getController() {
        return this.controller;
    }
}

const landingPageService = new LandingPageService();

if (document.readyState === 'loading') {
    DOMUtils.addEventListener(document, 'DOMContentLoaded', async () => {
        try {
            await landingPageService.initialize();
            debugLog('Landing page ready');
        } catch (error) {
            errorLog('Landing page initialization failed:', error);
            Notifications.error('Page failed to load properly. Please refresh the page.');
        }
    });
} else {
    landingPageService.initialize().catch(error => {
        errorLog('Landing page initialization failed:', error);
        Notifications.error('Page failed to load properly. Please refresh the page.');
    });
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        LandingPageController,
        LandingPageService,
        landingPageService
    };
} else {
    window.LandingPage = {
        controller: landingPageService.getController(),
        service: landingPageService
    };
}