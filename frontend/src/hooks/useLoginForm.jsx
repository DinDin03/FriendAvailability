import { useState } from "react";
import { useNavigate } from "react-router-dom"
import { authService } from "@/services/authService.js";
import toast from "react-hot-toast";

export const useLoginForm = () => {
    
    const navigate = useNavigate();
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
            setFormData(prev => ({
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
            const result = await toast.promise(
                authService.login(formData.email, formData.password),
                {
                    loading: "Logging in...",
                    success: "Welcome back!",
                    error: "Login failed",
                }
            );

            console.log("Login success", result);
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