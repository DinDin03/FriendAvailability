import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Mail, Clock, RefreshCw, ArrowLeft } from 'lucide-react';
import toast from 'react-hot-toast';

export function CheckEmail() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState('');
  const [isResending, setIsResending] = useState(false);
  const [timeRemaining, setTimeRemaining] = useState('24 hours');

  useEffect(() => {
    const emailParam = searchParams.get('email');
    if (emailParam) {
      setEmail(emailParam);
      localStorage.setItem('pendingVerificationEmail', emailParam);
    } else {
      const storedEmail = localStorage.getItem('pendingVerificationEmail');
      if (storedEmail) {
        setEmail(storedEmail);
      }
    }

    // Set expiry timer
    const expiryTime = localStorage.getItem('verificationExpiryTime');
    if (!expiryTime) {
      const expiry = Date.now() + (24 * 60 * 60 * 1000);
      localStorage.setItem('verificationExpiryTime', expiry.toString());
    }

    // Update countdown every minute
    const interval = setInterval(() => {
      const expiry = localStorage.getItem('verificationExpiryTime');
      if (expiry) {
        const remaining = parseInt(expiry) - Date.now();
        if (remaining > 0) {
          const hours = Math.floor(remaining / (1000 * 60 * 60));
          const minutes = Math.floor((remaining % (1000 * 60 * 60)) / (1000 * 60));

          if (hours > 0) {
            setTimeRemaining(`${hours} hour${hours !== 1 ? 's' : ''} ${minutes > 0 ? `and ${minutes} minute${minutes !== 1 ? 's' : ''}` : ''}`);
          } else if (minutes > 0) {
            setTimeRemaining(`${minutes} minute${minutes !== 1 ? 's' : ''}`);
          } else {
            setTimeRemaining('less than a minute');
          }
        } else {
          setTimeRemaining('expired');
        }
      }
    }, 60000);

    return () => clearInterval(interval);
  }, [searchParams]);

  const handleResendEmail = async () => {
    if (!email) {
      toast.error('Unable to resend email. Please try signing up again.');
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
        toast.success('Verification email sent! Please check your inbox.');

        // Reset expiry timer
        const newExpiry = Date.now() + (24 * 60 * 60 * 1000);
        localStorage.setItem('verificationExpiryTime', newExpiry.toString());
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

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-white flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl p-8 md:p-12 max-w-2xl w-full">
        {/* Icon */}
        <div className="flex justify-center mb-6">
          <div className="w-20 h-20 bg-blue-600 rounded-full flex items-center justify-center">
            <Mail className="w-10 h-10 text-white" />
          </div>
        </div>

        {/* Title */}
        <h1 className="text-3xl font-bold text-gray-900 text-center mb-2">
          Check Your Email
        </h1>

        {/* Subtitle */}
        <p className="text-lg text-blue-600 font-semibold text-center mb-6">
          Account created successfully!
        </p>

        {/* Email Display */}
        {email && (
          <div className="text-center mb-8 text-gray-700">
            We've sent a verification email to<br />
            <strong className="text-blue-600">{email}</strong>
          </div>
        )}

        {/* Instructions */}
        <div className="bg-gray-50 rounded-lg p-6 mb-8">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            What to do next:
          </h3>
          <ul className="space-y-2">
            <li className="flex items-start">
              <span className="text-blue-600 font-bold mr-3">✓</span>
              <span className="text-gray-700">Check your email inbox (and spam folder)</span>
            </li>
            <li className="flex items-start">
              <span className="text-blue-600 font-bold mr-3">✓</span>
              <span className="text-gray-700">Click the verification link in the email</span>
            </li>
            <li className="flex items-start">
              <span className="text-blue-600 font-bold mr-3">✓</span>
              <span className="text-gray-700">Your account will be activated instantly</span>
            </li>
            <li className="flex items-start">
              <span className="text-blue-600 font-bold mr-3">✓</span>
              <span className="text-gray-700">Return here to log in and start using LinkUp</span>
            </li>
          </ul>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
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
                <RefreshCw className="w-5 h-5" />
                Resend Email
              </>
            )}
          </button>
          <button
            onClick={() => navigate('/')}
            className="flex-1 bg-white border-2 border-gray-300 text-gray-700 py-3 px-6 rounded-lg font-medium hover:bg-gray-50 transition-colors flex items-center justify-center gap-2"
          >
            <ArrowLeft className="w-5 h-5" />
            Go to Login
          </button>
        </div>

        {/* Timer */}
        <div className="text-center text-sm text-gray-600 flex items-center justify-center gap-2">
          <Clock className="w-4 h-4" />
          Verification link expires in <span className="font-semibold">{timeRemaining}</span>
        </div>
      </div>
    </div>
  );
}
