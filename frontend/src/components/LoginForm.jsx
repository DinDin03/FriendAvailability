import { X } from "lucide-react"
import { useLoginForm } from '@/hooks/useLoginForm.jsx'

export const LoginForm = ({ onClose }) => {
    const { formData, errors, isSubmitting, handleInputChange, handleFormSubmit } = useLoginForm();

    return (
        <div 
            className="bg-white rounded-4xl p-8 max-w-md w-full mx-4 shadow-xl"
            onClick={(e) => e.stopPropagation()}
        >
            <div className="flex items-center mb-6 relative">
                    <h2 className="text-2xl font-bold text-gray-900 flex-1 text-center">Login</h2>
                    <button
                        type="button"
                        onClick={onClose}
                        className="text-gray-400 hover:text-gray-600 transition-colors absolute right-0"
                    >
                        <X size={24} />
                    </button>
                </div>
            
            {/* login form content */}

            <form 
                className="space-y-4"
                onSubmit={handleFormSubmit}
            >
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
                        placeholder="Enter your password"
                        disabled={isSubmitting}
                    />
                    {errors.password && (
                        <p className="text-red-500 text-sm mt-1">{errors.password}</p>
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
                            Logging in...
                        </>
                    ) : (
                        'Log in'
                    )}
                </button>
            </form>
        </div>
    )
}