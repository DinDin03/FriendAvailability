import { api, API_ENDPOINTS } from './api.js';

class UserService {
  // Get all users
  async getAllUsers() {
    try {
      const response = await api.get(API_ENDPOINTS.USERS.BASE);
      console.log('Retrieved all users:', response.length);
      return response;
    } catch (error) {
      console.error('Get all users error:', error);
      throw error;
    }
  }

  // Get user by ID
  async getUserById(userId) {
    try {
      const response = await api.get(API_ENDPOINTS.USERS.BY_ID(userId));
      console.log('Retrieved user:', response);
      return response;
    } catch (error) {
      console.error('Get user by ID error:', error);
      throw error;
    }
  }

  // Get user by email
  async getUserByEmail(email) {
    try {
      const response = await api.get(`${API_ENDPOINTS.USERS.BY_EMAIL}?email=${encodeURIComponent(email)}`);
      console.log('Retrieved user by email:', response);
      return response;
    } catch (error) {
      console.error('Get user by email error:', error);
      throw error;
    }
  }

  // Update user profile
  async updateProfile(profileData) {
    try {
      const response = await api.put(API_ENDPOINTS.USERS.UPDATE_PROFILE, profileData);
      console.log('Profile updated successfully:', response);
      return response;
    } catch (error) {
      console.error('Update profile error:', error);
      throw error;
    }
  }

  // Delete user account
  async deleteAccount(userId) {
    try {
      const response = await api.delete(API_ENDPOINTS.USERS.DELETE_ACCOUNT(userId));
      console.log('Account deleted successfully');
      return response;
    } catch (error) {
      console.error('Delete account error:', error);
      throw error;
    }
  }

  // Search users (if your backend supports it)
  async searchUsers(searchTerm) {
    try {
      const response = await api.get(`${API_ENDPOINTS.USERS.BASE}/search?q=${encodeURIComponent(searchTerm)}`);
      console.log('Search results:', response);
      return response;
    } catch (error) {
      console.error('Search users error:', error);
      throw error;
    }
  }

  // Get user statistics (if your backend supports it)
  async getUserStatistics() {
    try {
      const response = await api.get(`${API_ENDPOINTS.USERS.BASE}/statistics`);
      console.log('User statistics:', response);
      return response;
    } catch (error) {
      console.error('Get user statistics error:', error);
      throw error;
    }
  }
}

// Create and export user service instance
export const userService = new UserService();
