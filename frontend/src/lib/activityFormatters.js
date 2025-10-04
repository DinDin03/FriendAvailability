/**
 * Activity Formatting Utilities
 *
 * Shared utilities for formatting and displaying activity feed items.
 * These utilities help format activity messages, get appropriate icons,
 * and determine colors for different activity types.
 */

import { Clock, Calendar, User, Users, UserPlus, Bell, MessageCircle, Heart } from 'lucide-react';

/**
 * Activity Type Constants
 * Maps backend activity types to display names
 */
export const ACTIVITY_TYPES = {
  AVAILABILITY_CHANGE: 'availability_change',
  AVAILABILITY_SLOT_UPDATE: 'availability_slot_update',
  PROFILE_UPDATE: 'profile_update',
  CIRCLE_ACTIVITY: 'circle_activity',
  SOCIAL_ACTIVITY: 'social_activity',
  FRIEND_REQUEST: 'friend_request',
  FRIEND_REQUEST_ACCEPTED: 'friend_request_accepted',
  FRIEND_ADDED: 'friend_added',
  MESSAGE_RECEIVED: 'message_received',
  EVENT_CREATED: 'event_created',
  EVENT_UPDATED: 'event_updated',
};

/**
 * Get the appropriate Lucide icon component for an activity type
 *
 * @param {string} activityType - The activity type from backend
 * @returns {React.Component} Lucide icon component
 */
export const getActivityIcon = (activityType) => {
  const iconMap = {
    [ACTIVITY_TYPES.AVAILABILITY_CHANGE]: Clock,
    [ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE]: Calendar,
    [ACTIVITY_TYPES.PROFILE_UPDATE]: User,
    [ACTIVITY_TYPES.CIRCLE_ACTIVITY]: Users,
    [ACTIVITY_TYPES.SOCIAL_ACTIVITY]: UserPlus,
    [ACTIVITY_TYPES.FRIEND_REQUEST]: UserPlus,
    [ACTIVITY_TYPES.FRIEND_REQUEST_ACCEPTED]: UserPlus,
    [ACTIVITY_TYPES.FRIEND_ADDED]: UserPlus,
    [ACTIVITY_TYPES.MESSAGE_RECEIVED]: MessageCircle,
    [ACTIVITY_TYPES.EVENT_CREATED]: Calendar,
    [ACTIVITY_TYPES.EVENT_UPDATED]: Calendar,
  };

  return iconMap[activityType] || Bell;
};

/**
 * Get the color scheme (Tailwind classes) for an activity type
 *
 * @param {string} activityType - The activity type from backend
 * @returns {Object} Object with bg, text, and ring color classes
 */
export const getActivityColors = (activityType) => {
  const colorMap = {
    [ACTIVITY_TYPES.AVAILABILITY_CHANGE]: {
      bg: 'bg-blue-100',
      text: 'text-blue-600',
      ring: 'ring-blue-200',
      dot: 'bg-blue-600'
    },
    [ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE]: {
      bg: 'bg-green-100',
      text: 'text-green-600',
      ring: 'ring-green-200',
      dot: 'bg-green-600'
    },
    [ACTIVITY_TYPES.PROFILE_UPDATE]: {
      bg: 'bg-purple-100',
      text: 'text-purple-600',
      ring: 'ring-purple-200',
      dot: 'bg-purple-600'
    },
    [ACTIVITY_TYPES.CIRCLE_ACTIVITY]: {
      bg: 'bg-orange-100',
      text: 'text-orange-600',
      ring: 'ring-orange-200',
      dot: 'bg-orange-600'
    },
    [ACTIVITY_TYPES.SOCIAL_ACTIVITY]: {
      bg: 'bg-pink-100',
      text: 'text-pink-600',
      ring: 'ring-pink-200',
      dot: 'bg-pink-600'
    },
    [ACTIVITY_TYPES.FRIEND_REQUEST]: {
      bg: 'bg-indigo-100',
      text: 'text-indigo-600',
      ring: 'ring-indigo-200',
      dot: 'bg-indigo-600'
    },
    [ACTIVITY_TYPES.FRIEND_REQUEST_ACCEPTED]: {
      bg: 'bg-green-100',
      text: 'text-green-600',
      ring: 'ring-green-200',
      dot: 'bg-green-600'
    },
    [ACTIVITY_TYPES.FRIEND_ADDED]: {
      bg: 'bg-green-100',
      text: 'text-green-600',
      ring: 'ring-green-200',
      dot: 'bg-green-600'
    },
    [ACTIVITY_TYPES.MESSAGE_RECEIVED]: {
      bg: 'bg-purple-100',
      text: 'text-purple-600',
      ring: 'ring-purple-200',
      dot: 'bg-purple-600'
    },
    [ACTIVITY_TYPES.EVENT_CREATED]: {
      bg: 'bg-blue-100',
      text: 'text-blue-600',
      ring: 'ring-blue-200',
      dot: 'bg-blue-600'
    },
  };

  return colorMap[activityType] || {
    bg: 'bg-gray-100',
    text: 'text-gray-600',
    ring: 'ring-gray-200',
    dot: 'bg-gray-600'
  };
};

/**
 * Format an activity into a user-friendly message
 *
 * @param {Object} activity - Activity object from backend
 * @param {Object} activity.type - Activity type
 * @param {string} activity.userName - Name of user who performed activity
 * @param {string} activity.data - JSON string or object with activity details
 * @returns {string} Formatted message for display
 */
export const formatActivityMessage = (activity) => {
  if (!activity) return 'New activity';

  try {
    // Parse data if it's a JSON string
    const data = typeof activity.data === 'string'
      ? JSON.parse(activity.data)
      : activity.data || {};

    // Use message from data if available
    if (data.message) {
      return data.message;
    }

    // Generate message based on activity type
    const userName = activity.userName || 'Someone';

    const messageTemplates = {
      [ACTIVITY_TYPES.AVAILABILITY_CHANGE]: `${userName} updated their availability`,
      [ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE]: `${userName} updated their calendar`,
      [ACTIVITY_TYPES.PROFILE_UPDATE]: `${userName} updated their profile`,
      [ACTIVITY_TYPES.CIRCLE_ACTIVITY]: data.circleName
        ? `New activity in ${data.circleName}`
        : `New activity in a circle`,
      [ACTIVITY_TYPES.SOCIAL_ACTIVITY]: `${userName} has new activity`,
      [ACTIVITY_TYPES.FRIEND_REQUEST]: `${userName} sent you a friend request`,
      [ACTIVITY_TYPES.FRIEND_REQUEST_ACCEPTED]: `${userName} accepted your friend request`,
      [ACTIVITY_TYPES.FRIEND_ADDED]: `${userName} added you as a friend`,
      [ACTIVITY_TYPES.MESSAGE_RECEIVED]: `New message from ${userName}`,
      [ACTIVITY_TYPES.EVENT_CREATED]: data.eventName
        ? `${userName} created event: ${data.eventName}`
        : `${userName} created an event`,
      [ACTIVITY_TYPES.EVENT_UPDATED]: data.eventName
        ? `${userName} updated event: ${data.eventName}`
        : `${userName} updated an event`,
    };

    return messageTemplates[activity.type] || data.description || 'New activity';

  } catch (error) {
    console.error('Error formatting activity message:', error);
    return activity.data || 'New activity';
  }
};

/**
 * Format activity timestamp for display
 * Uses relative time for recent activities, absolute for older
 *
 * @param {string} timestamp - ISO timestamp string
 * @returns {string} Formatted time string
 */
export const formatActivityTime = (timestamp) => {
  if (!timestamp) return '';

  try {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    // Recent activities
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;

    // Older activities
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
    });

  } catch (error) {
    console.error('Error formatting activity time:', error);
    return '';
  }
};

/**
 * Categorize activity for grouping/filtering
 *
 * @param {Object} activity - Activity object
 * @returns {string} Category name
 */
export const categorizeActivity = (activity) => {
  const categoryMap = {
    [ACTIVITY_TYPES.AVAILABILITY_CHANGE]: 'availability',
    [ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE]: 'availability',
    [ACTIVITY_TYPES.PROFILE_UPDATE]: 'profile',
    [ACTIVITY_TYPES.CIRCLE_ACTIVITY]: 'social',
    [ACTIVITY_TYPES.SOCIAL_ACTIVITY]: 'social',
    [ACTIVITY_TYPES.FRIEND_REQUEST]: 'friends',
    [ACTIVITY_TYPES.FRIEND_REQUEST_ACCEPTED]: 'friends',
    [ACTIVITY_TYPES.FRIEND_ADDED]: 'friends',
    [ACTIVITY_TYPES.MESSAGE_RECEIVED]: 'messages',
    [ACTIVITY_TYPES.EVENT_CREATED]: 'events',
    [ACTIVITY_TYPES.EVENT_UPDATED]: 'events',
  };

  return categoryMap[activity?.type] || 'other';
};

/**
 * Check if an activity is actionable (requires user response)
 *
 * @param {Object} activity - Activity object
 * @returns {boolean} Whether the activity is actionable
 */
export const isActionableActivity = (activity) => {
  const actionableTypes = [
    ACTIVITY_TYPES.FRIEND_REQUEST,
    ACTIVITY_TYPES.EVENT_CREATED, // Might need RSVP
  ];

  return actionableTypes.includes(activity?.type);
};

/**
 * Get priority level for activity (for sorting)
 *
 * @param {Object} activity - Activity object
 * @returns {number} Priority level (higher = more important)
 */
export const getActivityPriority = (activity) => {
  const priorityMap = {
    [ACTIVITY_TYPES.FRIEND_REQUEST]: 5,
    [ACTIVITY_TYPES.MESSAGE_RECEIVED]: 4,
    [ACTIVITY_TYPES.EVENT_CREATED]: 3,
    [ACTIVITY_TYPES.AVAILABILITY_CHANGE]: 3,
    [ACTIVITY_TYPES.FRIEND_REQUEST_ACCEPTED]: 2,
    [ACTIVITY_TYPES.FRIEND_ADDED]: 2,
    [ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE]: 2,
    [ACTIVITY_TYPES.PROFILE_UPDATE]: 1,
    [ACTIVITY_TYPES.CIRCLE_ACTIVITY]: 1,
    [ACTIVITY_TYPES.SOCIAL_ACTIVITY]: 1,
  };

  return activity?.priority || priorityMap[activity?.type] || 1;
};
