import { api, API_ENDPOINTS } from './api.js';

/**
 * Dashboard Service - Handles all dashboard-related API operations and data aggregation
 *
 * This service provides centralized access to dashboard data including user profiles,
 * statistics, recent activity, and aggregated dashboard information. It extends the
 * existing service architecture pattern used by authService and userService.
 *
 * Features:
 * - Dashboard data aggregation and caching
 * - Comprehensive error handling with user-friendly messages
 * - Request/response validation
 * - Debug logging for development
 * - Performance optimization through caching
 *
 * @class DashboardService
 */
class DashboardService {
  constructor() {
    this.cache = new Map();
    this.cacheTimeout = 5 * 60 * 1000; // 5 minutes default cache timeout
  }

  /**
   * Get aggregated dashboard data for dashboard initialization
   *
   * This method performs parallel API calls to fetch all necessary dashboard data
   * efficiently. It includes user profile, statistics, and recent activity.
   *
   * @param {string} userId - The user ID to fetch dashboard data for
   * @returns {Promise<Object>} Aggregated dashboard data object
   * @throws {Error} When API calls fail or data validation fails
   *
   * @example
   * const dashboardData = await dashboardService.getDashboardData(user.id);
   * console.log(dashboardData.stats.friendsCount);
   */
  async getDashboardData(userId) {
    try {
      this.logApiCall('getDashboardData', { userId });

      // Check cache first
      const cacheKey = `dashboard_${userId}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        this.logApiCall('getDashboardData - cache hit', { userId });
        return cachedData;
      }

      // Perform parallel API calls for dashboard data
      const [profile, stats, recentActivity] = await Promise.allSettled([
        this.getUserProfile(userId),
        this.getDashboardStats(userId),
        this.getRecentActivity(userId)
      ]);

      // Handle results and create aggregated response
      const dashboardData = {
        profile: profile.status === 'fulfilled' ? profile.value : null,
        stats: stats.status === 'fulfilled' ? stats.value : { friendsCount: 0, eventsCount: 0, messagesCount: 0 },
        recentActivity: recentActivity.status === 'fulfilled' ? recentActivity.value : [],
        lastUpdated: new Date().toISOString()
      };

      // Validate the aggregated data
      this.validateDashboardData(dashboardData);

      // Cache the result
      this.cacheData(cacheKey, dashboardData);

      this.logApiCall('getDashboardData - success', { userId, dataKeys: Object.keys(dashboardData) });
      return dashboardData;

    } catch (error) {
      this.logApiCall('getDashboardData - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load dashboard data');
    }
  }

  /**
   * Get detailed user profile information for dashboard display
   *
   * @param {string} userId - The user ID to fetch profile for
   * @returns {Promise<Object>} User profile data
   * @throws {Error} When profile fetch fails
   */
  async getUserProfile(userId) {
    try {
      this.logApiCall('getUserProfile', { userId });

      const cacheKey = `profile_${userId}`;
      const cachedProfile = this.getCachedData(cacheKey);
      if (cachedProfile) {
        return cachedProfile;
      }

      const profile = await api.get(API_ENDPOINTS.USERS.BY_ID(userId));

      if (!profile) {
        throw new Error('Profile data not found');
      }

      // Cache profile data
      this.cacheData(cacheKey, profile);

      this.logApiCall('getUserProfile - success', { userId });
      return profile;

    } catch (error) {
      this.logApiCall('getUserProfile - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load user profile');
    }
  }

  /**
   * Get dashboard statistics (friends count, events count, messages count)
   *
   * @param {string} userId - The user ID to fetch statistics for
   * @returns {Promise<Object>} Dashboard statistics object
   * @throws {Error} When statistics fetch fails
   */
  async getDashboardStats(userId) {
    try {
      this.logApiCall('getDashboardStats', { userId });

      const cacheKey = `stats_${userId}`;
      const cachedStats = this.getCachedData(cacheKey);
      if (cachedStats) {
        return cachedStats;
      }

      // Perform parallel API calls for different statistics
      const [friends, events, messages] = await Promise.allSettled([
        api.get(API_ENDPOINTS.FRIENDS.BASE(userId)),
        api.get(API_ENDPOINTS.CALENDAR.EVENTS(userId)),
        api.get(API_ENDPOINTS.CHAT.ROOMS)
      ]);

      const stats = {
        friendsCount: friends.status === 'fulfilled' ? (friends.value?.length || 0) : 0,
        eventsCount: events.status === 'fulfilled' ? (events.value?.length || 0) : 0,
        messagesCount: messages.status === 'fulfilled' ? (messages.value?.length || 0) : 0,
        lastUpdated: new Date().toISOString()
      };

      // Cache statistics
      this.cacheData(cacheKey, stats, 2 * 60 * 1000); // 2 minutes cache for stats

      this.logApiCall('getDashboardStats - success', { userId, stats });
      return stats;

    } catch (error) {
      this.logApiCall('getDashboardStats - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load dashboard statistics');
    }
  }

  /**
   * Get recent activity for the user
   *
   * @param {string} userId - The user ID to fetch recent activity for
   * @returns {Promise<Array>} Array of recent activity items
   * @throws {Error} When activity fetch fails
   */
  async getRecentActivity(userId) {
    try {
      this.logApiCall('getRecentActivity', { userId });

      const cacheKey = `activity_${userId}`;
      const cachedActivity = this.getCachedData(cacheKey);
      if (cachedActivity) {
        return cachedActivity;
      }

      // For now, return default activity until backend endpoint is available
      const defaultActivity = [
        {
          id: 1,
          type: 'welcome',
          message: 'Welcome to Link Up! Start by adding friends.',
          timestamp: new Date().toISOString(),
          icon: 'blue'
        },
        {
          id: 2,
          type: 'account_created',
          message: 'Your account has been created successfully.',
          timestamp: new Date(Date.now() - 5 * 60 * 1000).toISOString(), // 5 minutes ago
          icon: 'green'
        }
      ];

      // Cache activity data
      this.cacheData(cacheKey, defaultActivity, 1 * 60 * 1000); // 1 minute cache for activity

      this.logApiCall('getRecentActivity - success', { userId, activityCount: defaultActivity.length });
      return defaultActivity;

    } catch (error) {
      this.logApiCall('getRecentActivity - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load recent activity');
    }
  }

  /**
   * Update user profile information
   *
   * @param {string} userId - The user ID to update
   * @param {Object} profileData - Updated profile data
   * @returns {Promise<Object>} Updated profile object
   * @throws {Error} When profile update fails
   */
  async updateUserProfile(userId, profileData) {
    try {
      this.logApiCall('updateUserProfile', { userId, updates: Object.keys(profileData) });

      const updatedProfile = await api.put(API_ENDPOINTS.USERS.UPDATE_PROFILE, profileData);

      // Invalidate cached profile data
      this.invalidateCache(`profile_${userId}`);
      this.invalidateCache(`dashboard_${userId}`);

      this.logApiCall('updateUserProfile - success', { userId });
      return updatedProfile;

    } catch (error) {
      this.logApiCall('updateUserProfile - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to update profile');
    }
  }

  /**
   * Refresh dashboard data by invalidating cache and fetching fresh data
   *
   * @param {string} userId - The user ID to refresh data for
   * @returns {Promise<Object>} Fresh dashboard data
   */
  async refreshDashboardData(userId) {
    try {
      this.logApiCall('refreshDashboardData', { userId });

      // Invalidate all cached data for this user
      this.invalidateCache(`dashboard_${userId}`);
      this.invalidateCache(`profile_${userId}`);
      this.invalidateCache(`stats_${userId}`);
      this.invalidateCache(`activity_${userId}`);

      // Fetch fresh data
      return await this.getDashboardData(userId);

    } catch (error) {
      this.logApiCall('refreshDashboardData - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to refresh dashboard data');
    }
  }

  /**
   * Handle API errors with user-friendly messages
   *
   * @param {Error} error - The original error
   * @param {string} defaultMessage - Default user-friendly message
   * @returns {Error} Enhanced error with user-friendly message
   */
  handleApiError(error, defaultMessage = 'An error occurred') {
    console.error('Dashboard API Error:', error);

    // Extract user-friendly error messages
    let userMessage = defaultMessage;

    if (error.message.includes('HTTP 401')) {
      userMessage = 'Authentication required. Please log in again.';
    } else if (error.message.includes('HTTP 403')) {
      userMessage = 'Access denied. You do not have permission for this action.';
    } else if (error.message.includes('HTTP 404')) {
      userMessage = 'The requested data was not found.';
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
   * @param {string} operation - The operation being performed
   * @param {Object} details - Additional details to log
   */
  logApiCall(operation, details = {}) {
    const logLevel = import.meta.env.DEV ? 'log' : 'debug';
    console[logLevel](`🔧 DashboardService.${operation}`, {
      timestamp: new Date().toISOString(),
      ...details
    });
  }

  /**
   * Validate dashboard data structure
   *
   * @param {Object} data - Dashboard data to validate
   * @throws {Error} When data validation fails
   */
  validateDashboardData(data) {
    if (!data || typeof data !== 'object') {
      throw new Error('Invalid dashboard data structure');
    }

    // Validate required fields
    const requiredFields = ['profile', 'stats', 'recentActivity', 'lastUpdated'];
    for (const field of requiredFields) {
      if (!(field in data)) {
        throw new Error(`Missing required field: ${field}`);
      }
    }

    // Validate stats structure
    if (data.stats && typeof data.stats === 'object') {
      const statsFields = ['friendsCount', 'eventsCount', 'messagesCount'];
      for (const field of statsFields) {
        if (typeof data.stats[field] !== 'number' || data.stats[field] < 0) {
          data.stats[field] = 0; // Set to 0 if invalid
        }
      }
    }

    // Validate recent activity is an array
    if (!Array.isArray(data.recentActivity)) {
      data.recentActivity = [];
    }
  }

  /**
   * Cache data with optional timeout
   *
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
   * @param {string} key - Cache key to invalidate
   */
  invalidateCache(key) {
    this.cache.delete(key);
    this.logApiCall('invalidateCache', { key });
  }

  /**
   * Clear all cached data
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

// Create and export dashboard service instance
export const dashboardService = new DashboardService();