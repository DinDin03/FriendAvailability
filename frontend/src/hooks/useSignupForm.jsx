import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authService } from "@/services/authService.js";
import toast from "react-hot-toast";

export const useSignupForm = () => {
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
    });
    const [errors, setErrors] = useState({});
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleInputChange = (field, value) => {
    setFormData(prev => ({
        ...prev,
        [field]: value
    }));

    if (errors[field]) {
        setErrors(prev => ({
            ...prev,
            [field]: null
        }));
    }
    };

    const handleFormSubmit = async (e) => {
        e.preventDefault();
        setErrors({});

        const newErrors = {};

        if (!formData.fullName.trim()) newErrors.fullName = 'Full name is required';
        else if (formData.fullName.length < 2 || formData.fullName.length > 50)
            newErrors.fullName = 'Name must be between 2 and 50 characters';
        if (!formData.email.trim()) newErrors.email = 'Email is required';
        if (!formData.password) newErrors.password = 'Password is required';
        else if (formData.password.length < 8 || formData.password.length > 100)
            newErrors.password = 'Password must be between 8 and 100 characters';
        if (formData.password !== formData.confirmPassword) newErrors.confirmPassword = 'Passwords do not match';

        // If there are validation errors, don't submit
        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            return;
        }

        // Start submission
        setIsSubmitting(true);
        try {
            const result = await toast.promise(
                authService.register({
                name: formData.fullName,
                email: formData.email,
                password: formData.password
            }),
                {
                    loading: "Signing up...",
                    success: "Registration successful! Please check your email for verification.",
                    error: "Sign up failed",
                },
                {
                    success: {
                        duration: 5000
                    }
                }
            )

            // Success - store email and redirect to check email page
            console.log('Signup successful:', result);

            // Store email in localStorage for the CheckEmail page
            localStorage.setItem('pendingVerificationEmail', formData.email);

            // Set expiry time (24 hours from now)
            const expiryTime = new Date();
            expiryTime.setHours(expiryTime.getHours() + 24);
            localStorage.setItem('verificationExpiryTime', expiryTime.toISOString());

            // Navigate to check-email page
            navigate(`/check-email?email=${encodeURIComponent(formData.email)}`)
            
            } catch (error) {
                // Handle error
                console.error('Signup error:', error);
                setErrors({ submit: error.message });
            } finally {
                setIsSubmitting(false);
            }
    };

    return { formData, errors, isSubmitting, handleInputChange, handleFormSubmit };
}