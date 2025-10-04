import { formatDistanceToNow, format, isToday, isYesterday, isThisWeek, isThisMonth, isThisYear } from 'date-fns';

/**
 * Time Utilities - Comprehensive time formatting and manipulation
 *
 * This utility module provides various time-related functions for the activity feed,
 * including relative time formatting, timestamp handling, and time grouping.
 * Enhanced with date-fns for robust time operations.
 *
 * Features:
 * - Relative time formatting (e.g., "2 hours ago", "yesterday")
 * - Smart timestamp handling with fallbacks
 * - Time grouping utilities for activity feed organization
 * - Internationalization ready structure
 * - Performance optimized with caching
 */

// Cache for formatted times to improve performance
const formatCache = new Map();
const CACHE_TTL = 60000; // 1 minute cache for relative times

/**
 * Format timestamp to relative time (e.g., "2 hours ago", "yesterday")
 *
 * @param {string|Date|number} timestamp - Timestamp to format
 * @param {Object} options - Formatting options
 * @param {boolean} options.short - Use short format (e.g., "2h" instead of "2 hours ago")
 * @param {string} options.locale - Locale for formatting (default: 'en-US')
 * @param {Date} options.now - Current time reference (for testing)
 * @returns {string} Formatted relative time
 */
export function formatRelativeTime(timestamp, options = {}) {
  const {
    short = false,
    locale = 'en-US',
    now = new Date()
  } = options;

  // Create cache key
  const cacheKey = `${timestamp}_${short}_${locale}_${Math.floor(now.getTime() / CACHE_TTL)}`;

  // Check cache first
  if (formatCache.has(cacheKey)) {
    return formatCache.get(cacheKey);
  }

  try {
    const date = new Date(timestamp);

    // Handle future dates
    if (date > now) {
      const result = short ? 'now' : 'just now';
      formatCache.set(cacheKey, result);
      return result;
    }

    // Use date-fns for more accurate relative time formatting
    let result;

    if (short) {
      // For short format, use a simplified version
      const distance = formatDistanceToNow(date, { addSuffix: false });

      // Convert to short format
      result = distance
        .replace(/about /g, '')
        .replace(/less than a /g, '<1')
        .replace(/minute/g, 'm')
        .replace(/hour/g, 'h')
        .replace(/day/g, 'd')
        .replace(/week/g, 'w')
        .replace(/month/g, 'mo')
        .replace(/year/g, 'y')
        .replace(/s\b/g, ''); // Remove plural 's'
    } else {
      // For full format, use date-fns with smart logic
      if (isToday(date)) {
        const diffMs = now.getTime() - date.getTime();
        const diffMinutes = Math.floor(diffMs / (1000 * 60));

        if (diffMinutes < 1) {
          result = 'just now';
        } else if (diffMinutes < 60) {
          result = formatDistanceToNow(date, { addSuffix: true });
        } else {
          result = formatDistanceToNow(date, { addSuffix: true });
        }
      } else if (isYesterday(date)) {
        result = 'yesterday';
      } else {
        result = formatDistanceToNow(date, { addSuffix: true });
      }
    }

    // Cache the result
    formatCache.set(cacheKey, result);

    // Clean up old cache entries periodically
    if (formatCache.size > 1000) {
      const oldKeys = Array.from(formatCache.keys()).slice(0, 500);
      oldKeys.forEach(key => formatCache.delete(key));
    }

    return result;

  } catch (error) {
    console.warn('Failed to format relative time:', error);
    return short ? 'now' : 'just now';
  }
}

/**
 * Format timestamp to absolute time with smart formatting
 *
 * @param {string|Date|number} timestamp - Timestamp to format
 * @param {Object} options - Formatting options
 * @param {boolean} options.includeTime - Include time in the format
 * @param {boolean} options.includeYear - Include year in the format
 * @param {string} options.locale - Locale for formatting
 * @returns {string} Formatted absolute time
 */
export function formatAbsoluteTime(timestamp, options = {}) {
  const {
    includeTime = true,
    includeYear = null,
    locale = 'en-US'
  } = options;

  try {
    const date = new Date(timestamp);
    const currentYear = new Date().getFullYear();
    const dateYear = date.getFullYear();

    // Auto-determine if year should be included
    const shouldIncludeYear = includeYear !== null ? includeYear : (dateYear !== currentYear);

    let formatString;

    if (includeTime) {
      if (shouldIncludeYear) {
        formatString = 'MMM d, yyyy h:mm a';
      } else {
        formatString = 'MMM d h:mm a';
      }
    } else {
      if (shouldIncludeYear) {
        formatString = 'MMM d, yyyy';
      } else {
        formatString = 'MMM d';
      }
    }

    return format(date, formatString);

  } catch (error) {
    console.warn('Failed to format absolute time:', error);
    return 'Invalid date';
  }
}

/**
 * Get time period for grouping activities (e.g., "Today", "Yesterday", "This week")
 *
 * @param {string|Date|number} timestamp - Timestamp to categorize
 * @param {Object} options - Grouping options
 * @param {Date} options.now - Current time reference
 * @param {string} options.locale - Locale for formatting
 * @returns {string} Time period label
 */
export function getTimePeriod(timestamp, options = {}) {
  const {
    now = new Date(),
    locale = 'en-US'
  } = options;

  try {
    const date = new Date(timestamp);

    if (isToday(date)) {
      return 'Today';
    } else if (isYesterday(date)) {
      return 'Yesterday';
    } else if (isThisWeek(date)) {
      return format(date, 'EEEE'); // Day name (e.g., "Monday")
    } else if (isThisMonth(date)) {
      const weeks = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24 * 7));
      return weeks === 1 ? 'Last week' : `${weeks} weeks ago`;
    } else if (isThisYear(date)) {
      return format(date, 'MMMM'); // Month name (e.g., "January")
    } else {
      return format(date, 'yyyy'); // Year (e.g., "2023")
    }

  } catch (error) {
    console.warn('Failed to get time period:', error);
    return 'Unknown';
  }
}

/**
 * Group activities by time periods for organized display
 *
 * @param {Array} activities - Array of activities with timestamp property
 * @param {Object} options - Grouping options
 * @param {string} options.timestampKey - Key name for timestamp in activity objects
 * @param {Date} options.now - Current time reference
 * @returns {Object} Activities grouped by time periods
 */
export function groupActivitiesByTime(activities, options = {}) {
  const {
    timestampKey = 'timestamp',
    now = new Date()
  } = options;

  const groups = {};

  activities.forEach(activity => {
    const timestamp = activity[timestampKey];
    if (!timestamp) return;

    const period = getTimePeriod(timestamp, { now });

    if (!groups[period]) {
      groups[period] = [];
    }

    groups[period].push(activity);
  });

  // Sort groups by recency
  const sortedGroups = {};
  const sortedPeriods = Object.keys(groups).sort((a, b) => {
    const periodOrder = ['Today', 'Yesterday'];
    const aIndex = periodOrder.indexOf(a);
    const bIndex = periodOrder.indexOf(b);

    if (aIndex !== -1 && bIndex !== -1) {
      return aIndex - bIndex;
    } else if (aIndex !== -1) {
      return -1;
    } else if (bIndex !== -1) {
      return 1;
    } else {
      return a.localeCompare(b);
    }
  });

  sortedPeriods.forEach(period => {
    sortedGroups[period] = groups[period];
  });

  return sortedGroups;
}

/**
 * Check if a timestamp is within a specific time range
 *
 * @param {string|Date|number} timestamp - Timestamp to check
 * @param {string} range - Time range: 'today', 'yesterday', 'week', 'month', 'year'
 * @param {Date} now - Current time reference
 * @returns {boolean} Whether timestamp is within range
 */
export function isWithinTimeRange(timestamp, range, now = new Date()) {
  try {
    const date = new Date(timestamp);
    const nowStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    switch (range.toLowerCase()) {
      case 'today':
        return diffDays === 0 && date.getDate() === now.getDate();

      case 'yesterday':
        return diffDays === 1;

      case 'week':
        return diffDays < 7;

      case 'month':
        return diffDays < 30;

      case 'year':
        return diffDays < 365;

      default:
        return true;
    }

  } catch (error) {
    console.warn('Failed to check time range:', error);
    return false;
  }
}

/**
 * Format duration between two timestamps
 *
 * @param {string|Date|number} startTime - Start timestamp
 * @param {string|Date|number} endTime - End timestamp
 * @param {Object} options - Formatting options
 * @param {boolean} options.short - Use short format
 * @param {boolean} options.precise - Include seconds/minutes for short durations
 * @returns {string} Formatted duration
 */
export function formatDuration(startTime, endTime, options = {}) {
  const {
    short = false,
    precise = false
  } = options;

  try {
    const start = new Date(startTime);
    const end = new Date(endTime);
    const diffMs = Math.abs(end.getTime() - start.getTime());

    const diffSeconds = Math.floor(diffMs / 1000);
    const diffMinutes = Math.floor(diffSeconds / 60);
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays > 0) {
      const unit = short ? 'd' : (diffDays === 1 ? ' day' : ' days');
      return `${diffDays}${unit}`;
    } else if (diffHours > 0) {
      const unit = short ? 'h' : (diffHours === 1 ? ' hour' : ' hours');
      let result = `${diffHours}${unit}`;

      if (precise && diffMinutes % 60 > 0) {
        const remainingMinutes = diffMinutes % 60;
        const minuteUnit = short ? 'm' : (remainingMinutes === 1 ? ' minute' : ' minutes');
        result += short ? ` ${remainingMinutes}${minuteUnit}` : ` ${remainingMinutes}${minuteUnit}`;
      }

      return result;
    } else if (diffMinutes > 0) {
      const unit = short ? 'm' : (diffMinutes === 1 ? ' minute' : ' minutes');
      return `${diffMinutes}${unit}`;
    } else {
      const unit = short ? 's' : (diffSeconds === 1 ? ' second' : ' seconds');
      return `${diffSeconds}${unit}`;
    }

  } catch (error) {
    console.warn('Failed to format duration:', error);
    return short ? '0s' : '0 seconds';
  }
}

/**
 * Get timezone information for the user
 *
 * @returns {Object} Timezone information
 */
export function getTimezoneInfo() {
  try {
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const now = new Date();
    const offset = -now.getTimezoneOffset() / 60;

    return {
      timeZone,
      offset,
      offsetString: `GMT${offset >= 0 ? '+' : ''}${offset}`,
      name: timeZone.split('/').pop().replace(/_/g, ' ')
    };

  } catch (error) {
    console.warn('Failed to get timezone info:', error);
    return {
      timeZone: 'UTC',
      offset: 0,
      offsetString: 'GMT+0',
      name: 'UTC'
    };
  }
}

/**
 * Clear the formatting cache (useful for testing or memory management)
 */
export function clearTimeCache() {
  formatCache.clear();
}

/**
 * Get cache statistics for debugging
 *
 * @returns {Object} Cache statistics
 */
export function getTimeCacheStats() {
  return {
    size: formatCache.size,
    keys: formatCache.size > 0 ? Array.from(formatCache.keys()).slice(0, 5) : []
  };
}