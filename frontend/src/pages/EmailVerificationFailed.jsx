import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AlertCircle, RefreshCw, ArrowLeft, Mail } from 'lucide-react';
import toast from 'react-hot-toast';

export function EmailVerificationFailed() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [errorMessage, setErrorMessage] = useState('');
  const [isResending, setIsResending] = useState(false);

  useEffect(() => {
    const error = searchParams.get('error');
    if (error) {
      setErrorMessage(decodeURIComponent(error));
    } else {
      setErrorMessage('Email verification failed. Please try again.');
    }
  }, [searchParams]);

  const handleResendEmail = async () => {
    const email = localStorage.getItem('pendingVerificationEmail');

    if (!email) {
      toast.error('Unable to resend email. Please register again.');
      navigate('/');
      return;
    }

    setIsResending(true);
    try {
      const response = await fetch('/api/auth/resend-verification', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email }),
      });

      if (response.ok) {
        toast.success('New verification email sent! Please check your inbox.');
        navigate(`/check-email?email=${encodeURIComponent(email)}`);
      } else if (response.status === 429) {
        toast.error('Too many requests. Please wait before requesting another email.');
      } else {
        const data = await response.json();
        toast.error(data.message || 'Failed to send verification email. Please try again.');
      }
    } catch (error) {
      console.error('Resend email error:', error);
      toast.error('Network error. Please check your connection and try again.');
    } finally {
      setIsResending(false);
    }
  };

  const getErrorType = () => {
    const msg = errorMessage.toLowerCase();
    if (msg.includes('expired')) return 'expired';
    if (msg.includes('used') || msg.includes('already')) return 'used';
    if (msg.includes('not found') || msg.includes('invalid')) return 'invalid';
    return 'general';
  };

  const getErrorDetails = () => {
    const type = getErrorType();
    switch (type) {
      case 'expired':
        return {
          title: 'Verification Link Expired',
          description: 'This verification link has expired for security reasons. Verification links are valid for 24 hours.',
          showResend: true,
        };
      case 'used':
        return {
          title: 'Link Already Used',
          description: 'This verification link has already been used. If your email is verified, you can log in directly.',
          showResend: false,
        };
      case 'invalid':
        return {
          title: 'Invalid Verification Link',
          description: 'This verification link is invalid or does not exist. Please request a new verification email.',
          showResend: true,
        };
      default:
        return {
          title: 'Verification Failed',
          description: 'We encountered an issue verifying your email. Please try again or contact support if the problem persists.',
          showResend: true,
        };
    }
  };

  const errorDetails = getErrorDetails();

  return (
    <div className="min-h-screen bg-gradient-to-br from-red-50 to-white flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl p-8 md:p-12 max-w-2xl w-full">
        {/* Error Icon */}
        <div className="flex justify-center mb-6">
          <div className="w-24 h-24 bg-red-100 rounded-full flex items-center justify-center">
            <AlertCircle className="w-14 h-14 text-red-600" />
          </div>
        </div>

        {/* Title */}
        <h1 className="text-3xl font-bold text-gray-900 text-center mb-2">
          {errorDetails.title}
        </h1>

        {/* Description */}
        <p className="text-gray-700 text-center mb-6">
          {errorDetails.description}
        </p>

        {/* Error Message Box */}
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-8">
          <p className="text-sm text-red-800">
            <strong>Error:</strong> {errorMessage}
          </p>
        </div>

        {/* Solutions */}
        <div className="bg-gray-50 rounded-lg p-6 mb-8">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            What you can do:
          </h3>
          <ul className="space-y-3">
            {errorDetails.showResend && (
              <li className="flex items-start">
                <span className="text-blue-600 font-bold mr-3">→</span>
                <span className="text-gray-700">Request a new verification email below</span>
              </li>
            )}
            <li className="flex items-start">
              <span className="text-blue-600 font-bold mr-3">→</span>
              <span className="text-gray-700">Try logging in if your email is already verified</span>
            </li>
            <li className="flex items-start">
              <span className="text-blue-600 font-bold mr-3">→</span>
              <span className="text-gray-700">Check your spam folder for the verification email</span>
            </li>
            <li className="flex items-start">
              <span className="text-blue-600 font-bold mr-3">→</span>
              <span className="text-gray-700">Contact support if the problem persists</span>
            </li>
          </ul>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4">
          {errorDetails.showResend && (
            <button
              onClick={handleResendEmail}
              disabled={isResending}
              className="flex-1 bg-blue-600 text-white py-3 px-6 rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {isResending ? (
                <>
                  <RefreshCw className="w-5 h-5 animate-spin" />
                  Sending...
                </>
              ) : (
                <>
                  <Mail className="w-5 h-5" />
                  Request New Email
                </>
              )}
            </button>
          )}
          <button
            onClick={() => navigate('/')}
            className={`${errorDetails.showResend ? 'flex-1' : 'w-full'} bg-white border-2 border-gray-300 text-gray-700 py-3 px-6 rounded-lg font-medium hover:bg-gray-50 transition-colors flex items-center justify-center gap-2`}
          >
            <ArrowLeft className="w-5 h-5" />
            Back to Login
          </button>
        </div>
      </div>
    </div>
  );
}
