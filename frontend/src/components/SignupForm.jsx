import { X } from "lucide-react"
import { useAuth } from "@/contexts/AuthContext";
import { useSignupForm } from '@/hooks/useSignupForm'
import { authService } from "@/services/authService";
import { GoogleLogin } from "@react-oauth/google";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";

export const SignupForm = ({ onClose }) => {

    const { updateUser } = useAuth();
    const navigate = useNavigate();
    const { formData, errors, isSubmitting, handleInputChange, handleFormSubmit } = useSignupForm();

    const handleGoogleSuccess = async (credentialResponse) => {
        try {
            const userData = await authService.googleLogin(credentialResponse);
            if (userData) {
                updateUser(userData)
                const userFirstName = userData.name.split(' ')[0];
                toast.success(`Welcome back, ${userFirstName || "User"}!`);
                navigate("/dashboard");
            }
        } catch (error) {
            console.log('Google sign-in failed, ', error)
            toast.error(error.message || "Google sign-in failed.")
        }
    } 

    const handleGoogleError = (error) => {
        console.error("Google login failed:", error);
        toast.error("Google sign-in failed. Please try again.")
    }

    return (
        <div 
            className="bg-white rounded-4xl p-8 max-w-md w-full mx-4 shadow-xl"
            onClick={(e) => e.stopPropagation()}
        >
            <div className="flex items-center mb-6 relative">
                    <h2 className="text-2xl font-bold text-gray-900 flex-1 text-center">Join LinkUp</h2>
                    <button
                        type="button"
                        onClick={onClose}
                        className="text-gray-400 hover:text-gray-600 transition-colors absolute right-0"
                    >
                        <X size={24} />
                    </button>
                </div>
            
            {/* sign up form content */}

            {/*Full Name */ }
            {/*Email*/}
            {/*Password*/}
            {/*Confirm password*/}
            {/*Sign in with google*/}

            <form 
                className="space-y-4"
                onSubmit={handleFormSubmit}
            >
                <div>
                    <label className="text-left block text-sm font-medium text-gray-700 mb-1">
                        Full Name
                    </label>
                    <input
                        type="text"
                        value={formData.fullName}
                        onChange={(e) => handleInputChange('fullName', e.target.value)}
                        className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                            errors.fullName ? 'border-red-500' : 'border-gray-300'
                        }`}
                        placeholder="Enter your full name"
                        disabled={isSubmitting}
                    />
                    {errors.fullName && (
                        <p className="text-red-500 text-sm mt-1">{errors.fullName}</p>
                    )}
                </div>

                <div>
                    <label className="text-left block text-sm font-medium text-gray-700 mb-1">
                        Email
                    </label>
                    <input
                        type="email"
                        value={formData.email}
                        onChange={(e) => handleInputChange('email', e.target.value)}
                        className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                            errors.email ? 'border-red-500' : 'border-gray-300'
                        }`}
                        placeholder="Enter your email"
                        disabled={isSubmitting}
                    />
                    {errors.email && (
                        <p className="text-red-500 text-sm mt-1">{errors.email}</p>
                    )}
                </div>
                
                <div>
                    <label className="text-left block text-sm font-medium text-gray-700 mb-1">
                        Password
                    </label>
                    <input
                        type="password"
                        value={formData.password}
                        onChange={(e) => handleInputChange('password', e.target.value)}
                        className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                            errors.password ? 'border-red-500' : 'border-gray-300'
                        }`}
                        placeholder="Create a strong password"
                        disabled={isSubmitting}
                    />
                    {errors.password && (
                        <p className="text-red-500 text-sm mt-1">{errors.password}</p>
                    )}
                </div>

                <div>
                    <label className="text-left block text-sm font-medium text-gray-700 mb-1">
                        Confirm Password
                    </label>
                    <input
                        type="password"
                        value={formData.confirmPassword}
                        onChange={(e) => handleInputChange('confirmPassword', e.target.value)}
                        className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary ${
                            errors.confirmPassword ? 'border-red-500' : 'border-gray-300'
                        }`}
                        placeholder="Confirm your password"
                        disabled={isSubmitting}
                    />
                    {errors.confirmPassword && (
                        <p className="text-red-500 text-sm mt-1">{errors.confirmPassword}</p>
                    )}
                </div>
                
                {/* Show general error message */}
                {errors.submit && (
                    <div className="bg-red-50 border border-red-200 rounded-md p-3">
                        <p className="text-red-600 text-sm">{errors.submit}</p>
                    </div>
                )}
                
                <button 
                    type="submit"
                    disabled={isSubmitting}
                    className={`w-full text-white mt-4 py-2 px-4 rounded-md transition-colors ${
                        isSubmitting 
                            ? 'bg-gray-400 cursor-not-allowed' 
                            : 'bg-blue-600 hover:bg-blue-700'
                    }`}
                >
                    {isSubmitting ? (
                        <>
                            <span className="inline-block animate-spin mr-2">⏳</span>
                            Creating Account...
                        </>
                    ) : (
                        'Create Account'
                    )}
                </button>
            </form>
                <div className="mt-10 border-t rounded-2xl border-gray-500/30"/>
                <label className="bg-white relative -top-3.5 px-5 text-gray-500">Or continue with</label>
            <div className="my-4 w-full inline-flex justify-center">
                <GoogleLogin
                    onSuccess={handleGoogleSuccess}
                    onError={handleGoogleError}
                />
            </div>
        </div>
    )
    
}