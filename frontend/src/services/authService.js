import { api, API_ENDPOINTS } from './api.js';

class AuthService {
  constructor() {
    this.currentUser = null;
    this.isAuthenticated = false;
  }

  // Login with email and password
  async login(email, password) {
    try {
      const response = await api.post(API_ENDPOINTS.AUTH.LOGIN, {
        email,
        password
      });

      if (response.success && response.userDto) {
        this.currentUser = response.userDto;
        this.isAuthenticated = true;

        // Store user data in localStorage for persistence
        localStorage.setItem('user', JSON.stringify(response.userDto));
        localStorage.setItem('isAuthenticated', 'true');

        console.log('Login successful:', response.userDto);
        return response.userDto;
      }

      throw new Error(response.message || 'Login failed');
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  }

  async googleLogin(credentialResponse) {
    try {
        const response = await fetch('/api/auth/google-signin', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                credential: credentialResponse.credential
            })
        });

        if (response.ok) {
          const userData = await response.json()
          this.currentUser = userData;
          this.isAuthenticated = true;
          localStorage.setItem('user', JSON.stringify(userData));
          localStorage.setItem('isAuthenticated', 'true');
          return userData;
        } else {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || "Google sign-in failed.");
        }

      } catch (error) {
          console.error("Network error:", error);
          throw error;
      }
  }

  // Register new user
  async register(userData) {
    try {
      const response = await api.post(API_ENDPOINTS.AUTH.REGISTER, userData);

      if (response.success) {
        console.log('Registration successful:', response);
        return response;
      }

      throw new Error(response.message || 'Registration failed');
    } catch (error) {
      console.error('Registration error:', error);
      throw error;
    }
  }

  // Logout
  async logout() {
    try {
      await api.post(API_ENDPOINTS.AUTH.LOGOUT);
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      // Clear local state regardless of API call success
      this.currentUser = null;
      this.isAuthenticated = false;
      localStorage.removeItem('user');
      localStorage.removeItem('isAuthenticated');
      console.log('User logged out');
    }
  }

  // Get current user from backend
  async getCurrentUser() {
    try {
      console.log('📡 Getting current user from backend...');
      const response = await api.get(API_ENDPOINTS.AUTH.CURRENT_USER);

      console.log('📦 Backend response:', response);

      // Your backend returns the user object directly, not wrapped in a success object
      if (response) {
        this.currentUser = response;
        this.isAuthenticated = true;

        // Update localStorage
        localStorage.setItem('user', JSON.stringify(response));
        localStorage.setItem('isAuthenticated', 'true');

        console.log('✅ Current user loaded:', response);
        return response;
      }

      throw new Error('No user data received');
    } catch (error) {
      console.error('❌ Get current user error:', error);
      await this.logout(); // Clear invalid session
      throw error;
    }
  }

  // Forgot password
  async forgotPassword(email) {
    try {
      const response = await api.post(API_ENDPOINTS.AUTH.FORGOT_PASSWORD, { email });
      console.log('Password reset email sent');
      return response;
    } catch (error) {
      console.error('Forgot password error:', error);
      throw error;
    }
  }

  // Reset password
  async resetPassword(token, newPassword) {
    try {
      const response = await api.post(API_ENDPOINTS.AUTH.RESET_PASSWORD, {
        token,
        newPassword
      });
      console.log('Password reset successful');
      return response;
    } catch (error) {
      console.error('Reset password error:', error);
      throw error;
    }
  }

  // Verify email
  async verifyEmail(token) {
    try {
      const response = await api.post(API_ENDPOINTS.AUTH.VERIFY_EMAIL, { token });
      console.log('Email verification successful');
      return response;
    } catch (error) {
      console.error('Email verification error:', error);
      throw error;
    }
  }

  // Resend verification email
  async resendVerification(email) {
    try {
      const response = await api.post(API_ENDPOINTS.AUTH.RESEND_VERIFICATION, { email });
      console.log('Verification email resent');
      return response;
    } catch (error) {
      console.error('Resend verification error:', error);
      throw error;
    }
  }

  // Initialize auth state from localStorage
  initializeAuth() {
    const storedUser = localStorage.getItem('user');
    const storedAuth = localStorage.getItem('isAuthenticated');

    if (storedUser && storedAuth === 'true') {
      try {
        this.currentUser = JSON.parse(storedUser);
        this.isAuthenticated = true;
        console.log('Auth state restored from localStorage');
      } catch (error) {
        console.error('Failed to restore auth state:', error);
        this.logout();
      }
    }
  }

  // Check if user is authenticated
  isUserAuthenticated() {
    return this.isAuthenticated;
  }

  // Get current user
  getUser() {
    return this.currentUser;
  }

  // Google OAuth login URL
  getGoogleLoginUrl() {
    return `http://localhost:8080${API_ENDPOINTS.AUTH.OAUTH_GOOGLE}`;
  }
}

// Create and export auth service instance
export const authService = new AuthService();

// Initialize auth state when module loads
authService.initializeAuth();