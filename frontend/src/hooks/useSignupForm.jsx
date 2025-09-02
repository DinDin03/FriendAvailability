import { useState } from "react";
import { useNavigate } from "react-router-dom"
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
        if (!formData.email.trim()) newErrors.email = 'Email is required';
        if (!formData.password) newErrors.password = 'Password is required';
        else if (formData.password.length < 8)
            newErrors.password = 'Password must be at least 8 characters';
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
                }
            )

            // Success - close modal and show success message
            console.log('Signup successful:', result);
            
            // Reset form
            setFormData({
                fullName: '',
                email: '',
                password: '',
                confirmPassword: ''
            });
            
            // You could show a success message here
            
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