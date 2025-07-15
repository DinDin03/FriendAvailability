/**
 * LinkUp Dashboard JavaScript
 * Main dashboard functionality for managing friends, calendar, and user interactions
 */

// Import dependencies (these will be loaded via script tags in HTML)
// Assumes: api.js, auth.js, notifications.js are already loaded

/**
 * GLOBAL STATE MANAGEMENT
 * Centralized state for dashboard data
 */
const DashboardState = {
    // User information
    currentUser: null,

    // Friends and social data
    friends: [],
    pendingRequests: [],

    // Calendar and time data
    currentDate: new Date(),
    events: [],

    // UI state
    isLoading: false,
    activeView: 'dashboard'
};

/**
 * DASHBOARD CONFIGURATION
 * Constants and settings for dashboard behavior
 */
const DashboardConfig = {
    // Refresh intervals (in milliseconds)
    FRIENDS_REFRESH_INTERVAL: 30000,  // 30 seconds
    REQUESTS_REFRESH_INTERVAL: 15000, // 15 seconds

    // UI settings
    MAX_FRIENDS_DISPLAY: 50,
    CALENDAR_MONTHS_TO_SHOW: 1,

    // API endpoints (extending base API config)
    ENDPOINTS: {
        CURRENT_USER: '/api/auth/current-user',
        FRIENDS: '/api/friends',
        USERS: '/api/users',
        LOGOUT: '/api/auth/logout'
    }
};

/**
 * DASHBOARD INITIALIZATION
 * Main entry point for dashboard functionality
 */
class Dashboard {
    constructor() {
        this.state = DashboardState;
        this.config = DashboardConfig;
        this.refreshTimers = [];

        console.log('🎯 Dashboard initialized');
    }

    /**
     * Initialize the complete dashboard
     * This is the main entry point called when page loads
     */
    async initialize() {
        console.log('🚀 Starting dashboard initialization...');

        try {
            // Step 1: Load and verify current user
            await this.loadCurrentUser();

            if (!this.state.currentUser) {
                console.log('❌ No authenticated user found, redirecting to login');
                window.location.href = '/';
                return;
            }

            console.log('✅ User authenticated:', this.state.currentUser.name);

            // Step 2: Load all dashboard data in parallel for better performance
            this.state.isLoading = true;
            this.showLoadingStates();

            await Promise.all([
                this.loadFriends(),
                this.loadPendingRequests(),
                this.loadUpcomingEvents()
            ]);

            // Step 3: Render all UI components
            this.renderAllComponents();

            // Step 4: Set up auto-refresh timers
            this.setupRefreshTimers();

            // Step 5: Set up event listeners
            this.setupEventListeners();

            this.state.isLoading = false;
            this.hideLoadingStates();

            // Show success message
            NotificationService.showSuccess('Dashboard loaded successfully!');

            console.log('✅ Dashboard initialization complete');

        } catch (error) {
            console.error('❌ Dashboard initialization failed:', error);
            this.state.isLoading = false;
            this.hideLoadingStates();
            NotificationService.showError('Failed to load dashboard. Please refresh the page.');
        }
    }

    /**
     * Load current authenticated user data
     */
    async loadCurrentUser() {
        console.log('👤 Loading current user...');

        try {
            const user = await ApiService.get(this.config.ENDPOINTS.CURRENT_USER);
            this.state.currentUser = user;

            // Update UI with user information
            this.renderUserInfo(user);

            console.log('✅ Current user loaded:', user.name);

        } catch (error) {
            console.error('❌ Failed to load current user:', error);
            throw new Error('Authentication failed');
        }
    }

    /**
     * Load user's friends list
     */
    async loadFriends() {
        console.log('👥 Loading friends...');

        try {
            const userId = this.state.currentUser.id;
            console.log(`📡 Fetching friends for user ID: ${userId}`);

            // Make API call to get friends
            const friends = await ApiService.get(`${this.config.ENDPOINTS.FRIENDS}/${userId}`);

            console.log(`✅ Loaded ${friends.length} friends:`, friends);

            // Store in state
            this.state.friends = friends || [];

            // Render friends list
            this.renderFriends();

        } catch (error) {
            console.error('❌ Failed to load friends:', error);
            this.state.friends = [];
            this.renderFriends(); // Render empty state
        }
    }

    /**
     * Load pending friend requests
     */
    async loadPendingRequests() {
        console.log('📩 Loading pending friend requests...');

        try {
            const userId = this.state.currentUser.id;
            const requests = await ApiService.get(`${this.config.ENDPOINTS.FRIENDS}/${userId}/pending`);

            console.log(`✅ Loaded ${requests.length} pending requests:`, requests);

            // Enhance requests with sender information
            for (const request of requests) {
                try {
                    const sender = await ApiService.get(`${this.config.ENDPOINTS.USERS}/${request.userId}`);
                    request.senderName = sender.name;
                    request.senderEmail = sender.email;
                    request.senderAvatar = sender.profilePictureUrl || this.generateAvatar(sender.name);
                } catch (error) {
                    console.error('❌ Failed to load sender details for request:', request.id);
                    // Set fallback values
                    request.senderName = 'Unknown User';
                    request.senderEmail = '';
                    request.senderAvatar = this.generateAvatar('Unknown');
                }
            }

            this.state.pendingRequests = requests;
            this.renderFriends(); // Re-render to include requests

        } catch (error) {
            console.error('❌ Failed to load pending requests:', error);
            this.state.pendingRequests = [];
        }
    }

    /**
     * Load upcoming events (placeholder for future implementation)
     */
    async loadUpcomingEvents() {
        console.log('📅 Loading upcoming events...');

        // TODO: Implement when event system is ready
        this.state.events = [];
        this.renderEvents();
    }

    /**
     * Render user information in header
     */
    renderUserInfo(user) {
        console.log('🎨 Rendering user info for:', user.name);

        // Update user name and email
        const userNameEl = document.getElementById('userName');
        const userEmailEl = document.getElementById('userEmail');
        const userAvatarEl = document.getElementById('userAvatar');

        if (userNameEl) userNameEl.textContent = user.name;
        if (userEmailEl) userEmailEl.textContent = user.email;
        if (userAvatarEl) {
            userAvatarEl.src = user.profilePictureUrl || this.generateAvatar(user.name);
            userAvatarEl.alt = `${user.name}'s avatar`;
        }
    }

    /**
     * Render friends list and pending requests
     */
    renderFriends() {
        console.log('🎨 Rendering friends and requests...');
        console.log(`📊 Rendering ${this.state.friends.length} friends, ${this.state.pendingRequests.length} requests`);

        const container = document.getElementById('friendsContainer');
        if (!container) {
            console.error('❌ Friends container not found');
            return;
        }

        let html = '';

        // Render pending requests section first
        if (this.state.pendingRequests.length > 0) {
            html += this.renderPendingRequestsSection();
        }

        // Render friends list
        if (this.state.friends.length === 0) {
            html += this.renderEmptyFriendsState();
        } else {
            html += this.renderFriendsList();
        }

        container.innerHTML = html;
        console.log('✅ Friends rendering complete');
    }

    /**
     * Render pending requests section
     */
    renderPendingRequestsSection() {
        const requestCount = this.state.pendingRequests.length;

        let html = `
            <div class="request-section">
                <h3 style="color: var(--primary-blue); margin-bottom: var(--spacing-sm);">
                    Friend Requests (${requestCount})
                </h3>
        `;

        this.state.pendingRequests.forEach(request => {
            html += `
                <div class="request-item">
                    <div style="display: flex; align-items: center; gap: var(--spacing-md);">
                        <img src="${request.senderAvatar}" 
                             alt="${request.senderName} avatar" 
                             class="friend-avatar"
                             onerror="this.src='${this.generateAvatar(request.senderName || 'Unknown')}'">
                        <div>
                            <div style="font-weight: 500;">${request.senderName}</div>
                            <div style="font-size: var(--font-size-sm); color: var(--gray-500);">
                                ${request.senderEmail}
                            </div>
                        </div>
                    </div>
                    <div class="request-actions">
                        <button class="btn btn-primary" onclick="dashboard.acceptRequest(${request.id})">
                            <i class="fas fa-check"></i> Accept
                        </button>
                        <button class="btn btn-secondary" onclick="dashboard.rejectRequest(${request.id})">
                            <i class="fas fa-times"></i> Reject
                        </button>
                    </div>
                </div>
            `;
        });

        html += '</div>';
        return html;
    }

    /**
     * Render empty friends state
     */
    renderEmptyFriendsState() {
        return `
            <div class="empty-state">
                <i class="fas fa-user-friends"></i>
                <p>No friends yet. Send some friend requests!</p>
                <button class="btn btn-primary" onclick="dashboard.showAddFriend()">
                    <i class="fas fa-user-plus"></i> Add Your First Friend
                </button>
            </div>
        `;
    }

    /**
     * Render friends list
     */
    renderFriendsList() {
        let html = '<div class="friends-list">';

        this.state.friends.forEach(friend => {
            const avatar = friend.profilePictureUrl || this.generateAvatar(friend.name);
            html += `
                <div class="friend-item" onclick="dashboard.viewFriendProfile(${friend.id})">
                    <img src="${avatar}" 
                         alt="${friend.name} avatar" 
                         class="friend-avatar"
                         onerror="this.src='${this.generateAvatar(friend.name)}'">
                    <div class="friend-info">
                        <div class="friend-name">${friend.name}</div>
                        <div class="friend-email">${friend.email}</div>
                    </div>
                    <div class="friend-actions">
                        <button class="btn btn-icon" onclick="event.stopPropagation(); dashboard.viewFriendCalendar(${friend.id})" title="View Calendar">
                            <i class="fas fa-calendar"></i>
                        </button>
                    </div>
                </div>
            `;
        });

        html += '</div>';
        return html;
    }

    /**
     * Render calendar component
     */
    renderCalendar() {
        console.log('📅 Rendering calendar...');

        const year = this.state.currentDate.getFullYear();
        const month = this.state.currentDate.getMonth();

        // Update month display
        const monthNames = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];

        const currentMonthEl = document.getElementById('currentMonth');
        if (currentMonthEl) {
            currentMonthEl.textContent = `${monthNames[month]} ${year}`;
        }

        // Calculate calendar days
        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        const daysInMonth = lastDay.getDate();
        const startingDayOfWeek = firstDay.getDay();

        let html = '';

        // Day headers
        const dayHeaders = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
        dayHeaders.forEach(day => {
            html += `<div class="calendar-day-header">${day}</div>`;
        });

        // Empty cells before month starts
        for (let i = 0; i < startingDayOfWeek; i++) {
            html += `<div class="calendar-day other-month"></div>`;
        }

        // Days of month
        const today = new Date();
        for (let day = 1; day <= daysInMonth; day++) {
            const isToday = year === today.getFullYear() &&
                month === today.getMonth() &&
                day === today.getDate();

            html += `
                <div class="calendar-day ${isToday ? 'today' : ''}" 
                     onclick="dashboard.selectDate(${year}, ${month}, ${day})">
                    ${day}
                </div>
            `;
        }

        const calendarGrid = document.getElementById('calendarGrid');
        if (calendarGrid) {
            calendarGrid.innerHTML = html;
        }

        console.log('✅ Calendar rendered');
    }

    /**
     * Render events list
     */
    renderEvents() {
        console.log('📋 Rendering events...');

        const container = document.getElementById('eventsContainer');
        if (!container) return;

        if (this.state.events.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-calendar-alt"></i>
                    <p>No upcoming events</p>
                    <button class="btn btn-primary" onclick="dashboard.showAddEvent()">
                        <i class="fas fa-plus"></i> Create Event
                    </button>
                </div>
            `;
        } else {
            // TODO: Render actual events when event system is implemented
            container.innerHTML = '<div class="loading">Events coming soon...</div>';
        }
    }

    /**
     * Render all dashboard components
     */
    renderAllComponents() {
        console.log('🎨 Rendering all dashboard components...');

        this.renderFriends();
        this.renderCalendar();
        this.renderEvents();

        console.log('✅ All components rendered');
    }

    /**
     * Show loading states for all components
     */
    showLoadingStates() {
        const containers = [
            'friendsContainer',
            'calendarGrid',
            'eventsContainer'
        ];

        containers.forEach(containerId => {
            const container = document.getElementById(containerId);
            if (container) {
                container.innerHTML = `
                    <div class="loading">
                        <i class="fas fa-spinner fa-spin"></i> Loading...
                    </div>
                `;
            }
        });
    }

    /**
     * Hide loading states
     */
    hideLoadingStates() {
        // Loading states are replaced by actual content in render methods
        console.log('✅ Loading states hidden');
    }

    /**
     * USER ACTION HANDLERS
     * Functions that handle user interactions
     */

    /**
     * Accept a friend request
     */
    async acceptRequest(requestId) {
        console.log(`✅ Accepting friend request: ${requestId}`);

        try {
            await ApiService.put(`${this.config.ENDPOINTS.FRIENDS}/${requestId}/accept?userId=${this.state.currentUser.id}`);

            NotificationService.showSuccess('Friend request accepted!');

            // Refresh data
            await Promise.all([
                this.loadFriends(),
                this.loadPendingRequests()
            ]);

        } catch (error) {
            console.error('❌ Failed to accept request:', error);
            NotificationService.showError('Failed to accept friend request');
        }
    }

    /**
     * Reject a friend request
     */
    async rejectRequest(requestId) {
        console.log(`❌ Rejecting friend request: ${requestId}`);

        try {
            await ApiService.put(`${this.config.ENDPOINTS.FRIENDS}/${requestId}/reject?userId=${this.state.currentUser.id}`);

            NotificationService.showInfo('Friend request rejected');

            // Refresh pending requests
            await this.loadPendingRequests();

        } catch (error) {
            console.error('❌ Failed to reject request:', error);
            NotificationService.showError('Failed to reject friend request');
        }
    }

    /**
     * Show add friend dialog
     */
    async showAddFriend() {
        console.log('👥 Showing add friend dialog...');

        const email = prompt('Enter friend\'s email address:');
        if (!email || !email.trim()) {
            return;
        }

        try {
            // Find user by email
            const user = await ApiService.get(`${this.config.ENDPOINTS.USERS}/by-email?email=${encodeURIComponent(email.trim())}`);

            if (!user) {
                NotificationService.showError('User not found with that email address');
                return;
            }

            // Check if already friends or request exists
            if (this.state.friends.some(friend => friend.id === user.id)) {
                NotificationService.showWarning('You are already friends with this user');
                return;
            }

            // Send friend request
            await ApiService.post(`${this.config.ENDPOINTS.FRIENDS}/request?fromUserId=${this.state.currentUser.id}&toUserId=${user.id}`);

            NotificationService.showSuccess(`Friend request sent to ${user.name}!`);

        } catch (error) {
            console.error('❌ Failed to send friend request:', error);
            NotificationService.showError('Failed to send friend request');
        }
    }

    /**
     * View friend's profile (placeholder)
     */
    viewFriendProfile(friendId) {
        console.log(`👤 Viewing friend profile: ${friendId}`);
        NotificationService.showInfo('Friend profiles coming soon!');
    }

    /**
     * View friend's calendar (placeholder)
     */
    viewFriendCalendar(friendId) {
        console.log(`📅 Viewing friend calendar: ${friendId}`);
        NotificationService.showInfo('Friend calendars coming soon!');
    }

    /**
     * CALENDAR ACTIONS
     */

    /**
     * Change calendar month
     */
    changeMonth(direction) {
        console.log(`📅 Changing month by: ${direction}`);

        this.state.currentDate.setMonth(this.state.currentDate.getMonth() + direction);
        this.renderCalendar();
    }

    /**
     * Select a date on calendar
     */
    selectDate(year, month, day) {
        console.log(`📅 Selected date: ${year}-${month + 1}-${day}`);

        const selectedDate = new Date(year, month, day);
        NotificationService.showInfo(`Selected: ${selectedDate.toLocaleDateString()}`);

        // TODO: Show events for selected date or create new event
    }

    /**
     * Show add event dialog (placeholder)
     */
    showAddEvent() {
        console.log('📅 Showing add event dialog...');
        NotificationService.showInfo('Event creation coming soon!');
    }

    /**
     * Logout user
     */
    async logout() {
        console.log('🚪 Logging out user...');

        try {
            await ApiService.post(this.config.ENDPOINTS.LOGOUT);
            NotificationService.showSuccess('Logged out successfully');

            // Redirect to home page
            setTimeout(() => {
                window.location.href = '/';
            }, 1000);

        } catch (error) {
            console.error('❌ Logout failed:', error);
            // Force redirect even if API call fails
            window.location.href = '/';
        }
    }

    /**
     * UTILITY FUNCTIONS
     */

    /**
     * Generate avatar placeholder for users without profile pictures
     */
    generateAvatar(name) {
        if (!name || name.trim() === '') {
            name = 'User';
        }

        const initials = name.split(' ')
            .map(n => n[0])
            .join('')
            .toUpperCase()
            .substring(0, 2);

        // Use a variety of colors
        const colors = ['0052CC', '00875A', 'DE350B', 'FF8B00', '6554C0', '008DA6', 'BF2600'];
        const colorIndex = name.length % colors.length;
        const color = colors[colorIndex];

        return `https://via.placeholder.com/48x48/${color}/FFFFFF?text=${initials}`;
    }

    /**
     * Setup auto-refresh timers for real-time updates
     */
    setupRefreshTimers() {
        console.log('⏰ Setting up refresh timers...');

        // Refresh friends list periodically
        const friendsTimer = setInterval(() => {
            if (!this.state.isLoading) {
                this.loadFriends();
            }
        }, this.config.FRIENDS_REFRESH_INTERVAL);

        // Refresh requests more frequently
        const requestsTimer = setInterval(() => {
            if (!this.state.isLoading) {
                this.loadPendingRequests();
            }
        }, this.config.REQUESTS_REFRESH_INTERVAL);

        this.refreshTimers.push(friendsTimer, requestsTimer);

        console.log('✅ Refresh timers set up');
    }

    /**
     * Setup event listeners for dashboard interactions
     */
    setupEventListeners() {
        console.log('🎧 Setting up event listeners...');

        // Keyboard shortcuts
        document.addEventListener('keydown', (event) => {
            // Ctrl/Cmd + F to focus on add friend
            if ((event.ctrlKey || event.metaKey) && event.key === 'f') {
                event.preventDefault();
                this.showAddFriend();
            }
        });

        // Window beforeunload to clean up timers
        window.addEventListener('beforeunload', () => {
            this.cleanup();
        });

        console.log('✅ Event listeners set up');
    }

    /**
     * Cleanup function to clear timers and prevent memory leaks
     */
    cleanup() {
        console.log('🧹 Cleaning up dashboard...');

        this.refreshTimers.forEach(timer => clearInterval(timer));
        this.refreshTimers = [];

        console.log('✅ Dashboard cleaned up');
    }
}

class DashboardChat {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.currentRoomId = null;
        this.currentUserId = null;
        this.currentUserName = null;
        this.chatRooms = [];
        this.unreadCounts = new Map();
        
        this.init();
    }
    
    async init() {
        console.log('🚀 Initializing Dashboard Chat...');
        
        // Get current user info from dashboard state
        this.currentUserId = DashboardState.currentUser?.id || 1; // Fallback for testing
        this.currentUserName = DashboardState.currentUser?.name || 'User';
        
        // Load initial chat data
        await this.loadChatRooms();
        
        // Connect to WebSocket
        this.connectWebSocket();
        
        console.log('✅ Dashboard Chat initialized');
    }
    
    async loadChatRooms() {
        try {
            document.getElementById('chatRoomsLoading').style.display = 'flex';
            
            const response = await fetch(`/api/chat/rooms?userId=${this.currentUserId}`);
            const data = await response.json();
            
            if (response.ok) {
                this.chatRooms = data.chatRooms || [];
                await this.loadUnreadCounts();
                this.renderChatRooms();
            } else {
                console.error('Failed to load chat rooms:', data.error);
                this.showEmptyState();
            }
        } catch (error) {
            console.error('Error loading chat rooms:', error);
            this.showEmptyState();
        } finally {
            document.getElementById('chatRoomsLoading').style.display = 'none';
        }
    }
    
    async loadUnreadCounts() {
        for (const room of this.chatRooms) {
            try {
                const response = await fetch(`/api/chat/rooms/${room.id}/messages/unread?userId=${this.currentUserId}`);
                const data = await response.json();
                
                if (response.ok) {
                    this.unreadCounts.set(room.id, data.unreadCount);
                }
            } catch (error) {
                console.error(`Error loading unread count for room ${room.id}:`, error);
            }
        }
        this.updateTotalUnreadBadge();
    }
    
    renderChatRooms() {
        const container = document.getElementById('chatRoomsItems');
        const emptyState = document.getElementById('chatRoomsEmpty');
        
        if (this.chatRooms.length === 0) {
            this.showEmptyState();
            return;
        }
        
        emptyState.style.display = 'none';
        
        container.innerHTML = this.chatRooms.map(room => {
            const unreadCount = this.unreadCounts.get(room.id) || 0;
            const displayName = room.displayName || room.name || 'Chat';
            const roomType = room.type || 'PRIVATE';
            const avatar = this.getRoomAvatar(room);
            
            return `
                <div class="chat-room-item" onclick="openChatRoom(${room.id})">
                    <div class="chat-room-avatar" style="background: ${avatar.bg}; color: ${avatar.color}">
                        ${avatar.text}
                    </div>
                    <div class="chat-room-info">
                        <div class="chat-room-name">${displayName}</div>
                        <div class="chat-room-last-message">
                            ${roomType === 'GROUP' ? 'Group Chat' : 'Private Chat'}
                        </div>
                    </div>
                    <div class="chat-room-meta">
                        <div class="chat-room-time">${this.formatTime(room.updatedAt)}</div>
                        ${unreadCount > 0 ? `<div class="unread-badge">${unreadCount}</div>` : ''}
                    </div>
                </div>
            `;
        }).join('');
    }
    
    showEmptyState() {
        document.getElementById('chatRoomsEmpty').style.display = 'flex';
        document.getElementById('chatRoomsItems').innerHTML = '';
    }
    
    getRoomAvatar(room) {
        const name = room.displayName || room.name || 'Chat';
        const firstLetter = name.charAt(0).toUpperCase();
        
        // Generate consistent colors based on room ID
        const colors = [
            { bg: '#0052CC', color: '#ffffff' },
            { bg: '#00875A', color: '#ffffff' },
            { bg: '#DE350B', color: '#ffffff' },
            { bg: '#FF8B00', color: '#ffffff' },
            { bg: '#6554C0', color: '#ffffff' }
        ];
        
        const colorIndex = room.id % colors.length;
        return {
            text: firstLetter,
            ...colors[colorIndex]
        };
    }
    
    formatTime(dateString) {
        if (!dateString) return '';
        
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMins / 60);
        const diffDays = Math.floor(diffHours / 24);
        
        if (diffMins < 1) return 'now';
        if (diffMins < 60) return `${diffMins}m`;
        if (diffHours < 24) return `${diffHours}h`;
        if (diffDays < 7) return `${diffDays}d`;
        
        return date.toLocaleDateString();
    }
    
    updateTotalUnreadBadge() {
        const totalUnread = Array.from(this.unreadCounts.values()).reduce((sum, count) => sum + count, 0);
        const badge = document.getElementById('totalUnreadBadge');
        
        if (totalUnread > 0) {
            badge.textContent = totalUnread > 99 ? '99+' : totalUnread;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }
    
    connectWebSocket() {
        try {
            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);
            
            this.stompClient.connect({}, (frame) => {
                console.log('✅ WebSocket connected:', frame);
                this.connected = true;
                this.updateConnectionStatus();
            }, (error) => {
                console.error('❌ WebSocket connection failed:', error);
                this.connected = false;
                this.updateConnectionStatus();
                
                // Retry connection after 5 seconds
                setTimeout(() => this.connectWebSocket(), 5000);
            });
        } catch (error) {
            console.error('Error connecting to WebSocket:', error);
        }
    }
    
    updateConnectionStatus() {
        const statusElement = document.getElementById('chatConnectionStatus');
        const icon = statusElement.querySelector('i');
        const text = statusElement.querySelector('span');
        
        if (this.connected) {
            icon.className = 'fas fa-circle status-online';
            text.textContent = 'Online';
        } else {
            icon.className = 'fas fa-circle status-offline';
            text.textContent = 'Offline';
        }
    }
}

// === LIVE CHAT FRONTEND INTEGRATION ===

let stompClient = null;
let currentRoomId = null;

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        // Optionally subscribe to a default room or user notifications here
    }, function (error) {
        console.error('WebSocket error:', error);
        setTimeout(connectWebSocket, 5000); // Retry on disconnect
    });
}

function subscribeToRoom(roomId) {
    if (stompClient && stompClient.connected) {
        stompClient.subscribe(`/topic/chat/${roomId}`, function (message) {
            const msg = JSON.parse(message.body);
            displayNewMessage(msg);
        });
    }
}

function sendChatMessage() {
    const input = document.getElementById('chatMessageInput');
    const message = input.value.trim();
    if (!message || !stompClient || !stompClient.connected || !currentRoomId) return;

    stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
        senderId: DashboardState.currentUser.id,
        roomId: currentRoomId,
        content: message
    }));

    input.value = '';
}

function displayNewMessage(msg) {
    const container = document.getElementById('chatMessagesContainer');
    const isOwn = msg.senderId === DashboardState.currentUser.id;
    const div = document.createElement('div');
    div.className = 'message' + (isOwn ? ' own' : '');
    div.innerHTML = `
        <div class="message-bubble">
            <div class="message-info">
                <span class="sender-name">${isOwn ? 'You' : msg.senderName}</span>
                <span class="message-time">${new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
            </div>
            <div class="message-content">${msg.content}</div>
        </div>
    `;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

async function openChatRoom(roomId) {
    currentRoomId = roomId;
    document.getElementById('chatRoomsList').style.display = 'none';
    document.getElementById('activeChatView').style.display = 'flex';

    await loadChatMessages(roomId);
    subscribeToRoom(roomId);
}

async function loadChatMessages(roomId) {
    const response = await fetch(`/api/chat/rooms/${roomId}/messages/recent?userId=${DashboardState.currentUser.id}&limit=50`);
    const data = await response.json();
    const container = document.getElementById('chatMessagesContainer');
    container.innerHTML = '';
    data.messages.reverse().forEach(displayNewMessage);
    container.scrollTop = container.scrollHeight;
}

function handleMessageKeyPress(event) {
    if (event.key === 'Enter') sendChatMessage();
}

function showChatRoomsList() {
    document.getElementById('activeChatView').style.display = 'none';
    document.getElementById('chatRoomsList').style.display = 'flex';
    currentRoomId = null;
}

// Initialize chat when dashboard loads
document.addEventListener('DOMContentLoaded', function() {
    // Wait for dashboard to initialize first
    setTimeout(() => {
        dashboardChat = new DashboardChat();
    }, 1000);
});

/**
 * GLOBAL DASHBOARD INSTANCE
 * Create and expose dashboard instance globally
 */
let dashboard;

/**
 * INITIALIZATION
 * Initialize dashboard when DOM is ready
 */
document.addEventListener('DOMContentLoaded', async () => {
    console.log('🌟 DOM loaded, initializing dashboard...');

    try {
        // Create dashboard instance
        dashboard = new Dashboard();

        // Make dashboard available globally for onclick handlers
        window.dashboard = dashboard;

        // Initialize dashboard
        await dashboard.initialize();

    } catch (error) {
        console.error('💥 Fatal error initializing dashboard:', error);
        NotificationService.showError('Failed to initialize dashboard. Please refresh the page.');
    }
});

/**
 * GLOBAL FUNCTIONS FOR HTML onclick HANDLERS
 * These functions bridge HTML onclick attributes to dashboard methods
 */

// Calendar navigation
function changeMonth(direction) {
    if (dashboard) {
        dashboard.changeMonth(direction);
    }
}

// User actions
function showAddFriend() {
    if (dashboard) {
        dashboard.showAddFriend();
    }
}

function showAddEvent() {
    if (dashboard) {
        dashboard.showAddEvent();
    }
}

function logout() {
    if (dashboard) {
        dashboard.logout();
    }
}

// Export for module systems (if needed)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { Dashboard, DashboardState, DashboardConfig };
}

