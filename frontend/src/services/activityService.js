import { api, API_ENDPOINTS } from './api.js';

/**
 * Activity Service - Comprehensive friends activity feed processing
 *
 * This service handles all activity-related operations including activity fetching,
 * real-time updates, activity categorization, infinite scroll pagination, and
 * intelligent filtering. It follows the established service architecture pattern
 * with caching and error handling.
 *
 * Features:
 * - Comprehensive activity feed management with real-time updates
 * - Infinite scroll pagination with cursor-based loading
 * - Activity categorization and intelligent sorting
 * - Advanced filtering by friend, activity type, and date range
 * - Performance optimization with caching and virtual scrolling support
 * - WebSocket integration for live activity updates
 *
 * @class ActivityService
 */
class ActivityService {
  constructor() {
    this.cache = new Map();
    this.cacheTimeout = 5 * 60 * 1000; // 5 minutes default cache timeout
    this.shortCacheTimeout = 1 * 60 * 1000; // 1 minute for real-time data
    this.longCacheTimeout = 15 * 60 * 1000; // 15 minutes for historical data

    // Activity type priorities for feed ordering
    this.activityPriorities = {
      'availability_change': 3,
      'availability_slot_update': 2,
      'profile_update': 2,
      'circle_activity': 2,
      'social_activity': 1
    };

    // WebSocket connection for real-time updates
    this.websocket = null;
    this.wsReconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.wsReconnectDelay = 1000; // Start with 1 second
  }

  /**
   * Get activity feed for a user with infinite scroll support
   *
   * @param {string|number} userId - The user ID to get activities for
   * @param {Object} options - Pagination and filtering options
   * @param {string} options.cursor - Cursor for pagination (timestamp-based)
   * @param {number} options.limit - Items per page (default: 20)
   * @param {Array} options.activityTypes - Filter by activity types
   * @param {Array} options.friendIds - Filter by specific friends
   * @param {string} options.dateRange - Date range filter: 'today', 'week', 'month', 'all'
   * @param {string} options.sort - Sort order: 'recent', 'priority' (default: 'recent')
   * @returns {Promise<Object>} Activity feed data with pagination metadata
   */
  async getActivityFeed(userId, options = {}) {
    try {
      const {
        cursor = null,
        limit = 20,
        activityTypes = [],
        friendIds = [],
        dateRange = 'all',
        sort = 'recent'
      } = options;

      this.logApiCall('getActivityFeed', { userId, cursor, limit, activityTypes, friendIds, dateRange, sort });

      // Check cache first (shorter timeout for real-time data)
      const cacheKey = `activity_feed_${userId}_${cursor}_${limit}_${activityTypes.join(',')}_${friendIds.join(',')}_${dateRange}_${sort}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        this.logApiCall('getActivityFeed - cache hit', { userId, cursor });
        return cachedData;
      }

      // Build query parameters
      const params = new URLSearchParams({
        limit: limit.toString(),
        sort,
        dateRange
      });

      if (cursor) {
        params.append('cursor', cursor);
      }

      // Add activity type filters
      activityTypes.forEach(type => {
        params.append('types', type);
      });

      // Add friend ID filters
      friendIds.forEach(friendId => {
        params.append('friends', friendId.toString());
      });

      const response = await api.get(`${API_ENDPOINTS.ACTIVITIES.FEED(userId)}?${params}`);

      // Validate response structure
      this.validateActivityFeedResponse(response);

      // Process and enhance activities
      const enhancedActivities = await this.enhanceActivities(response.activities, userId);

      const enhancedResponse = {
        ...response,
        activities: enhancedActivities
      };

      // Cache the result with short timeout for real-time data
      this.cacheData(cacheKey, enhancedResponse, this.shortCacheTimeout);

      this.logApiCall('getActivityFeed - success', {
        userId,
        cursor,
        activitiesCount: enhancedActivities.length,
        hasMore: response.hasMore
      });

      return enhancedResponse;

    } catch (error) {
      this.logApiCall('getActivityFeed - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load activity feed');
    }
  }

  /**
   * Get activities for specific friends
   *
   * @param {string|number} userId - Current user ID
   * @param {Array} friendIds - Array of friend IDs to get activities for
   * @param {Object} options - Pagination and filtering options
   * @returns {Promise<Object>} Friend activities data
   */
  async getFriendActivities(userId, friendIds, options = {}) {
    try {
      const {
        cursor = null,
        limit = 20,
        activityTypes = []
      } = options;

      this.logApiCall('getFriendActivities', { userId, friendIds, cursor, limit });

      const cacheKey = `friend_activities_${userId}_${friendIds.join(',')}_${cursor}_${limit}_${activityTypes.join(',')}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      const params = new URLSearchParams({
        limit: limit.toString()
      });

      if (cursor) {
        params.append('cursor', cursor);
      }

      friendIds.forEach(friendId => {
        params.append('friendIds', friendId.toString());
      });

      activityTypes.forEach(type => {
        params.append('types', type);
      });

      const response = await api.get(`${API_ENDPOINTS.ACTIVITIES.FRIENDS(userId)}?${params}`);

      // Enhance activities with additional metadata
      const enhancedActivities = await this.enhanceActivities(response.activities, userId);

      const enhancedResponse = {
        ...response,
        activities: enhancedActivities
      };

      this.cacheData(cacheKey, enhancedResponse, this.cacheTimeout);

      this.logApiCall('getFriendActivities - success', { userId, friendIds, count: enhancedActivities.length });
      return enhancedResponse;

    } catch (error) {
      this.logApiCall('getFriendActivities - error', { userId, friendIds, error: error.message });
      throw this.handleApiError(error, 'Failed to load friend activities');
    }
  }


  /**
   * Mark activity as read
   *
   * @param {string|number} activityId - Activity ID to mark as read
   * @param {string|number} userId - Current user ID
   * @returns {Promise<Object>} Updated activity
   */
  async markActivityAsRead(activityId, userId) {
    try {
      this.logApiCall('markActivityAsRead', { activityId, userId });

      const response = await api.put(API_ENDPOINTS.ACTIVITIES.MARK_READ(activityId), { userId });

      // Invalidate activity feed caches
      this.invalidateActivityCaches(userId);

      this.logApiCall('markActivityAsRead - success', { activityId, userId });
      return response;

    } catch (error) {
      this.logApiCall('markActivityAsRead - error', { activityId, userId, error: error.message });
      throw this.handleApiError(error, 'Failed to mark activity as read');
    }
  }

  /**
   * Get activity statistics for dashboard
   *
   * @param {string|number} userId - User ID to get statistics for
   * @returns {Promise<Object>} Activity statistics
   */
  async getActivityStatistics(userId) {
    try {
      this.logApiCall('getActivityStatistics', { userId });

      const cacheKey = `activity_stats_${userId}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      const stats = await api.get(API_ENDPOINTS.ACTIVITIES.STATS(userId));

      // Cache statistics with medium timeout
      this.cacheData(cacheKey, stats, this.cacheTimeout);

      this.logApiCall('getActivityStatistics - success', { userId, stats });
      return stats;

    } catch (error) {
      this.logApiCall('getActivityStatistics - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to get activity statistics');
    }
  }

  /**
   * Enhance activities with additional metadata and formatting
   *
   * @private
   * @param {Array} activities - Raw activities from API
   * @param {string|number} userId - Current user ID for context
   * @returns {Promise<Array>} Enhanced activities
   */
  async enhanceActivities(activities, userId) {
    if (!Array.isArray(activities)) {
      return [];
    }

    return Promise.all(
      activities.map(async (activity) => {
        try {
          const enhanced = {
            ...activity,
            // Add display metadata
            displayTime: this.formatActivityTime(activity.timestamp),
            priority: this.activityPriorities[activity.type] || 1,
            category: this.categorizeActivity(activity),
            // Add UI helpers
            icon: this.getActivityIcon(activity.type),
            color: this.getActivityColor(activity.type),
            // Add interaction metadata
            isRead: activity.readBy?.includes(userId) || false,
            canInteract: this.canInteractWithActivity(activity, userId)
          };

          // Add friend information if available and not already present
          if (activity.userId && activity.userId !== userId && !activity.friend) {
            enhanced.friend = await this.getFriendInfo(activity.userId);
          } else if (activity.friend) {
            // Use existing friend data
            enhanced.friend = activity.friend;
          }

          return enhanced;
        } catch (error) {
          // Return original activity if enhancement fails
          console.warn('Failed to enhance activity:', activity.id, error);
          return activity;
        }
      })
    );
  }

  /**
   * Categorize activity for display grouping
   *
   * @private
   * @param {Object} activity - Activity object
   * @returns {string} Activity category
   */
  categorizeActivity(activity) {
    const typeMap = {
      'availability_change': 'availability',
      'availability_slot_update': 'availability',
      'profile_update': 'profile',
      'circle_activity': 'social',
      'social_activity': 'social'
    };

    return typeMap[activity.type] || 'other';
  }

  /**
   * Get icon for activity type
   *
   * @private
   * @param {string} activityType - Type of activity
   * @returns {string} Icon name for Lucide React
   */
  getActivityIcon(activityType) {
    const iconMap = {
      'availability_change': 'Clock',
      'availability_slot_update': 'Calendar',
      'profile_update': 'User',
      'circle_activity': 'Users',
      'social_activity': 'UserPlus'
    };

    return iconMap[activityType] || 'Activity';
  }

  /**
   * Get color scheme for activity type
   *
   * @private
   * @param {string} activityType - Type of activity
   * @returns {string} Tailwind color class
   */
  getActivityColor(activityType) {
    const colorMap = {
      'availability_change': 'blue',
      'availability_slot_update': 'green',
      'profile_update': 'purple',
      'circle_activity': 'orange',
      'social_activity': 'pink'
    };

    return colorMap[activityType] || 'gray';
  }

  /**
   * Format activity timestamp for display
   *
   * @private
   * @param {string} timestamp - ISO timestamp
   * @returns {string} Formatted relative time
   */
  formatActivityTime(timestamp) {
    // This will be enhanced when we add date-fns
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} minutes ago`;
    if (diffHours < 24) return `${diffHours} hours ago`;
    if (diffDays < 7) return `${diffDays} days ago`;

    return date.toLocaleDateString();
  }

  /**
   * Check if user can interact with activity
   *
   * @private
   * @param {Object} activity - Activity object
   * @param {string|number} userId - Current user ID
   * @returns {boolean} Whether user can interact
   */
  canInteractWithActivity(activity, userId) {
    // Users can interact with their own activities and friend activities
    return activity.userId === userId || activity.visibility === 'friends';
  }

  /**
   * Get friend information for activity display
   *
   * @private
   * @param {string|number} friendId - Friend user ID
   * @returns {Promise<Object>} Friend information
   */
  async getFriendInfo(friendId) {
    try {
      const cacheKey = `friend_info_${friendId}`;
      const cachedInfo = this.getCachedData(cacheKey);
      if (cachedInfo) {
        return cachedInfo;
      }

      // This would ideally come from friendService or userService
      const friendInfo = await api.get(API_ENDPOINTS.USERS.BY_ID(friendId));

      // Cache friend info with longer timeout (stable data)
      this.cacheData(cacheKey, friendInfo, this.longCacheTimeout);

      return friendInfo;
    } catch (error) {
      // Return minimal info if friend data fetch fails
      return {
        id: friendId,
        name: 'Unknown User',
        avatar: null
      };
    }
  }

  /**
   * Initialize WebSocket connection for real-time activity updates
   *
   * @param {string|number} userId - User ID for connection
   * @param {Function} onActivityUpdate - Callback for new activities
   * @returns {Promise<WebSocket>} WebSocket connection
   */
  async initializeWebSocket(userId, onActivityUpdate) {
    try {
      if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
        return this.websocket;
      }

      const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/activities/${userId}`;

      this.websocket = new WebSocket(wsUrl);

      this.websocket.onopen = () => {
        this.logApiCall('WebSocket connected', { userId });
        this.wsReconnectAttempts = 0;
        this.wsReconnectDelay = 1000;
      };

      this.websocket.onmessage = (event) => {
        try {
          const activity = JSON.parse(event.data);
          this.logApiCall('WebSocket activity received', { activityId: activity.id, type: activity.type });

          // Invalidate relevant caches
          this.invalidateActivityCaches(userId);

          // Call the callback with enhanced activity
          if (onActivityUpdate) {
            this.enhanceActivities([activity], userId).then(enhanced => {
              onActivityUpdate(enhanced[0]);
            });
          }
        } catch (error) {
          console.error('Failed to process WebSocket message:', error);
        }
      };

      this.websocket.onclose = () => {
        this.logApiCall('WebSocket disconnected', { userId });
        this.handleWebSocketReconnect(userId, onActivityUpdate);
      };

      this.websocket.onerror = (error) => {
        console.error('WebSocket error:', error);
      };

      return this.websocket;

    } catch (error) {
      this.logApiCall('WebSocket initialization failed', { userId, error: error.message });
      throw error;
    }
  }

  /**
   * Handle WebSocket reconnection with exponential backoff
   *
   * @private
   * @param {string|number} userId - User ID for connection
   * @param {Function} onActivityUpdate - Callback for new activities
   */
  handleWebSocketReconnect(userId, onActivityUpdate) {
    if (this.wsReconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Maximum WebSocket reconnection attempts reached');
      return;
    }

    setTimeout(() => {
      this.wsReconnectAttempts++;
      this.wsReconnectDelay *= 2; // Exponential backoff
      this.logApiCall('WebSocket reconnection attempt', { attempt: this.wsReconnectAttempts });
      this.initializeWebSocket(userId, onActivityUpdate);
    }, this.wsReconnectDelay);
  }

  /**
   * Close WebSocket connection
   */
  closeWebSocket() {
    if (this.websocket) {
      this.websocket.close();
      this.websocket = null;
      this.logApiCall('WebSocket connection closed');
    }
  }

  /**
   * Validate activity feed response structure
   *
   * @private
   * @param {Object} response - API response to validate
   * @throws {Error} When validation fails
   */
  validateActivityFeedResponse(response) {
    if (!response || typeof response !== 'object') {
      throw new Error('Invalid activity feed response structure');
    }

    if (!Array.isArray(response.activities)) {
      throw new Error('Activities data must be an array');
    }

    // Ensure pagination metadata exists
    if (typeof response.hasMore !== 'boolean') {
      response.hasMore = false;
    }

    if (!response.nextCursor && response.activities.length > 0) {
      // Generate cursor from last activity timestamp
      response.nextCursor = response.activities[response.activities.length - 1].timestamp;
    }
  }

  /**
   * Invalidate activity-related caches for a user
   *
   * @private
   * @param {string|number} userId - User ID to invalidate caches for
   */
  invalidateActivityCaches(userId) {
    const patterns = [
      `activity_feed_${userId}`,
      `friend_activities_${userId}`,
      `activity_stats_${userId}`
    ];

    patterns.forEach(pattern => {
      Array.from(this.cache.keys())
        .filter(key => key.startsWith(pattern))
        .forEach(key => this.invalidateCache(key));
    });
  }

  /**
   * Handle API errors with user-friendly messages
   *
   * @private
   * @param {Error} error - The original error
   * @param {string} defaultMessage - Default user-friendly message
   * @returns {Error} Enhanced error with user-friendly message
   */
  handleApiError(error, defaultMessage = 'An error occurred') {
    console.error('Activity API Error:', error);

    let userMessage = defaultMessage;

    if (error.message.includes('HTTP 401')) {
      userMessage = 'Authentication required. Please log in again.';
    } else if (error.message.includes('HTTP 403')) {
      userMessage = 'Access denied. You do not have permission to view this activity.';
    } else if (error.message.includes('HTTP 404')) {
      userMessage = 'Activity data not found.';
    } else if (error.message.includes('HTTP 500')) {
      userMessage = 'Server error. Please try again later.';
    } else if (error.message.includes('Network')) {
      userMessage = 'Network error. Please check your connection.';
    }

    const enhancedError = new Error(userMessage);
    enhancedError.originalError = error;
    enhancedError.timestamp = new Date().toISOString();

    return enhancedError;
  }

  /**
   * Log API calls for debugging purposes
   *
   * @private
   * @param {string} operation - The operation being performed
   * @param {Object} details - Additional details to log
   */
  logApiCall(operation, details = {}) {
    const logLevel = import.meta.env.DEV ? 'log' : 'debug';
    console[logLevel](`📋 ActivityService.${operation}`, {
      timestamp: new Date().toISOString(),
      ...details
    });
  }

  /**
   * Cache data with optional timeout
   *
   * @private
   * @param {string} key - Cache key
   * @param {*} data - Data to cache
   * @param {number} timeout - Cache timeout in milliseconds (optional)
   */
  cacheData(key, data, timeout = this.cacheTimeout) {
    const cacheEntry = {
      data,
      timestamp: Date.now(),
      timeout
    };

    this.cache.set(key, cacheEntry);
    this.logApiCall('cacheData', { key, timeout });
  }

  /**
   * Get cached data if not expired
   *
   * @private
   * @param {string} key - Cache key
   * @returns {*} Cached data or null if not found/expired
   */
  getCachedData(key) {
    const cacheEntry = this.cache.get(key);

    if (!cacheEntry) {
      return null;
    }

    const isExpired = Date.now() - cacheEntry.timestamp > cacheEntry.timeout;

    if (isExpired) {
      this.cache.delete(key);
      this.logApiCall('getCachedData - expired', { key });
      return null;
    }

    this.logApiCall('getCachedData - hit', { key });
    return cacheEntry.data;
  }

  /**
   * Invalidate cached data
   *
   * @private
   * @param {string} key - Cache key to invalidate
   */
  invalidateCache(key) {
    this.cache.delete(key);
    this.logApiCall('invalidateCache', { key });
  }

  /**
   * Clear all cached data
   *
   * @private
   */
  clearCache() {
    this.cache.clear();
    this.logApiCall('clearCache');
  }

  /**
   * Get cache statistics for debugging
   *
   * @returns {Object} Cache statistics
   */
  getCacheStats() {
    return {
      size: this.cache.size,
      keys: Array.from(this.cache.keys()),
      memoryUsage: JSON.stringify(Array.from(this.cache.entries())).length
    };
  }
}

// Create and export activity service instance
export const activityService = new ActivityService();