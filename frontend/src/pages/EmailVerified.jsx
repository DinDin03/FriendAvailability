import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, LogIn, LayoutDashboard } from 'lucide-react';

export function EmailVerified() {
  const navigate = useNavigate();

  useEffect(() => {
    // Clean up verification tokens from localStorage
    localStorage.removeItem('pendingVerificationEmail');
    localStorage.removeItem('verificationExpiryTime');

    // Auto-redirect to login after 10 seconds if user doesn't click anything
    const timer = setTimeout(() => {
      if (!sessionStorage.getItem('userClicked')) {
        navigate('/');
      }
    }, 10000);

    return () => clearTimeout(timer);
  }, [navigate]);

  const handleClick = () => {
    sessionStorage.setItem('userClicked', 'true');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-white flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl p-8 md:p-12 max-w-2xl w-full text-center animate-in fade-in duration-500">
        {/* Success Icon */}
        <div className="flex justify-center mb-6">
          <div className="w-24 h-24 bg-green-600 rounded-full flex items-center justify-center animate-in zoom-in duration-700">
            <CheckCircle className="w-14 h-14 text-white" />
          </div>
        </div>

        {/* Title */}
        <h1 className="text-4xl font-bold text-gray-900 mb-4">
          Email Verified Successfully!
        </h1>

        {/* Subtitle */}
        <p className="text-xl text-blue-600 font-semibold mb-6">
          Your LinkUp account is now fully active
        </p>

        {/* Description */}
        <p className="text-gray-700 mb-8 leading-relaxed">
          Congratulations! Your email has been verified and you're ready to start
          coordinating with your friends like never before.
        </p>

        {/* Features List */}
        <div className="bg-gray-50 rounded-lg p-6 mb-8 text-left">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 text-center">
            What you can do now:
          </h3>
          <ul className="space-y-3">
            <li className="flex items-start">
              <span className="text-green-600 font-bold mr-3 text-xl">✓</span>
              <span className="text-gray-700">Set your availability and sync with Google Calendar</span>
            </li>
            <li className="flex items-start">
              <span className="text-green-600 font-bold mr-3 text-xl">✓</span>
              <span className="text-gray-700">Connect with friends and build your network</span>
            </li>
            <li className="flex items-start">
              <span className="text-green-600 font-bold mr-3 text-xl">✓</span>
              <span className="text-gray-700">Find perfect meeting times that work for everyone</span>
            </li>
            <li className="flex items-start">
              <span className="text-green-600 font-bold mr-3 text-xl">✓</span>
              <span className="text-gray-700">Create friend circles for easier group planning</span>
            </li>
            <li className="flex items-start">
              <span className="text-green-600 font-bold mr-3 text-xl">✓</span>
              <span className="text-gray-700">Schedule activities and events together</span>
            </li>
          </ul>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
          <button
            onClick={() => {
              handleClick();
              navigate('/');
            }}
            className="flex-1 bg-blue-600 text-white py-3 px-6 rounded-lg font-medium hover:bg-blue-700 transition-colors flex items-center justify-center gap-2"
          >
            <LogIn className="w-5 h-5" />
            Login to Your Account
          </button>
          <button
            onClick={() => {
              handleClick();
              navigate('/dashboard');
            }}
            className="flex-1 bg-white border-2 border-gray-300 text-gray-700 py-3 px-6 rounded-lg font-medium hover:bg-gray-50 transition-colors flex items-center justify-center gap-2"
          >
            <LayoutDashboard className="w-5 h-5" />
            Go to Dashboard
          </button>
        </div>

        {/* Info Text */}
        <p className="text-sm text-gray-500">
          You can safely close this window or use the buttons above to continue.
        </p>
      </div>
    </div>
  );
}
