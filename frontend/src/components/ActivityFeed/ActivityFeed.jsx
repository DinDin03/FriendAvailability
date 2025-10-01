import { useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { activityService } from '../../services/activityService';
import { filterActivities, sortActivities } from '../../lib/activityUtils';
import { formatRelativeTime, groupActivitiesByTime } from '../../lib/timeUtils';
import { ActivityItem } from './ActivityItem';
import { ActivityFilters } from './ActivityFilters';
import { ActivitySkeleton } from './ActivitySkeleton';
import { RefreshCw, Filter, AlertCircle, CheckCircle } from 'lucide-react';
import toast from 'react-hot-toast';

/**
 * ActivityFeed Component - Main activity feed with infinite scroll
 *
 * This component provides a comprehensive activity feed with real-time updates,
 * infinite scroll pagination, advanced filtering, and performance optimizations.
 *
 * Features:
 * - Infinite scroll with intersection observer
 * - Real-time WebSocket updates
 * - Activity filtering and grouping
 * - Performance optimization with virtual scrolling support
 * - Error handling and retry mechanisms
 */
export const ActivityFeed = ({
  className = '',
  maxHeight = 'max-h-96',
  showFilters = true,
  showGrouping = true,
  autoRefresh = true,
  refreshInterval = 30000, // 30 seconds
  pageSize = 20
}) => {
  const { user } = useAuth();

  // Activity feed state
  const [activities, setActivities] = useState([]);
  const [filteredActivities, setFilteredActivities] = useState([]);
  const [groupedActivities, setGroupedActivities] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState(null);
  const [nextCursor, setNextCursor] = useState(null);

  // Filter and sort state
  const [filters, setFilters] = useState({
    types: [],
    categories: [],
    friendIds: [],
    dateRange: 'all',
    unreadOnly: false
  });
  const [sortBy, setSortBy] = useState('recent');
  const [showFiltersPanel, setShowFiltersPanel] = useState(false);

  // UI state
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  // Refs for infinite scroll
  const containerRef = useRef(null);
  const observerRef = useRef(null);
  const loadMoreTriggerRef = useRef(null);

  // WebSocket and auto-refresh
  const refreshIntervalRef = useRef(null);
  const wsRef = useRef(null);

  /**
   * Load initial activity feed
   */
  const loadActivities = useCallback(async (options = {}) => {
    if (!user?.id) return;

    try {
      const { append = false, cursor = null } = options;

      if (!append) {
        setIsLoading(true);
        setError(null);
      } else {
        setIsLoadingMore(true);
      }

      const response = await activityService.getActivityFeed(user.id, {
        cursor: cursor || nextCursor,
        limit: pageSize,
        activityTypes: filters.types,
        friendIds: filters.friendIds,
        dateRange: filters.dateRange,
        sort: sortBy
      });

      const newActivities = response.activities || [];

      if (append) {
        setActivities(prev => [...prev, ...newActivities]);
      } else {
        setActivities(newActivities);
      }

      setHasMore(response.hasMore || false);
      setNextCursor(response.nextCursor || null);

      // Update unread count
      const unread = newActivities.filter(activity => !activity.isRead).length;
      if (!append) {
        setUnreadCount(unread);
      } else {
        setUnreadCount(prev => prev + unread);
      }

    } catch (error) {
      console.error('Failed to load activities:', error);
      setError(error.message);
      toast.error('Failed to load activity feed');
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  }, [user?.id, nextCursor, pageSize, filters, sortBy]);

  /**
   * Load more activities for infinite scroll
   */
  const loadMoreActivities = useCallback(() => {
    if (!hasMore || isLoadingMore || isLoading) return;

    loadActivities({ append: true });
  }, [hasMore, isLoadingMore, isLoading, loadActivities]);

  /**
   * Refresh activity feed
   */
  const refreshActivities = useCallback(async () => {
    setIsRefreshing(true);
    setNextCursor(null);
    await loadActivities();
    setIsRefreshing(false);
    toast.success('Activity feed refreshed');
  }, [loadActivities]);

  /**
   * Handle real-time activity updates
   */
  const handleNewActivity = useCallback((newActivity) => {
    setActivities(prev => {
      // Check if activity already exists
      const exists = prev.some(activity => activity.id === newActivity.id);
      if (exists) return prev;

      // Add new activity at the top
      return [newActivity, ...prev];
    });

    // Update unread count if activity is unread
    if (!newActivity.isRead) {
      setUnreadCount(prev => prev + 1);
    }

    // Show toast notification for important activities
    if (newActivity.priority >= 3) {
      toast.success(newActivity.displayMessage || 'New activity from your friends!');
    }
  }, []);

  /**
   * Mark activity as read
   */
  const markActivityAsRead = useCallback(async (activityId) => {
    try {
      await activityService.markActivityAsRead(activityId, user.id);

      setActivities(prev =>
        prev.map(activity =>
          activity.id === activityId
            ? { ...activity, isRead: true }
            : activity
        )
      );

      setUnreadCount(prev => Math.max(0, prev - 1));

    } catch (error) {
      console.error('Failed to mark activity as read:', error);
      toast.error('Failed to update activity status');
    }
  }, [user?.id]);

  /**
   * Apply filters and sorting to activities
   */
  useEffect(() => {
    let filtered = filterActivities(activities, filters);
    filtered = sortActivities(filtered, sortBy, 'desc');
    setFilteredActivities(filtered);

    // Group activities by time if grouping is enabled
    if (showGrouping) {
      const grouped = groupActivitiesByTime(filtered);
      setGroupedActivities(grouped);
    }
  }, [activities, filters, sortBy, showGrouping]);

  /**
   * Setup intersection observer for infinite scroll
   */
  useEffect(() => {
    if (!loadMoreTriggerRef.current) return;

    observerRef.current = new IntersectionObserver(
      (entries) => {
        const [entry] = entries;
        if (entry.isIntersecting && hasMore && !isLoadingMore) {
          loadMoreActivities();
        }
      },
      {
        threshold: 0.1,
        rootMargin: '100px'
      }
    );

    observerRef.current.observe(loadMoreTriggerRef.current);

    return () => {
      if (observerRef.current) {
        observerRef.current.disconnect();
      }
    };
  }, [hasMore, isLoadingMore, loadMoreActivities]);

  /**
   * Setup WebSocket for real-time updates
   */
  useEffect(() => {
    if (!user?.id) return;

    const initWebSocket = async () => {
      try {
        wsRef.current = await activityService.initializeWebSocket(user.id, handleNewActivity);
      } catch (error) {
        console.warn('WebSocket connection failed:', error);
        // Fallback to polling if WebSocket fails
        if (autoRefresh) {
          refreshIntervalRef.current = setInterval(refreshActivities, refreshInterval);
        }
      }
    };

    initWebSocket();

    return () => {
      if (wsRef.current) {
        activityService.closeWebSocket();
      }
      if (refreshIntervalRef.current) {
        clearInterval(refreshIntervalRef.current);
      }
    };
  }, [user?.id, handleNewActivity, autoRefresh, refreshInterval, refreshActivities]);

  /**
   * Load activities on component mount and filter changes
   */
  useEffect(() => {
    loadActivities();
  }, [loadActivities]);

  /**
   * Handle filter changes
   */
  const handleFilterChange = useCallback((newFilters) => {
    setFilters(newFilters);
    setNextCursor(null);
    setActivities([]);
  }, []);

  /**
   * Handle sort change
   */
  const handleSortChange = useCallback((newSortBy) => {
    setSortBy(newSortBy);
  }, []);

  if (isLoading) {
    return (
      <div className={`bg-white rounded-lg shadow-sm ${className}`}>
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Activity Feed</h3>
            <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
          </div>
          <ActivitySkeleton count={5} />
        </div>
      </div>
    );
  }

  const renderActivityGroups = () => {
    if (!showGrouping) {
      return filteredActivities.map((activity) => (
        <ActivityItem
          key={activity.id}
          activity={activity}
          onMarkAsRead={markActivityAsRead}
          className="mb-3"
        />
      ));
    }

    return Object.entries(groupedActivities).map(([period, groupActivities]) => (
      <div key={period} className="mb-6">
        <h4 className="text-sm font-medium text-gray-500 mb-3 px-2">
          {period}
        </h4>
        <div className="space-y-3">
          {groupActivities.map((activity) => (
            <ActivityItem
              key={activity.id}
              activity={activity}
              onMarkAsRead={markActivityAsRead}
              showTimestamp={false} // Hide individual timestamps when grouped
            />
          ))}
        </div>
      </div>
    ));
  };

  return (
    <div className={`bg-white rounded-lg shadow-sm ${className}`}>
      {/* Header */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <h3 className="text-lg font-semibold text-gray-900">Activity Feed</h3>
            {unreadCount > 0 && (
              <span className="bg-blue-100 text-blue-800 text-xs font-medium px-2 py-1 rounded-full">
                {unreadCount} new
              </span>
            )}
          </div>
          <div className="flex items-center space-x-2">
            {showFilters && (
              <button
                onClick={() => setShowFiltersPanel(!showFiltersPanel)}
                className={`p-2 rounded-lg border transition-colors ${
                  showFiltersPanel
                    ? 'bg-blue-50 border-blue-200 text-blue-600'
                    : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                }`}
                title="Toggle filters"
              >
                <Filter className="w-4 h-4" />
              </button>
            )}
            <button
              onClick={refreshActivities}
              disabled={isRefreshing}
              className="p-2 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors disabled:opacity-50"
              title="Refresh feed"
            >
              <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Filters Panel */}
        {showFilters && showFiltersPanel && (
          <ActivityFilters
            filters={filters}
            sortBy={sortBy}
            onFilterChange={handleFilterChange}
            onSortChange={handleSortChange}
            className="mt-4"
          />
        )}
      </div>

      {/* Activity Feed Content */}
      <div
        ref={containerRef}
        className={`overflow-y-auto ${maxHeight}`}
        style={{ scrollBehavior: 'smooth' }}
      >
        {error ? (
          <div className="p-6 text-center">
            <div className="flex items-center justify-center space-x-2 text-red-600 mb-2">
              <AlertCircle className="w-5 h-5" />
              <span className="font-medium">Failed to load activities</span>
            </div>
            <p className="text-sm text-gray-600 mb-4">{error}</p>
            <button
              onClick={() => loadActivities()}
              className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors text-sm"
            >
              Try Again
            </button>
          </div>
        ) : filteredActivities.length === 0 ? (
          <div className="p-6 text-center">
            <CheckCircle className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500 font-medium">No activities to show</p>
            <p className="text-sm text-gray-400 mt-1">
              {filters.types.length > 0 || filters.friendIds.length > 0
                ? 'Try adjusting your filters to see more activities.'
                : 'Activity from your friends will appear here.'}
            </p>
          </div>
        ) : (
          <div className="p-6">
            {renderActivityGroups()}

            {/* Load More Trigger */}
            {hasMore && (
              <div
                ref={loadMoreTriggerRef}
                className="flex items-center justify-center py-4"
              >
                {isLoadingMore ? (
                  <div className="flex items-center space-x-2 text-gray-600">
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-600"></div>
                    <span className="text-sm">Loading more activities...</span>
                  </div>
                ) : (
                  <button
                    onClick={loadMoreActivities}
                    className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                  >
                    Load more activities
                  </button>
                )}
              </div>
            )}

            {/* End of Feed */}
            {!hasMore && filteredActivities.length > 0 && (
              <div className="text-center py-4">
                <p className="text-sm text-gray-500">You've caught up on all activities!</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};