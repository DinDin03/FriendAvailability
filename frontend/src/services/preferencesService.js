/**
 * User Preferences Service - Manages user preferences with localStorage persistence
 *
 * This service provides centralized management of user preferences including
 * theme settings, notification preferences, privacy settings, and UI customization.
 * All preferences are automatically persisted to localStorage and synchronized
 * across browser sessions.
 *
 * Features:
 * - Automatic localStorage persistence
 * - Type-safe preference validation
 * - Default value management
 * - Preference change listeners
 * - Bulk preference updates
 * - Export/import functionality
 *
 * @class PreferencesService
 */
class PreferencesService {
  constructor() {
    this.storageKey = 'linkups_user_preferences';
    this.defaultPreferences = {
      // Theme and UI preferences
      theme: 'light', // 'light', 'dark', 'auto'
      language: 'en',
      dateFormat: 'MM/DD/YYYY',
      timeFormat: '12h', // '12h', '24h'

      // Notification preferences
      notifications: {
        email: true,
        push: true,
        friendRequests: true,
        messageAlerts: true,
        eventReminders: true,
        weeklyDigest: false
      },

      // Privacy preferences
      privacy: {
        profileVisibility: 'friends', // 'public', 'friends', 'private'
        showOnlineStatus: true,
        allowFriendRequests: true,
        showLastSeen: true
      },

      // Dashboard preferences
      dashboard: {
        showTestPanel: false,
        autoRefresh: true,
        refreshInterval: 300000, // 5 minutes in milliseconds
        compactView: false
      },

      // Calendar preferences
      calendar: {
        defaultView: 'week', // 'day', 'week', 'month'
        startOfWeek: 0, // 0 = Sunday, 1 = Monday
        businessHours: {
          start: '09:00',
          end: '17:00'
        },
        showWeekends: true
      }
    };

    this.preferences = this.loadPreferences();
    this.changeListeners = new Set();
  }

  /**
   * Load preferences from localStorage with fallback to defaults
   *
   * @returns {Object} Loaded preferences object
   */
  loadPreferences() {
    try {
      const stored = localStorage.getItem(this.storageKey);
      if (stored) {
        const parsed = JSON.parse(stored);
        // Merge with defaults to ensure all keys exist
        return this.mergeWithDefaults(parsed);
      }
    } catch (error) {
      console.warn('Failed to load preferences from localStorage:', error);
    }

    // Return defaults if loading fails
    return { ...this.defaultPreferences };
  }

  /**
   * Save preferences to localStorage
   *
   * @param {Object} preferences - Preferences object to save
   */
  savePreferences(preferences = this.preferences) {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(preferences));
      this.notifyListeners(preferences);
    } catch (error) {
      console.error('Failed to save preferences to localStorage:', error);
    }
  }

  /**
   * Merge stored preferences with defaults to ensure completeness
   *
   * @param {Object} stored - Stored preferences
   * @returns {Object} Merged preferences
   */
  mergeWithDefaults(stored) {
    const merged = { ...this.defaultPreferences };

    // Deep merge for nested objects
    Object.keys(stored).forEach(key => {
      if (typeof stored[key] === 'object' && stored[key] !== null && !Array.isArray(stored[key])) {
        merged[key] = { ...merged[key], ...stored[key] };
      } else {
        merged[key] = stored[key];
      }
    });

    return merged;
  }

  /**
   * Get all preferences
   *
   * @returns {Object} Current preferences object
   */
  getAllPreferences() {
    return { ...this.preferences };
  }

  /**
   * Get a specific preference value
   *
   * @param {string} key - Preference key (supports dot notation for nested keys)
   * @returns {*} Preference value or undefined if not found
   */
  getPreference(key) {
    const keys = key.split('.');
    let value = this.preferences;

    for (const k of keys) {
      if (value && typeof value === 'object' && k in value) {
        value = value[k];
      } else {
        return undefined;
      }
    }

    return value;
  }

  /**
   * Set a specific preference value
   *
   * @param {string} key - Preference key (supports dot notation for nested keys)
   * @param {*} value - New preference value
   * @returns {boolean} Success status
   */
  setPreference(key, value) {
    try {
      const keys = key.split('.');
      const lastKey = keys.pop();
      let target = this.preferences;

      // Navigate to the parent object
      for (const k of keys) {
        if (!target[k] || typeof target[k] !== 'object') {
          target[k] = {};
        }
        target = target[k];
      }

      // Set the value
      target[lastKey] = value;

      // Save to localStorage
      this.savePreferences();

      return true;
    } catch (error) {
      console.error('Failed to set preference:', error);
      return false;
    }
  }

  /**
   * Update multiple preferences at once
   *
   * @param {Object} updates - Object with preference updates
   * @returns {boolean} Success status
   */
  updatePreferences(updates) {
    try {
      Object.keys(updates).forEach(key => {
        this.setPreference(key, updates[key]);
      });
      return true;
    } catch (error) {
      console.error('Failed to update preferences:', error);
      return false;
    }
  }

  /**
   * Reset preferences to defaults
   *
   * @param {string[]} keys - Specific keys to reset (optional, resets all if not provided)
   * @returns {boolean} Success status
   */
  resetPreferences(keys = null) {
    try {
      if (keys && Array.isArray(keys)) {
        // Reset specific keys
        keys.forEach(key => {
          const defaultValue = this.getDefaultValue(key);
          if (defaultValue !== undefined) {
            this.setPreference(key, defaultValue);
          }
        });
      } else {
        // Reset all preferences
        this.preferences = { ...this.defaultPreferences };
        this.savePreferences();
      }
      return true;
    } catch (error) {
      console.error('Failed to reset preferences:', error);
      return false;
    }
  }

  /**
   * Get default value for a preference key
   *
   * @param {string} key - Preference key
   * @returns {*} Default value or undefined
   */
  getDefaultValue(key) {
    const keys = key.split('.');
    let value = this.defaultPreferences;

    for (const k of keys) {
      if (value && typeof value === 'object' && k in value) {
        value = value[k];
      } else {
        return undefined;
      }
    }

    return value;
  }

  /**
   * Add a change listener
   *
   * @param {Function} listener - Function to call when preferences change
   * @returns {Function} Unsubscribe function
   */
  addChangeListener(listener) {
    this.changeListeners.add(listener);

    // Return unsubscribe function
    return () => {
      this.changeListeners.delete(listener);
    };
  }

  /**
   * Notify all change listeners
   *
   * @param {Object} preferences - Updated preferences
   */
  notifyListeners(preferences) {
    this.changeListeners.forEach(listener => {
      try {
        listener(preferences);
      } catch (error) {
        console.error('Preference change listener error:', error);
      }
    });
  }

  /**
   * Export preferences as JSON string
   *
   * @returns {string} JSON string of preferences
   */
  exportPreferences() {
    return JSON.stringify(this.preferences, null, 2);
  }

  /**
   * Import preferences from JSON string
   *
   * @param {string} jsonString - JSON string of preferences
   * @returns {boolean} Success status
   */
  importPreferences(jsonString) {
    try {
      const imported = JSON.parse(jsonString);
      this.preferences = this.mergeWithDefaults(imported);
      this.savePreferences();
      return true;
    } catch (error) {
      console.error('Failed to import preferences:', error);
      return false;
    }
  }

  /**
   * Check if a preference exists
   *
   * @param {string} key - Preference key
   * @returns {boolean} True if preference exists
   */
  hasPreference(key) {
    return this.getPreference(key) !== undefined;
  }

  /**
   * Get preferences schema for validation
   *
   * @returns {Object} Preferences schema
   */
  getSchema() {
    return {
      theme: ['light', 'dark', 'auto'],
      language: ['en', 'es', 'fr', 'de'],
      dateFormat: ['MM/DD/YYYY', 'DD/MM/YYYY', 'YYYY-MM-DD'],
      timeFormat: ['12h', '24h'],
      'privacy.profileVisibility': ['public', 'friends', 'private'],
      'calendar.defaultView': ['day', 'week', 'month'],
      'calendar.startOfWeek': [0, 1, 2, 3, 4, 5, 6]
    };
  }

  /**
   * Validate a preference value against schema
   *
   * @param {string} key - Preference key
   * @param {*} value - Value to validate
   * @returns {boolean} True if valid
   */
  validatePreference(key, value) {
    const schema = this.getSchema();

    if (key in schema) {
      return schema[key].includes(value);
    }

    // For preferences not in schema, basic type checking
    const currentValue = this.getPreference(key);
    if (currentValue !== undefined) {
      return typeof value === typeof currentValue;
    }

    return true; // Allow new preferences
  }

  /**
   * Get preference statistics for debugging
   *
   * @returns {Object} Preference statistics
   */
  getStats() {
    const totalKeys = Object.keys(this.preferences).length;
    const nestedKeys = Object.keys(this.preferences).filter(key =>
      typeof this.preferences[key] === 'object' && this.preferences[key] !== null
    ).length;

    return {
      totalKeys,
      nestedKeys,
      storageSize: JSON.stringify(this.preferences).length,
      lastModified: new Date().toISOString(),
      listenersCount: this.changeListeners.size
    };
  }
}

// Create and export preferences service instance
export const preferencesService = new PreferencesService();