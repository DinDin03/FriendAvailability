import React, { useState, useEffect, useCallback } from 'react';
import { Users, Search, Filter, MessageCircle, Calendar, UserPlus, MoreVertical, RefreshCw, Eye, UserMinus } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { friendService } from '../services/friendService';
import { userStatusService } from '../services/userStatusService';
import { UserStatusIndicator } from './UserStatusIndicator';
import { FriendProfile } from './FriendProfile';
import { useConfirmation, confirmations } from './ConfirmationDialog';
import toast from 'react-hot-toast';

/**
 * FriendsList - Comprehensive friends management component
 *
 * Features:
 * - Friends list with real-time status indicators
 * - Search and filtering capabilities
 * - Pagination for large friend lists
 * - Quick action buttons for friend interactions
 * - Optimistic UI updates for better UX
 */
export const FriendsList = ({
  className = '',
  showQuickActions = true,
  maxHeight = 'max-h-96',
  compact = false
}) => {
  const { user } = useAuth();
  const { showConfirmation, ConfirmationComponent } = useConfirmation();

  // Friends data state
  const [friends, setFriends] = useState([]);
  const [friendStatuses, setFriendStatuses] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // UI state
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all'); // all, online, available
  const [sortBy, setSortBy] = useState('name'); // name, recent, status
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const [hasMorePages, setHasMorePages] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  // Friend profile modal state
  const [selectedFriendId, setSelectedFriendId] = useState(null);
  const [showFriendProfile, setShowFriendProfile] = useState(false);

  const itemsPerPage = compact ? 5 : 10;

  // Load friends and their statuses
  const loadFriends = useCallback(async (page = 1, append = false) => {
    if (!user?.id) return;

    try {
      if (!append) {
        setIsLoading(true);
        setError(null);
      } else {
        setIsLoadingMore(true);
      }

      // Load friends with pagination and filtering
      const friendsData = await friendService.getFriends(user.id, {
        page,
        limit: itemsPerPage,
        sort: sortBy,
        filter: filterStatus
      });

      // Extract friend IDs for status lookup
      const friendIds = friendsData.friends?.map(friend => friend.friendId || friend.userId) || [];

      // Load statuses for all friends in batch
      let statusData = {};
      if (friendIds.length > 0) {
        try {
          const statusResponse = await userStatusService.getBatchUserStatuses(friendIds);
          statusData = statusResponse.statuses || {};
        } catch (statusError) {
          console.warn('Failed to load friend statuses:', statusError);
          // Continue without status data rather than failing completely
        }
      }

      if (append) {
        setFriends(prev => [...prev, ...(friendsData.friends || [])]);
      } else {
        setFriends(friendsData.friends || []);
      }

      setFriendStatuses(prev => ({ ...prev, ...statusData }));
      setHasMorePages(page < Math.ceil((friendsData.totalCount || 0) / itemsPerPage));
      setCurrentPage(page);

    } catch (error) {
      console.error('Failed to load friends:', error);
      setError(error.message || 'Failed to load friends');
      if (!append) {
        setFriends([]);
      }
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  }, [user?.id, itemsPerPage, sortBy, filterStatus]);

  // Load more friends (pagination)
  const loadMoreFriends = useCallback(() => {
    if (!isLoadingMore && hasMorePages) {
      loadFriends(currentPage + 1, true);
    }
  }, [currentPage, hasMorePages, isLoadingMore, loadFriends]);

  // Refresh friends list
  const refreshFriends = useCallback(async () => {
    setIsRefreshing(true);
    await loadFriends(1, false);
    setIsRefreshing(false);
    toast.success('Friends list refreshed');
  }, [loadFriends]);

  // Load friends when component mounts or dependencies change
  useEffect(() => {
    loadFriends(1, false);
  }, [loadFriends]);

  // Filter friends based on search term
  const filteredFriends = friends.filter(friend => {
    if (!searchTerm) return true;

    const friendName = friend.name || friend.email || `User ${friend.friendId || friend.userId}`;
    return friendName.toLowerCase().includes(searchTerm.toLowerCase());
  });

  // Get friend status with fallback
  const getFriendStatus = (friend) => {
    const friendId = friend.friendId || friend.userId;
    const status = friendStatuses[friendId];
    return status?.status || 'OFFLINE';
  };

  // Get friend last seen
  const getFriendLastSeen = (friend) => {
    const friendId = friend.friendId || friend.userId;
    const status = friendStatuses[friendId];
    return status?.lastSeen;
  };

  // Friend profile handlers
  const handleViewProfile = (friend) => {
    const friendId = friend.friendId || friend.userId || friend.id;
    setSelectedFriendId(friendId);
    setShowFriendProfile(true);
  };

  const handleFriendUpdated = (action, friendData) => {
    if (action === 'removed' || action === 'blocked') {
      // Remove friend from local state (optimistic update)
      setFriends(prev => prev.filter(f =>
        (f.friendId || f.userId || f.id) !== (friendData.friendId || friendData.userId || friendData.id)
      ));

      // Remove from status cache
      const friendId = friendData.friendId || friendData.userId || friendData.id;
      setFriendStatuses(prev => {
        const updated = { ...prev };
        delete updated[friendId];
        return updated;
      });
    }
  };

  // Quick action handlers with optimistic updates
  const handleMessageFriend = (friend) => {
    // TODO: Integrate with chat service
    toast.success(`Opening chat with ${friend.name || 'friend'}`);
  };

  const handleCheckAvailability = (friend) => {
    // TODO: Integrate with availability service
    toast.success(`Checking availability for ${friend.name || 'friend'}`);
  };

  const handleInviteToCircle = (friend) => {
    // TODO: Integrate with circles service
    toast.success(`Inviting ${friend.name || 'friend'} to circle`);
  };

  const handleRemoveFriend = (friend) => {
    const friendName = friend.name || 'this friend';
    const friendId = friend.friendId || friend.userId || friend.id;

    showConfirmation(confirmations.removeFriend(
      friendName,
      async () => {
        // Optimistic update - remove immediately
        setFriends(prev => prev.filter(f =>
          (f.friendId || f.userId || f.id) !== friendId
        ));

        try {
          await friendService.removeFriend(user.id, friendId);
          toast.success(`Removed ${friendName} from friends`);
        } catch (error) {
          // Revert optimistic update on error
          loadFriends(1, false);
          toast.error(error.message || 'Failed to remove friend');
          throw error;
        }
      },
      async () => {
        // Undo - send friend request again
        try {
          await friendService.sendFriendRequest(user.id, friendId);
          // Reload friends to get updated state
          loadFriends(1, false);
          toast.success(`Friend request sent to ${friendName}`);
        } catch (error) {
          toast.error('Failed to undo friend removal');
        }
      }
    ));
  };

  if (error) {
    return (
      <div className={`bg-white rounded-lg shadow-sm p-6 ${className}`}>
        <div className="text-center">
          <Users className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">Error Loading Friends</h3>
          <p className="text-gray-600 mb-4">{error}</p>
          <button
            onClick={() => loadFriends(1, false)}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-white rounded-lg shadow-sm ${className}`}>
      {/* Header */}
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <Users className="w-6 h-6 text-blue-600" />
            <h3 className="text-lg font-semibold text-gray-900">
              Friends {friends.length > 0 && `(${friends.length})`}
            </h3>
          </div>

          <button
            onClick={refreshFriends}
            disabled={isRefreshing}
            className="text-gray-400 hover:text-gray-600 transition-colors disabled:opacity-50"
            title="Refresh friends list"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          </button>
        </div>

        {!compact && (
          <>
            {/* Search */}
            <div className="relative mb-4">
              <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Search friends..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            {/* Filters */}
            <div className="flex items-center space-x-4 text-sm">
              <div className="flex items-center space-x-2">
                <Filter className="w-4 h-4 text-gray-400" />
                <select
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value)}
                  className="border border-gray-300 rounded px-2 py-1 focus:ring-2 focus:ring-blue-500"
                >
                  <option value="all">All Friends</option>
                  <option value="online">Online Only</option>
                  <option value="available">Available</option>
                </select>
              </div>

              <div className="flex items-center space-x-2">
                <span className="text-gray-500">Sort:</span>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  className="border border-gray-300 rounded px-2 py-1 focus:ring-2 focus:ring-blue-500"
                >
                  <option value="name">Name</option>
                  <option value="recent">Recent Activity</option>
                  <option value="status">Status</option>
                </select>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Friends List */}
      <div className={`${maxHeight} overflow-y-auto`}>
        {isLoading ? (
          <div className="p-6 text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p className="text-gray-600">Loading friends...</p>
          </div>
        ) : filteredFriends.length === 0 ? (
          <div className="p-6 text-center">
            <Users className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              {searchTerm ? 'No friends found' : 'No friends yet'}
            </h3>
            <p className="text-gray-600">
              {searchTerm
                ? 'Try adjusting your search or filters'
                : 'Start building your network by sending friend requests'
              }
            </p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
            {filteredFriends.map((friend) => (
              <FriendItem
                key={friend.id || friend.friendId}
                friend={friend}
                status={getFriendStatus(friend)}
                lastSeen={getFriendLastSeen(friend)}
                compact={compact}
                showQuickActions={showQuickActions}
                onMessage={handleMessageFriend}
                onCheckAvailability={handleCheckAvailability}
                onInviteToCircle={handleInviteToCircle}
                onViewProfile={handleViewProfile}
                onRemoveFriend={handleRemoveFriend}
              />
            ))}
          </div>
        )}

        {/* Load More Button */}
        {hasMorePages && !isLoading && (
          <div className="p-4 border-t border-gray-200">
            <button
              onClick={loadMoreFriends}
              disabled={isLoadingMore}
              className="w-full bg-gray-100 hover:bg-gray-200 text-gray-700 py-2 px-4 rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center space-x-2"
            >
              {isLoadingMore ? (
                <>
                  <div className="w-4 h-4 border-2 border-gray-600 border-t-transparent rounded-full animate-spin"></div>
                  <span>Loading...</span>
                </>
              ) : (
                <>
                  <UserPlus className="w-4 h-4" />
                  <span>Load More Friends</span>
                </>
              )}
            </button>
          </div>
        )}
      </div>

      {/* Friend Profile Modal */}
      <FriendProfile
        friendId={selectedFriendId}
        isOpen={showFriendProfile}
        onClose={() => setShowFriendProfile(false)}
        onFriendUpdated={handleFriendUpdated}
      />

      <ConfirmationComponent />
    </div>
  );
};

/**
 * Individual Friend Item Component
 */
const FriendItem = ({
  friend,
  status,
  lastSeen,
  compact = false,
  showQuickActions = true,
  onMessage,
  onCheckAvailability,
  onInviteToCircle,
  onViewProfile,
  onRemoveFriend
}) => {
  const [showActions, setShowActions] = useState(false);

  const friendName = friend.name || friend.email || `User ${friend.friendId || friend.userId}`;

  return (
    <div className="p-4 hover:bg-gray-50 transition-colors">
      <div className="flex items-center justify-between">
        {/* Friend Info */}
        <div className="flex items-center space-x-3 flex-1 min-w-0">
          <div className="relative">
            <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
              <span className="text-blue-600 font-medium">
                {friendName.charAt(0).toUpperCase()}
              </span>
            </div>
            <div className="absolute -bottom-1 -right-1">
              <UserStatusIndicator
                status={status}
                size="sm"
                showTooltip={true}
                lastSeen={lastSeen}
              />
            </div>
          </div>

          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-900 truncate">
              {friendName}
            </p>
            {!compact && (
              <p className="text-xs text-gray-500 truncate">
                {status === 'ONLINE' ? 'Active now' : `Status: ${status.toLowerCase().replace('_', ' ')}`}
              </p>
            )}
          </div>
        </div>

        {/* Quick Actions */}
        {showQuickActions && (
          <div className="flex items-center space-x-1">
            {!compact && (
              <>
                <button
                  onClick={() => onMessage(friend)}
                  className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                  title="Send message"
                >
                  <MessageCircle className="w-4 h-4" />
                </button>
                <button
                  onClick={() => onCheckAvailability(friend)}
                  className="p-2 text-gray-400 hover:text-green-600 hover:bg-green-50 rounded-lg transition-colors"
                  title="Check availability"
                >
                  <Calendar className="w-4 h-4" />
                </button>
              </>
            )}

            <div className="relative">
              <button
                onClick={() => setShowActions(!showActions)}
                className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                title="More actions"
              >
                <MoreVertical className="w-4 h-4" />
              </button>

              {/* Actions Menu */}
              {showActions && (
                <div className="absolute right-0 top-full mt-1 w-48 bg-white border border-gray-200 rounded-lg shadow-lg z-10">
                  <div className="py-1">
                    <button
                      onClick={() => {
                        onMessage(friend);
                        setShowActions(false);
                      }}
                      className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center space-x-2"
                    >
                      <MessageCircle className="w-4 h-4" />
                      <span>Send Message</span>
                    </button>
                    <button
                      onClick={() => {
                        onCheckAvailability(friend);
                        setShowActions(false);
                      }}
                      className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center space-x-2"
                    >
                      <Calendar className="w-4 h-4" />
                      <span>Check Availability</span>
                    </button>
                    <button
                      onClick={() => {
                        onInviteToCircle(friend);
                        setShowActions(false);
                      }}
                      className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center space-x-2"
                    >
                      <UserPlus className="w-4 h-4" />
                      <span>Invite to Circle</span>
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default FriendsList;