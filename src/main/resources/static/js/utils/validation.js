/* ===== VALIDATION UTILITIES ===== */
/*
  This file contains validation functions and form validation logic.
  Think of this as your validation annotations in Spring Boot:
  - @Valid, @NotNull, @NotEmpty
  - @Email, @Size, @Pattern
  - @Min, @Max, @Range

  These validation functions provide client-side validation that mirrors
  your backend validation rules for better user experience.
*/

/**
 * Basic Field Validators
 * Similar to Bean Validation annotations in Spring Boot
 */
const FieldValidators = {
    /**
     * Check if field is required (not empty)
     * Similar to @NotNull and @NotEmpty annotations
     *
     * @param {string} value - Value to validate
     * @param {string} fieldName - Field name for error message
     * @returns {Object} Validation result
     */
    required(value, fieldName = 'Field') {
        const isValid = value !== null && value !== undefined && value.toString().trim().length > 0;

        return {
            isValid,
            errorMessage: isValid ? null : `${fieldName} is required`
        };
    },

    /**
     * Validate email format
     * Similar to @Email annotation
     *
     * @param {string} email - Email to validate
     * @returns {Object} Validation result
     */
    email(email) {
        if (!email) {
            return { isValid: true, errorMessage: null }; // Allow empty if not required
        }

        // Comprehensive email regex that matches most valid email formats
        const emailRegex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;
        const isValid = emailRegex.test(email.trim());

        return {
            isValid,
            errorMessage: isValid ? null : 'Please enter a valid email address'
        };
    },

    /**
     * Validate string length
     * Similar to @Size annotation
     *
     * @param {string} value - Value to validate
     * @param {number} min - Minimum length
     * @param {number} max - Maximum length
     * @param {string} fieldName - Field name for error message
     * @returns {Object} Validation result
     */
    length(value, min = 0, max = Infinity, fieldName = 'Field') {
        if (!value) {
            return { isValid: true, errorMessage: null }; // Allow empty if not required
        }

        const length = value.toString().trim().length;
        const isValid = length >= min && length <= max;

        let errorMessage = null;
        if (!isValid) {
            if (length < min) {
                errorMessage = `${fieldName} must be at least ${min} characters long`;
            } else {
                errorMessage = `${fieldName} must not exceed ${max} characters`;
            }
        }

        return { isValid, errorMessage };
    },

    /**
     * Validate password strength
     * Custom validation similar to creating custom validators in Spring Boot
     *
     * @param {string} password - Password to validate
     * @returns {Object} Validation result with strength level
     */
    password(password) {
        if (!password) {
            return {
                isValid: true,
                errorMessage: null,
                strength: 'none',
                strengthScore: 0
            };
        }

        const minLength = APP_CONFIG.VALIDATION.PASSWORD_MIN_LENGTH;
        let score = 0;
        let feedback = [];

        // Length check
        if (password.length < minLength) {
            return {
                isValid: false,
                errorMessage: `Password must be at least ${minLength} characters long`,
                strength: 'weak',
                strengthScore: 0
            };
        }

        // Strength criteria
        const criteria = {
            length: password.length >= 8,
            lowercase: /[a-z]/.test(password),
            uppercase: /[A-Z]/.test(password),
            numbers: /\d/.test(password),
            symbols: /[!@#$%^&*(),.?":{}|<>]/.test(password)
        };

        // Calculate score
        Object.values(criteria).forEach(met => {
            if (met) score++;
        });

        // Additional points for length
        if (password.length >= 12) score++;
        if (password.length >= 16) score++;

        // Determine strength
        let strength;
        if (score <= 2) strength = 'weak';
        else if (score <= 4) strength = 'fair';
        else if (score <= 5) strength = 'good';
        else strength = 'strong';

        // Generate feedback
        if (!criteria.lowercase) feedback.push('Add lowercase letters');
        if (!criteria.uppercase) feedback.push('Add uppercase letters');
        if (!criteria.numbers) feedback.push('Add numbers');
        if (!criteria.symbols) feedback.push('Add special characters');
        if (password.length < 12) feedback.push('Use 12+ characters for better security');

        return {
            isValid: score >= 3, // Minimum fair strength required
            errorMessage: score >= 3 ? null : 'Password is too weak. ' + feedback.join(', '),
            strength,
            strengthScore: score,
            feedback: feedback
        };
    },

    /**
     * Validate password confirmation
     * Similar to custom cross-field validation in Spring Boot
     *
     * @param {string} password - Original password
     * @param {string} confirmPassword - Confirmation password
     * @returns {Object} Validation result
     */
    passwordConfirmation(password, confirmPassword) {
        if (!confirmPassword) {
            return { isValid: true, errorMessage: null }; // Allow empty if not required
        }

        const isValid = password === confirmPassword;

        return {
            isValid,
            errorMessage: isValid ? null : 'Passwords do not match'
        };
    },

    /**
     * Validate phone number
     * Similar to @Pattern annotation
     *
     * @param {string} phone - Phone number to validate
     * @returns {Object} Validation result
     */
    phone(phone) {
        if (!phone) {
            return { isValid: true, errorMessage: null }; // Allow empty if not required
        }

        // Remove all non-digit characters for validation
        const digitsOnly = phone.replace(/\D/g, '');

        // Check for valid length (10-15 digits)
        const isValid = digitsOnly.length >= 10 && digitsOnly.length <= 15;

        return {
            isValid,
            errorMessage: isValid ? null : 'Please enter a valid phone number'
        };
    },

    /**
     * Validate URL format
     * Similar to @URL annotation
     *
     * @param {string} url - URL to validate
     * @returns {Object} Validation result
     */
    url(url) {
        if (!url) {
            return { isValid: true, errorMessage: null }; // Allow empty if not required
        }

        try {
            new URL(url);
            return { isValid: true, errorMessage: null };
        } catch {
            return { isValid: false, errorMessage: 'Please enter a valid URL' };
        }
    },

    /**
     * Validate date format and range
     * Similar to @Past, @Future, @DateTimeFormat annotations
     *
     * @param {string} dateString - Date string to validate
     * @param {Object} options - Validation options
     * @returns {Object} Validation result
     */
    date(dateString, options = {}) {
        if (!dateString) {
            return { isValid: true, errorMessage: null }; // Allow empty if not required
        }

        const date = new Date(dateString);

        // Check if date is valid
        if (isNaN(date.getTime())) {
            return { isValid: false, errorMessage: 'Please enter a valid date' };
        }

        const now = new Date();

        // Check future constraint
        if (options.future && date <= now) {
            return { isValid: false, errorMessage: 'Date must be in the future' };
        }

        // Check past constraint
        if (options.past && date >= now) {
            return { isValid: false, errorMessage: 'Date must be in the past' };
        }

        // Check minimum date
        if (options.min && date < new Date(options.min)) {
            return { isValid: false, errorMessage: `Date must be after ${DateUtils.formatDate(options.min)}` };
        }

        // Check maximum date
        if (options.max && date > new Date(options.max)) {
            return { isValid: false, errorMessage: `Date must be before ${DateUtils.formatDate(options.max)}` };
        }

        return { isValid: true, errorMessage: null };
    },

    /**
     * Validate numeric range
     * Similar to @Min, @Max, @Range annotations
     *
     * @param {string|number} value - Value to validate
     * @param {number} min - Minimum value
     * @param {number} max - Maximum value
     * @param {string} fieldName - Field name for error message
     * @returns {Object} Validation result
     */
    range(value, min = -Infinity, max = Infinity, fieldName = 'Value') {
        if (!value && value !== 0) {
            return { isValid: true, errorMessage: null }; // Allow empty if not required
        }

        const numValue = parseFloat(value);

        if (isNaN(numValue)) {
            return { isValid: false, errorMessage: `${fieldName} must be a valid number` };
        }

        const isValid = numValue >= min && numValue <= max;

        let errorMessage = null;
        if (!isValid) {
            if (numValue < min) {
                errorMessage = `${fieldName} must be at least ${min}`;
            } else {
                errorMessage = `${fieldName} must not exceed ${max}`;
            }
        }

        return { isValid, errorMessage };
    }
};

/**
 * Form Validation Engine
 * Similar to @Valid annotation processing in Spring Boot
 */
const FormValidator = {
    /**
     * Validation rules registry
     * Similar to having validation configuration in Spring Boot
     */
    rules: new Map(),

    /**
     * Register validation rules for a form
     * Similar to defining validation groups in Spring Boot
     *
     * @param {string} formId - Form identifier
     * @param {Object} rules - Validation rules
     */
    registerRules(formId, rules) {
        this.rules.set(formId, rules);
        debugLog(`Registered validation rules for form: ${formId}`, rules);
    },

    /**
     * Validate a single field
     * Similar to field-level validation in Spring Boot
     *
     * @param {string} value - Field value
     * @param {Array} validators - Array of validation functions
     * @param {string} fieldName - Field name for error messages
     * @returns {Object} Validation result
     */
    validateField(value, validators, fieldName) {
        for (const validator of validators) {
            const result = validator(value, fieldName);
            if (!result.isValid) {
                return result;
            }
        }

        return { isValid: true, errorMessage: null };
    },

    /**
     * Validate entire form
     * Similar to @Valid annotation on DTOs in Spring Boot
     *
     * @param {string|HTMLFormElement} form - Form element or ID
     * @param {Object} customRules - Optional custom rules
     * @returns {Object} Validation result with all errors
     */
    validateForm(form, customRules = null) {
        const formElement = typeof form === 'string' ?
            DOMUtils.getElementById(form) : form;

        if (!formElement) {
            errorLog('Form not found for validation');
            return { isValid: false, errors: { form: 'Form not found' } };
        }

        const formId = formElement.id;
        const rules = customRules || this.rules.get(formId);

        if (!rules) {
            debugLog(`No validation rules found for form: ${formId}`);
            return { isValid: true, errors: {} };
        }

        const errors = {};
        let isFormValid = true;

        // Validate each field
        for (const [fieldName, validators] of Object.entries(rules)) {
            const field = DOMUtils.querySelector(`[name="${fieldName}"]`, formElement);

            if (!field) {
                debugLog(`Field not found: ${fieldName}`);
                continue;
            }

            const value = this.getFieldValue(field);
            const result = this.validateField(value, validators, fieldName);

            if (!result.isValid) {
                errors[fieldName] = result.errorMessage;
                isFormValid = false;

                // Add visual feedback
                this.showFieldError(field, result.errorMessage);
            } else {
                // Clear previous errors
                this.clearFieldError(field);
            }
        }

        return {
            isValid: isFormValid,
            errors,
            errorCount: Object.keys(errors).length
        };
    },

    /**
     * Get field value based on field type
     * @param {HTMLElement} field - Form field element
     * @returns {string} Field value
     */
    getFieldValue(field) {
        switch (field.type) {
            case 'checkbox':
                return field.checked;
            case 'radio':
                const radioGroup = DOMUtils.querySelectorAll(`[name="${field.name}"]`);
                const checkedRadio = Array.from(radioGroup).find(radio => radio.checked);
                return checkedRadio ? checkedRadio.value : '';
            case 'select-multiple':
                return Array.from(field.selectedOptions).map(option => option.value);
            default:
                return field.value.trim();
        }
    },

    /**
     * Show field validation error
     * @param {HTMLElement} field - Form field element
     * @param {string} errorMessage - Error message to display
     */
    showFieldError(field, errorMessage) {
        // Add error class to field
        field.classList.add('error');

        // Find or create error display element
        let errorElement = field.parentNode.querySelector('.form-error');

        if (!errorElement) {
            errorElement = document.createElement('span');
            errorElement.className = 'form-error';
            field.parentNode.appendChild(errorElement);
        }

        errorElement.textContent = errorMessage;
        errorElement.style.display = 'block';
    },

    /**
     * Clear field validation error
     * @param {HTMLElement} field - Form field element
     */
    clearFieldError(field) {
        field.classList.remove('error', 'success');

        const errorElement = field.parentNode.querySelector('.form-error');
        if (errorElement) {
            errorElement.style.display = 'none';
        }
    },

    /**
     * Show field success state
     * @param {HTMLElement} field - Form field element
     */
    showFieldSuccess(field) {
        field.classList.remove('error');
        field.classList.add('success');

        const errorElement = field.parentNode.querySelector('.form-error');
        if (errorElement) {
            errorElement.style.display = 'none';
        }
    },

    /**
     * Real-time validation setup
     * Similar to having validation groups that trigger on different events
     *
     * @param {string|HTMLFormElement} form - Form element or ID
     * @param {Object} options - Validation options
     */
    setupRealTimeValidation(form, options = {}) {
        const formElement = typeof form === 'string' ?
            DOMUtils.getElementById(form) : form;

        if (!formElement) return;

        const defaultOptions = {
            validateOnBlur: true,
            validateOnInput: false,
            debounceTime: 300
        };

        const config = { ...defaultOptions, ...options };
        const formId = formElement.id;
        const rules = this.rules.get(formId);

        if (!rules) return;

        // Setup validation for each field
        for (const fieldName of Object.keys(rules)) {
            const field = DOMUtils.querySelector(`[name="${fieldName}"]`, formElement);
            if (!field) continue;

            // Validate on blur
            if (config.validateOnBlur) {
                DOMUtils.addEventListener(field, 'blur', () => {
                    this.validateSingleField(field, fieldName, rules[fieldName]);
                });
            }

            // Validate on input (debounced)
            if (config.validateOnInput) {
                const debouncedValidation = PerformanceUtils.debounce(() => {
                    this.validateSingleField(field, fieldName, rules[fieldName]);
                }, config.debounceTime);

                DOMUtils.addEventListener(field, 'input', debouncedValidation);
            }
        }
    },

    /**
     * Validate single field
     * @param {HTMLElement} field - Form field element
     * @param {string} fieldName - Field name
     * @param {Array} validators - Field validators
     */
    validateSingleField(field, fieldName, validators) {
        const value = this.getFieldValue(field);
        const result = this.validateField(value, validators, fieldName);

        if (!result.isValid) {
            this.showFieldError(field, result.errorMessage);
        } else {
            this.showFieldSuccess(field);
        }

        return result;
    }
};

/**
 * Pre-defined validation rule sets
 * Similar to validation groups in Spring Boot
 */
const ValidationRules = {
    // Login form validation
    login: {
        email: [
            (value) => FieldValidators.required(value, 'Email'),
            (value) => FieldValidators.email(value)
        ],
        password: [
            (value) => FieldValidators.required(value, 'Password')
        ]
    },

    // Registration form validation
    signup: {
        name: [
            (value) => FieldValidators.required(value, 'Full name'),
            (value) => FieldValidators.length(value, 2, 50, 'Full name')
        ],
        email: [
            (value) => FieldValidators.required(value, 'Email'),
            (value) => FieldValidators.email(value),
            (value) => FieldValidators.length(value, 0, 100, 'Email')
        ],
        password: [
            (value) => FieldValidators.required(value, 'Password'),
            (value) => FieldValidators.password(value)
        ],
        confirmPassword: [
            (value) => FieldValidators.required(value, 'Password confirmation')
            // Note: Password match validation is handled separately as it requires two fields
        ]
    },

    // Password reset form validation
    passwordReset: {
        newPassword: [
            (value) => FieldValidators.required(value, 'New password'),
            (value) => FieldValidators.password(value)
        ],
        confirmPassword: [
            (value) => FieldValidators.required(value, 'Password confirmation')
        ]
    },

    // Forgot password form validation
    forgotPassword: {
        email: [
            (value) => FieldValidators.required(value, 'Email'),
            (value) => FieldValidators.email(value)
        ]
    },

    // Profile update form validation
    profile: {
        name: [
            (value) => FieldValidators.required(value, 'Full name'),
            (value) => FieldValidators.length(value, 2, 50, 'Full name')
        ],
        email: [
            (value) => FieldValidators.required(value, 'Email'),
            (value) => FieldValidators.email(value)
        ],
        phone: [
            (value) => FieldValidators.phone(value)
        ],
        website: [
            (value) => FieldValidators.url(value)
        ]
    }
};

/**
 * Initialize validation for common forms
 * Similar to @PostConstruct methods in Spring Boot
 */
function initializeValidation() {
    // Register validation rules for common forms
    for (const [formType, rules] of Object.entries(ValidationRules)) {
        FormValidator.registerRules(formType, rules);
    }

    debugLog('Validation system initialized');
}

/**
 * Export validation utilities
 * Similar to exporting validation components in Spring Boot
 */
if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment (for testing)
    module.exports = {
        FieldValidators,
        FormValidator,
        ValidationRules,
        initializeValidation
    };
} else {
    // Browser environment - make available globally
    window.LinkUpValidation = {
        FieldValidators,
        FormValidator,
        ValidationRules,
        initializeValidation
    };

    // Auto-initialize when DOM is ready
    if (document.readyState === 'loading') {
        DOMUtils.addEventListener(document, 'DOMContentLoaded', initializeValidation);
    } else {
        initializeValidation();
    }
}