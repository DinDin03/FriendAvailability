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
                {/* <button
                    className="w-full inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm bg-white text-sm font-medium text-gray-500 hover:bg-gray-50"
                    type="button"
                    onClick={handleGoogleLogin}
                >
                    <svg className="w-5 h-5" viewBox="0 0 24 24">
                        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                        <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                    </svg>
                    <span className="ml-2">Sign in with Google</span>
                </button> */}

                <GoogleLogin
                    onSuccess={handleGoogleSuccess}
                    onError={handleGoogleError}
                />
                
            </div>
        </div>
    )
    
}