import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/authService';

const Dashboard = () => {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadUserData();
    }, []);

    const loadUserData = async () => {
        try {
            console.log('🔍 Loading user data...');

            // Check if user is stored locally first
            const localUser = authService.getUser();
            if (localUser) {
                console.log('✅ Found local user:', localUser);
                setUser(localUser);
                setLoading(false);
                return;
            }

            // If no local user, try to get current user from backend
            console.log('📡 Fetching current user from backend...');
            const currentUser = await authService.getCurrentUser();

            if (currentUser) {
                console.log('✅ Got current user from backend:', currentUser);
                setUser(currentUser);
            } else {
                console.log('❌ No authenticated user found');
                setError('No authenticated user found');
                // Redirect to login after short delay
                setTimeout(() => navigate('/login'), 2000);
            }

        } catch (error) {
            console.error('❌ Failed to load user data:', error);
            setError('Failed to load user data: ' + error.message);
            // Redirect to login after short delay
            setTimeout(() => navigate('/login'), 2000);
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = async () => {
        try {
            console.log('🚪 Logging out...');
            await authService.logout();
            console.log('✅ Logout successful');
            navigate('/login');
        } catch (error) {
            console.error('❌ Logout error:', error);
            // Force logout even if API call fails
            navigate('/login');
        }
    };

    // Loading state
    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-gray-600">Loading dashboard...</p>
                </div>
            </div>
        );
    }

    // Error state
    if (error) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="text-center">
                    <div className="text-red-600 text-5xl mb-4">⚠️</div>
                    <h2 className="text-2xl font-bold text-gray-900 mb-2">Authentication Error</h2>
                    <p className="text-gray-600 mb-4">{error}</p>
                    <p className="text-sm text-gray-500">Redirecting to login...</p>
                </div>
            </div>
        );
    }

    // Main dashboard
    return (
        <div className="min-h-screen bg-gray-50">
            {/* Header */}
            <header className="bg-white shadow">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center py-6">
                        <div className="flex items-center">
                            <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
                        </div>
                        <div className="flex items-center space-x-4">
                            <span className="text-gray-700">Welcome, {user?.name}!</span>
                            <button
                                onClick={handleLogout}
                                className="bg-red-600 text-white px-4 py-2 rounded-md hover:bg-red-700 transition-colors"
                            >
                                Logout
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
                <div className="px-4 py-6 sm:px-0">

                    {/* Welcome Card */}
                    <div className="bg-white overflow-hidden shadow rounded-lg mb-6">
                        <div className="px-4 py-5 sm:p-6">
                            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
                                🎉 Google OAuth Success!
                            </h3>
                            <p className="text-gray-600 mb-4">
                                You have successfully signed in with Google. Here's your account information:
                            </p>
                        </div>
                    </div>

                    {/* User Info Card */}
                    <div className="bg-white overflow-hidden shadow rounded-lg">
                        <div className="px-4 py-5 sm:p-6">
                            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
                                Your Account Details
                            </h3>

                            <dl className="grid grid-cols-1 gap-x-4 gap-y-6 sm:grid-cols-2">
                                <div>
                                    <dt className="text-sm font-medium text-gray-500">Name</dt>
                                    <dd className="mt-1 text-sm text-gray-900">{user?.name || 'N/A'}</dd>
                                </div>

                                <div>
                                    <dt className="text-sm font-medium text-gray-500">Email</dt>
                                    <dd className="mt-1 text-sm text-gray-900">{user?.email || 'N/A'}</dd>
                                </div>

                                <div>
                                    <dt className="text-sm font-medium text-gray-500">User ID</dt>
                                    <dd className="mt-1 text-sm text-gray-900">{user?.id || 'N/A'}</dd>
                                </div>

                                <div>
                                    <dt className="text-sm font-medium text-gray-500">Google ID</dt>
                                    <dd className="mt-1 text-sm text-gray-900">
                                        {user?.oauthUser ? 'Connected via Google OAuth' : 'N/A'}
                                    </dd>
                                </div>

                                <div>
                                    <dt className="text-sm font-medium text-gray-500">Account Status</dt>
                                    <dd className="mt-1 text-sm text-gray-900">
                                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                                            user?.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                                        }`}>
                                            {user?.active ? 'Active' : 'Inactive'}
                                        </span>
                                    </dd>
                                </div>

                                <div>
                                    <dt className="text-sm font-medium text-gray-500">Email Verified</dt>
                                    <dd className="mt-1 text-sm text-gray-900">
                                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                                            user?.emailVerified ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                                        }`}>
                                            {user?.emailVerified ? 'Verified' : 'Pending'}
                                        </span>
                                    </dd>
                                </div>
                            </dl>
                        </div>
                    </div>

                    {/* Debug Info (helpful for development) */}
                    <div className="bg-gray-100 overflow-hidden shadow rounded-lg mt-6">
                        <div className="px-4 py-5 sm:p-6">
                            <h3 className="text-lg leading-6 font-medium text-gray-700 mb-4">
                                🔧 Debug Information
                            </h3>
                            <details className="text-sm">
                                <summary className="cursor-pointer text-gray-600 hover:text-gray-800">
                                    Click to view raw user data
                                </summary>
                                <pre className="mt-2 bg-white p-3 rounded border text-xs overflow-x-auto">
                                    {JSON.stringify(user, null, 2)}
                                </pre>
                            </details>
                        </div>
                    </div>

                </div>
            </main>
        </div>
    );
};

export default Dashboard;