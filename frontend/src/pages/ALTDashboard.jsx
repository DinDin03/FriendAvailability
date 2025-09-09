import { useAuth } from '../contexts/AuthContext';
import { Link } from 'react-router-dom';
import { Calendar, Users, MessageCircle, Settings, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

export const ALTDashboard = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        try {
            await logout();
            toast.success("Logged out successfully!")
            navigate("/");
        } catch (error) {
            console.error('Logout failed:', error);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50">
            {/* Header */}
            <header className="bg-white shadow-sm border-b">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center py-4">
                        <div className="flex items-center">
                            <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
                        </div>
                        <div className="flex items-center space-x-4">
                            <div className="flex items-center space-x-2">
                                <div className="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center">
                                    <span className="text-white text-sm font-medium">
                                        {user?.name?.charAt(0)?.toUpperCase() || 'U'}
                                    </span>
                                </div>
                                <span className="text-gray-700 font-medium">{user?.name}</span>
                            </div>
                            <button
                                onClick={handleLogout}
                                className="flex items-center space-x-1 text-gray-600 hover:text-gray-900 transition-colors"
                            >
                                <LogOut className="w-4 h-4" />
                                <span>Logout</span>
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Welcome Section */}
                <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
                    <h2 className="text-3xl font-bold text-gray-900 mb-2">
                        Welcome back, {user?.name}! 👋
                    </h2>
                    <p className="text-gray-600">
                        Here's what's happening with your friends and availability.
                    </p>
                </div>

                {/* Quick Actions Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                    <Link
                        to="/calendar"
                        className="bg-white p-6 rounded-lg shadow-sm hover:shadow-md transition-shadow border border-gray-200 hover:border-blue-300"
                    >
                        <div className="flex items-center space-x-3">
                            <div className="p-2 bg-blue-100 rounded-lg">
                                <Calendar className="w-6 h-6 text-blue-600" />
                            </div>
                            <div>
                                <h3 className="font-semibold text-gray-900">Calendar</h3>
                                <p className="text-sm text-gray-600">Manage availability</p>
                            </div>
                        </div>
                    </Link>

                    <Link
                        to="/friends"
                        className="bg-white p-6 rounded-lg shadow-sm hover:shadow-md transition-shadow border border-gray-200 hover:border-green-300"
                    >
                        <div className="flex items-center space-x-3">
                            <div className="p-2 bg-green-100 rounded-lg">
                                <Users className="w-6 h-6 text-green-600" />
                            </div>
                            <div>
                                <h3 className="font-semibold text-gray-900">Friends</h3>
                                <p className="text-sm text-gray-600">Manage connections</p>
                            </div>
                        </div>
                    </Link>

                    <Link
                        to="/chat"
                        className="bg-white p-6 rounded-lg shadow-sm hover:shadow-md transition-shadow border border-gray-200 hover:border-purple-300"
                    >
                        <div className="flex items-center space-x-3">
                            <div className="p-2 bg-purple-100 rounded-lg">
                                <MessageCircle className="w-6 h-6 text-purple-600" />
                            </div>
                            <div>
                                <h3 className="font-semibold text-gray-900">Chat</h3>
                                <p className="text-sm text-gray-600">Message friends</p>
                            </div>
                        </div>
                    </Link>

                    <Link
                        to="/settings"
                        className="bg-white p-6 rounded-lg shadow-sm hover:shadow-md transition-shadow border border-gray-200 hover:border-gray-300"
                    >
                        <div className="flex items-center space-x-3">
                            <div className="p-2 bg-gray-100 rounded-lg">
                                <Settings className="w-6 h-6 text-gray-600" />
                            </div>
                            <div>
                                <h3 className="font-semibold text-gray-900">Settings</h3>
                                <p className="text-sm text-gray-600">Account settings</p>
                            </div>
                        </div>
                    </Link>
                </div>

                {/* Recent Activity */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    <div className="bg-white rounded-lg shadow-sm p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Activity</h3>
                        <div className="space-y-4">
                            <div className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
                                <div className="w-2 h-2 bg-blue-600 rounded-full"></div>
                                <p className="text-sm text-gray-600">Welcome to Link Up! Start by adding friends.</p>
                            </div>
                            <div className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
                                <div className="w-2 h-2 bg-green-600 rounded-full"></div>
                                <p className="text-sm text-gray-600">Your account has been created successfully.</p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-white rounded-lg shadow-sm p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Stats</h3>
                        <div className="space-y-4">
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Friends</span>
                                <span className="font-semibold text-gray-900">0</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Events</span>
                                <span className="font-semibold text-gray-900">0</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Messages</span>
                                <span className="font-semibold text-gray-900">0</span>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
};