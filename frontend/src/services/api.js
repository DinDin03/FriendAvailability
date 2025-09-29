// API Configuration and Base Service
const API_BASE_URL = '/api';

// API Response handler
const handleResponse = async (response) => {
  if (!response.ok) {
    const error = await response.text();
    throw new Error(`HTTP ${response.status}: ${error}`);
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return await response.json();
  }

  return await response.text();
};

// Base API class
class ApiService {
  constructor(baseURL = API_BASE_URL) {
    this.baseURL = baseURL;
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;

    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      credentials: 'include',
      ...options,
    };

    // Add body if it's a POST/PUT/PATCH request
    if (options.body && typeof options.body === 'object') {
      config.body = JSON.stringify(options.body);
    }

    try {
      console.log(`Making ${config.method || 'GET'} request to:`, url);
      const response = await fetch(url, config);
      return await handleResponse(response);
    } catch (error) {
      console.error('API Request failed:', error);
      throw error;
    }
  }

  // HTTP Methods
  async get(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'GET' });
  }

  async post(endpoint, body, options = {}) {
    return this.request(endpoint, { ...options, method: 'POST', body });
  }

  async put(endpoint, body, options = {}) {
    return this.request(endpoint, { ...options, method: 'PUT', body });
  }

  async delete(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'DELETE' });
  }
}

// Create and export API instance
export const api = new ApiService();

// API Endpoints Configuration (matching your backend)
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    REGISTER: '/auth/register',
    CURRENT_USER: '/auth/current-user',
    REFRESH_TOKEN: '/auth/refresh',
    FORGOT_PASSWORD: '/auth/forgot-password',
    RESET_PASSWORD: '/auth/reset-password',
    VERIFY_EMAIL: '/auth/verify-email',
    RESEND_VERIFICATION: '/auth/resend-verification',
    OAUTH_GOOGLE: '/oauth2/authorization/google'
  },
  USERS: {
    BASE: '/users',
    BY_ID: (id) => `/users/${id}`,
    BY_EMAIL: '/users/by-email',
    PROFILE: '/users/profile',
    UPDATE_PROFILE: '/users/profile',
    DELETE_ACCOUNT: (id) => `/users/${id}`
  },
  FRIENDS: {
    BASE: (userId) => `/friends/${userId}`,
    PENDING: (userId) => `/friends/${userId}/pending`,
    SEND_REQUEST: '/friends/request',
    ACCEPT: (requestId, userId) => `/friends/${requestId}/accept?userId=${userId}`,
    REJECT: (requestId, userId) => `/friends/${requestId}/reject?userId=${userId}`,
    REMOVE: '/friends/remove',
    REMOVE_ALL: (userId) => `/friends/${userId}/all`,
    MUTUAL: (userId1, userId2) => `/friends/mutual/${userId1}/${userId2}`,
    CHECK: '/friends/check',
    STATS: (userId) => `/friends/${userId}/stats`
  },
  CALENDAR: {
    AVAILABILITY: (userId) => `/calendar/${userId}/availability`,
    EVENTS: (userId) => `/calendar/${userId}/events`,
    CREATE_EVENT: '/calendar/events',
    UPDATE_EVENT: (eventId) => `/calendar/events/${eventId}`,
    DELETE_EVENT: (eventId) => `/calendar/events/${eventId}`,
    SYNC_GOOGLE: '/calendar/sync/google'
  },
  CHAT: {
    ROOMS: '/chat/rooms',
    CREATE_ROOM: '/chat/rooms',
    JOIN_ROOM: (roomId) => `/chat/rooms/${roomId}/join`,
    LEAVE_ROOM: (roomId) => `/chat/rooms/${roomId}/leave`,
    MESSAGES: (roomId) => `/chat/rooms/${roomId}/messages`,
    SEND_MESSAGE: (roomId) => `/chat/rooms/${roomId}/messages`
  },
  CIRCLES: {
    BASE: '/circles',
    BY_ID: (id) => `/circles/${id}`,
    CREATE: '/circles',
    JOIN: (circleId) => `/circles/${circleId}/join`,
    LEAVE: (circleId) => `/circles/${circleId}/leave`,
    MEMBERS: (circleId) => `/circles/${circleId}/members`
  },
  ACTIVITIES: {
    FEED: (userId) => `/activities/${userId}/feed`,
    FRIENDS: (userId) => `/friends/${userId}/activities`,
    MARK_READ: (activityId) => `/activities/${activityId}/read`,
    STATS: (userId) => `/activities/${userId}/stats`
  }
};