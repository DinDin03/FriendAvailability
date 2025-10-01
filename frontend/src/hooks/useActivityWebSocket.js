import { useCallback, useRef, useEffect } from 'react';
import { useWebSocket } from './useWebSocket';
import { useAuth } from '../contexts/AuthContext';
import { activityService } from '../services/activityService';

/**
 * useActivityWebSocket Hook - Activity-specific WebSocket integration
 *
 * This hook provides real-time activity updates through WebSocket connection,
 * with activity-specific message handling, deduplication, and optimistic updates.
 *
 * Features:
 * - Activity-specific WebSocket connection
 * - Real-time activity updates
 * - Activity deduplication
 * - Optimistic UI updates
 * - Error handling and fallback to polling
 */
export const useActivityWebSocket = (onActivityUpdate, options = {}) => {
  const { user } = useAuth();
  const {
    enableActivityUpdates = true,
    enableNotifications = true,
    dedupTimeWindow = 5000, // 5 seconds
    fallbackToPolling = true,
    pollingInterval = 30000 // 30 seconds
  } = options;

  // Activity deduplication tracking
  const activityCacheRef = useRef(new Map());
  const pollingIntervalRef = useRef(null);

  /**
   * Generate WebSocket URL for activity updates
   */
  const getWebSocketUrl = useCallback(() => {
    if (!user?.id) return null;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    return `${protocol}//${host}/ws/activities/${user.id}`;
  }, [user?.id]);

  /**
   * Handle incoming WebSocket messages
   */
  const handleMessage = useCallback((event) => {
    try {
      const data = JSON.parse(event.data);

      // Handle different message types
      switch (data.type) {
        case 'activity_update':
          handleActivityUpdate(data.payload);
          break;

        case 'activity_batch':
          handleActivityBatch(data.payload);
          break;

        case 'activity_deleted':
          handleActivityDeletion(data.payload);
          break;

        case 'pong':
          // Heartbeat response, ignore
          break;

        default:
          console.warn('Unknown WebSocket message type:', data.type);
      }
    } catch (error) {
      console.error('Failed to parse WebSocket message:', error);
    }
  }, []);

  /**
   * Handle single activity update
   */
  const handleActivityUpdate = useCallback((activity) => {
    if (!activity || !activity.id) return;

    // Check for duplicates within time window
    const cacheKey = activity.id;
    const now = Date.now();
    const cached = activityCacheRef.current.get(cacheKey);

    if (cached && (now - cached.timestamp) < dedupTimeWindow) {
      // Duplicate within time window, ignore
      return;
    }

    // Cache the activity
    activityCacheRef.current.set(cacheKey, {
      activity,
      timestamp: now
    });

    // Clean up old cache entries (keep last 100)
    if (activityCacheRef.current.size > 100) {
      const entries = Array.from(activityCacheRef.current.entries());
      const sorted = entries.sort((a, b) => b[1].timestamp - a[1].timestamp);
      const toKeep = sorted.slice(0, 100);

      activityCacheRef.current.clear();
      toKeep.forEach(([key, value]) => {
        activityCacheRef.current.set(key, value);
      });
    }

    // Process and enhance the activity
    processActivityUpdate(activity);
  }, [dedupTimeWindow]);

  /**
   * Handle batch activity updates
   */
  const handleActivityBatch = useCallback((activities) => {
    if (!Array.isArray(activities)) return;

    activities.forEach(activity => {
      handleActivityUpdate(activity);
    });
  }, [handleActivityUpdate]);

  /**
   * Handle activity deletion
   */
  const handleActivityDeletion = useCallback((deletionData) => {
    const { activityId } = deletionData;

    if (activityId) {
      onActivityUpdate?.({
        type: 'delete',
        activityId
      });
    }
  }, [onActivityUpdate]);

  /**
   * Process and enhance activity update
   */
  const processActivityUpdate = useCallback(async (activity) => {
    try {
      // Enhance activity with additional metadata
      const enhancedActivities = await activityService.enhanceActivities([activity], user.id);
      const enhancedActivity = enhancedActivities[0];

      if (enhancedActivity) {
        // Call the update callback
        onActivityUpdate?.(enhancedActivity);

        // Show notification if enabled and activity is important
        if (enableNotifications && enhancedActivity.priority >= 3) {
          showActivityNotification(enhancedActivity);
        }
      }
    } catch (error) {
      console.error('Failed to process activity update:', error);
      // Still call the callback with unenhanced activity
      onActivityUpdate?.(activity);
    }
  }, [user?.id, onActivityUpdate, enableNotifications]);

  /**
   * Show browser notification for important activities
   */
  const showActivityNotification = useCallback((activity) => {
    if (!('Notification' in window) || Notification.permission !== 'granted') {
      return;
    }

    const title = 'New Activity';
    const body = activity.displayMessage || `New activity from ${activity.friend?.name || 'a friend'}`;
    const icon = activity.friend?.avatar || '/favicon.ico';

    const notification = new Notification(title, {
      body,
      icon,
      tag: `activity-${activity.id}`, // Prevent duplicate notifications
      requireInteraction: false
    });

    // Auto-close after 5 seconds
    setTimeout(() => {
      notification.close();
    }, 5000);

    // Handle notification click
    notification.onclick = () => {
      window.focus();
      notification.close();
    };
  }, []);

  /**
   * Setup polling fallback
   */
  const setupPolling = useCallback(() => {
    if (!fallbackToPolling || !user?.id) return;

    pollingIntervalRef.current = setInterval(async () => {
      try {
        // Get recent activities as fallback
        const response = await activityService.getActivityFeed(user.id, {
          limit: 5,
          dateRange: 'today'
        });

        const activities = response.activities || [];
        activities.forEach(activity => {
          // Check if this is a new activity we haven't seen
          const cacheKey = activity.id;
          const cached = activityCacheRef.current.get(cacheKey);

          if (!cached) {
            handleActivityUpdate(activity);
          }
        });
      } catch (error) {
        console.error('Polling fallback failed:', error);
      }
    }, pollingInterval);
  }, [fallbackToPolling, user?.id, pollingInterval, handleActivityUpdate]);

  /**
   * Clear polling fallback
   */
  const clearPolling = useCallback(() => {
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
      pollingIntervalRef.current = null;
    }
  }, []);

  /**
   * Handle WebSocket connection events
   */
  const handleOpen = useCallback(() => {
    console.log('Activity WebSocket connected');
    clearPolling(); // Stop polling when WebSocket is connected
  }, [clearPolling]);

  const handleClose = useCallback(() => {
    console.log('Activity WebSocket disconnected');
    if (fallbackToPolling) {
      setupPolling(); // Start polling when WebSocket disconnects
    }
  }, [fallbackToPolling, setupPolling]);

  const handleError = useCallback((error) => {
    console.error('Activity WebSocket error:', error);
    if (fallbackToPolling) {
      setupPolling(); // Start polling on error
    }
  }, [fallbackToPolling, setupPolling]);

  // Initialize WebSocket connection
  const wsUrl = getWebSocketUrl();
  const {
    connectionStatus,
    sendMessage,
    isConnected,
    connectionAttempts
  } = useWebSocket(enableActivityUpdates ? wsUrl : null, {
    onOpen: handleOpen,
    onClose: handleClose,
    onError: handleError,
    onMessage: handleMessage,
    shouldReconnect: true,
    maxReconnectAttempts: 5,
    reconnectInterval: 1000,
    heartbeatInterval: 30000
  });

  /**
   * Send activity read status update
   */
  const markActivityAsRead = useCallback((activityId) => {
    if (isConnected) {
      sendMessage({
        type: 'mark_read',
        activityId
      });
      return true;
    }
    return false;
  }, [isConnected, sendMessage]);

  /**
   * Subscribe to specific activity types
   */
  const subscribeToActivityTypes = useCallback((activityTypes) => {
    if (isConnected) {
      sendMessage({
        type: 'subscribe',
        activityTypes
      });
      return true;
    }
    return false;
  }, [isConnected, sendMessage]);

  /**
   * Request permission for browser notifications
   */
  const requestNotificationPermission = useCallback(async () => {
    if (!('Notification' in window)) {
      return 'not-supported';
    }

    if (Notification.permission === 'granted') {
      return 'granted';
    }

    if (Notification.permission === 'denied') {
      return 'denied';
    }

    const permission = await Notification.requestPermission();
    return permission;
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      clearPolling();
      activityCacheRef.current.clear();
    };
  }, [clearPolling]);

  return {
    connectionStatus,
    isConnected,
    connectionAttempts,
    markActivityAsRead,
    subscribeToActivityTypes,
    requestNotificationPermission,
    activityCacheSize: activityCacheRef.current.size
  };
};