const FieldValidators = {
    required(value, fieldName = 'Field') {
        const isValid = value !== null && value !== undefined && value.toString().trim().length > 0;
        return {
            isValid,
            errorMessage: isValid ? null : `${fieldName} is required`
        };
    },

    email(email) {
        if (!email) {
            return { isValid: true, errorMessage: null };
        }
        const emailRegex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;
        const isValid = emailRegex.test(email.trim());
        return {
            isValid,
            errorMessage: isValid ? null : 'Please enter a valid email address'
        };
    },

    length(value, min = 0, max = Infinity, fieldName = 'Field') {
        if (!value) {
            return { isValid: true, errorMessage: null };
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
        if (password.length < minLength) {
            return {
                isValid: false,
                errorMessage: `Password must be at least ${minLength} characters long`,
                strength: 'weak',
                strengthScore: 0
            };
        }
        const criteria = {
            length: password.length >= 8,
            lowercase: /[a-z]/.test(password),
            uppercase: /[A-Z]/.test(password),
            numbers: /\d/.test(password),
            symbols: /[!@#$%^&*(),.?":{}|<>]/.test(password)
        };
        Object.values(criteria).forEach(met => {
            if (met) score++;
        });
        if (password.length >= 12) score++;
        if (password.length >= 16) score++;
        let strength;
        if (score <= 2) strength = 'weak';
        else if (score <= 4) strength = 'fair';
        else if (score <= 5) strength = 'good';
        else strength = 'strong';
        if (!criteria.lowercase) feedback.push('Add lowercase letters');
        if (!criteria.uppercase) feedback.push('Add uppercase letters');
        if (!criteria.numbers) feedback.push('Add numbers');
        if (!criteria.symbols) feedback.push('Add special characters');
        if (password.length < 12) feedback.push('Use 12+ characters for better security');
        return {
            isValid: score >= 3,
            errorMessage: score >= 3 ? null : 'Password is too weak. ' + feedback.join(', '),
            strength,
            strengthScore: score,
            feedback: feedback
        };
    },

    passwordConfirmation(password, confirmPassword) {
        if (!confirmPassword) {
            return { isValid: true, errorMessage: null };
        }
        const isValid = password === confirmPassword;
        return {
            isValid,
            errorMessage: isValid ? null : 'Passwords do not match'
        };
    },

    phone(phone) {
        if (!phone) {
            return { isValid: true, errorMessage: null };
        }
        const digitsOnly = phone.replace(/\D/g, '');
        const isValid = digitsOnly.length >= 10 && digitsOnly.length <= 15;
        return {
            isValid,
            errorMessage: isValid ? null : 'Please enter a valid phone number'
        };
    },

    url(url) {
        if (!url) {
            return { isValid: true, errorMessage: null };
        }
        try {
            new URL(url);
            return { isValid: true, errorMessage: null };
        } catch {
            return { isValid: false, errorMessage: 'Please enter a valid URL' };
        }
    },

    date(dateString, options = {}) {
        if (!dateString) {
            return { isValid: true, errorMessage: null };
        }
        const date = new Date(dateString);
        if (isNaN(date.getTime())) {
            return { isValid: false, errorMessage: 'Please enter a valid date' };
        }
        const now = new Date();
        if (options.future && date <= now) {
            return { isValid: false, errorMessage: 'Date must be in the future' };
        }
        if (options.past && date >= now) {
            return { isValid: false, errorMessage: 'Date must be in the past' };
        }
        if (options.min && date < new Date(options.min)) {
            return { isValid: false, errorMessage: `Date must be after ${DateUtils.formatDate(options.min)}` };
        }
        if (options.max && date > new Date(options.max)) {
            return { isValid: false, errorMessage: `Date must be before ${DateUtils.formatDate(options.max)}` };
        }
        return { isValid: true, errorMessage: null };
    },

    range(value, min = -Infinity, max = Infinity, fieldName = 'Value') {
        if (!value && value !== 0) {
            return { isValid: true, errorMessage: null };
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

const FormValidator = {
    rules: new Map(),

    registerRules(formId, rules) {
        this.rules.set(formId, rules);
        debugLog(`Registered validation rules for form: ${formId}`, rules);
    },

    validateField(value, validators, fieldName) {
        for (const validator of validators) {
            const result = validator(value, fieldName);
            if (!result.isValid) {
                return result;
            }
        }
        return { isValid: true, errorMessage: null };
    },

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
                this.showFieldError(field, result.errorMessage);
            } else {
                this.clearFieldError(field);
            }
        }
        return {
            isValid: isFormValid,
            errors,
            errorCount: Object.keys(errors).length
        };
    },

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

    showFieldError(field, errorMessage) {
        field.classList.add('error');
        let errorElement = field.parentNode.querySelector('.form-error');
        if (!errorElement) {
            errorElement = document.createElement('span');
            errorElement.className = 'form-error';
            field.parentNode.appendChild(errorElement);
        }
        errorElement.textContent = errorMessage;
        errorElement.style.display = 'block';
    },

    clearFieldError(field) {
        field.classList.remove('error', 'success');
        const errorElement = field.parentNode.querySelector('.form-error');
        if (errorElement) {
            errorElement.style.display = 'none';
        }
    },

    showFieldSuccess(field) {
        field.classList.remove('error');
        field.classList.add('success');
        const errorElement = field.parentNode.querySelector('.form-error');
        if (errorElement) {
            errorElement.style.display = 'none';
        }
    },

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
        for (const fieldName of Object.keys(rules)) {
            const field = DOMUtils.querySelector(`[name="${fieldName}"]`, formElement);
            if (!field) continue;
            if (config.validateOnBlur) {
                DOMUtils.addEventListener(field, 'blur', () => {
                    this.validateSingleField(field, fieldName, rules[fieldName]);
                });
            }
            if (config.validateOnInput) {
                const debouncedValidation = PerformanceUtils.debounce(() => {
                    this.validateSingleField(field, fieldName, rules[fieldName]);
                }, config.debounceTime);
                DOMUtils.addEventListener(field, 'input', debouncedValidation);
            }
        }
    },

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

const ValidationRules = {
    login: {
        email: [
            (value) => FieldValidators.required(value, 'Email'),
            (value) => FieldValidators.email(value)
        ],
        password: [
            (value) => FieldValidators.required(value, 'Password')
        ]
    },

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
        ]
    },

    passwordReset: {
        newPassword: [
            (value) => FieldValidators.required(value, 'New password'),
            (value) => FieldValidators.password(value)
        ],
        confirmPassword: [
            (value) => FieldValidators.required(value, 'Password confirmation')
        ]
    },

    forgotPassword: {
        email: [
            (value) => FieldValidators.required(value, 'Email'),
            (value) => FieldValidators.email(value)
        ]
    },

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

function initializeValidation() {
    for (const [formType, rules] of Object.entries(ValidationRules)) {
        FormValidator.registerRules(formType, rules);
    }
    debugLog('Validation system initialized');
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        FieldValidators,
        FormValidator,
        ValidationRules,
        initializeValidation
    };
} else {
    window.LinkUpValidation = {
        FieldValidators,
        FormValidator,
        ValidationRules,
        initializeValidation
    };
    if (document.readyState === 'loading') {
        DOMUtils.addEventListener(document, 'DOMContentLoaded', initializeValidation);
    } else {
        initializeValidation();
    }
}