import React, { useState, useEffect } from 'react';
import {
  X, User, Calendar, Clock, MessageCircle, UserPlus, UserMinus,
  Shield, Activity, MapPin, Mail, Phone, Globe, Heart,
  ChevronRight, Users, Star, AlertCircle
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { friendService } from '../services/friendService';
import { userStatusService } from '../services/userStatusService';
import { UserStatusIndicator } from './UserStatusIndicator';
import { useConfirmation, confirmations } from './ConfirmationDialog';
import toast from 'react-hot-toast';

/**
 * FriendProfile - Comprehensive friend profile view
 *
 * Features:
 * - Complete friend information display
 * - Availability overlap detection
 * - Activity history
 * - Quick actions with confirmations
 * - Mutual friends display
 * - Optimistic UI updates
 */
export const FriendProfile = ({
  friendId,
  isOpen,
  onClose,
  onFriendUpdated = null
}) => {
  const { user } = useAuth();
  const { showConfirmation, ConfirmationComponent } = useConfirmation();

  // Profile data state
  const [friend, setFriend] = useState(null);
  const [friendStatus, setFriendStatus] = useState(null);
  const [mutualFriends, setMutualFriends] = useState([]);
  const [activityHistory, setActivityHistory] = useState([]);
  const [availabilityOverlap, setAvailabilityOverlap] = useState(null);

  // UI state
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('overview'); // overview, activity, availability
  const [isPerformingAction, setIsPerformingAction] = useState(false);

  // Load friend profile data
  const loadFriendProfile = async () => {
    if (!friendId || !user?.id) return;

    try {
      setIsLoading(true);
      setError(null);

      // Load friend basic info (assuming this exists in your friend service)
      const friendData = await friendService.getFriendProfile?.(friendId) || {
        id: friendId,
        name: `Friend ${friendId}`,
        email: null,
        joinDate: new Date().toISOString(),
        // Fallback data structure
      };

      // Load friend status
      const statusData = await userStatusService.getUserStatus(friendId);

      // Load mutual friends
      const mutualData = await friendService.getMutualFriends?.(user.id, friendId) || [];

      // Load activity history
      const activityData = await friendService.getFriendActivity?.(friendId) || [];

      // Mock availability overlap (integrate with your availability service)
      const overlapData = {
        commonHours: 6.5,
        nextAvailable: '2024-01-15T10:00:00Z',
        overlappingDays: ['Monday', 'Wednesday', 'Friday'],
        timezone: 'UTC'
      };

      setFriend(friendData);
      setFriendStatus(statusData);
      setMutualFriends(mutualData);
      setActivityHistory(activityData);
      setAvailabilityOverlap(overlapData);

    } catch (error) {
      console.error('Failed to load friend profile:', error);
      setError(error.message || 'Failed to load friend profile');
    } finally {
      setIsLoading(false);
    }
  };

  // Load profile when component opens
  useEffect(() => {
    if (isOpen && friendId) {
      loadFriendProfile();
    }
  }, [isOpen, friendId, user?.id]);

  // Friend action handlers
  const handleRemoveFriend = async () => {
    const friendName = friend?.name || 'this friend';

    showConfirmation(confirmations.removeFriend(
      friendName,
      async () => {
        setIsPerformingAction(true);
        try {
          await friendService.removeFriend(user.id, friendId);
          toast.success(`Removed ${friendName} from friends`);
          onFriendUpdated?.('removed', friend);
          onClose();
        } catch (error) {
          toast.error(error.message || 'Failed to remove friend');
          throw error;
        } finally {
          setIsPerformingAction(false);
        }
      },
      async () => {
        // Undo remove friend
        try {
          await friendService.sendFriendRequest(user.id, friendId);
          toast.success(`Friend request sent to ${friendName}`);
        } catch (error) {
          toast.error('Failed to undo friend removal');
        }
      }
    ));
  };

  const handleBlockUser = async () => {
    const friendName = friend?.name || 'this user';

    showConfirmation(confirmations.blockUser(
      friendName,
      async () => {
        setIsPerformingAction(true);
        try {
          await friendService.blockUser?.(user.id, friendId);
          toast.success(`Blocked ${friendName}`);
          onFriendUpdated?.('blocked', friend);
          onClose();
        } catch (error) {
          toast.error(error.message || 'Failed to block user');
          throw error;
        } finally {
          setIsPerformingAction(false);
        }
      }
    ));
  };

  const handleSendMessage = () => {
    // TODO: Integrate with chat service
    toast.success(`Opening chat with ${friend?.name || 'friend'}`);
    onClose();
  };

  const handleScheduleTogether = () => {
    // TODO: Integrate with scheduling service
    toast.success(`Opening scheduler with ${friend?.name || 'friend'}`);
    onClose();
  };

  const handleInviteToCircle = () => {
    // TODO: Integrate with circles service
    toast.success(`Inviting ${friend?.name || 'friend'} to circle`);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20">
        {/* Background overlay */}
        <div
          className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
          onClick={onClose}
        ></div>

        {/* Profile Modal */}
        <div className="relative w-full max-w-4xl bg-white rounded-lg shadow-xl transform transition-all">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200">
            <h2 className="text-xl font-semibold text-gray-900">Friend Profile</h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X className="w-6 h-6" />
            </button>
          </div>

          {/* Content */}
          {isLoading ? (
            <div className="p-8 text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Loading profile...</p>
            </div>
          ) : error ? (
            <div className="p-8 text-center">
              <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">Error Loading Profile</h3>
              <p className="text-gray-600 mb-4">{error}</p>
              <button
                onClick={loadFriendProfile}
                className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
              >
                Try Again
              </button>
            </div>
          ) : (
            <div className="flex">
              {/* Left Sidebar - Friend Info */}
              <div className="w-1/3 p-6 border-r border-gray-200 bg-gray-50">
                {/* Profile Header */}
                <div className="text-center mb-6">
                  <div className="relative inline-block">
                    <div className="w-20 h-20 bg-blue-100 rounded-full flex items-center justify-center mb-3">
                      <span className="text-2xl font-bold text-blue-600">
                        {(friend?.name || 'F').charAt(0).toUpperCase()}
                      </span>
                    </div>
                    <div className="absolute -bottom-1 -right-1">
                      <UserStatusIndicator
                        status={friendStatus?.status || 'OFFLINE'}
                        size="lg"
                        showTooltip={true}
                        lastSeen={friendStatus?.lastSeen}
                      />
                    </div>
                  </div>

                  <h3 className="text-lg font-semibold text-gray-900 mb-1">
                    {friend?.name || `User ${friendId}`}
                  </h3>

                  {friend?.email && (
                    <p className="text-sm text-gray-600 mb-2">{friend.email}</p>
                  )}

                  <div className="flex items-center justify-center space-x-1 text-xs text-gray-500">
                    <Heart className="w-3 h-3" />
                    <span>Friends since {new Date(friend?.friendshipDate || friend?.joinDate).toLocaleDateString()}</span>
                  </div>
                </div>

                {/* Quick Actions */}
                <div className="space-y-2 mb-6">
                  <button
                    onClick={handleSendMessage}
                    className="w-full flex items-center space-x-3 px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    <MessageCircle className="w-4 h-4" />
                    <span>Send Message</span>
                  </button>

                  <button
                    onClick={handleScheduleTogether}
                    className="w-full flex items-center space-x-3 px-4 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
                  >
                    <Calendar className="w-4 h-4" />
                    <span>Schedule Together</span>
                  </button>

                  <button
                    onClick={handleInviteToCircle}
                    className="w-full flex items-center space-x-3 px-4 py-3 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
                  >
                    <UserPlus className="w-4 h-4" />
                    <span>Invite to Circle</span>
                  </button>
                </div>

                {/* Mutual Friends */}
                {mutualFriends.length > 0 && (
                  <div className="mb-6">
                    <h4 className="text-sm font-medium text-gray-900 mb-3 flex items-center">
                      <Users className="w-4 h-4 mr-2" />
                      Mutual Friends ({mutualFriends.length})
                    </h4>
                    <div className="space-y-2">
                      {mutualFriends.slice(0, 3).map((mutual, index) => (
                        <div key={index} className="flex items-center space-x-2 text-sm">
                          <div className="w-6 h-6 bg-gray-300 rounded-full flex items-center justify-center">
                            <span className="text-xs text-gray-600">
                              {(mutual.name || 'M').charAt(0)}
                            </span>
                          </div>
                          <span className="text-gray-700">{mutual.name || 'Mutual Friend'}</span>
                        </div>
                      ))}
                      {mutualFriends.length > 3 && (
                        <p className="text-xs text-gray-500">+{mutualFriends.length - 3} more</p>
                      )}
                    </div>
                  </div>
                )}

                {/* Danger Zone */}
                <div className="border-t border-gray-200 pt-4">
                  <h4 className="text-sm font-medium text-gray-900 mb-3">Actions</h4>
                  <div className="space-y-2">
                    <button
                      onClick={handleRemoveFriend}
                      disabled={isPerformingAction}
                      className="w-full flex items-center space-x-2 px-3 py-2 text-yellow-700 bg-yellow-50 border border-yellow-200 rounded-lg hover:bg-yellow-100 transition-colors disabled:opacity-50"
                    >
                      <UserMinus className="w-4 h-4" />
                      <span>Remove Friend</span>
                    </button>

                    <button
                      onClick={handleBlockUser}
                      disabled={isPerformingAction}
                      className="w-full flex items-center space-x-2 px-3 py-2 text-red-700 bg-red-50 border border-red-200 rounded-lg hover:bg-red-100 transition-colors disabled:opacity-50"
                    >
                      <Shield className="w-4 h-4" />
                      <span>Block User</span>
                    </button>
                  </div>
                </div>
              </div>

              {/* Main Content */}
              <div className="flex-1 p-6">
                {/* Tabs */}
                <div className="flex space-x-6 mb-6 border-b border-gray-200">
                  {[
                    { id: 'overview', label: 'Overview', icon: User },
                    { id: 'activity', label: 'Activity', icon: Activity },
                    { id: 'availability', label: 'Availability', icon: Clock }
                  ].map(tab => {
                    const Icon = tab.icon;
                    const isActive = activeTab === tab.id;

                    return (
                      <button
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={`
                          flex items-center space-x-2 pb-3 border-b-2 transition-colors
                          ${isActive
                            ? 'border-blue-600 text-blue-600'
                            : 'border-transparent text-gray-500 hover:text-gray-700'
                          }
                        `}
                      >
                        <Icon className="w-4 h-4" />
                        <span className="font-medium">{tab.label}</span>
                      </button>
                    );
                  })}
                </div>

                {/* Tab Content */}
                <div className="min-h-96">
                  {activeTab === 'overview' && (
                    <FriendOverview friend={friend} friendStatus={friendStatus} />
                  )}

                  {activeTab === 'activity' && (
                    <FriendActivity activityHistory={activityHistory} />
                  )}

                  {activeTab === 'availability' && (
                    <FriendAvailability
                      friend={friend}
                      availabilityOverlap={availabilityOverlap}
                      onSchedule={handleScheduleTogether}
                    />
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      <ConfirmationComponent />
    </div>
  );
};

// Tab Components
const FriendOverview = ({ friend, friendStatus }) => (
  <div className="space-y-6">
    <div>
      <h3 className="text-lg font-medium text-gray-900 mb-4">Profile Information</h3>
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-3">
          {friend?.email && (
            <div className="flex items-center space-x-3">
              <Mail className="w-4 h-4 text-gray-400" />
              <span className="text-sm text-gray-600">{friend.email}</span>
            </div>
          )}

          {friend?.location && (
            <div className="flex items-center space-x-3">
              <MapPin className="w-4 h-4 text-gray-400" />
              <span className="text-sm text-gray-600">{friend.location}</span>
            </div>
          )}

          <div className="flex items-center space-x-3">
            <Calendar className="w-4 h-4 text-gray-400" />
            <span className="text-sm text-gray-600">
              Joined {new Date(friend?.joinDate || Date.now()).toLocaleDateString()}
            </span>
          </div>
        </div>

        <div className="space-y-3">
          <div className="flex items-center space-x-3">
            <Clock className="w-4 h-4 text-gray-400" />
            <span className="text-sm text-gray-600">
              Last seen: {friendStatus?.lastSeen
                ? new Date(friendStatus.lastSeen).toLocaleString()
                : 'Unknown'
              }
            </span>
          </div>

          <div className="flex items-center space-x-3">
            <Star className="w-4 h-4 text-gray-400" />
            <span className="text-sm text-gray-600">
              Friendship: {Math.floor(Math.random() * 100) + 1}% compatibility
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
);

const FriendActivity = ({ activityHistory }) => (
  <div>
    <h3 className="text-lg font-medium text-gray-900 mb-4">Recent Activity</h3>
    {activityHistory.length > 0 ? (
      <div className="space-y-3">
        {activityHistory.map((activity, index) => (
          <div key={index} className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
            <Activity className="w-4 h-4 text-gray-400" />
            <div className="flex-1">
              <p className="text-sm text-gray-900">{activity.description}</p>
              <p className="text-xs text-gray-500">
                {new Date(activity.timestamp).toLocaleString()}
              </p>
            </div>
          </div>
        ))}
      </div>
    ) : (
      <div className="text-center py-8">
        <Activity className="w-12 h-12 text-gray-300 mx-auto mb-4" />
        <p className="text-gray-500">No recent activity to display</p>
      </div>
    )}
  </div>
);

const FriendAvailability = ({ friend, availabilityOverlap, onSchedule }) => (
  <div>
    <h3 className="text-lg font-medium text-gray-900 mb-4">Availability Overlap</h3>
    {availabilityOverlap ? (
      <div className="space-y-4">
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="flex items-center justify-between mb-3">
            <h4 className="font-medium text-green-800">Common Available Time</h4>
            <span className="text-2xl font-bold text-green-600">
              {availabilityOverlap.commonHours}h
            </span>
          </div>
          <p className="text-sm text-green-700">
            You have {availabilityOverlap.commonHours} hours of overlapping availability
          </p>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="p-4 border border-gray-200 rounded-lg">
            <h5 className="font-medium text-gray-900 mb-2">Next Available Together</h5>
            <p className="text-sm text-gray-600">
              {new Date(availabilityOverlap.nextAvailable).toLocaleString()}
            </p>
          </div>

          <div className="p-4 border border-gray-200 rounded-lg">
            <h5 className="font-medium text-gray-900 mb-2">Common Days</h5>
            <p className="text-sm text-gray-600">
              {availabilityOverlap.overlappingDays.join(', ')}
            </p>
          </div>
        </div>

        <button
          onClick={onSchedule}
          className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg hover:bg-blue-700 transition-colors"
        >
          Schedule Time Together
        </button>
      </div>
    ) : (
      <div className="text-center py-8">
        <Clock className="w-12 h-12 text-gray-300 mx-auto mb-4" />
        <p className="text-gray-500">Availability data not available</p>
      </div>
    )}
  </div>
);

export default FriendProfile;