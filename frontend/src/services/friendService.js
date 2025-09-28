import { api, API_ENDPOINTS } from './api.js';

/**
 * Friend Service - Comprehensive friend management and social interactions
 *
 * This service handles all friend-related operations including friend requests,
 * friend lists with pagination, friend search, availability integration,
 * mutual friends calculation, and friend recommendations. It follows the
 * established service architecture pattern with caching and error handling.
 *
 * Features:
 * - Complete friend lifecycle management (send, accept, decline, remove)
 * - Efficient pagination for large friend lists (500+ friends)
 * - Friend search with filtering and sorting capabilities
 * - Friend availability integration with batch loading
 * - Mutual friends calculation and friend recommendations
 * - Comprehensive caching for performance optimization
 * - Real-time synchronization with optimistic UI updates
 *
 * @class FriendService
 */
class FriendService {
  constructor() {
    this.cache = new Map();
    this.cacheTimeout = 5 * 60 * 1000; // 5 minutes default cache timeout
    this.shortCacheTimeout = 1 * 60 * 1000; // 1 minute for frequently changing data
    this.longCacheTimeout = 15 * 60 * 1000; // 15 minutes for stable data
  }

  /**
   * Get paginated friends list for a user
   *
   * @param {string|number} userId - The user ID to get friends for
   * @param {Object} options - Pagination and filtering options
   * @param {number} options.page - Page number (default: 1)
   * @param {number} options.limit - Items per page (default: 50)
   * @param {string} options.sort - Sort order: 'name', 'recent', 'availability' (default: 'name')
   * @param {string} options.filter - Filter type: 'all', 'online', 'available' (default: 'all')
   * @returns {Promise<Object>} Paginated friends data with metadata
   */
  async getFriends(userId, options = {}) {
    try {
      const {
        page = 1,
        limit = 50,
        sort = 'name',
        filter = 'all'
      } = options;

      this.logApiCall('getFriends', { userId, page, limit, sort, filter });

      // Check cache first
      const cacheKey = `friends_${userId}_${page}_${limit}_${sort}_${filter}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        this.logApiCall('getFriends - cache hit', { userId, page });
        return cachedData;
      }

      // Build query parameters
      const params = new URLSearchParams({
        page: page.toString(),
        limit: limit.toString(),
        sort,
        filter
      });

      const response = await api.get(`${API_ENDPOINTS.FRIENDS.BASE(userId)}?${params}`);

      // Validate response structure
      this.validateFriendsResponse(response);

      // Cache the result with appropriate timeout
      this.cacheData(cacheKey, response, this.cacheTimeout);

      this.logApiCall('getFriends - success', {
        userId,
        page,
        totalFriends: response.totalCount,
        returnedCount: response.friends?.length || 0
      });

      return response;

    } catch (error) {
      this.logApiCall('getFriends - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load friends list');
    }
  }

  /**
   * Get all friends for a user (without pagination)
   *
   * @param {string|number} userId - The user ID to get friends for
   * @returns {Promise<Array>} Array of friend objects
   */
  async getAllFriends(userId) {
    try {
      this.logApiCall('getAllFriends', { userId });

      const cacheKey = `all_friends_${userId}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      const friends = await api.get(API_ENDPOINTS.FRIENDS.BASE(userId));

      // Cache all friends list
      this.cacheData(cacheKey, friends, this.cacheTimeout);

      this.logApiCall('getAllFriends - success', { userId, count: friends.length });
      return friends;

    } catch (error) {
      this.logApiCall('getAllFriends - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load friends');
    }
  }

  /**
   * Send a friend request to another user
   *
   * @param {string|number} fromUserId - The user sending the request
   * @param {string|number} toUserId - The user receiving the request
   * @returns {Promise<Object>} Created friend request object
   */
  async sendFriendRequest(fromUserId, toUserId) {
    try {
      this.logApiCall('sendFriendRequest', { fromUserId, toUserId });

      // Validate input
      if (fromUserId === toUserId) {
        throw new Error('Cannot send friend request to yourself');
      }

      const params = new URLSearchParams({
        fromUserId: fromUserId.toString(),
        toUserId: toUserId.toString()
      });

      const friendRequest = await api.post(`${API_ENDPOINTS.FRIENDS.SEND_REQUEST}?${params}`);

      // Invalidate relevant caches
      this.invalidateUserFriendCaches(fromUserId);
      this.invalidateUserFriendCaches(toUserId);

      this.logApiCall('sendFriendRequest - success', { fromUserId, toUserId, requestId: friendRequest.id });
      return friendRequest;

    } catch (error) {
      this.logApiCall('sendFriendRequest - error', { fromUserId, toUserId, error: error.message });
      throw this.handleApiError(error, 'Failed to send friend request');
    }
  }

  /**
   * Accept a friend request
   *
   * @param {string|number} requestId - The friend request ID
   * @param {string|number} userId - The user accepting the request
   * @returns {Promise<Object>} Updated friend request object
   */
  async acceptFriendRequest(requestId, userId) {
    try {
      this.logApiCall('acceptFriendRequest', { requestId, userId });

      const updatedRequest = await api.put(API_ENDPOINTS.FRIENDS.ACCEPT(requestId, userId));

      // Invalidate relevant caches
      this.invalidateUserFriendCaches(userId);
      this.invalidateUserFriendCaches(updatedRequest.userId);
      this.invalidateCache(`pending_requests_${userId}`);

      this.logApiCall('acceptFriendRequest - success', { requestId, userId });
      return updatedRequest;

    } catch (error) {
      this.logApiCall('acceptFriendRequest - error', { requestId, userId, error: error.message });
      throw this.handleApiError(error, 'Failed to accept friend request');
    }
  }

  /**
   * Reject a friend request
   *
   * @param {string|number} requestId - The friend request ID
   * @param {string|number} userId - The user rejecting the request
   * @returns {Promise<Object>} Updated friend request object
   */
  async rejectFriendRequest(requestId, userId) {
    try {
      this.logApiCall('rejectFriendRequest', { requestId, userId });

      const updatedRequest = await api.put(API_ENDPOINTS.FRIENDS.REJECT(requestId, userId));

      // Invalidate relevant caches
      this.invalidateCache(`pending_requests_${userId}`);

      this.logApiCall('rejectFriendRequest - success', { requestId, userId });
      return updatedRequest;

    } catch (error) {
      this.logApiCall('rejectFriendRequest - error', { requestId, userId, error: error.message });
      throw this.handleApiError(error, 'Failed to reject friend request');
    }
  }

  /**
   * Get pending friend requests for a user
   *
   * @param {string|number} userId - The user ID to get pending requests for
   * @returns {Promise<Array>} Array of pending friend request objects
   */
  async getPendingRequests(userId) {
    try {
      this.logApiCall('getPendingRequests', { userId });

      const cacheKey = `pending_requests_${userId}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      const pendingRequests = await api.get(API_ENDPOINTS.FRIENDS.PENDING(userId));

      // Cache pending requests with shorter timeout (more dynamic data)
      this.cacheData(cacheKey, pendingRequests, this.shortCacheTimeout);

      this.logApiCall('getPendingRequests - success', { userId, count: pendingRequests.length });
      return pendingRequests;

    } catch (error) {
      this.logApiCall('getPendingRequests - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load pending requests');
    }
  }

  /**
   * Remove a friendship between two users
   *
   * @param {string|number} userId1 - First user ID
   * @param {string|number} userId2 - Second user ID
   * @returns {Promise<Object>} Removal result
   */
  async removeFriendship(userId1, userId2) {
    try {
      this.logApiCall('removeFriendship', { userId1, userId2 });

      const params = new URLSearchParams({
        userId1: userId1.toString(),
        userId2: userId2.toString()
      });

      const result = await api.delete(`${API_ENDPOINTS.FRIENDS.REMOVE}?${params}`);

      // Invalidate relevant caches
      this.invalidateUserFriendCaches(userId1);
      this.invalidateUserFriendCaches(userId2);

      this.logApiCall('removeFriendship - success', { userId1, userId2, success: result.success });
      return result;

    } catch (error) {
      this.logApiCall('removeFriendship - error', { userId1, userId2, error: error.message });
      throw this.handleApiError(error, 'Failed to remove friendship');
    }
  }

  /**
   * Search for friends with advanced filtering and sorting
   *
   * @param {string} query - Search query (name, email)
   * @param {Object} options - Search options
   * @param {string|number} options.userId - Current user ID for context
   * @param {Array} options.filters - Filter options: ['friends', 'non-friends', 'online', 'available']
   * @param {string} options.sort - Sort order: 'name', 'mutual-friends', 'recent'
   * @param {number} options.limit - Maximum results (default: 20)
   * @returns {Promise<Array>} Array of search results with metadata
   */
  async searchFriends(query, options = {}) {
    try {
      const {
        userId,
        filters = [],
        sort = 'name',
        limit = 20
      } = options;

      this.logApiCall('searchFriends', { query, userId, filters, sort, limit });

      // Check cache first
      const cacheKey = `search_${query}_${userId}_${filters.join(',')}_${sort}_${limit}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        this.logApiCall('searchFriends - cache hit', { query, userId });
        return cachedData;
      }

      const params = new URLSearchParams({
        q: query,
        ...(userId && { userId: userId.toString() }),
        sort,
        limit: limit.toString()
      });

      // Add filters to params
      filters.forEach(filter => {
        params.append('filters', filter);
      });

      const searchResults = await api.get(`/api/users/search?${params}`);

      // Enhance results with friend status and mutual friends count
      const enhancedResults = await this.enhanceSearchResults(searchResults, userId);

      // Cache search results with shorter timeout
      this.cacheData(cacheKey, enhancedResults, this.shortCacheTimeout);

      this.logApiCall('searchFriends - success', { query, userId, count: enhancedResults.length });
      return enhancedResults;

    } catch (error) {
      this.logApiCall('searchFriends - error', { query, userId, error: error.message });
      throw this.handleApiError(error, 'Failed to search friends');
    }
  }

  /**
   * Get mutual friends between two users
   *
   * @param {string|number} userId - Current user ID
   * @param {string|number} friendId - Friend user ID
   * @returns {Promise<Array>} Array of mutual friends
   */
  async getMutualFriends(userId, friendId) {
    try {
      this.logApiCall('getMutualFriends', { userId, friendId });

      const cacheKey = `mutual_friends_${userId}_${friendId}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      // Use backend endpoint for mutual friends calculation
      const mutualFriends = await api.get(API_ENDPOINTS.FRIENDS.MUTUAL(userId, friendId));

      // Cache mutual friends with longer timeout (stable data)
      this.cacheData(cacheKey, mutualFriends, this.longCacheTimeout);

      this.logApiCall('getMutualFriends - success', { userId, friendId, count: mutualFriends.length });
      return mutualFriends;

    } catch (error) {
      this.logApiCall('getMutualFriends - error', { userId, friendId, error: error.message });
      throw this.handleApiError(error, 'Failed to get mutual friends');
    }
  }

  /**
   * Get friend recommendations based on mutual connections
   *
   * @param {string|number} userId - The user ID to get recommendations for
   * @param {number} limit - Maximum recommendations (default: 10)
   * @returns {Promise<Array>} Array of friend recommendations with scores
   */
  async getFriendRecommendations(userId, limit = 10) {
    try {
      this.logApiCall('getFriendRecommendations', { userId, limit });

      const cacheKey = `recommendations_${userId}_${limit}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      // Get user's current friends
      const currentFriends = await this.getAllFriends(userId);
      const currentFriendIds = new Set(currentFriends.map(f => f.id));

      // Calculate recommendations based on mutual friends
      const recommendationMap = new Map();

      // For each friend, get their friends and count mutual connections
      for (const friend of currentFriends.slice(0, 20)) { // Limit to avoid too many API calls
        try {
          const friendsFriends = await this.getAllFriends(friend.id);

          for (const potentialFriend of friendsFriends) {
            // Skip if already friends or is the user themselves
            if (currentFriendIds.has(potentialFriend.id) || potentialFriend.id === userId) {
              continue;
            }

            const key = potentialFriend.id;
            if (recommendationMap.has(key)) {
              recommendationMap.get(key).score += 1;
              recommendationMap.get(key).mutualFriends.push(friend);
            } else {
              recommendationMap.set(key, {
                user: potentialFriend,
                score: 1,
                mutualFriends: [friend]
              });
            }
          }
        } catch (error) {
          // Skip friends where we can't get their friend list
          continue;
        }
      }

      // Sort recommendations by score and return top results
      const recommendations = Array.from(recommendationMap.values())
        .sort((a, b) => b.score - a.score)
        .slice(0, limit);

      // Cache recommendations with medium timeout
      this.cacheData(cacheKey, recommendations, this.cacheTimeout);

      this.logApiCall('getFriendRecommendations - success', { userId, count: recommendations.length });
      return recommendations;

    } catch (error) {
      this.logApiCall('getFriendRecommendations - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to get friend recommendations');
    }
  }

  /**
   * Get availability status for a friend
   *
   * @param {string|number} friendId - Friend user ID
   * @returns {Promise<Object>} Friend availability data
   */
  async getFriendAvailability(friendId) {
    try {
      this.logApiCall('getFriendAvailability', { friendId });

      const cacheKey = `availability_${friendId}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      const availability = await api.get(API_ENDPOINTS.CALENDAR.AVAILABILITY(friendId));

      // Cache availability with short timeout (frequently changing data)
      this.cacheData(cacheKey, availability, this.shortCacheTimeout);

      this.logApiCall('getFriendAvailability - success', { friendId });
      return availability;

    } catch (error) {
      this.logApiCall('getFriendAvailability - error', { friendId, error: error.message });
      throw this.handleApiError(error, 'Failed to get friend availability');
    }
  }

  /**
   * Get availability status for multiple friends in batch
   *
   * @param {Array} friendIds - Array of friend user IDs
   * @returns {Promise<Object>} Object with friendId as key and availability as value
   */
  async getBatchFriendAvailability(friendIds) {
    try {
      this.logApiCall('getBatchFriendAvailability', { count: friendIds.length });

      const availabilityPromises = friendIds.map(async (friendId) => {
        try {
          const availability = await this.getFriendAvailability(friendId);
          return { friendId, availability, success: true };
        } catch (error) {
          return { friendId, availability: null, success: false, error: error.message };
        }
      });

      const results = await Promise.allSettled(availabilityPromises);

      const availabilityMap = {};
      results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          const { friendId, availability, success } = result.value;
          availabilityMap[friendId] = success ? availability : null;
        } else {
          availabilityMap[friendIds[index]] = null;
        }
      });

      this.logApiCall('getBatchFriendAvailability - success', {
        requested: friendIds.length,
        successful: Object.values(availabilityMap).filter(Boolean).length
      });

      return availabilityMap;

    } catch (error) {
      this.logApiCall('getBatchFriendAvailability - error', { count: friendIds.length, error: error.message });
      throw this.handleApiError(error, 'Failed to get batch friend availability');
    }
  }

  /**
   * Get friendship statistics for a user
   *
   * @param {string|number} userId - The user ID to get statistics for
   * @returns {Promise<Object>} Friendship statistics
   */
  async getFriendshipStatistics(userId) {
    try {
      this.logApiCall('getFriendshipStatistics', { userId });

      const cacheKey = `friend_stats_${userId}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      const stats = await api.get(API_ENDPOINTS.FRIENDS.STATS(userId));

      // Cache statistics
      this.cacheData(cacheKey, stats, this.cacheTimeout);

      this.logApiCall('getFriendshipStatistics - success', { userId, stats });
      return stats;

    } catch (error) {
      this.logApiCall('getFriendshipStatistics - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to get friendship statistics');
    }
  }

  /**
   * Check if two users are friends
   *
   * @param {string|number} userId1 - First user ID
   * @param {string|number} userId2 - Second user ID
   * @returns {Promise<boolean>} True if users are friends
   */
  async areFriends(userId1, userId2) {
    try {
      this.logApiCall('areFriends', { userId1, userId2 });

      const params = new URLSearchParams({
        userId1: userId1.toString(),
        userId2: userId2.toString()
      });

      const result = await api.get(`${API_ENDPOINTS.FRIENDS.CHECK}?${params}`);

      this.logApiCall('areFriends - success', { userId1, userId2, areFriends: result.areFriends });
      return result.areFriends;

    } catch (error) {
      this.logApiCall('areFriends - error', { userId1, userId2, error: error.message });
      throw this.handleApiError(error, 'Failed to check friendship status');
    }
  }

  /**
   * Enhance search results with friend status and mutual friends
   *
   * @private
   * @param {Array} searchResults - Raw search results
   * @param {string|number} userId - Current user ID
   * @returns {Promise<Array>} Enhanced search results
   */
  async enhanceSearchResults(searchResults, userId) {
    if (!userId || !Array.isArray(searchResults)) {
      return searchResults;
    }

    const enhancedResults = await Promise.all(
      searchResults.map(async (user) => {
        try {
          const [areFriends, mutualFriends] = await Promise.all([
            this.areFriends(userId, user.id),
            this.getMutualFriends(userId, user.id)
          ]);

          return {
            ...user,
            friendStatus: areFriends ? 'friends' : 'not-friends',
            mutualFriendsCount: mutualFriends.length,
            mutualFriends: mutualFriends.slice(0, 3) // Show first 3 mutual friends
          };
        } catch (error) {
          return {
            ...user,
            friendStatus: 'unknown',
            mutualFriendsCount: 0,
            mutualFriends: []
          };
        }
      })
    );

    return enhancedResults;
  }

  /**
   * Invalidate all friend-related caches for a user
   *
   * @private
   * @param {string|number} userId - User ID to invalidate caches for
   */
  invalidateUserFriendCaches(userId) {
    const patterns = [
      `friends_${userId}`,
      `all_friends_${userId}`,
      `friend_stats_${userId}`,
      `recommendations_${userId}`,
      `mutual_friends_${userId}`,
      `pending_requests_${userId}`
    ];

    patterns.forEach(pattern => {
      Array.from(this.cache.keys())
        .filter(key => key.startsWith(pattern))
        .forEach(key => this.invalidateCache(key));
    });
  }

  /**
   * Validate friends response structure
   *
   * @private
   * @param {Object} response - API response to validate
   * @throws {Error} When validation fails
   */
  validateFriendsResponse(response) {
    if (!response || typeof response !== 'object') {
      throw new Error('Invalid friends response structure');
    }

    // For paginated responses
    if ('friends' in response) {
      if (!Array.isArray(response.friends)) {
        throw new Error('Friends data must be an array');
      }

      if (typeof response.totalCount !== 'number') {
        response.totalCount = response.friends.length;
      }

      if (typeof response.page !== 'number') {
        response.page = 1;
      }

      if (typeof response.limit !== 'number') {
        response.limit = response.friends.length;
      }
    }
    // For simple array responses
    else if (Array.isArray(response)) {
      // Valid array response
      return;
    } else {
      throw new Error('Response must contain friends array or be an array itself');
    }
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
    console.error('Friend API Error:', error);

    let userMessage = defaultMessage;

    if (error.message.includes('HTTP 401')) {
      userMessage = 'Authentication required. Please log in again.';
    } else if (error.message.includes('HTTP 403')) {
      userMessage = 'Access denied. You do not have permission for this action.';
    } else if (error.message.includes('HTTP 404')) {
      userMessage = 'The requested friend data was not found.';
    } else if (error.message.includes('HTTP 409')) {
      userMessage = 'Friend request already exists or users are already friends.';
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
    console[logLevel](`👥 FriendService.${operation}`, {
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

// Create and export friend service instance
export const friendService = new FriendService();