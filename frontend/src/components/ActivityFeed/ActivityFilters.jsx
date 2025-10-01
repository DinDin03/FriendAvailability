import { useState, useEffect, useCallback } from 'react';
import { Search, X, ChevronDown, Clock, Calendar, User, Users, UserPlus } from 'lucide-react';
import { ACTIVITY_TYPES, ACTIVITY_CATEGORIES } from '../../lib/activityUtils';
import { friendService } from '../../services/friendService';
import { useAuth } from '../../contexts/AuthContext';

/**
 * ActivityFilters Component - Advanced filtering interface for activity feed
 *
 * This component provides comprehensive filtering options for the activity feed,
 * including friend selection, activity type filtering, date range selection,
 * and sorting options.
 *
 * Features:
 * - Friend selection with search functionality
 * - Activity type and category filtering
 * - Date range selection
 * - Sort options
 * - Clear filters functionality
 * - Responsive design
 */
export const ActivityFilters = ({
  filters,
  sortBy,
  onFilterChange,
  onSortChange,
  className = ''
}) => {
  const { user } = useAuth();

  // Local state for UI management
  const [friends, setFriends] = useState([]);
  const [friendsLoading, setFriendsLoading] = useState(false);
  const [friendSearch, setFriendSearch] = useState('');
  const [showFriendDropdown, setShowFriendDropdown] = useState(false);
  const [showTypeDropdown, setShowTypeDropdown] = useState(false);

  /**
   * Load friends list for filtering
   */
  useEffect(() => {
    const loadFriends = async () => {
      if (!user?.id) return;

      try {
        setFriendsLoading(true);
        const friendsData = await friendService.getFriends(user.id, {
          limit: 100, // Get more friends for filtering
          sort: 'name'
        });
        setFriends(friendsData.friends || []);
      } catch (error) {
        console.error('Failed to load friends:', error);
      } finally {
        setFriendsLoading(false);
      }
    };

    loadFriends();
  }, [user?.id]);

  /**
   * Filter friends based on search
   */
  const filteredFriends = friends.filter(friend =>
    friend.name.toLowerCase().includes(friendSearch.toLowerCase())
  );

  /**
   * Handle filter updates
   */
  const updateFilters = useCallback((updates) => {
    onFilterChange({ ...filters, ...updates });
  }, [filters, onFilterChange]);

  /**
   * Toggle activity type filter
   */
  const toggleActivityType = useCallback((type) => {
    const currentTypes = filters.types || [];
    const newTypes = currentTypes.includes(type)
      ? currentTypes.filter(t => t !== type)
      : [...currentTypes, type];

    updateFilters({ types: newTypes });
  }, [filters.types, updateFilters]);

  /**
   * Toggle activity category filter
   */
  const toggleActivityCategory = useCallback((category) => {
    const currentCategories = filters.categories || [];
    const newCategories = currentCategories.includes(category)
      ? currentCategories.filter(c => c !== category)
      : [...currentCategories, category];

    updateFilters({ categories: newCategories });
  }, [filters.categories, updateFilters]);

  /**
   * Toggle friend filter
   */
  const toggleFriend = useCallback((friendId) => {
    const currentFriends = filters.friendIds || [];
    const newFriends = currentFriends.includes(friendId)
      ? currentFriends.filter(id => id !== friendId)
      : [...currentFriends, friendId];

    updateFilters({ friendIds: newFriends });
  }, [filters.friendIds, updateFilters]);

  /**
   * Clear all filters
   */
  const clearAllFilters = useCallback(() => {
    onFilterChange({
      types: [],
      categories: [],
      friendIds: [],
      dateRange: 'all',
      unreadOnly: false
    });
    setFriendSearch('');
  }, [onFilterChange]);

  /**
   * Get active filter count
   */
  const getActiveFilterCount = () => {
    let count = 0;
    if (filters.types?.length > 0) count += filters.types.length;
    if (filters.categories?.length > 0) count += filters.categories.length;
    if (filters.friendIds?.length > 0) count += filters.friendIds.length;
    if (filters.dateRange !== 'all') count += 1;
    if (filters.unreadOnly) count += 1;
    return count;
  };

  const activeFilterCount = getActiveFilterCount();

  // Activity type options with icons and labels
  const activityTypeOptions = [
    {
      value: ACTIVITY_TYPES.AVAILABILITY_CHANGE,
      label: 'Availability Changes',
      icon: Clock,
      description: 'Status updates (Available, Busy, Away)'
    },
    {
      value: ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE,
      label: 'Schedule Updates',
      icon: Calendar,
      description: 'New available time slots'
    },
    {
      value: ACTIVITY_TYPES.PROFILE_UPDATE,
      label: 'Profile Updates',
      icon: User,
      description: 'Profile picture, bio, status changes'
    },
    {
      value: ACTIVITY_TYPES.CIRCLE_ACTIVITY,
      label: 'Circle Activities',
      icon: Users,
      description: 'Circle joins, events, activities'
    },
    {
      value: ACTIVITY_TYPES.SOCIAL_ACTIVITY,
      label: 'Social Activities',
      icon: UserPlus,
      description: 'New friendships, connections'
    }
  ];

  // Date range options
  const dateRangeOptions = [
    { value: 'all', label: 'All Time' },
    { value: 'today', label: 'Today' },
    { value: 'week', label: 'This Week' },
    { value: 'month', label: 'This Month' }
  ];

  // Sort options
  const sortOptions = [
    { value: 'recent', label: 'Most Recent' },
    { value: 'priority', label: 'By Priority' },
    { value: 'friend', label: 'By Friend' },
    { value: 'type', label: 'By Type' }
  ];

  return (
    <div className={`bg-gray-50 rounded-lg p-4 space-y-4 ${className}`}>
      {/* Filter Header */}
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-medium text-gray-900">
          Activity Filters
          {activeFilterCount > 0 && (
            <span className="ml-2 bg-blue-100 text-blue-800 text-xs font-medium px-2 py-0.5 rounded-full">
              {activeFilterCount}
            </span>
          )}
        </h4>
        {activeFilterCount > 0 && (
          <button
            onClick={clearAllFilters}
            className="text-sm text-blue-600 hover:text-blue-700 font-medium"
          >
            Clear All
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Friend Filter */}
        <div className="relative">
          <label className="block text-xs font-medium text-gray-700 mb-1">
            Friends
          </label>
          <div className="relative">
            <button
              onClick={() => setShowFriendDropdown(!showFriendDropdown)}
              className="w-full p-2 text-left bg-white border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <div className="flex items-center justify-between">
                <span className="truncate">
                  {filters.friendIds?.length > 0
                    ? `${filters.friendIds.length} friend${filters.friendIds.length !== 1 ? 's' : ''} selected`
                    : 'All friends'
                  }
                </span>
                <ChevronDown className="w-4 h-4 text-gray-400" />
              </div>
            </button>

            {/* Friend Dropdown */}
            {showFriendDropdown && (
              <div className="absolute z-20 w-full mt-1 bg-white border border-gray-300 rounded-md shadow-lg">
                <div className="p-2 border-b border-gray-200">
                  <div className="relative">
                    <Search className="absolute left-2 top-2 w-4 h-4 text-gray-400" />
                    <input
                      type="text"
                      placeholder="Search friends..."
                      value={friendSearch}
                      onChange={(e) => setFriendSearch(e.target.value)}
                      className="w-full pl-8 pr-3 py-1 text-sm border border-gray-300 rounded focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                  </div>
                </div>
                <div className="max-h-48 overflow-y-auto">
                  {friendsLoading ? (
                    <div className="p-3 text-center text-sm text-gray-500">
                      Loading friends...
                    </div>
                  ) : filteredFriends.length === 0 ? (
                    <div className="p-3 text-center text-sm text-gray-500">
                      No friends found
                    </div>
                  ) : (
                    filteredFriends.map((friend) => (
                      <label
                        key={friend.id}
                        className="flex items-center p-2 hover:bg-gray-100 cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={filters.friendIds?.includes(friend.id) || false}
                          onChange={() => toggleFriend(friend.id)}
                          className="mr-2 rounded border-gray-300 focus:ring-blue-500"
                        />
                        <span className="text-sm text-gray-900 truncate">
                          {friend.name}
                        </span>
                      </label>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Activity Type Filter */}
        <div className="relative">
          <label className="block text-xs font-medium text-gray-700 mb-1">
            Activity Types
          </label>
          <div className="relative">
            <button
              onClick={() => setShowTypeDropdown(!showTypeDropdown)}
              className="w-full p-2 text-left bg-white border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <div className="flex items-center justify-between">
                <span className="truncate">
                  {filters.types?.length > 0
                    ? `${filters.types.length} type${filters.types.length !== 1 ? 's' : ''} selected`
                    : 'All types'
                  }
                </span>
                <ChevronDown className="w-4 h-4 text-gray-400" />
              </div>
            </button>

            {/* Type Dropdown */}
            {showTypeDropdown && (
              <div className="absolute z-20 w-full mt-1 bg-white border border-gray-300 rounded-md shadow-lg">
                <div className="max-h-64 overflow-y-auto">
                  {activityTypeOptions.map((option) => {
                    const IconComponent = option.icon;
                    return (
                      <label
                        key={option.value}
                        className="flex items-start p-3 hover:bg-gray-100 cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={filters.types?.includes(option.value) || false}
                          onChange={() => toggleActivityType(option.value)}
                          className="mt-1 mr-3 rounded border-gray-300 focus:ring-blue-500"
                        />
                        <div className="flex-1">
                          <div className="flex items-center space-x-2">
                            <IconComponent className="w-4 h-4 text-gray-600" />
                            <span className="text-sm font-medium text-gray-900">
                              {option.label}
                            </span>
                          </div>
                          <p className="text-xs text-gray-500 mt-1">
                            {option.description}
                          </p>
                        </div>
                      </label>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Date Range Filter */}
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">
            Date Range
          </label>
          <select
            value={filters.dateRange || 'all'}
            onChange={(e) => updateFilters({ dateRange: e.target.value })}
            className="w-full p-2 bg-white border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            {dateRangeOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        {/* Sort Options */}
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">
            Sort By
          </label>
          <select
            value={sortBy}
            onChange={(e) => onSortChange(e.target.value)}
            className="w-full p-2 bg-white border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            {sortOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Additional Options */}
      <div className="flex items-center space-x-4 pt-2 border-t border-gray-200">
        <label className="flex items-center">
          <input
            type="checkbox"
            checked={filters.unreadOnly || false}
            onChange={(e) => updateFilters({ unreadOnly: e.target.checked })}
            className="mr-2 rounded border-gray-300 focus:ring-blue-500"
          />
          <span className="text-sm text-gray-700">Unread only</span>
        </label>
      </div>

      {/* Click outside handlers */}
      {showFriendDropdown && (
        <div
          className="fixed inset-0 z-10"
          onClick={() => setShowFriendDropdown(false)}
        />
      )}
      {showTypeDropdown && (
        <div
          className="fixed inset-0 z-10"
          onClick={() => setShowTypeDropdown(false)}
        />
      )}
    </div>
  );
};