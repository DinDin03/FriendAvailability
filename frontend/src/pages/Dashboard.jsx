import { useAuth } from '../contexts/AuthContext';
import { Link } from 'react-router-dom';
import { Calendar, Users, MessageCircle, Settings, LogOut, RefreshCw, TestTube, Database, AlertCircle, CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useState, useEffect } from 'react';
import { dashboardService } from '../services/dashboardService';

export const Dashboard = () => {
    const { user, logout, isLoading } = useAuth();
    const navigate = useNavigate();

    // Dashboard state management
    const [dashboardData, setDashboardData] = useState(null);
    const [isDashboardLoading, setIsDashboardLoading] = useState(true);
    const [dashboardError, setDashboardError] = useState(null);
    const [isRefreshing, setIsRefreshing] = useState(false);

    // Testing state
    const [showTestPanel, setShowTestPanel] = useState(false);
    const [testResults, setTestResults] = useState({});
    const [testInProgress, setTestInProgress] = useState({});

    // Load dashboard data on component mount or user change
    useEffect(() => {
        if (user?.id) {
            loadDashboardData();
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [user?.id]);

    const loadDashboardData = async () => {
        try {
            setIsDashboardLoading(true);
            setDashboardError(null);

            const data = await dashboardService.getDashboardData(user.id);
            setDashboardData(data);
        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            setDashboardError(error.message);
            toast.error(error.message);
        } finally {
            setIsDashboardLoading(false);
        }
    };

    const handleRefreshData = async () => {
        try {
            setIsRefreshing(true);
            const data = await dashboardService.refreshDashboardData(user.id);
            setDashboardData(data);
            toast.success('Dashboard data refreshed!');
        } catch (error) {
            console.error('Failed to refresh dashboard data:', error);
            toast.error(error.message);
        } finally {
            setIsRefreshing(false);
        }
    };

    // Test functions for dashboard service validation
    const runServiceTest = async (testName, testFunction) => {
        setTestInProgress(prev => ({ ...prev, [testName]: true }));
        try {
            const startTime = Date.now();
            const result = await testFunction();
            const endTime = Date.now();

            setTestResults(prev => ({
                ...prev,
                [testName]: {
                    success: true,
                    result,
                    duration: endTime - startTime,
                    timestamp: new Date().toISOString()
                }
            }));
            toast.success(`✅ ${testName} test passed!`);
        } catch (error) {
            setTestResults(prev => ({
                ...prev,
                [testName]: {
                    success: false,
                    error: error.message,
                    duration: 0,
                    timestamp: new Date().toISOString()
                }
            }));
            toast.error(`❌ ${testName} test failed: ${error.message}`);
        } finally {
            setTestInProgress(prev => ({ ...prev, [testName]: false }));
        }
    };

    const testGetUserProfile = () => runServiceTest('getUserProfile', async () => {
        return await dashboardService.getUserProfile(user.id);
    });

    const testGetDashboardStats = () => runServiceTest('getDashboardStats', async () => {
        return await dashboardService.getDashboardStats(user.id);
    });

    const testGetRecentActivity = () => runServiceTest('getRecentActivity', async () => {
        return await dashboardService.getRecentActivity(user.id);
    });

    const testGetDashboardData = () => runServiceTest('getDashboardData', async () => {
        return await dashboardService.getDashboardData(user.id);
    });

    const testCacheManagement = () => runServiceTest('cacheManagement', async () => {
        // Test cache operations
        const cacheKey = 'test_cache_key';
        const testData = { test: 'data', timestamp: Date.now() };

        // Test cache set
        dashboardService.cacheData(cacheKey, testData, 1000);

        // Test cache get
        const cachedData = dashboardService.getCachedData(cacheKey);
        if (!cachedData || cachedData.test !== testData.test) {
            throw new Error('Cache set/get failed');
        }

        // Test cache invalidation
        dashboardService.invalidateCache(cacheKey);
        const invalidatedData = dashboardService.getCachedData(cacheKey);
        if (invalidatedData !== null) {
            throw new Error('Cache invalidation failed');
        }

        // Test cache stats
        const stats = dashboardService.getCacheStats();

        return {
            message: 'Cache operations successful',
            stats
        };
    });

    const testErrorHandling = () => runServiceTest('errorHandling', async () => {
        try {
            // Test error handling with invalid user ID
            await dashboardService.getUserProfile('invalid-user-id');
        } catch (error) {
            // This should throw an error, which is expected
            return {
                message: 'Error handling working correctly',
                errorMessage: error.message,
                hasOriginalError: !!error.originalError,
                hasTimestamp: !!error.timestamp
            };
        }
        throw new Error('Error handling test failed - no error was thrown');
    });

    const runAllTests = async () => {
        toast.promise(
            Promise.all([
                testGetUserProfile(),
                testGetDashboardStats(),
                testGetRecentActivity(),
                testGetDashboardData(),
                testCacheManagement(),
                testErrorHandling()
            ]),
            {
                loading: 'Running all dashboard service tests...',
                success: 'All tests completed! Check individual results.',
                error: 'Some tests failed. Check individual results.'
            }
        );
    };

    if (isLoading) {
        return <div className='p-8'>Loading...</div>;
    }

    if (isDashboardLoading) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-gray-600">Loading your dashboard...</p>
                </div>
            </div>
        );
    }

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
                            <button
                                onClick={() => setShowTestPanel(!showTestPanel)}
                                className="flex items-center space-x-1 text-gray-600 hover:text-gray-900 transition-colors"
                                title="Toggle test panel"
                            >
                                <TestTube className="w-4 h-4" />
                                <span className="hidden sm:inline">Tests</span>
                            </button>
                            <button
                                onClick={handleRefreshData}
                                disabled={isRefreshing}
                                className="flex items-center space-x-1 text-gray-600 hover:text-gray-900 transition-colors disabled:opacity-50"
                                title="Refresh dashboard data"
                            >
                                <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
                                <span className="hidden sm:inline">Refresh</span>
                            </button>
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
                        Welcome back, {user?.name.split(' ')[0]}!
                    </h2>
                    <p className="text-gray-600">
                        Here's what's happening with your friends and availability.
                    </p>
                </div>

                {/* Test Panel */}
                {showTestPanel && (
                    <div className="bg-white rounded-lg shadow-sm p-6 mb-8 border-l-4 border-blue-500">
                        <div className="flex items-center justify-between mb-6">
                            <div className="flex items-center space-x-3">
                                <TestTube className="w-6 h-6 text-blue-600" />
                                <h3 className="text-xl font-semibold text-gray-900">Dashboard Service Tests</h3>
                            </div>
                            <button
                                onClick={runAllTests}
                                className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium"
                            >
                                Run All Tests
                            </button>
                        </div>

                        {/* Individual Test Buttons */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
                            <button
                                onClick={testGetUserProfile}
                                disabled={testInProgress.getUserProfile}
                                className="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:border-blue-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">User Profile</span>
                                {testInProgress.getUserProfile ? (
                                    <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.getUserProfile ? (
                                    testResults.getUserProfile.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>

                            <button
                                onClick={testGetDashboardStats}
                                disabled={testInProgress.getDashboardStats}
                                className="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:border-blue-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Dashboard Stats</span>
                                {testInProgress.getDashboardStats ? (
                                    <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.getDashboardStats ? (
                                    testResults.getDashboardStats.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>

                            <button
                                onClick={testGetRecentActivity}
                                disabled={testInProgress.getRecentActivity}
                                className="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:border-blue-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Recent Activity</span>
                                {testInProgress.getRecentActivity ? (
                                    <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.getRecentActivity ? (
                                    testResults.getRecentActivity.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>

                            <button
                                onClick={testGetDashboardData}
                                disabled={testInProgress.getDashboardData}
                                className="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:border-blue-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Dashboard Data</span>
                                {testInProgress.getDashboardData ? (
                                    <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.getDashboardData ? (
                                    testResults.getDashboardData.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>

                            <button
                                onClick={testCacheManagement}
                                disabled={testInProgress.cacheManagement}
                                className="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:border-blue-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Cache Management</span>
                                {testInProgress.cacheManagement ? (
                                    <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.cacheManagement ? (
                                    testResults.cacheManagement.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>

                            <button
                                onClick={testErrorHandling}
                                disabled={testInProgress.errorHandling}
                                className="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:border-blue-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Error Handling</span>
                                {testInProgress.errorHandling ? (
                                    <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.errorHandling ? (
                                    testResults.errorHandling.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>
                        </div>

                        {/* Test Results Display */}
                        {Object.keys(testResults).length > 0 && (
                            <div className="border-t border-gray-200 pt-6">
                                <div className="flex items-center space-x-2 mb-4">
                                    <Database className="w-5 h-5 text-gray-600" />
                                    <h4 className="text-lg font-medium text-gray-900">Test Results</h4>
                                </div>
                                <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                                    {Object.entries(testResults).map(([testName, result]) => (
                                        <div key={testName} className="bg-gray-50 rounded-lg p-4">
                                            <div className="flex items-center justify-between mb-2">
                                                <span className="font-medium text-gray-900">{testName}</span>
                                                <div className="flex items-center space-x-2">
                                                    {result.success ? (
                                                        <CheckCircle className="w-4 h-4 text-green-600" />
                                                    ) : (
                                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                                    )}
                                                    <span className="text-xs text-gray-500">
                                                        {result.duration}ms
                                                    </span>
                                                </div>
                                            </div>
                                            {result.success ? (
                                                <div className="text-sm text-gray-600">
                                                    <span className="text-green-600 font-medium">✓ Success</span>
                                                    {typeof result.result === 'object' && (
                                                        <details className="mt-2">
                                                            <summary className="cursor-pointer text-blue-600 hover:text-blue-700">
                                                                View Result
                                                            </summary>
                                                            <pre className="mt-2 text-xs bg-white p-2 rounded border overflow-x-auto">
                                                                {JSON.stringify(result.result, null, 2)}
                                                            </pre>
                                                        </details>
                                                    )}
                                                </div>
                                            ) : (
                                                <div className="text-sm">
                                                    <span className="text-red-600 font-medium">✗ Failed</span>
                                                    <p className="text-red-700 mt-1">{result.error}</p>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                )}

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

                {/* Error State */}
                {dashboardError && (
                    <div className="bg-red-50 border border-red-200 rounded-lg p-6 mb-8">
                        <div className="flex items-center space-x-3">
                            <div className="flex-shrink-0">
                                <div className="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
                                    <span className="text-red-600 text-sm">!</span>
                                </div>
                            </div>
                            <div className="flex-1">
                                <h3 className="text-sm font-medium text-red-800">Dashboard Error</h3>
                                <p className="text-sm text-red-700 mt-1">{dashboardError}</p>
                            </div>
                            <button
                                onClick={loadDashboardData}
                                className="text-sm text-red-600 hover:text-red-700 font-medium"
                            >
                                Retry
                            </button>
                        </div>
                    </div>
                )}

                {/* Recent Activity */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    <div className="bg-white rounded-lg shadow-sm p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Activity</h3>
                        <div className="space-y-4">
                            {dashboardData?.recentActivity?.length > 0 ? (
                                dashboardData.recentActivity.map((activity) => (
                                    <div key={activity.id} className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
                                        <div className={`w-2 h-2 rounded-full ${
                                            activity.icon === 'blue' ? 'bg-blue-600' :
                                            activity.icon === 'green' ? 'bg-green-600' :
                                            activity.icon === 'purple' ? 'bg-purple-600' :
                                            'bg-gray-600'
                                        }`}></div>
                                        <p className="text-sm text-gray-600">{activity.message}</p>
                                    </div>
                                ))
                            ) : (
                                <div className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
                                    <div className="w-2 h-2 bg-blue-600 rounded-full"></div>
                                    <p className="text-sm text-gray-600">No recent activity to display.</p>
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="bg-white rounded-lg shadow-sm p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Stats</h3>
                        <div className="space-y-4">
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Friends</span>
                                <span className="font-semibold text-gray-900">
                                    {dashboardData?.stats?.friendsCount ?? 0}
                                </span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Events</span>
                                <span className="font-semibold text-gray-900">
                                    {dashboardData?.stats?.eventsCount ?? 0}
                                </span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-gray-600">Messages</span>
                                <span className="font-semibold text-gray-900">
                                    {dashboardData?.stats?.messagesCount ?? 0}
                                </span>
                            </div>
                        </div>
                        {dashboardData?.stats?.lastUpdated && (
                            <div className="mt-4 pt-4 border-t border-gray-200">
                                <p className="text-xs text-gray-500">
                                    Last updated: {new Date(dashboardData.stats.lastUpdated).toLocaleTimeString()}
                                </p>
                            </div>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
};