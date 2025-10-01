import { useAuth } from '../contexts/AuthContext';
import { Link } from 'react-router-dom';
import { Calendar, Users, MessageCircle, Settings, LogOut, RefreshCw, TestTube, Database, AlertCircle, CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useState, useEffect } from 'react';
import { dashboardService } from '../services/dashboardService';
import { friendService } from '../services/friendService';
import { userStatusService } from '../services/userStatusService';
import { ActivityFeed } from '../components/ActivityFeed/ActivityFeed';
import { UserStatusIndicator, UserStatusSelector } from '../components/UserStatusIndicator';

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

    // Friend request state
    const [friendRequestUserId, setFriendRequestUserId] = useState('');
    const [isSendingRequest, setIsSendingRequest] = useState(false);

    // Pending requests state
    const [pendingRequests, setPendingRequests] = useState([]);
    const [isLoadingRequests, setIsLoadingRequests] = useState(false);
    const [processingRequests, setProcessingRequests] = useState(new Set());

    // User status state
    const [userStatus, setUserStatus] = useState('OFFLINE');
    const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
    const [statusError, setStatusError] = useState(null);

    // Load dashboard data on component mount or user change
    useEffect(() => {
        if (user?.id) {
            loadDashboardData();
            loadPendingRequests();
            loadUserStatus();
            // Start heartbeat for online presence
            userStatusService.startHeartbeat(user.id);
        }

        // Cleanup heartbeat on component unmount
        return () => {
            userStatusService.stopHeartbeat();
        };
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

    // Load pending friend requests
    const loadPendingRequests = async () => {
        try {
            setIsLoadingRequests(true);
            const requests = await friendService.getPendingRequests(user.id);
            setPendingRequests(requests);
        } catch (error) {
            console.error('Failed to load pending requests:', error);
            toast.error('Failed to load pending requests');
        } finally {
            setIsLoadingRequests(false);
        }
    };

    // Load current user status
    const loadUserStatus = async () => {
        try {
            setStatusError(null);
            const statusData = await userStatusService.getUserStatus(user.id);
            setUserStatus(statusData.status || 'OFFLINE');
        } catch (error) {
            console.error('Failed to load user status:', error);
            setStatusError('Failed to load status');
            // Default to OFFLINE if we can't load status
            setUserStatus('OFFLINE');
        }
    };

    // Update user status
    const handleStatusChange = async (newStatus) => {
        if (newStatus === userStatus) return; // No change needed

        try {
            setIsUpdatingStatus(true);
            setStatusError(null);

            await userStatusService.updateUserStatus(user.id, newStatus);
            setUserStatus(newStatus);

            toast.success(`Status updated to ${newStatus.toLowerCase().replace('_', ' ')}`);

        } catch (error) {
            console.error('Failed to update status:', error);
            setStatusError(error.message);
            toast.error(error.message || 'Failed to update status');
        } finally {
            setIsUpdatingStatus(false);
        }
    };

    // Accept friend request
    const handleAcceptRequest = async (requestId) => {
        try {
            setProcessingRequests(prev => new Set([...prev, requestId]));

            await friendService.acceptFriendRequest(requestId, user.id);

            toast.success('Friend request accepted!');

            // Remove the accepted request from the list
            setPendingRequests(prev => prev.filter(req => req.id !== requestId));

        } catch (error) {
            console.error('Failed to accept friend request:', error);
            toast.error(error.message || 'Failed to accept friend request');
        } finally {
            setProcessingRequests(prev => {
                const newSet = new Set(prev);
                newSet.delete(requestId);
                return newSet;
            });
        }
    };

    // Reject friend request
    const handleRejectRequest = async (requestId) => {
        try {
            setProcessingRequests(prev => new Set([...prev, requestId]));

            await friendService.rejectFriendRequest(requestId, user.id);

            toast.success('Friend request rejected');

            // Remove the rejected request from the list
            setPendingRequests(prev => prev.filter(req => req.id !== requestId));

        } catch (error) {
            console.error('Failed to reject friend request:', error);
            toast.error(error.message || 'Failed to reject friend request');
        } finally {
            setProcessingRequests(prev => {
                const newSet = new Set(prev);
                newSet.delete(requestId);
                return newSet;
            });
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

    // Friend Service Tests
    const testGetFriends = () => runServiceTest('getFriends', async () => {
        return await friendService.getFriends(user.id, { page: 1, limit: 10 });
    });

    const testGetPendingRequests = () => runServiceTest('getPendingRequests', async () => {
        return await friendService.getPendingRequests(user.id);
    });

    const testFriendshipStatistics = () => runServiceTest('friendshipStatistics', async () => {
        return await friendService.getFriendshipStatistics(user.id);
    });

    const testFriendRecommendations = () => runServiceTest('friendRecommendations', async () => {
        return await friendService.getFriendRecommendations(user.id, 5);
    });

    const testFriendSearch = () => runServiceTest('friendSearch', async () => {
        return await friendService.searchFriends('test', {
            userId: user.id,
            filters: ['non-friends'],
            sort: 'name',
            limit: 5
        });
    });

    const testFriendCacheManagement = () => runServiceTest('friendCacheManagement', async () => {
        // Test cache operations
        const cacheKey = 'test_friend_cache';
        const testData = { test: 'friend_data', timestamp: Date.now() };

        // Test cache set
        friendService.cacheData(cacheKey, testData, 1000);

        // Test cache get
        const cachedData = friendService.getCachedData(cacheKey);
        if (!cachedData || cachedData.test !== testData.test) {
            throw new Error('Friend cache set/get failed');
        }

        // Test cache invalidation
        friendService.invalidateCache(cacheKey);
        const invalidatedData = friendService.getCachedData(cacheKey);
        if (invalidatedData !== null) {
            throw new Error('Friend cache invalidation failed');
        }

        // Test cache stats
        const stats = friendService.getCacheStats();

        return {
            message: 'Friend cache operations successful',
            stats
        };
    });

    // Friend request function
    const handleSendFriendRequest = async () => {
        if (!friendRequestUserId.trim()) {
            toast.error('Please enter a user ID');
            return;
        }

        const targetUserId = parseInt(friendRequestUserId.trim());
        if (isNaN(targetUserId)) {
            toast.error('Please enter a valid numeric user ID');
            return;
        }

        if (targetUserId === user?.id) {
            toast.error('You cannot send a friend request to yourself');
            return;
        }

        try {
            setIsSendingRequest(true);

            await friendService.sendFriendRequest(user.id, targetUserId);

            toast.success(`Friend request sent to user ${targetUserId}!`);
            setFriendRequestUserId(''); // Clear the input

        } catch (error) {
            console.error('Failed to send friend request:', error);
            toast.error(error.message || 'Failed to send friend request');
        } finally {
            setIsSendingRequest(false);
        }
    };

    const runAllTests = async () => {
        toast.promise(
            Promise.all([
                testGetUserProfile(),
                testGetDashboardStats(),
                testGetRecentActivity(),
                testGetDashboardData(),
                testCacheManagement(),
                testErrorHandling(),
                testGetFriends(),
                testGetPendingRequests(),
                testFriendshipStatistics(),
                testFriendRecommendations(),
                testFriendSearch(),
                testFriendCacheManagement()
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
            // Set user offline before logout
            if (user?.id) {
                await userStatusService.setUserOffline(user.id);
            }

            await logout();
            toast.success("Logged out successfully!")
            navigate("/");
        } catch (error) {
            console.error('Logout failed:', error);
            // Continue with logout even if status update fails
            try {
                await logout();
                navigate("/");
            } catch (logoutError) {
                console.error('Final logout failed:', logoutError);
            }
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
                            <div className="flex items-center space-x-4">
                                {/* User Status Selector */}
                                <div className="flex items-center space-x-2">
                                    <UserStatusIndicator
                                        status={userStatus}
                                        size="md"
                                        showTooltip={true}
                                    />
                                    <div className="hidden sm:block min-w-0">
                                        <select
                                            value={userStatus}
                                            onChange={(e) => handleStatusChange(e.target.value)}
                                            disabled={isUpdatingStatus}
                                            className="text-sm border border-gray-300 rounded px-2 py-1 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50"
                                        >
                                            <option value="ONLINE">Online</option>
                                            <option value="AWAY">Away</option>
                                            <option value="BUSY">Busy</option>
                                            <option value="DO_NOT_DISTURB">Do Not Disturb</option>
                                            <option value="OFFLINE">Offline</option>
                                        </select>
                                    </div>
                                    {isUpdatingStatus && (
                                        <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
                                    )}
                                </div>

                                {/* User Info */}
                                <div className="flex items-center space-x-2">
                                    <div className="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center">
                                        <span className="text-white text-sm font-medium">
                                            {user?.name?.charAt(0)?.toUpperCase() || 'U'}
                                        </span>
                                    </div>
                                    <span className="text-gray-700 font-medium">{user?.name}</span>
                                </div>
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

                {/* Friend Request Section */}
                <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
                    <div className="flex items-center space-x-3 mb-4">
                        <Users className="w-6 h-6 text-green-600" />
                        <h3 className="text-lg font-semibold text-gray-900">Send Friend Request</h3>
                    </div>
                    <div className="flex space-x-4">
                        <div className="flex-1">
                            <label htmlFor="friendUserId" className="block text-sm font-medium text-gray-700 mb-2">
                                User ID
                            </label>
                            <input
                                id="friendUserId"
                                type="text"
                                value={friendRequestUserId}
                                onChange={(e) => setFriendRequestUserId(e.target.value)}
                                placeholder="Enter user ID"
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500"
                                disabled={isSendingRequest}
                            />
                        </div>
                        <div className="flex items-end">
                            <button
                                onClick={handleSendFriendRequest}
                                disabled={isSendingRequest || !friendRequestUserId.trim()}
                                className="bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
                            >
                                {isSendingRequest ? (
                                    <>
                                        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                        <span>Sending...</span>
                                    </>
                                ) : (
                                    <>
                                        <Users className="w-4 h-4" />
                                        <span>Send Request</span>
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                    <p className="text-sm text-gray-600 mt-2">
                        Enter the user ID of the person you want to send a friend request to.
                    </p>
                </div>

                {/* Pending Friend Requests Section */}
                <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
                    <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center space-x-3">
                            <MessageCircle className="w-6 h-6 text-purple-600" />
                            <h3 className="text-lg font-semibold text-gray-900">Pending Friend Requests</h3>
                            {pendingRequests.length > 0 && (
                                <span className="bg-purple-100 text-purple-800 text-xs font-medium px-2 py-1 rounded-full">
                                    {pendingRequests.length}
                                </span>
                            )}
                        </div>
                        <button
                            onClick={loadPendingRequests}
                            disabled={isLoadingRequests}
                            className="text-purple-600 hover:text-purple-700 flex items-center space-x-1 text-sm"
                        >
                            <RefreshCw className={`w-4 h-4 ${isLoadingRequests ? 'animate-spin' : ''}`} />
                            <span>Refresh</span>
                        </button>
                    </div>

                    {isLoadingRequests ? (
                        <div className="flex items-center justify-center py-8">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600"></div>
                            <span className="ml-3 text-gray-600">Loading requests...</span>
                        </div>
                    ) : pendingRequests.length === 0 ? (
                        <div className="text-center py-8">
                            <MessageCircle className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                            <p className="text-gray-500">No pending friend requests</p>
                            <p className="text-sm text-gray-400">When someone sends you a friend request, it will appear here.</p>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            {pendingRequests.map((request) => (
                                <div key={request.id} className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:border-purple-300 transition-colors">
                                    <div className="flex items-center space-x-3">
                                        <div className="w-10 h-10 bg-purple-100 rounded-full flex items-center justify-center">
                                            <Users className="w-5 h-5 text-purple-600" />
                                        </div>
                                        <div>
                                            <p className="font-medium text-gray-900">
                                                Friend request from {request.friendName || `User ${request.userId}`}
                                            </p>
                                            <p className="text-sm text-gray-500">
                                                {new Date(request.createdAt).toLocaleDateString()} at {new Date(request.createdAt).toLocaleTimeString()}
                                            </p>
                                        </div>
                                    </div>
                                    <div className="flex items-center space-x-2">
                                        <button
                                            onClick={() => handleAcceptRequest(request.id)}
                                            disabled={processingRequests.has(request.id)}
                                            className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
                                        >
                                            {processingRequests.has(request.id) ? (
                                                <>
                                                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                                    <span>Processing...</span>
                                                </>
                                            ) : (
                                                <>
                                                    <CheckCircle className="w-4 h-4" />
                                                    <span>Accept</span>
                                                </>
                                            )}
                                        </button>
                                        <button
                                            onClick={() => handleRejectRequest(request.id)}
                                            disabled={processingRequests.has(request.id)}
                                            className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
                                        >
                                            {processingRequests.has(request.id) ? (
                                                <>
                                                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                                    <span>Processing...</span>
                                                </>
                                            ) : (
                                                <>
                                                    <AlertCircle className="w-4 h-4" />
                                                    <span>Reject</span>
                                                </>
                                            )}
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Test Panel */}
                {showTestPanel && (
                    <div className="bg-white rounded-lg shadow-sm p-6 mb-8 border-l-4 border-blue-500">
                        <div className="flex items-center justify-between mb-6">
                            <div className="flex items-center space-x-3">
                                <TestTube className="w-6 h-6 text-blue-600" />
                                <h3 className="text-xl font-semibold text-gray-900">Service Layer Tests</h3>
                                <span className="text-sm text-gray-600">(Dashboard & Friend Services)</span>
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

                            {/* Friend Service Tests */}
                            <button
                                onClick={testGetFriends}
                                disabled={testInProgress.getFriends}
                                className="flex items-center justify-between p-3 border border-green-200 rounded-lg hover:border-green-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Get Friends</span>
                                {testInProgress.getFriends ? (
                                    <div className="w-4 h-4 border-2 border-green-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.getFriends ? (
                                    testResults.getFriends.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>

                            <button
                                onClick={testGetPendingRequests}
                                disabled={testInProgress.getPendingRequests}
                                className="flex items-center justify-between p-3 border border-green-200 rounded-lg hover:border-green-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Pending Requests</span>
                                {testInProgress.getPendingRequests ? (
                                    <div className="w-4 h-4 border-2 border-green-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.getPendingRequests ? (
                                    testResults.getPendingRequests.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>

                            <button
                                onClick={testFriendshipStatistics}
                                disabled={testInProgress.friendshipStatistics}
                                className="flex items-center justify-between p-3 border border-green-200 rounded-lg hover:border-green-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Friend Statistics</span>
                                {testInProgress.friendshipStatistics ? (
                                    <div className="w-4 h-4 border-2 border-green-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.friendshipStatistics ? (
                                    testResults.friendshipStatistics.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>

                            <button
                                onClick={testFriendRecommendations}
                                disabled={testInProgress.friendRecommendations}
                                className="flex items-center justify-between p-3 border border-green-200 rounded-lg hover:border-green-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Friend Recommendations</span>
                                {testInProgress.friendRecommendations ? (
                                    <div className="w-4 h-4 border-2 border-green-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.friendRecommendations ? (
                                    testResults.friendRecommendations.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>

                            <button
                                onClick={testFriendSearch}
                                disabled={testInProgress.friendSearch}
                                className="flex items-center justify-between p-3 border border-green-200 rounded-lg hover:border-green-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Friend Search</span>
                                {testInProgress.friendSearch ? (
                                    <div className="w-4 h-4 border-2 border-green-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.friendSearch ? (
                                    testResults.friendSearch.success ?
                                        <CheckCircle className="w-4 h-4 text-green-600" /> :
                                        <AlertCircle className="w-4 h-4 text-red-600" />
                                ) : null}
                            </button>

                            <button
                                onClick={testFriendCacheManagement}
                                disabled={testInProgress.friendCacheManagement}
                                className="flex items-center justify-between p-3 border border-green-200 rounded-lg hover:border-green-300 transition-colors disabled:opacity-50"
                            >
                                <span className="text-sm font-medium">Friend Cache</span>
                                {testInProgress.friendCacheManagement ? (
                                    <div className="w-4 h-4 border-2 border-green-600 border-t-transparent rounded-full animate-spin"></div>
                                ) : testResults.friendCacheManagement ? (
                                    testResults.friendCacheManagement.success ?
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

                {/* Activity Feed and Quick Stats */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Activity Feed - Takes 2/3 of the space */}
                    <div className="lg:col-span-2">
                        <ActivityFeed
                            className="h-full"
                            maxHeight="max-h-[600px]"
                            showFilters={true}
                            showGrouping={true}
                            autoRefresh={true}
                            pageSize={15}
                        />
                    </div>

                    {/* Quick Stats - Takes 1/3 of the space */}
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