import { api, API_ENDPOINTS } from './api.js';
import { Client } from '@stomp/stompjs';

/**
 * Chat Service - Comprehensive chat and real-time messaging management
 *
 * This service handles all chat-related operations including chat room management,
 * message history, real-time WebSocket messaging via STOMP, and intelligent caching.
 * It follows the established service architecture pattern with robust error handling.
 *
 * Features:
 * - Chat room management (create, list, get details)
 * - Message operations (send, receive, history, unread counts)
 * - Real-time WebSocket messaging via STOMP over SockJS
 * - Automatic reconnection with exponential backoff
 * - Room subscription management
 * - Performance optimization with caching
 * - Comprehensive error handling and logging
 *
 * @class ChatService
 */
class ChatService {
  constructor() {
    // Cache configuration
    this.cache = new Map();
    this.cacheTimeout = 2 * 60 * 1000; // 2 minutes default for chat rooms
    this.messageCacheTimeout = 5 * 60 * 1000; // 5 minutes for message history

    // WebSocket/STOMP configuration
    this.stompClient = null;
    this.isConnected = false;
    this.isConnecting = false;

    // Reconnection parameters
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 1000; // Start with 1 second
    this.maxReconnectDelay = 30000; // Max 30 seconds

    // Room subscriptions tracking
    this.activeSubscriptions = new Map(); // roomId -> subscription object
    this.messageCallbacks = new Map(); // roomId -> callback function
    this.roomSyncData = new Map(); // roomId -> { userId, lastMessageTime }

    // Connection event handlers
    this.onConnectCallbacks = [];
    this.onDisconnectCallbacks = [];
    this.onErrorCallbacks = [];
  }

  // ===========================
  // REST API METHODS
  // ===========================

  /**
   * Get all chat rooms for a user
   *
   * @param {string|number} userId - User ID to get chat rooms for
   * @param {Object} options - Pagination options
   * @param {number} options.page - Page number (default: 0)
   * @param {number} options.size - Page size (default: 10, 0 for all)
   * @returns {Promise<Object>} Chat rooms list with metadata
   */
  async getUserChatRooms(userId, options = {}) {
    try {
      const { page = 0, size = 0 } = options;

      this.logApiCall('getUserChatRooms', { userId, page, size });

      // Check cache
      const cacheKey = `chat_rooms_${userId}_${page}_${size}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        this.logApiCall('getUserChatRooms - cache hit', { userId });
        return cachedData;
      }

      // Build query params
      const params = new URLSearchParams({
        userId: userId.toString(),
        page: page.toString(),
        size: size.toString()
      });

      const response = await api.get(`${API_ENDPOINTS.CHAT.ROOMS}?${params}`);

      // Cache the result
      this.cacheData(cacheKey, response, this.cacheTimeout);

      this.logApiCall('getUserChatRooms - success', {
        userId,
        roomsCount: response.chatRooms?.length || response.content?.length || 0
      });

      return response;

    } catch (error) {
      this.logApiCall('getUserChatRooms - error', { userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load chat rooms');
    }
  }

  /**
   * Get chat room details with participants
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - Current user ID
   * @returns {Promise<Object>} Chat room details
   */
  async getChatRoomDetails(roomId, userId) {
    try {
      this.logApiCall('getChatRoomDetails', { roomId, userId });

      const cacheKey = `chat_room_${roomId}_${userId}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      const response = await api.get(API_ENDPOINTS.CHAT.ROOM_DETAILS(roomId, userId));

      this.cacheData(cacheKey, response, this.cacheTimeout);

      this.logApiCall('getChatRoomDetails - success', { roomId, userId });
      return response;

    } catch (error) {
      this.logApiCall('getChatRoomDetails - error', { roomId, userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load chat room details');
    }
  }

  /**
   * Get message history for a chat room (paginated)
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - Current user ID
   * @param {number} page - Page number (default: 0)
   * @param {number} size - Page size (default: 20)
   * @returns {Promise<Object>} Message history with pagination metadata
   */
  async getMessageHistory(roomId, userId, page = 0, size = 20) {
    try {
      this.logApiCall('getMessageHistory', { roomId, userId, page, size });

      const cacheKey = `messages_${roomId}_${page}_${size}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      const params = new URLSearchParams({
        userId: userId.toString(),
        page: page.toString(),
        size: size.toString()
      });

      const endpoint = `${API_ENDPOINTS.CHAT.MESSAGES(roomId, userId).split('?')[0]}?${params}`;
      const response = await api.get(endpoint);

      this.cacheData(cacheKey, response, this.messageCacheTimeout);

      this.logApiCall('getMessageHistory - success', {
        roomId,
        messagesCount: response.messages?.length || 0,
        page
      });

      return response;

    } catch (error) {
      this.logApiCall('getMessageHistory - error', { roomId, userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load message history');
    }
  }

  /**
   * Get recent messages for a chat room
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - Current user ID
   * @param {number} limit - Number of recent messages (default: 50)
   * @returns {Promise<Object>} Recent messages
   */
  async getRecentMessages(roomId, userId, limit = 50) {
    try {
      this.logApiCall('getRecentMessages', { roomId, userId, limit });

      const cacheKey = `recent_messages_${roomId}_${limit}`;
      const cachedData = this.getCachedData(cacheKey);
      if (cachedData) {
        return cachedData;
      }

      const response = await api.get(API_ENDPOINTS.CHAT.RECENT_MESSAGES(roomId, userId, limit));

      // Shorter cache for recent messages
      this.cacheData(cacheKey, response, 30000); // 30 seconds

      this.logApiCall('getRecentMessages - success', {
        roomId,
        messagesCount: response.messages?.length || 0
      });

      return response;

    } catch (error) {
      this.logApiCall('getRecentMessages - error', { roomId, userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load recent messages');
    }
  }

  /**
   * Get unread messages for a chat room
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - Current user ID
   * @returns {Promise<Object>} Unread messages with count
   */
  async getUnreadMessages(roomId, userId) {
    try {
      this.logApiCall('getUnreadMessages', { roomId, userId });

      const response = await api.get(API_ENDPOINTS.CHAT.UNREAD_MESSAGES(roomId, userId));

      this.logApiCall('getUnreadMessages - success', {
        roomId,
        unreadCount: response.unreadCount || 0
      });

      return response;

    } catch (error) {
      this.logApiCall('getUnreadMessages - error', { roomId, userId, error: error.message });
      throw this.handleApiError(error, 'Failed to load unread messages');
    }
  }

  /**
   * Mark messages as read in a chat room
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - Current user ID
   * @returns {Promise<Object>} Success response
   */
  async markMessagesAsRead(roomId, userId) {
    try {
      this.logApiCall('markMessagesAsRead', { roomId, userId });

      const response = await api.post(API_ENDPOINTS.CHAT.MARK_READ(roomId, userId));

      // Invalidate unread messages cache
      this.invalidateRoomCache(roomId);

      this.logApiCall('markMessagesAsRead - success', { roomId, userId });
      return response;

    } catch (error) {
      this.logApiCall('markMessagesAsRead - error', { roomId, userId, error: error.message });
      throw this.handleApiError(error, 'Failed to mark messages as read');
    }
  }

  /**
   * Get messages sent after a specific timestamp
   * Used for syncing missed messages when offline
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - Current user ID
   * @param {string} afterTime - ISO timestamp to get messages after
   * @returns {Promise<Object>} Messages list
   */
  async getMessagesAfter(roomId, userId, afterTime) {
    try {
      this.logApiCall('getMessagesAfter', { roomId, userId, afterTime });

      // Backend endpoint: GET /api/chat/rooms/{roomId}/messages/after?userId={userId}&afterTime={afterTime}
      const response = await api.get(`/chat/rooms/${roomId}/messages/after?userId=${userId}&afterTime=${encodeURIComponent(afterTime)}`);

      this.logApiCall('getMessagesAfter - success', { roomId, userId, count: response?.messages?.length || 0 });
      return response;

    } catch (error) {
      this.logApiCall('getMessagesAfter - error', { roomId, userId, afterTime, error: error.message });
      // Return empty array on error to not break the app
      return { messages: [] };
    }
  }

  /**
   * Sync missed messages when reconnecting or returning to chat
   * Fetches messages sent after the last known message
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - Current user ID
   * @param {string} lastMessageTime - Timestamp of last known message
   * @returns {Promise<Array>} Array of new messages
   */
  async syncMissedMessages(roomId, userId, lastMessageTime) {
    try {
      if (!lastMessageTime) {
        console.log('No last message time provided, skipping sync');
        return [];
      }

      this.logApiCall('syncMissedMessages', { roomId, userId, lastMessageTime });

      const response = await this.getMessagesAfter(roomId, userId, lastMessageTime);
      const newMessages = response?.messages || [];

      this.logApiCall('syncMissedMessages - success', { roomId, userId, count: newMessages.length });
      return newMessages;

    } catch (error) {
      this.logApiCall('syncMissedMessages - error', { roomId, userId, error: error.message });
      return [];
    }
  }

  /**
   * Create or get existing private chat between two users
   *
   * @param {string|number} userId1 - First user ID
   * @param {string|number} userId2 - Second user ID
   * @returns {Promise<Object>} Chat room details
   */
  async createPrivateChat(userId1, userId2) {
    try {
      this.logApiCall('createPrivateChat', { userId1, userId2 });

      const response = await api.post(API_ENDPOINTS.CHAT.CREATE_PRIVATE_CHAT, {
        userId1,
        userId2
      });

      // Invalidate chat rooms cache
      this.invalidateUserRoomsCache(userId1);
      this.invalidateUserRoomsCache(userId2);

      this.logApiCall('createPrivateChat - success', { userId1, userId2, roomId: response.id });
      return response;

    } catch (error) {
      this.logApiCall('createPrivateChat - error', { userId1, userId2, error: error.message });
      throw this.handleApiError(error, 'Failed to create private chat');
    }
  }

  /**
   * Create a new group chat
   *
   * @param {string|number} creatorId - Creator user ID
   * @param {string} chatName - Group chat name
   * @param {Array<number>} participantIds - Array of participant user IDs
   * @returns {Promise<Object>} Chat room details
   */
  async createGroupChat(creatorId, chatName, participantIds) {
    try {
      this.logApiCall('createGroupChat', { creatorId, chatName, participantCount: participantIds.length });

      const response = await api.post(API_ENDPOINTS.CHAT.CREATE_GROUP_CHAT, {
        creatorId,
        chatName,
        participantIds
      });

      // Invalidate chat rooms cache for all participants
      participantIds.forEach(userId => {
        this.invalidateUserRoomsCache(userId);
      });

      this.logApiCall('createGroupChat - success', { creatorId, chatName, roomId: response.id });
      return response;

    } catch (error) {
      this.logApiCall('createGroupChat - error', { creatorId, chatName, error: error.message });
      throw this.handleApiError(error, 'Failed to create group chat');
    }
  }

  /**
   * Search messages in a chat room
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - Current user ID
   * @param {string} searchTerm - Search term
   * @returns {Promise<Object>} Search results
   */
  async searchMessages(roomId, userId, searchTerm) {
    try {
      this.logApiCall('searchMessages', { roomId, userId, searchTerm });

      const response = await api.get(API_ENDPOINTS.CHAT.SEARCH_MESSAGES(roomId, userId, searchTerm));

      this.logApiCall('searchMessages - success', {
        roomId,
        resultsCount: response.messages?.length || 0
      });

      return response;

    } catch (error) {
      this.logApiCall('searchMessages - error', { roomId, userId, error: error.message });
      throw this.handleApiError(error, 'Failed to search messages');
    }
  }

  // ===========================
  // WEBSOCKET METHODS
  // ===========================

  /**
   * Initialize WebSocket connection with STOMP over SockJS
   *
   * @returns {Promise<void>}
   */
  async initializeWebSocket() {
    if (this.isConnected) {
      this.logApiCall('initializeWebSocket - already connected');
      return Promise.resolve();
    }

    if (this.isConnecting) {
      this.logApiCall('initializeWebSocket - connection in progress');
      return new Promise((resolve) => {
        const checkConnection = setInterval(() => {
          if (this.isConnected) {
            clearInterval(checkConnection);
            resolve();
          }
        }, 100);
      });
    }

    return new Promise((resolve, reject) => {
      try {
        this.isConnecting = true;

        // Get WebSocket URL
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.hostname;
        const port = import.meta.env.DEV ? '8080' : window.location.port;
        const wsUrl = `${protocol}//${host}:${port}/ws`;

        this.logApiCall('initializeWebSocket', { wsUrl });

        // Create STOMP client with native WebSocket
        this.stompClient = new Client({
          // Use native WebSocket
          brokerURL: wsUrl,

          // Connection timeout
          connectionTimeout: 10000,

          // Reconnection configuration
          reconnectDelay: this.reconnectDelay,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,

          // Debug logging in development
          debug: (str) => {
            if (import.meta.env.DEV) {
              console.log('STOMP: ' + str);
            }
          },

          // Connection success handler
          onConnect: (frame) => {
            this.isConnected = true;
            this.isConnecting = false;
            const wasReconnecting = this.reconnectAttempts > 0;
            this.reconnectAttempts = 0;
            this.reconnectDelay = 1000;

            this.logApiCall('WebSocket connected', { frame, wasReconnecting });

            // If this was a reconnection, trigger missed message sync for all subscribed rooms
            if (wasReconnecting) {
              console.log('WebSocket reconnected - syncing missed messages for subscribed rooms');
              this.syncAllSubscribedRooms();
            }

            // Trigger connect callbacks
            this.onConnectCallbacks.forEach(callback => {
              try {
                callback(frame);
              } catch (error) {
                console.error('Connect callback error:', error);
              }
            });

            resolve();
          },

          // Connection error handler
          onStompError: (frame) => {
            this.isConnected = false;
            this.isConnecting = false;

            console.error('STOMP error:', frame);
            this.logApiCall('WebSocket STOMP error', { error: frame.headers?.message || 'Unknown error' });

            // Trigger error callbacks
            this.onErrorCallbacks.forEach(callback => {
              try {
                callback(frame);
              } catch (error) {
                console.error('Error callback error:', error);
              }
            });

            reject(new Error('STOMP connection failed: ' + (frame.headers?.message || 'Unknown error')));
          },

          // WebSocket error handler
          onWebSocketError: (event) => {
            this.isConnected = false;
            this.isConnecting = false;

            console.error('WebSocket error:', event);
            this.logApiCall('WebSocket error', { event });

            reject(new Error('WebSocket connection failed'));
          },

          // Connection close handler
          onWebSocketClose: (event) => {
            this.isConnected = false;
            this.isConnecting = false;

            this.logApiCall('WebSocket closed', { code: event.code, reason: event.reason });

            // Trigger disconnect callbacks
            this.onDisconnectCallbacks.forEach(callback => {
              try {
                callback(event);
              } catch (error) {
                console.error('Disconnect callback error:', error);
              }
            });

            // Attempt reconnection if not a clean close
            if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
              this.handleReconnection();
            }
          }
        });

        // Activate the STOMP client
        this.stompClient.activate();

      } catch (error) {
        this.isConnecting = false;
        this.logApiCall('initializeWebSocket - error', { error: error.message });
        reject(error);
      }
    });
  }

  /**
   * Handle WebSocket reconnection with exponential backoff
   *
   * @private
   */
  handleReconnection() {
    this.reconnectAttempts++;
    this.reconnectDelay = Math.min(
      this.reconnectDelay * 2,
      this.maxReconnectDelay
    );

    this.logApiCall('WebSocket reconnection attempt', {
      attempt: this.reconnectAttempts,
      maxAttempts: this.maxReconnectAttempts,
      delay: this.reconnectDelay
    });

    setTimeout(() => {
      if (!this.isConnected && !this.isConnecting) {
        this.initializeWebSocket().catch(error => {
          console.error('Reconnection failed:', error);
        });
      }
    }, this.reconnectDelay);
  }

  /**
   * Sync missed messages for all subscribed rooms after reconnection
   * Called automatically when WebSocket reconnects
   *
   * @private
   */
  async syncAllSubscribedRooms() {
    try {
      console.log('Syncing missed messages for', this.activeSubscriptions.size, 'subscribed rooms');

      // Get all subscribed rooms
      const roomIds = Array.from(this.activeSubscriptions.keys());

      // Sync each room
      for (const roomId of roomIds) {
        const roomData = this.roomSyncData?.get(roomId);
        if (roomData && roomData.lastMessageTime && roomData.userId) {
          console.log(`Syncing room ${roomId} from ${roomData.lastMessageTime}`);

          const newMessages = await this.syncMissedMessages(
            roomId,
            roomData.userId,
            roomData.lastMessageTime
          );

          // Deliver new messages via the callback
          const callback = this.messageCallbacks.get(roomId);
          if (callback && newMessages.length > 0) {
            console.log(`Delivering ${newMessages.length} missed messages to room ${roomId}`);
            newMessages.forEach(message => {
              try {
                callback(message);
              } catch (error) {
                console.error('Error delivering missed message:', error);
              }
            });

            // Update last message time
            const latestMessage = newMessages[newMessages.length - 1];
            if (latestMessage?.sentAt) {
              roomData.lastMessageTime = latestMessage.sentAt;
            }
          }
        }
      }

      console.log('Finished syncing missed messages');
    } catch (error) {
      console.error('Error syncing missed messages:', error);
    }
  }

  /**
   * Set room sync data for tracking last message time
   * Should be called before subscribing to a room
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - User ID
   * @param {string} lastMessageTime - ISO timestamp of last message
   */
  setRoomSyncData(roomId, userId, lastMessageTime) {
    this.roomSyncData.set(roomId, {
      userId,
      lastMessageTime
    });
    console.log(`Room sync data set for room ${roomId}:`, { userId, lastMessageTime });
  }

  /**
   * Subscribe to chat room messages
   *
   * @param {string|number} roomId - Room ID to subscribe to
   * @param {Function} onMessage - Callback for new messages
   * @returns {Promise<void>}
   */
  async subscribeToRoom(roomId, onMessage) {
    try {
      // Ensure WebSocket is connected
      if (!this.isConnected) {
        await this.initializeWebSocket();
      }

      // Check if already subscribed
      if (this.activeSubscriptions.has(roomId)) {
        this.logApiCall('subscribeToRoom - already subscribed', { roomId });
        // Update callback
        this.messageCallbacks.set(roomId, onMessage);
        return;
      }

      this.logApiCall('subscribeToRoom', { roomId });

      const destination = `/topic/chat/${roomId}`;

      const subscription = this.stompClient.subscribe(destination, (message) => {
        try {
          const messageData = JSON.parse(message.body);
          this.logApiCall('Message received', { roomId, messageId: messageData.id });

          // Update last message time for sync tracking
          const roomData = this.roomSyncData.get(roomId);
          if (roomData && messageData.sentAt) {
            roomData.lastMessageTime = messageData.sentAt;
          }

          // Invalidate message cache for this room
          this.invalidateRoomCache(roomId);

          // Call the message callback
          const callback = this.messageCallbacks.get(roomId);
          if (callback) {
            callback(messageData);
          }
        } catch (error) {
          console.error('Error processing message:', error);
        }
      });

      this.activeSubscriptions.set(roomId, subscription);
      this.messageCallbacks.set(roomId, onMessage);

      this.logApiCall('subscribeToRoom - success', { roomId, destination });

    } catch (error) {
      this.logApiCall('subscribeToRoom - error', { roomId, error: error.message });
      throw this.handleApiError(error, 'Failed to subscribe to chat room');
    }
  }

  /**
   * Unsubscribe from chat room messages
   *
   * @param {string|number} roomId - Room ID to unsubscribe from
   */
  unsubscribeFromRoom(roomId) {
    try {
      const subscription = this.activeSubscriptions.get(roomId);
      if (subscription) {
        subscription.unsubscribe();
        this.activeSubscriptions.delete(roomId);
        this.messageCallbacks.delete(roomId);

        this.logApiCall('unsubscribeFromRoom - success', { roomId });
      } else {
        this.logApiCall('unsubscribeFromRoom - not subscribed', { roomId });
      }
    } catch (error) {
      console.error('Error unsubscribing from room:', error);
    }
  }

  /**
   * Send a message via WebSocket
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} senderId - Sender user ID
   * @param {string} content - Message content
   * @returns {boolean} Success status
   */
  sendMessage(roomId, senderId, content) {
    try {
      if (!this.isConnected) {
        throw new Error('WebSocket not connected. Please try again.');
      }

      if (!content || content.trim() === '') {
        throw new Error('Message content cannot be empty');
      }

      this.logApiCall('sendMessage', { roomId, senderId, contentLength: content.length });

      const messagePayload = {
        roomId,
        senderId,
        content: content.trim()
      };

      this.stompClient.publish({
        destination: '/app/chat.sendMessage',
        body: JSON.stringify(messagePayload)
      });

      this.logApiCall('sendMessage - success', { roomId, senderId });
      return true;

    } catch (error) {
      this.logApiCall('sendMessage - error', { roomId, senderId, error: error.message });
      throw this.handleApiError(error, 'Failed to send message');
    }
  }

  /**
   * Send join room notification via WebSocket
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - User ID
   * @returns {boolean} Success status
   */
  joinRoom(roomId, userId) {
    try {
      if (!this.isConnected) {
        throw new Error('WebSocket not connected');
      }

      this.logApiCall('joinRoom', { roomId, userId });

      const payload = {
        roomId,
        userId
      };

      this.stompClient.publish({
        destination: '/app/chat.connectToChat',
        body: JSON.stringify(payload)
      });

      this.logApiCall('joinRoom - success', { roomId, userId });
      return true;

    } catch (error) {
      this.logApiCall('joinRoom - error', { roomId, userId, error: error.message });
      console.error('Failed to send join notification:', error);
      return false;
    }
  }

  /**
   * Send leave room notification via WebSocket
   *
   * @param {string|number} roomId - Room ID
   * @param {string|number} userId - User ID
   * @returns {boolean} Success status
   */
  leaveRoom(roomId, userId) {
    try {
      if (!this.isConnected) {
        this.logApiCall('leaveRoom - not connected', { roomId, userId });
        return false;
      }

      this.logApiCall('leaveRoom', { roomId, userId });

      const payload = {
        roomId,
        userId
      };

      this.stompClient.publish({
        destination: '/app/chat.disconnectFromChat',
        body: JSON.stringify(payload)
      });

      this.logApiCall('leaveRoom - success', { roomId, userId });
      return true;

    } catch (error) {
      this.logApiCall('leaveRoom - error', { roomId, userId, error: error.message });
      console.error('Failed to send leave notification:', error);
      return false;
    }
  }

  /**
   * Disconnect WebSocket connection
   */
  async disconnectWebSocket() {
    try {
      this.logApiCall('disconnectWebSocket');

      // Unsubscribe from all rooms
      this.activeSubscriptions.forEach((subscription, roomId) => {
        subscription.unsubscribe();
      });

      this.activeSubscriptions.clear();
      this.messageCallbacks.clear();

      // Deactivate STOMP client
      if (this.stompClient) {
        await this.stompClient.deactivate();
        this.stompClient = null;
      }

      this.isConnected = false;
      this.isConnecting = false;

      this.logApiCall('disconnectWebSocket - success');

    } catch (error) {
      console.error('Error disconnecting WebSocket:', error);
    }
  }

  /**
   * Check if WebSocket is connected
   *
   * @returns {boolean} Connection status
   */
  isWebSocketConnected() {
    return this.isConnected && this.stompClient && this.stompClient.connected;
  }

  /**
   * Add connection event handler
   *
   * @param {Function} callback - Callback function
   */
  onConnect(callback) {
    this.onConnectCallbacks.push(callback);
  }

  /**
   * Add disconnection event handler
   *
   * @param {Function} callback - Callback function
   */
  onDisconnect(callback) {
    this.onDisconnectCallbacks.push(callback);
  }

  /**
   * Add error event handler
   *
   * @param {Function} callback - Callback function
   */
  onError(callback) {
    this.onErrorCallbacks.push(callback);
  }

  // ===========================
  // CACHE MANAGEMENT
  // ===========================

  /**
   * Cache data with optional timeout
   *
   * @private
   * @param {string} key - Cache key
   * @param {*} data - Data to cache
   * @param {number} timeout - Cache timeout in milliseconds
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
   * @param {string} key - Cache key to invalidate
   */
  invalidateCache(key) {
    this.cache.delete(key);
    this.logApiCall('invalidateCache', { key });
  }

  /**
   * Invalidate all cache for a specific room
   *
   * @private
   * @param {string|number} roomId - Room ID
   */
  invalidateRoomCache(roomId) {
    const patterns = [
      `messages_${roomId}`,
      `recent_messages_${roomId}`,
      `chat_room_${roomId}`
    ];

    patterns.forEach(pattern => {
      Array.from(this.cache.keys())
        .filter(key => key.startsWith(pattern))
        .forEach(key => this.invalidateCache(key));
    });
  }

  /**
   * Invalidate user's chat rooms cache
   *
   * @private
   * @param {string|number} userId - User ID
   */
  invalidateUserRoomsCache(userId) {
    const pattern = `chat_rooms_${userId}`;
    Array.from(this.cache.keys())
      .filter(key => key.startsWith(pattern))
      .forEach(key => this.invalidateCache(key));
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

  // ===========================
  // ERROR HANDLING & LOGGING
  // ===========================

  /**
   * Handle API errors with user-friendly messages
   *
   * @private
   * @param {Error} error - The original error
   * @param {string} defaultMessage - Default user-friendly message
   * @returns {Error} Enhanced error with user-friendly message
   */
  handleApiError(error, defaultMessage = 'An error occurred') {
    console.error('Chat API Error:', error);

    let userMessage = defaultMessage;

    if (error.message.includes('HTTP 401')) {
      userMessage = 'Authentication required. Please log in again.';
    } else if (error.message.includes('HTTP 403')) {
      userMessage = 'Access denied. You do not have permission to access this chat.';
    } else if (error.message.includes('HTTP 404')) {
      userMessage = 'Chat room or message not found.';
    } else if (error.message.includes('HTTP 500')) {
      userMessage = 'Server error. Please try again later.';
    } else if (error.message.includes('Network') || error.message.includes('Failed to fetch')) {
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
    console[logLevel](`💬 ChatService.${operation}`, {
      timestamp: new Date().toISOString(),
      ...details
    });
  }
}

// Create and export chat service instance
export const chatService = new ChatService();