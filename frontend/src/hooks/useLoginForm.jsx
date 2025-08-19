import { useState } from "react";
import { authService } from "@/services/authService.js";

export const useeLoginForm = () => {

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
            const result = authService.login(formData.email, formData.password);

            console.log("Login successful");
            
            setIsSubmitting(false);

        } catch (error) {
            console.error("Login error:", error);
            setErrors({ submit: error.message });
        } finally {
            setIsSubmitting(false);
        }
    }

    return { formData, errors, isSubmitting, handleInputChange, handleFormSubmit };
}