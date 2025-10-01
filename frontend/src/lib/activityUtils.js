/**
 * Activity Utilities - Activity categorization and processing helpers
 *
 * This utility module provides functions for activity categorization, priority
 * calculation, message generation, and various activity-related helper functions
 * for the friends activity feed processing system.
 *
 * Features:
 * - Activity categorization and type detection
 * - Priority scoring for feed ordering
 * - Activity message generation with templates
 * - Icon and color mapping for different activity types
 * - Activity filtering and sorting utilities
 * - Activity interaction and visibility helpers
 */

/**
 * Activity type definitions and metadata
 */
export const ACTIVITY_TYPES = {
  AVAILABILITY_CHANGE: 'availability_change',
  AVAILABILITY_SLOT_UPDATE: 'availability_slot_update',
  PROFILE_UPDATE: 'profile_update',
  CIRCLE_ACTIVITY: 'circle_activity',
  SOCIAL_ACTIVITY: 'social_activity'
};

/**
 * Activity categories for grouping and filtering
 */
export const ACTIVITY_CATEGORIES = {
  AVAILABILITY: 'availability',
  PROFILE: 'profile',
  SOCIAL: 'social',
  OTHER: 'other'
};

/**
 * Activity priority levels for feed ordering
 */
export const ACTIVITY_PRIORITIES = {
  HIGH: 3,
  MEDIUM: 2,
  LOW: 1
};

/**
 * Availability status mappings
 */
export const AVAILABILITY_STATUSES = {
  AVAILABLE: 'available',
  BUSY: 'busy',
  AWAY: 'away',
  OFFLINE: 'offline'
};

/**
 * Categorize activity by type
 *
 * @param {Object} activity - Activity object
 * @returns {string} Activity category
 */
export function categorizeActivity(activity) {
  if (!activity || !activity.type) {
    return ACTIVITY_CATEGORIES.OTHER;
  }

  const categoryMap = {
    [ACTIVITY_TYPES.AVAILABILITY_CHANGE]: ACTIVITY_CATEGORIES.AVAILABILITY,
    [ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE]: ACTIVITY_CATEGORIES.AVAILABILITY,
    [ACTIVITY_TYPES.PROFILE_UPDATE]: ACTIVITY_CATEGORIES.PROFILE,
    [ACTIVITY_TYPES.CIRCLE_ACTIVITY]: ACTIVITY_CATEGORIES.SOCIAL,
    [ACTIVITY_TYPES.SOCIAL_ACTIVITY]: ACTIVITY_CATEGORIES.SOCIAL
  };

  return categoryMap[activity.type] || ACTIVITY_CATEGORIES.OTHER;
}

/**
 * Calculate activity priority for feed ordering
 *
 * @param {Object} activity - Activity object
 * @param {Object} options - Priority calculation options
 * @param {boolean} options.boostRecent - Boost priority for recent activities
 * @param {number} options.recencyHours - Hours to consider as recent (default: 24)
 * @returns {number} Priority score
 */
export function calculateActivityPriority(activity, options = {}) {
  const {
    boostRecent = true,
    recencyHours = 24
  } = options;

  if (!activity || !activity.type) {
    return ACTIVITY_PRIORITIES.LOW;
  }

  // Base priority by type
  const typePriorities = {
    [ACTIVITY_TYPES.AVAILABILITY_CHANGE]: ACTIVITY_PRIORITIES.HIGH,
    [ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE]: ACTIVITY_PRIORITIES.MEDIUM,
    [ACTIVITY_TYPES.PROFILE_UPDATE]: ACTIVITY_PRIORITIES.MEDIUM,
    [ACTIVITY_TYPES.CIRCLE_ACTIVITY]: ACTIVITY_PRIORITIES.MEDIUM,
    [ACTIVITY_TYPES.SOCIAL_ACTIVITY]: ACTIVITY_PRIORITIES.LOW
  };

  let priority = typePriorities[activity.type] || ACTIVITY_PRIORITIES.LOW;

  // Boost priority for recent activities
  if (boostRecent && activity.timestamp) {
    const activityTime = new Date(activity.timestamp);
    const now = new Date();
    const hoursDiff = (now - activityTime) / (1000 * 60 * 60);

    if (hoursDiff <= recencyHours) {
      priority += 1;
    }
  }

  // Boost priority for activities involving close friends
  if (activity.data?.friendshipLevel === 'close') {
    priority += 0.5;
  }

  return Math.min(priority, 5); // Cap at max priority of 5
}

/**
 * Generate human-readable message for activity
 *
 * @param {Object} activity - Activity object
 * @param {Object} options - Message generation options
 * @param {boolean} options.includeTime - Include time in message
 * @param {boolean} options.short - Use short format
 * @returns {string} Human-readable activity message
 */
export function generateActivityMessage(activity, options = {}) {
  const {
    includeTime = false,
    short = false
  } = options;

  if (!activity || !activity.type) {
    return 'Unknown activity';
  }

  const friendName = activity.friend?.name || activity.data?.userName || 'Someone';
  let message = '';

  switch (activity.type) {
    case ACTIVITY_TYPES.AVAILABILITY_CHANGE:
      message = generateAvailabilityChangeMessage(activity, friendName, short);
      break;

    case ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE:
      message = generateAvailabilitySlotMessage(activity, friendName, short);
      break;

    case ACTIVITY_TYPES.PROFILE_UPDATE:
      message = generateProfileUpdateMessage(activity, friendName, short);
      break;

    case ACTIVITY_TYPES.CIRCLE_ACTIVITY:
      message = generateCircleActivityMessage(activity, friendName, short);
      break;

    case ACTIVITY_TYPES.SOCIAL_ACTIVITY:
      message = generateSocialActivityMessage(activity, friendName, short);
      break;

    default:
      message = `${friendName} had some activity`;
  }

  // Add time if requested
  if (includeTime && activity.displayTime) {
    message += ` • ${activity.displayTime}`;
  }

  return message;
}

/**
 * Generate availability change message
 *
 * @private
 * @param {Object} activity - Activity object
 * @param {string} friendName - Friend's name
 * @param {boolean} short - Use short format
 * @returns {string} Availability change message
 */
function generateAvailabilityChangeMessage(activity, friendName, short) {
  const { fromStatus, toStatus } = activity.data || {};

  if (!fromStatus || !toStatus) {
    return short ? `${friendName} updated status` : `${friendName} updated their availability status`;
  }

  const statusEmojis = {
    [AVAILABILITY_STATUSES.AVAILABLE]: '🟢',
    [AVAILABILITY_STATUSES.BUSY]: '🔴',
    [AVAILABILITY_STATUSES.AWAY]: '🟡',
    [AVAILABILITY_STATUSES.OFFLINE]: '⚫'
  };

  const statusNames = {
    [AVAILABILITY_STATUSES.AVAILABLE]: 'Available',
    [AVAILABILITY_STATUSES.BUSY]: 'Busy',
    [AVAILABILITY_STATUSES.AWAY]: 'Away',
    [AVAILABILITY_STATUSES.OFFLINE]: 'Offline'
  };

  if (short) {
    return `${friendName} is now ${statusNames[toStatus]?.toLowerCase()}`;
  }

  const fromEmoji = statusEmojis[fromStatus] || '';
  const toEmoji = statusEmojis[toStatus] || '';

  return `${friendName} changed from ${fromEmoji} ${statusNames[fromStatus]} to ${toEmoji} ${statusNames[toStatus]}`;
}

/**
 * Generate availability slot update message
 *
 * @private
 * @param {Object} activity - Activity object
 * @param {string} friendName - Friend's name
 * @param {boolean} short - Use short format
 * @returns {string} Availability slot message
 */
function generateAvailabilitySlotMessage(activity, friendName, short) {
  const { action, slotCount, dateRange } = activity.data || {};

  if (short) {
    return `${friendName} updated availability`;
  }

  switch (action) {
    case 'added':
      if (slotCount > 1) {
        return `${friendName} added ${slotCount} new available time slots${dateRange ? ` for ${dateRange}` : ''}`;
      }
      return `${friendName} added a new available time slot${dateRange ? ` for ${dateRange}` : ''}`;

    case 'removed':
      if (slotCount > 1) {
        return `${friendName} removed ${slotCount} available time slots`;
      }
      return `${friendName} removed an available time slot`;

    case 'modified':
      return `${friendName} updated their available times${dateRange ? ` for ${dateRange}` : ''}`;

    default:
      return `${friendName} updated their availability schedule`;
  }
}

/**
 * Generate profile update message
 *
 * @private
 * @param {Object} activity - Activity object
 * @param {string} friendName - Friend's name
 * @param {boolean} short - Use short format
 * @returns {string} Profile update message
 */
function generateProfileUpdateMessage(activity, friendName, short) {
  const { updateType, fieldName } = activity.data || {};

  if (short) {
    return `${friendName} updated profile`;
  }

  switch (updateType) {
    case 'avatar':
      return `${friendName} updated their profile picture`;

    case 'status':
      return `${friendName} updated their status message`;

    case 'bio':
      return `${friendName} updated their bio`;

    case 'preferences':
      return `${friendName} updated their preferences`;

    default:
      if (fieldName) {
        return `${friendName} updated their ${fieldName}`;
      }
      return `${friendName} updated their profile`;
  }
}

/**
 * Generate circle activity message
 *
 * @private
 * @param {Object} activity - Activity object
 * @param {string} friendName - Friend's name
 * @param {boolean} short - Use short format
 * @returns {string} Circle activity message
 */
function generateCircleActivityMessage(activity, friendName, short) {
  const { action, circleName, eventName, memberCount } = activity.data || {};

  if (short) {
    return `${friendName} active in circles`;
  }

  switch (action) {
    case 'joined':
      return `${friendName} joined the ${circleName || 'circle'}${memberCount ? ` (${memberCount} members)` : ''}`;

    case 'created':
      return `${friendName} created a new circle: ${circleName || 'Untitled Circle'}`;

    case 'created_event':
      return `${friendName} created an event${eventName ? `: ${eventName}` : ''}${circleName ? ` in ${circleName}` : ''}`;

    case 'left':
      return `${friendName} left the ${circleName || 'circle'}`;

    default:
      return `${friendName} was active in ${circleName || 'a circle'}`;
  }
}

/**
 * Generate social activity message
 *
 * @private
 * @param {Object} activity - Activity object
 * @param {string} friendName - Friend's name
 * @param {boolean} short - Use short format
 * @returns {string} Social activity message
 */
function generateSocialActivityMessage(activity, friendName, short) {
  const { action, targetName, mutualFriends } = activity.data || {};

  if (short) {
    return `${friendName} made new connections`;
  }

  switch (action) {
    case 'new_friend':
      let message = `${friendName} is now friends with ${targetName || 'someone'}`;
      if (mutualFriends > 0) {
        message += ` (${mutualFriends} mutual friends)`;
      }
      return message;

    case 'friend_recommendation':
      return `${friendName} and ${targetName || 'someone'} were recommended as friends`;

    case 'mutual_connection':
      return `${friendName} connected with ${targetName || 'someone'} through mutual friends`;

    default:
      return `${friendName} made a new connection`;
  }
}

/**
 * Get icon name for activity type (Lucide React icons)
 *
 * @param {string} activityType - Type of activity
 * @param {Object} activityData - Additional activity data for context
 * @returns {string} Icon component name
 */
export function getActivityIcon(activityType, activityData = {}) {
  const iconMap = {
    [ACTIVITY_TYPES.AVAILABILITY_CHANGE]: 'Clock',
    [ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE]: 'Calendar',
    [ACTIVITY_TYPES.PROFILE_UPDATE]: 'User',
    [ACTIVITY_TYPES.CIRCLE_ACTIVITY]: 'Users',
    [ACTIVITY_TYPES.SOCIAL_ACTIVITY]: 'UserPlus'
  };

  // Special cases based on activity data
  if (activityType === ACTIVITY_TYPES.PROFILE_UPDATE) {
    const { updateType } = activityData;
    if (updateType === 'avatar') return 'Camera';
    if (updateType === 'status') return 'MessageCircle';
  }

  if (activityType === ACTIVITY_TYPES.CIRCLE_ACTIVITY) {
    const { action } = activityData;
    if (action === 'created_event') return 'CalendarPlus';
    if (action === 'created') return 'PlusCircle';
  }

  return iconMap[activityType] || 'Activity';
}

/**
 * Get color scheme for activity type
 *
 * @param {string} activityType - Type of activity
 * @param {Object} activityData - Additional activity data for context
 * @returns {Object} Color scheme object with Tailwind classes
 */
export function getActivityColors(activityType, activityData = {}) {
  const colorSchemes = {
    [ACTIVITY_TYPES.AVAILABILITY_CHANGE]: {
      bg: 'bg-blue-50',
      border: 'border-blue-200',
      icon: 'text-blue-600',
      text: 'text-blue-900'
    },
    [ACTIVITY_TYPES.AVAILABILITY_SLOT_UPDATE]: {
      bg: 'bg-green-50',
      border: 'border-green-200',
      icon: 'text-green-600',
      text: 'text-green-900'
    },
    [ACTIVITY_TYPES.PROFILE_UPDATE]: {
      bg: 'bg-purple-50',
      border: 'border-purple-200',
      icon: 'text-purple-600',
      text: 'text-purple-900'
    },
    [ACTIVITY_TYPES.CIRCLE_ACTIVITY]: {
      bg: 'bg-orange-50',
      border: 'border-orange-200',
      icon: 'text-orange-600',
      text: 'text-orange-900'
    },
    [ACTIVITY_TYPES.SOCIAL_ACTIVITY]: {
      bg: 'bg-pink-50',
      border: 'border-pink-200',
      icon: 'text-pink-600',
      text: 'text-pink-900'
    }
  };

  // Special case for availability changes based on status
  if (activityType === ACTIVITY_TYPES.AVAILABILITY_CHANGE) {
    const { toStatus } = activityData;
    if (toStatus === AVAILABILITY_STATUSES.AVAILABLE) {
      return {
        bg: 'bg-green-50',
        border: 'border-green-200',
        icon: 'text-green-600',
        text: 'text-green-900'
      };
    } else if (toStatus === AVAILABILITY_STATUSES.BUSY) {
      return {
        bg: 'bg-red-50',
        border: 'border-red-200',
        icon: 'text-red-600',
        text: 'text-red-900'
      };
    } else if (toStatus === AVAILABILITY_STATUSES.AWAY) {
      return {
        bg: 'bg-yellow-50',
        border: 'border-yellow-200',
        icon: 'text-yellow-600',
        text: 'text-yellow-900'
      };
    }
  }

  return colorSchemes[activityType] || {
    bg: 'bg-gray-50',
    border: 'border-gray-200',
    icon: 'text-gray-600',
    text: 'text-gray-900'
  };
}

/**
 * Filter activities by various criteria
 *
 * @param {Array} activities - Array of activities to filter
 * @param {Object} filters - Filter criteria
 * @param {Array} filters.types - Activity types to include
 * @param {Array} filters.categories - Activity categories to include
 * @param {Array} filters.friendIds - Friend IDs to include
 * @param {string} filters.dateRange - Date range filter
 * @param {boolean} filters.unreadOnly - Show only unread activities
 * @returns {Array} Filtered activities
 */
export function filterActivities(activities, filters = {}) {
  if (!Array.isArray(activities)) {
    return [];
  }

  const {
    types = [],
    categories = [],
    friendIds = [],
    dateRange = 'all',
    unreadOnly = false
  } = filters;

  return activities.filter(activity => {
    // Filter by activity type
    if (types.length > 0 && !types.includes(activity.type)) {
      return false;
    }

    // Filter by category
    if (categories.length > 0) {
      const activityCategory = categorizeActivity(activity);
      if (!categories.includes(activityCategory)) {
        return false;
      }
    }

    // Filter by friend
    if (friendIds.length > 0 && !friendIds.includes(activity.userId)) {
      return false;
    }

    // Filter by read status
    if (unreadOnly && activity.isRead) {
      return false;
    }

    // Filter by date range
    if (dateRange !== 'all') {
      const activityTime = new Date(activity.timestamp);
      const now = new Date();
      const diffMs = now - activityTime;
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

      switch (dateRange) {
        case 'today':
          if (diffDays > 0) return false;
          break;
        case 'week':
          if (diffDays > 7) return false;
          break;
        case 'month':
          if (diffDays > 30) return false;
          break;
      }
    }

    return true;
  });
}

/**
 * Sort activities by various criteria
 *
 * @param {Array} activities - Array of activities to sort
 * @param {string} sortBy - Sort criteria: 'recent', 'priority', 'friend', 'type'
 * @param {string} order - Sort order: 'asc' or 'desc'
 * @returns {Array} Sorted activities
 */
export function sortActivities(activities, sortBy = 'recent', order = 'desc') {
  if (!Array.isArray(activities)) {
    return [];
  }

  const sortedActivities = [...activities];

  sortedActivities.sort((a, b) => {
    let comparison = 0;

    switch (sortBy) {
      case 'recent':
        comparison = new Date(a.timestamp) - new Date(b.timestamp);
        break;

      case 'priority':
        const aPriority = a.priority || calculateActivityPriority(a);
        const bPriority = b.priority || calculateActivityPriority(b);
        comparison = aPriority - bPriority;
        break;

      case 'friend':
        const aName = a.friend?.name || a.data?.userName || '';
        const bName = b.friend?.name || b.data?.userName || '';
        comparison = aName.localeCompare(bName);
        break;

      case 'type':
        comparison = a.type.localeCompare(b.type);
        break;

      default:
        comparison = 0;
    }

    return order === 'desc' ? -comparison : comparison;
  });

  return sortedActivities;
}

/**
 * Get activity interaction options based on type and user permissions
 *
 * @param {Object} activity - Activity object
 * @param {string|number} currentUserId - Current user ID
 * @returns {Array} Available interaction options
 */
export function getActivityInteractions(activity, currentUserId) {
  const interactions = [];

  // All activities can be marked as read/unread
  interactions.push({
    type: activity.isRead ? 'mark_unread' : 'mark_read',
    label: activity.isRead ? 'Mark as unread' : 'Mark as read',
    icon: activity.isRead ? 'EyeOff' : 'Eye'
  });

  // Availability activities might allow status queries
  if (activity.type === ACTIVITY_TYPES.AVAILABILITY_CHANGE) {
    interactions.push({
      type: 'check_availability',
      label: 'Check full availability',
      icon: 'Calendar'
    });
  }

  // Profile updates might allow viewing profile
  if (activity.type === ACTIVITY_TYPES.PROFILE_UPDATE) {
    interactions.push({
      type: 'view_profile',
      label: 'View profile',
      icon: 'User'
    });
  }

  // Circle activities might allow joining
  if (activity.type === ACTIVITY_TYPES.CIRCLE_ACTIVITY && activity.data?.action === 'created') {
    interactions.push({
      type: 'view_circle',
      label: 'View circle',
      icon: 'Users'
    });
  }

  // Social activities might allow viewing mutual connections
  if (activity.type === ACTIVITY_TYPES.SOCIAL_ACTIVITY) {
    interactions.push({
      type: 'view_mutual_friends',
      label: 'View mutual friends',
      icon: 'UserPlus'
    });
  }

  return interactions;
}

/**
 * Check if activity should be highlighted (e.g., important or urgent)
 *
 * @param {Object} activity - Activity object
 * @param {Object} options - Highlighting options
 * @returns {boolean} Whether activity should be highlighted
 */
export function shouldHighlightActivity(activity, options = {}) {
  if (!activity) return false;

  const {
    highlightUnread = true,
    highlightRecent = true,
    recentHours = 1
  } = options;

  // Highlight unread activities
  if (highlightUnread && !activity.isRead) {
    return true;
  }

  // Highlight very recent activities
  if (highlightRecent && activity.timestamp) {
    const activityTime = new Date(activity.timestamp);
    const now = new Date();
    const hoursDiff = (now - activityTime) / (1000 * 60 * 60);

    if (hoursDiff <= recentHours) {
      return true;
    }
  }

  // Highlight high priority activities
  const priority = activity.priority || calculateActivityPriority(activity);
  if (priority >= ACTIVITY_PRIORITIES.HIGH) {
    return true;
  }

  return false;
}