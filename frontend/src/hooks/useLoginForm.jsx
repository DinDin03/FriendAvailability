import { useState } from "react";
import { useNavigate } from "react-router-dom"
import { useAuth } from "@/contexts/AuthContext";
import { authService } from "@/services/authService.js";
import toast from "react-hot-toast";

export const useLoginForm = () => {

    const navigate = useNavigate();
    const { updateUser } = useAuth();
    const [formData, setFormData] = useState({
        email: '',
        password: '',
    });
    const [errors, setErrors] = useState({});
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleInputChange = (field, value) => {
        setFormData(prev => ({
            ...prev,
            [field]: value,
        }));

        if (errors[field]) {
            setErrors(prev => ({
                ...prev,
                [field]: null
            }));
        }
    }

    const handleFormSubmit = async (e) => {
        e.preventDefault()
        setErrors({})

        const newErrors = {}

        if (!formData.email.trim()) newErrors.email = "Email is required."
        if (!formData.password) newErrors.password = "Password is required."

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            return;
        }

        setIsSubmitting(true);

        try {
            const userData = await toast.promise(
                authService.login(formData.email, formData.password),
                {
                    loading: "Logging in...",
                    success: (data) => `Welcome back, ${data.name.split(' ')[0]}!`,
                    error: "Login failed",
                },
                {
                    success: {
                        duration: 5000,
                        icon: '😄',
                    }
                }
            );

            console.log("Login success", userData);

            // Update AuthContext with the logged-in user
            updateUser(userData);

            // Navigate to dashboard
            navigate("/dashboard", { replace: true });

        } catch (error) {
            console.error("Login error:", error);
            setErrors({ submit: error.message });
        } finally {
            setIsSubmitting(false);
        }
    }

    return { formData, errors, isSubmitting, handleInputChange, handleFormSubmit };
}