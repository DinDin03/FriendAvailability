/* ===== LANDING PAGE CONTROLLER ===== */
/*
  This file handles all landing page specific functionality.
  Think of this as your @Controller class in Spring Boot - it handles
  the presentation logic and user interactions for the landing page.

  This refactored code uses our modular services:
  - AuthService for authentication
  - ValidationService for form validation
  - Modals for login/signup dialogs
  - Notifications for user feedback
  - API services for backend communication

  Similar to having a clean separation between your controller,
  service, and repository layers in Spring Boot.
*/

/**
 * Landing Page Controller Class
 * Similar to a @Controller class in Spring Boot
 */
class LandingPageController {
    constructor() {
        this.currentModal = null;
        this.isLoading = false;

        // Initialize page
        this.initialize();

        debugLog('LandingPageController initialized');
    }

    /**
     * Initialize landing page
     * Similar to @PostConstruct method
     */
    async initialize() {
        try {
            // Check if user is already authenticated
            const authStatus = await AuthService.checkAuthStatus();
            if (authStatus.isAuthenticated) {
                debugLog('User already authenticated, redirecting to dashboard');
                window.location.href = '/pages/dashboard.html';
                return;
            }

            // Setup page components
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

    /**
     * Setup event listeners
     * Similar to mapping request handlers in Spring Boot
     */
    setupEventListeners() {
        // Navigation buttons
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

        // Google OAuth button
        const googleButtons = DOMUtils.querySelectorAll('[data-action="google-auth"]');
        googleButtons.forEach(button => {
            DOMUtils.addEventListener(button, 'click', (e) => {
                e.preventDefault();
                this.handleGoogleAuth();
            });
        });

        // Smooth scrolling for navigation links
        const navLinks = DOMUtils.querySelectorAll('a[href^="#"]');
        navLinks.forEach(link => {
            DOMUtils.addEventListener(link, 'click', (e) => {
                e.preventDefault();
                this.handleSmoothScroll(link.getAttribute('href'));
            });
        });

        // Header scroll effect
        DOMUtils.addEventListener(window, 'scroll',
            PerformanceUtils.throttle(() => {
                this.handleHeaderScroll();
            }, 16) // ~60fps
        );

        debugLog('Event listeners setup completed');
    }

    /**
     * Setup form validation
     * Similar to @Valid annotations in Spring Boot
     */
    setupFormValidation() {
        // Setup validation for login modal
        FormValidator.registerRules('loginForm', {
            email: [
                (value) => FieldValidators.required(value, 'Email'),
                (value) => FieldValidators.email(value)
            ],
            password: [
                (value) => FieldValidators.required(value, 'Password')
            ]
        });

        // Setup validation for signup modal
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

        // Setup real-time validation
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

    /**
     * Setup scroll animations
     * Similar to AOP aspects in Spring Boot
     */
    setupScrollAnimations() {
        // Intersection Observer for scroll animations
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

        // Observe elements for animation
        const animatedElements = DOMUtils.querySelectorAll('.fade-in-up, .fade-in-left, .fade-in-right');
        animatedElements.forEach(element => {
            observer.observe(element);
        });

        debugLog('Scroll animations setup completed');
    }

    /**
     * Setup navigation effects
     */
    setupNavigationEffects() {
        // Mobile menu toggle (if exists)
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

    /**
     * Open login modal
     * Similar to handling GET /login in Spring Boot
     */
    openLoginModal() {
        try {
            this.currentModal = 'loginModal';

            // Clear any previous form data
            this.resetForm('loginForm');

            // Open modal
            Modals.open('loginModal');

            debugLog('Login modal opened');
        } catch (error) {
            errorLog('Error opening login modal:', error);
            Notifications.error('Unable to open login form. Please try again.');
        }
    }

    /**
     * Open signup modal
     * Similar to handling GET /register in Spring Boot
     */
    openSignupModal() {
        try {
            this.currentModal = 'signupModal';

            // Clear any previous form data
            this.resetForm('signupForm');

            // Open modal
            Modals.open('signupModal');

            debugLog('Signup modal opened');
        } catch (error) {
            errorLog('Error opening signup modal:', error);
            Notifications.error('Unable to open signup form. Please try again.');
        }
    }

    /**
     * Handle login form submission - FIXED VERSION
     * Similar to POST /login endpoint in Spring Boot
     */
    async handleLogin(event) {
        event.preventDefault();

        if (this.isLoading) {
            debugLog('Login already in progress');
            return;
        }

        let loadingId = null; // Track loading notification ID
        let submitButton = null;
        let originalButtonText = '';

        try {
            // Validate form
            const validationResult = FormValidator.validateForm('loginForm');
            if (!validationResult.isValid) {
                Notifications.showValidationErrors(validationResult.errors);
                return;
            }

            // Get form data
            const formData = {
                email: DOMUtils.getElementById('loginEmail')?.value?.trim(),
                password: DOMUtils.getElementById('loginPassword')?.value
            };

            // Set loading state BEFORE API call
            this.isLoading = true;
            loadingId = Notifications.loading('Signing you in...');

            // Also show spinner on the button
            submitButton = event.target.querySelector('button[type="submit"]');
            if (submitButton) {
                originalButtonText = submitButton.innerHTML;
                submitButton.disabled = true;
                submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Signing in...';
            }

            // Attempt login
            const result = await AuthService.login(formData);

            if (result.success) {
                // Success path
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
                // Handle authentication errors (wrong password, etc.)
                this.handleLoginError(result);

                // Clear password field for security
                const passwordField = DOMUtils.getElementById('loginPassword');
                if (passwordField) {
                    passwordField.value = '';
                }
            }

        } catch (error) {
            // Handle network errors and unexpected errors
            errorLog('Login error:', error);

            // Determine error type
            if (error.message && (error.message.includes('fetch') ||
                error.message.includes('Network') ||
                error.name === 'TypeError')) {
                Notifications.error('Network error. Please check your connection and try again.');
            } else {
                Notifications.error('An unexpected error occurred. Please try again.');
            }

            // Clear password field
            const passwordField = DOMUtils.getElementById('loginPassword');
            if (passwordField) {
                passwordField.value = '';
            }

        } finally {
            // CRITICAL: Always cleanup loading state and button
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

    /**
     * Handle login errors - ENHANCED VERSION
     * @param {Object} result - Login result with error info
     */
    handleLoginError(result) {
        // Clear loading state first
        this.isLoading = false;

        // Always re-enable the button and hide spinner if present
        const loginForm = document.getElementById('loginForm');
        const submitButton = loginForm ? loginForm.querySelector('button[type="submit"]') : null;
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.innerHTML = '<i class="fas fa-sign-in-alt"></i> Sign In';
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
                // Generic error handling
                const errorMessage = result.error || 'Login failed. Please try again.';
                Notifications.error(errorMessage);

                // Log for debugging
                errorLog('Unhandled login error:', result);
        }
    }

    /**
     * Show email not verified dialog
     * @param {string} email - User email
     */
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

    /**
     * Handle signup form submission
     * Similar to POST /register endpoint in Spring Boot
     */
    async handleSignup(event) {
        event.preventDefault();

        if (this.isLoading) {
            debugLog('Signup already in progress');
            return;
        }

        try {
            // Validate form
            const validationResult = FormValidator.validateForm('signupForm');
            if (!validationResult.isValid) {
                Notifications.showValidationErrors(validationResult.errors);
                return;
            }

            // Get form data
            const formData = {
                name: DOMUtils.getElementById('signupName')?.value?.trim(),
                email: DOMUtils.getElementById('signupEmail')?.value?.trim(),
                password: DOMUtils.getElementById('signupPassword')?.value,
                confirmPassword: DOMUtils.getElementById('confirmPassword')?.value
            };

            // Additional validation for password confirmation
            if (formData.password !== formData.confirmPassword) {
                Notifications.error('Passwords do not match.');
                return;
            }

            this.isLoading = true;
            const loadingId = Notifications.loading('Creating your account...');

            // Attempt registration
            const result = await AuthService.register(formData);

            Notifications.hide(loadingId);
            this.isLoading = false;

            if (result.success) {
                // Success - redirect to email verification page
                Modals.close('signupModal');
                window.location.href = `/email/check-email.html?email=${encodeURIComponent(formData.email)}`;
            } else {
                // Handle different error types
                this.handleSignupError(result, formData.email);
            }
        } catch (error) {
            this.isLoading = false;
            errorLog('Signup error:', error);
            Notifications.error('An unexpected error occurred. Please try again.');
        }
    }

    /**
     * Handle signup errors
     * @param {Object} result - Signup result with error info
     * @param {string} email - User email
     */
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

    /**
     * Handle Google OAuth authentication
     * Similar to OAuth2 configuration in Spring Boot
     */
    handleGoogleAuth() {
        try {
            debugLog('Initiating Google OAuth');
            Notifications.loading('Redirecting to Google...');

            // Redirect to Google OAuth endpoint
            window.location.href = API_CONFIG.ENDPOINTS.AUTH.OAUTH_GOOGLE;
        } catch (error) {
            errorLog('Google auth error:', error);
            Notifications.error('Unable to connect to Google. Please try again.');
        }
    }

    /**
     * Handle forgot password
     * Similar to POST /forgot-password endpoint
     */
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

    /**
     * Resend verification email
     * @param {string} email - Email address
     */
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

    /**
     * Handle smooth scrolling
     * @param {string} target - Target element selector
     */
    handleSmoothScroll(target) {
        const targetElement = DOMUtils.querySelector(target);
        if (targetElement) {
            DOMUtils.scrollIntoView(targetElement, {
                behavior: 'smooth',
                block: 'start'
            });
        }
    }

    /**
     * Handle header scroll effects
     */
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

    /**
     * Reset form
     * @param {string} formId - Form ID
     */
    resetForm(formId) {
        const form = DOMUtils.getElementById(formId);
        if (form) {
            form.reset();

            // Clear validation errors
            const errorElements = form.querySelectorAll('.form-error');
            errorElements.forEach(element => {
                element.style.display = 'none';
            });

            // Remove error classes
            const inputElements = form.querySelectorAll('.form-input');
            inputElements.forEach(element => {
                element.classList.remove('error', 'success');
            });
        }
    }

    /**
     * Switch between login and signup modals
     */
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

    /**
     * Show forgot password form
     */
    showForgotPasswordForm() {
        Modals.close('loginModal');
        setTimeout(() => {
            // You could open a dedicated forgot password modal here
            // For now, we'll handle it inline
            this.handleForgotPassword();
        }, 200);
    }
}

/**
 * Landing Page Service
 * Initialize and manage landing page
 */
class LandingPageService {
    constructor() {
        this.controller = null;
        this.initialized = false;
    }

    /**
     * Initialize landing page
     */
    async initialize() {
        if (this.initialized) {
            debugLog('Landing page already initialized');
            return;
        }

        try {
            // Create controller
            this.controller = new LandingPageController();

            // Setup form handlers
            this.setupFormHandlers();

            // Setup modal event handlers
            this.setupModalHandlers();

            this.initialized = true;
            debugLog('Landing page service initialized');
        } catch (error) {
            errorLog('Failed to initialize landing page service:', error);
            throw error;
        }
    }

    /**
     * Setup form event handlers
     */
    setupFormHandlers() {
        // Login form
        const loginForm = DOMUtils.getElementById('loginForm');
        if (loginForm) {
            DOMUtils.addEventListener(loginForm, 'submit', (e) => {
                this.controller.handleLogin(e);
            });
        }

        // Signup form
        const signupForm = DOMUtils.getElementById('signupForm');
        if (signupForm) {
            DOMUtils.addEventListener(signupForm, 'submit', (e) => {
                this.controller.handleSignup(e);
            });
        }
    }

    /**
     * Setup modal event handlers
     */
    setupModalHandlers() {
        // Global functions for modal switching (legacy support)
        window.switchToSignup = () => this.controller.switchToSignup();
        window.switchToLogin = () => this.controller.switchToLogin();
        window.showForgotPasswordForm = () => this.controller.showForgotPasswordForm();
        window.loginWithGoogle = () => this.controller.handleGoogleAuth();
        window.openLoginModal = () => this.controller.openLoginModal();
        window.openSignupModal = () => this.controller.openSignupModal();
    }

    /**
     * Get controller instance
     * @returns {LandingPageController} Controller instance
     */
    getController() {
        return this.controller;
    }
}

/**
 * Create and initialize landing page service
 */
const landingPageService = new LandingPageService();

/**
 * Auto-initialize when DOM is ready
 */
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
    // DOM already loaded
    landingPageService.initialize().catch(error => {
        errorLog('Landing page initialization failed:', error);
        Notifications.error('Page failed to load properly. Please refresh the page.');
    });
}

/**
 * Export for testing and external use
 */
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