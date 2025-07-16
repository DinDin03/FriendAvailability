const DashboardState = {
    currentUser: null,
    friends: [],
    pendingRequests: [],
    currentDate: new Date(),
    events: [],
    isLoading: false,
    activeView: 'dashboard',
    chat: {
        stompClient: null,
        connected: false,
        currentRoomId: null,
        availableRooms: [],
        messages: new Map(),
        typingUsers: new Set()
    }
};
const DashboardConfig = {
    FRIENDS_REFRESH_INTERVAL: 30000,
    REQUESTS_REFRESH_INTERVAL: 15000,
    MAX_FRIENDS_DISPLAY: 50,
    CALENDAR_MONTHS_TO_SHOW: 1,
    ENDPOINTS: {
        CURRENT_USER: '/api/auth/current-user',
        FRIENDS: '/api/friends',
        USERS: '/api/users',
        LOGOUT: '/api/auth/logout'
    }
};

class Dashboard {
    constructor() {
        this.state = DashboardState;
        this.config = DashboardConfig;
        this.refreshTimers = [];
        console.log('Dashboard initialized');
    }

    async initialize() {
        console.log('Starting dashboard initialization...');
        try {
            await this.loadCurrentUser();
            if (!this.state.currentUser) {
                console.log('No authenticated user found, redirecting to login');
                window.location.href = '/';
                return;
            }
            console.log('User authenticated:', this.state.currentUser.name);
            this.state.isLoading = true;
            this.showLoadingStates();
            this.initializeChat();
            await Promise.all([
                this.loadFriends(),
                this.loadPendingRequests(),
                this.loadUpcomingEvents()
            ]);
            this.renderAllComponents();
            this.setupRefreshTimers();
            this.setupEventListeners();
            this.state.isLoading = false;
            this.hideLoadingStates();
            NotificationService.showSuccess('Dashboard loaded successfully!');
            console.log('Dashboard initialization complete');
        } catch (error) {
            console.error('Dashboard initialization failed:', error);
            this.state.isLoading = false;
            this.hideLoadingStates();
            NotificationService.showError('Failed to load dashboard. Please refresh the page.');
        }
    }

    async loadCurrentUser() {
        console.log('👤 Loading current user...');
        try {
            const user = await ApiService.get(this.config.ENDPOINTS.CURRENT_USER);
            this.state.currentUser = user;
            this.renderUserInfo(user);
            console.log('Current user loaded:', user.name);
        } catch (error) {
            console.error('Failed to load current user:', error);
            throw new Error('Authentication failed');
        }
    }

    async loadFriends() {
        console.log('👥 Loading friends...');
        try {
            const userId = this.state.currentUser.id;
            console.log(`📡 Fetching friends for user ID: ${userId}`);
            const friends = await ApiService.get(`${this.config.ENDPOINTS.FRIENDS}/${userId}`);
            console.log(`✅ Loaded ${friends.length} friends:`, friends);
            this.state.friends = friends || [];
            this.renderFriends();
        } catch (error) {
            console.error('Failed to load friends:', error);
            this.state.friends = [];
            this.renderFriends();
        }
    }

    async loadPendingRequests() {
        console.log('Loading pending friend requests...');
        try {
            const userId = this.state.currentUser.id;
            const requests = await ApiService.get(`${this.config.ENDPOINTS.FRIENDS}/${userId}/pending`);
            console.log(`Loaded ${requests.length} pending requests:`, requests);
            for (const request of requests) {
                try {
                    const sender = await ApiService.get(`${this.config.ENDPOINTS.USERS}/${request.userId}`);
                    request.senderName = sender.name;
                    request.senderEmail = sender.email;
                    request.senderAvatar = sender.profilePictureUrl || this.generateAvatar(sender.name);
                } catch (error) {
                    console.error('Failed to load sender details for request:', request.id);
                    request.senderName = 'Unknown User';
                    request.senderEmail = '';
                    request.senderAvatar = this.generateAvatar('Unknown');
                }
            }
            this.state.pendingRequests = requests;
            this.renderFriends();
        } catch (error) {
            console.error('Failed to load pending requests:', error);
            this.state.pendingRequests = [];
        }
    }

    async loadUpcomingEvents() {
        console.log('Loading upcoming events...');
        this.state.events = [];
        this.renderEvents();
    }

    renderUserInfo(user) {
        console.log('Rendering user info for:', user.name);
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

    renderFriends() {
        console.log('Rendering friends and requests...');
        console.log(`Rendering ${this.state.friends.length} friends, ${this.state.pendingRequests.length} requests`);
        const container = document.getElementById('friendsContainer');
        if (!container) {
            console.error('Friends container not found');
            return;
        }
        let html = '';
        if (this.state.pendingRequests.length > 0) {
            html += this.renderPendingRequestsSection();
        }
        if (this.state.friends.length === 0) {
            html += this.renderEmptyFriendsState();
        } else {
            html += this.renderFriendsList();
        }
        container.innerHTML = html;
        console.log('Friends rendering complete');
    }

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

    renderCalendar() {
        console.log('Rendering calendar...');
        const year = this.state.currentDate.getFullYear();
        const month = this.state.currentDate.getMonth();
        const monthNames = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];
        const currentMonthEl = document.getElementById('currentMonth');
        if (currentMonthEl) {
            currentMonthEl.textContent = `${monthNames[month]} ${year}`;
        }
        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        const daysInMonth = lastDay.getDate();
        const startingDayOfWeek = firstDay.getDay();
        let html = '';
        const dayHeaders = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
        dayHeaders.forEach(day => {
            html += `<div class="calendar-day-header">${day}</div>`;
        });
        for (let i = 0; i < startingDayOfWeek; i++) {
            html += `<div class="calendar-day other-month"></div>`;
        }
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
        console.log('Calendar rendered');
    }

    renderEvents() {
        console.log('Rendering events...');
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
            container.innerHTML = '<div class="loading">Events coming soon...</div>';
        }
    }

    renderAllComponents() {
        console.log('Rendering all dashboard components...');
        this.renderFriends();
        this.renderCalendar();
        this.renderEvents();
        console.log('All components rendered');
    }

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

    hideLoadingStates() {
        console.log('Loading states hidden');
    }

    async acceptRequest(requestId) {
        console.log(`Accepting friend request: ${requestId}`);
        try {
            await ApiService.put(`${this.config.ENDPOINTS.FRIENDS}/${requestId}/accept?userId=${this.state.currentUser.id}`);
            NotificationService.showSuccess('Friend request accepted!');
            await Promise.all([
                this.loadFriends(),
                this.loadPendingRequests()
            ]);
        } catch (error) {
            console.error('Failed to accept request:', error);
            NotificationService.showError('Failed to accept friend request');
        }
    }

    async rejectRequest(requestId) {
        console.log(`Rejecting friend request: ${requestId}`);
        try {
            await ApiService.put(`${this.config.ENDPOINTS.FRIENDS}/${requestId}/reject?userId=${this.state.currentUser.id}`);
            NotificationService.showInfo('Friend request rejected');
            await this.loadPendingRequests();
        } catch (error) {
            console.error('Failed to reject request:', error);
            NotificationService.showError('Failed to reject friend request');
        }
    }

    async showAddFriend() {
        console.log('Showing add friend dialog...');
        const email = prompt('Enter friend\'s email address:');
        if (!email || !email.trim()) {
            return;
        }
        try {
            const user = await ApiService.get(`${this.config.ENDPOINTS.USERS}/by-email?email=${encodeURIComponent(email.trim())}`);
            if (!user) {
                NotificationService.showError('User not found with that email address');
                return;
            }
            if (this.state.friends.some(friend => friend.id === user.id)) {
                NotificationService.showWarning('You are already friends with this user');
                return;
            }
            await ApiService.post(`${this.config.ENDPOINTS.FRIENDS}/request?fromUserId=${this.state.currentUser.id}&toUserId=${user.id}`);
            NotificationService.showSuccess(`Friend request sent to ${user.name}!`);
        } catch (error) {
            console.error('Failed to send friend request:', error);
            NotificationService.showError('Failed to send friend request');
        }
    }

    viewFriendProfile(friendId) {
        console.log(`Viewing friend profile: ${friendId}`);
        NotificationService.showInfo('Friend profiles coming soon!');
    }

    viewFriendCalendar(friendId) {
        console.log(`Viewing friend calendar: ${friendId}`);
        NotificationService.showInfo('Friend calendars coming soon!');
    }

    changeMonth(direction) {
        console.log(`Changing month by: ${direction}`);
        this.state.currentDate.setMonth(this.state.currentDate.getMonth() + direction);
        this.renderCalendar();
    }

    selectDate(year, month, day) {
        console.log(`Selected date: ${year}-${month + 1}-${day}`);
        const selectedDate = new Date(year, month, day);
        NotificationService.showInfo(`Selected: ${selectedDate.toLocaleDateString()}`);
    }

    showAddEvent() {
        console.log('Showing add event dialog...');
        NotificationService.showInfo('Event creation coming soon!');
    }

    async logout() {
        console.log('Logging out user...');
        try {
            await ApiService.post(this.config.ENDPOINTS.LOGOUT);
            NotificationService.showSuccess('Logged out successfully');
            setTimeout(() => {
                window.location.href = '/';
            }, 1000);
        } catch (error) {
            console.error('Logout failed:', error);
            window.location.href = '/';
        }
    }

    generateAvatar(name) {
        if (!name || name.trim() === '') {
            name = 'User';
        }
        const initials = name.split(' ')
            .map(n => n[0])
            .join('')
            .toUpperCase()
            .substring(0, 2);
        const colors = ['0052CC', '00875A', 'DE350B', 'FF8B00', '6554C0', '008DA6', 'BF2600'];
        const colorIndex = name.length % colors.length;
        const color = colors[colorIndex];
        return `https://via.placeholder.com/48x48/${color}/FFFFFF?text=${initials}`;
    }

    setupRefreshTimers() {
        console.log('⏰ Setting up refresh timers...');
        const friendsTimer = setInterval(() => {
            if (!this.state.isLoading) {
                this.loadFriends();
            }
        }, this.config.FRIENDS_REFRESH_INTERVAL);
        const requestsTimer = setInterval(() => {
            if (!this.state.isLoading) {
                this.loadPendingRequests();
            }
        }, this.config.REQUESTS_REFRESH_INTERVAL);
        this.refreshTimers.push(friendsTimer, requestsTimer);
        console.log('Refresh timers set up');
    }

    setupEventListeners() {
        console.log('Setting up event listeners...');
        document.addEventListener('keydown', (event) => {
            if ((event.ctrlKey || event.metaKey) && event.key === 'f') {
                event.preventDefault();
                this.showAddFriend();
            }
        });
        window.addEventListener('beforeunload', () => {
            this.cleanup();
        });
        console.log('Event listeners set up');
    }

    cleanup() {
        console.log('Cleaning up dashboard...');
        this.refreshTimers.forEach(timer => clearInterval(timer));
        this.refreshTimers = [];
        console.log('Dashboard cleaned up');
    }

    initializeChat() {
        console.log('Initializing chat system...');
        this.connectWebSocket();
        this.setupChatEventListeners();
    }

    connectWebSocket() {
        try {
            const socket = new SockJS('/ws');
            this.state.chat.stompClient = Stomp.over(socket);
            this.state.chat.stompClient.connect({}, (frame) => {
                console.log('WebSocket connected:', frame);
                this.state.chat.connected = true;
                this.updateChatConnectionStatus();
                this.state.chat.stompClient.subscribe(
                    `/user/${this.state.currentUser.id}/queue/errors`,
                    (message) => this.handleChatError(JSON.parse(message.body))
                );
            }, (error) => {
                console.error('WebSocket connection failed:', error);
                this.state.chat.connected = false;
                this.updateChatConnectionStatus();
                setTimeout(() => this.connectWebSocket(), 5000);
            });
        } catch (error) {
            console.error('Error connecting to WebSocket:', error);
        }
    }

    updateChatConnectionStatus() {
        const statusElement = document.getElementById('chatConnectionStatus');
        if (!statusElement) return;
        const icon = statusElement.querySelector('i');
        const text = statusElement.querySelector('span');
        if (this.state.chat.connected) {
            icon.className = 'fas fa-circle status-online';
            text.textContent = 'Online';
        } else {
            icon.className = 'fas fa-circle status-offline';
            text.textContent = 'Offline';
        }
    }

    setupChatEventListeners() {
        const roomSelect = document.getElementById('chatRoomSelect');
        if (roomSelect) {
            roomSelect.addEventListener('change', (e) => {
                this.joinChatRoom(e.target.value);
            });
        }
        const sendBtn = document.getElementById('chatSendBtn');
        if (sendBtn) {
            sendBtn.addEventListener('click', () => this.sendChatMessage());
        }
        const messageInput = document.getElementById('chatMessageInput');
        if (messageInput) {
            messageInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.sendChatMessage();
                }
            });
            let typingTimer;
            messageInput.addEventListener('input', () => {
                if (this.state.chat.currentRoomId) {
                    this.sendTypingIndicator(true);
                    clearTimeout(typingTimer);
                    typingTimer = setTimeout(() => {
                        this.sendTypingIndicator(false);
                    }, 2000);
                }
            });
        }
        const refreshBtn = document.getElementById('refreshRoomsBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => this.loadUserChatRooms());
        }
    }

    async loadUserChatRooms() {
        try {
            const response = await fetch(`/api/chat/rooms?userId=${this.state.currentUser.id}`);
            const rooms = await response.json();
            this.state.chat.availableRooms = rooms;
            this.renderChatRooms();
        } catch (error) {
            console.error('Error loading chat rooms:', error);
            NotificationService.showError('Failed to load chat rooms');
        }
    }

    renderChatRooms() {
        const roomSelect = document.getElementById('chatRoomSelect');
        if (!roomSelect) return;
        while (roomSelect.children.length > 1) {
            roomSelect.removeChild(roomSelect.lastChild);
        }
        this.state.chat.availableRooms.forEach(room => {
            const option = document.createElement('option');
            option.value = room.id;
            option.textContent = room.name || `Chat Room ${room.id}`;
            roomSelect.appendChild(option);
        });
    }

    joinChatRoom(roomId) {
        if (!roomId || !this.state.chat.connected) return;
        if (this.state.chat.currentRoomId) {
            this.leaveChatRoom();
        }
        this.state.chat.currentRoomId = roomId;
        this.state.chat.stompClient.subscribe(
            `/topic/chat/${roomId}`,
            (message) => this.handleChatMessage(JSON.parse(message.body))
        );
        this.state.chat.stompClient.subscribe(
            `/topic/chat/${roomId}/typing`,
            (message) => this.handleTypingIndicator(JSON.parse(message.body))
        );
        const messageInput = document.getElementById('chatMessageInput');
        const sendBtn = document.getElementById('chatSendBtn');
        if (messageInput && sendBtn) {
            messageInput.disabled = false;
            sendBtn.disabled = false;
            messageInput.focus();
        }
        this.loadRoomMessages(roomId);
        console.log(`Joined chat room: ${roomId}`);
    }

    leaveChatRoom() {
        if (this.state.chat.currentRoomId) {
            console.log(`Left chat room: ${this.state.chat.currentRoomId}`);
            this.state.chat.currentRoomId = null;
        }
        const messageInput = document.getElementById('chatMessageInput');
        const sendBtn = document.getElementById('chatSendBtn');
        if (messageInput && sendBtn) {
            messageInput.disabled = true;
            sendBtn.disabled = true;
        }
    }

    sendChatMessage() {
        const messageInput = document.getElementById('chatMessageInput');
        if (!messageInput || !this.state.chat.currentRoomId) return;
        const content = messageInput.value.trim();
        if (!content) return;
        const message = {
            senderId: this.state.currentUser.id,
            roomId: this.state.chat.currentRoomId,
            content: content
        };
        this.state.chat.stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(message));
        messageInput.value = '';
    }

    handleChatMessage(messageData) {
        console.log('Received message:', messageData);
        this.displayChatMessage(messageData);
    }

    displayChatMessage(messageData) {
        const messagesContainer = document.getElementById('chatMessages');
        if (!messagesContainer) return;
        const welcomeMsg = messagesContainer.querySelector('.chat-welcome');
        if (welcomeMsg) {
            welcomeMsg.remove();
        }
        const messageElement = document.createElement('div');
        const isOwnMessage = messageData.senderId === this.state.currentUser.id;
        messageElement.className = `message ${isOwnMessage ? 'own' : 'other'}`;
        messageElement.innerHTML = `
        <div class="message-bubble">
            <div class="message-info">
                <span class="sender-name">${messageData.senderName}</span>
                <span class="message-time">${new Date(messageData.timestamp).toLocaleTimeString()}</span>
            </div>
            <div class="message-content">${this.escapeHtml(messageData.content)}</div>
        </div>
    `;
        messagesContainer.appendChild(messageElement);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    sendTypingIndicator(isTyping) {
        if (!this.state.chat.currentRoomId || !this.state.chat.connected) return;
        const typingData = {
            userId: this.state.currentUser.id,
            userName: this.state.currentUser.name,
            roomId: this.state.chat.currentRoomId,
            isTyping: isTyping
        };
        this.state.chat.stompClient.send('/app/chat.typing', {}, JSON.stringify(typingData));
    }

    handleTypingIndicator(typingData) {
        if (typingData.userId === this.state.currentUser.id) return;
        console.log('⌨Typing indicator:', typingData);
    }

    async loadRoomMessages(roomId) {
        try {
            const response = await fetch(
                `/api/chat/rooms/${roomId}/messages/recent?userId=${this.state.currentUser.id}&limit=20`
            );
            const messages = await response.json();
            const messagesContainer = document.getElementById('chatMessages');
            if (messagesContainer) {
                messagesContainer.innerHTML = '';
            }
            messages.forEach(message => this.displayChatMessage(message));
        } catch (error) {
            console.error('Error loading room messages:', error);
        }
    }

    handleChatError(errorData) {
        console.error('Chat error:', errorData);
        NotificationService.showError(`Chat Error: ${errorData.error}`);
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
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
        console.log('Initializing Dashboard Chat...');
        this.currentUserId = DashboardState.currentUser?.id || 1;
        this.currentUserName = DashboardState.currentUser?.name || 'User';
        await this.loadChatRooms();
        this.connectWebSocket();
        console.log('Dashboard Chat initialized');
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
                console.log('WebSocket connected:', frame);
                this.connected = true;
                this.updateConnectionStatus();
            }, (error) => {
                console.error('WebSocket connection failed:', error);
                this.connected = false;
                this.updateConnectionStatus();
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

let stompClient = null;
let currentRoomId = null;

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
    }, function (error) {
        console.error('WebSocket error:', error);
        setTimeout(connectWebSocket, 5000);
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

document.addEventListener('DOMContentLoaded', function () {
    setTimeout(() => {
        dashboardChat = new DashboardChat();
    }, 1000);
});

let dashboard;

document.addEventListener('DOMContentLoaded', async () => {
    console.log('DOM loaded, initializing dashboard...');
    try {
        dashboard = new Dashboard();
        window.dashboard = dashboard;
        await dashboard.initialize();
    } catch (error) {
        console.error('Fatal error initializing dashboard:', error);
        NotificationService.showError('Failed to initialize dashboard. Please refresh the page.');
    }
});

function changeMonth(direction) {
    if (dashboard) {
        dashboard.changeMonth(direction);
    }
}

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

if (typeof module !== 'undefined' && module.exports) {
    module.exports = { Dashboard, DashboardState, DashboardConfig };
}