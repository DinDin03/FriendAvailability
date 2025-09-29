import { api, API_ENDPOINTS } from './api.js';

/**
 * User Status Service - Handles all user status related API operations
 *
 * This service manages user online/offline status, provides efficient batch
 * operations for friend status checking, and integrates with the dashboard
 * for real-time status updates.
 *
 * Features:
 * - Individual user status management
 * - Batch status retrieval for friend lists
 * - Real-time status updates with heartbeat functionality
 * - Caching for performance optimization
 * - Error handling with user-friendly messages
 */
class UserStatusService {
  constructor() {
    this.cache = new Map();
    this.cacheTimeout = 2 * 60 * 1000; // 2 minutes cache for status data
    this.heartbeatInterval = null;
    this.isHeartbeatActive = false;
  }

  /**
   * Get user status by user ID
   */
  async getUserStatus(userId) {
    try {
      this.logApiCall('getUserStatus', { userId });

      // Check cache first
      const cacheKey = `user_status_${userId}`;
      const cachedStatus = this.getCachedData(cacheKey);
      if (cachedStatus) {
        this.logApiCall('getUserStatus - cache hit', { userId });
        return cachedStatus;
      }

      const response = await api.get(`/users/${userId}/status`);

      // Cache the result
      this.cacheData(cacheKey, response, this.cacheTimeout);

      this.logApiCall('getUserStatus - success', { userId, status: response.status });
      return response;

    } catch (error) {
      this.logApiCall('getUserStatus - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to get user status');
    }
  }

  /**
   * Update user status
   */
  async updateUserStatus(userId, status) {
    try {
      this.logApiCall('updateUserStatus', { userId, status });

      const response = await api.put(`/users/${userId}/status`, { status });

      // Update cache with new status
      const cacheKey = `user_status_${userId}`;
      this.cacheData(cacheKey, response, this.cacheTimeout);

      // Invalidate related caches
      this.invalidateUserRelatedCaches(userId);

      this.logApiCall('updateUserStatus - success', { userId, newStatus: status });
      return response;

    } catch (error) {
      this.logApiCall('updateUserStatus - error', { userId, status, error: error.message });
      throw this.handleApiError(error, 'Failed to update user status');
    }
  }

  /**
   * Get statuses for multiple users (efficient for friend lists)
   */
  async getBatchUserStatuses(userIds) {
    try {
      this.logApiCall('getBatchUserStatuses', { userCount: userIds.length });

      if (!userIds || userIds.length === 0) {
        return { statuses: {}, requestedCount: 0, foundCount: 0 };
      }

      // Check cache for some users
      const uncachedUserIds = [];
      const cachedStatuses = {};

      userIds.forEach(userId => {
        const cacheKey = `user_status_${userId}`;
        const cachedStatus = this.getCachedData(cacheKey);
        if (cachedStatus) {
          cachedStatuses[userId] = cachedStatus;
        } else {
          uncachedUserIds.push(userId);
        }
      });

      let batchResponse = { statuses: {} };

      // Fetch uncached statuses
      if (uncachedUserIds.length > 0) {
        batchResponse = await api.post('/users/statuses', {
          userIds: uncachedUserIds
        });

        // Cache the new results
        Object.entries(batchResponse.statuses).forEach(([userId, status]) => {
          const cacheKey = `user_status_${userId}`;
          this.cacheData(cacheKey, status, this.cacheTimeout);
        });
      }

      // Combine cached and fresh results
      const allStatuses = {
        ...cachedStatuses,
        ...batchResponse.statuses
      };

      const result = {
        statuses: allStatuses,
        requestedCount: userIds.length,
        foundCount: Object.keys(allStatuses).length
      };

      this.logApiCall('getBatchUserStatuses - success', {
        requested: userIds.length,
        cached: Object.keys(cachedStatuses).length,
        fetched: uncachedUserIds.length,
        total: result.foundCount
      });

      return result;

    } catch (error) {
      this.logApiCall('getBatchUserStatuses - error', { userCount: userIds.length, error: error.message });
      throw this.handleApiError(error, 'Failed to get user statuses');
    }
  }

  /**
   * Set user online (typically called on login)
   */
  async setUserOnline(userId) {
    try {
      this.logApiCall('setUserOnline', { userId });

      const response = await api.post(`/users/${userId}/online`);

      // Update cache
      const cacheKey = `user_status_${userId}`;
      this.cacheData(cacheKey, response, this.cacheTimeout);

      this.logApiCall('setUserOnline - success', { userId });
      return response;

    } catch (error) {
      this.logApiCall('setUserOnline - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to set user online');
    }
  }

  /**
   * Set user offline (typically called on logout)
   */
  async setUserOffline(userId) {
    try {
      this.logApiCall('setUserOffline', { userId });

      const response = await api.post(`/users/${userId}/offline`);

      // Update cache
      const cacheKey = `user_status_${userId}`;
      this.cacheData(cacheKey, response, this.cacheTimeout);

      // Stop heartbeat if it's for this user
      this.stopHeartbeat();

      this.logApiCall('setUserOffline - success', { userId });
      return response;

    } catch (error) {
      this.logApiCall('setUserOffline - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to set user offline');
    }
  }

  /**
   * Send heartbeat to update last seen timestamp
   */
  async sendHeartbeat(userId) {
    try {
      await api.post(`/users/${userId}/heartbeat`);
      this.logApiCall('sendHeartbeat - success', { userId });

    } catch (error) {
      this.logApiCall('sendHeartbeat - error', { userId, error: error.message });
      // Don't throw error for heartbeat failures to avoid disrupting user experience
      console.warn('Heartbeat failed:', error.message);
    }
  }

  /**
   * Start automatic heartbeat for a user
   */
  startHeartbeat(userId, intervalMinutes = 5) {
    if (this.isHeartbeatActive) {
      this.stopHeartbeat();
    }

    this.logApiCall('startHeartbeat', { userId, intervalMinutes });

    this.heartbeatInterval = setInterval(() => {
      this.sendHeartbeat(userId);
    }, intervalMinutes * 60 * 1000);

    this.isHeartbeatActive = true;

    // Send initial heartbeat
    this.sendHeartbeat(userId);
  }

  /**
   * Stop automatic heartbeat
   */
  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
      this.isHeartbeatActive = false;
      this.logApiCall('stopHeartbeat', {});
    }
  }

  /**
   * Get all online users
   */
  async getOnlineUsers() {
    try {
      this.logApiCall('getOnlineUsers', {});

      const cacheKey = 'online_users';
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        this.logApiCall('getOnlineUsers - cache hit', {});
        return cachedData;
      }

      const response = await api.get('/users/online');

      // Cache for shorter time since this changes frequently
      this.cacheData(cacheKey, response, 30000); // 30 seconds

      this.logApiCall('getOnlineUsers - success', { count: response.count });
      return response;

    } catch (error) {
      this.logApiCall('getOnlineUsers - error', { error: error.message });
      throw this.handleApiError(error, 'Failed to get online users');
    }
  }

  /**
   * Get status statistics (for admin/analytics)
   */
  async getStatusStatistics() {
    try {
      this.logApiCall('getStatusStatistics', {});

      const response = await api.get('/users/status-statistics');

      this.logApiCall('getStatusStatistics - success', { totalUsers: response.totalUsers });
      return response;

    } catch (error) {
      this.logApiCall('getStatusStatistics - error', { error: error.message });
      throw this.handleApiError(error, 'Failed to get status statistics');
    }
  }

  /**
   * Invalidate caches related to a specific user
   */
  invalidateUserRelatedCaches(userId) {
    const keysToDelete = [];

    for (const key of this.cache.keys()) {
      if (key.includes(`user_status_${userId}`) || key.includes('online_users')) {
        keysToDelete.push(key);
      }
    }

    keysToDelete.forEach(key => this.cache.delete(key));
    this.logApiCall('invalidateUserRelatedCaches', { userId, deletedKeys: keysToDelete.length });
  }

  // ============================================================================
  // UTILITY METHODS (following your existing service patterns)
  // ============================================================================

  /**
   * Cache data with timestamp and TTL
   */
  cacheData(key, data, ttl = this.cacheTimeout) {
    const cacheEntry = {
      data,
      timestamp: Date.now(),
      ttl
    };
    this.cache.set(key, cacheEntry);
  }

  /**
   * Get cached data if not expired
   */
  getCachedData(key) {
    const cacheEntry = this.cache.get(key);
    if (!cacheEntry) return null;

    const now = Date.now();
    if (now - cacheEntry.timestamp > cacheEntry.ttl) {
      this.cache.delete(key);
      return null;
    }

    return cacheEntry.data;
  }

  /**
   * Invalidate specific cache entry
   */
  invalidateCache(key) {
    this.cache.delete(key);
  }

  /**
   * Clear all cache
   */
  clearCache() {
    this.cache.clear();
  }

  /**
   * Get cache statistics
   */
  getCacheStats() {
    return {
      totalEntries: this.cache.size,
      entries: Array.from(this.cache.keys())
    };
  }

  /**
   * Log API calls for debugging
   */
  logApiCall(operation, details) {
    console.log(`[UserStatusService] ${operation}:`, details);
  }

  /**
   * Handle and format API errors
   */
  handleApiError(error, defaultMessage) {
    const apiError = new Error(defaultMessage);
    apiError.originalError = error;
    apiError.timestamp = new Date().toISOString();

    if (error.response) {
      apiError.status = error.response.status;
      apiError.message = error.response.data?.error || error.response.data?.message || defaultMessage;
    } else if (error.request) {
      apiError.message = 'Network error - please check your connection';
    } else {
      apiError.message = error.message || defaultMessage;
    }

    return apiError;
  }
}

// Create and export service instance
export const userStatusService = new UserStatusService();
export default userStatusService;