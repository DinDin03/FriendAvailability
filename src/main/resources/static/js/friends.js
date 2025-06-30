
// State management for friends page
let friendsState = {
    currentUser: null,
    friends: [],
    pendingRequests: [],
    allUsers: []
};

// Initialize friends page when DOM is loaded
document.addEventListener('DOMContentLoaded', async function() {
    console.log('Friends page loaded');
    await initializeFriendsPage();
});

/**
 * Initialize the friends page
 * 1. Load current user
 * 2. Load friends list
 * 3. Load pending requests
 * 4. Load all users for friend discovery
 */
async function initializeFriendsPage() {
    try {
        // Show loading state
        showLoadingState();

        // Load current user first
        await loadCurrentUser();

        // Load all friend-related data in parallel for better performance
        await Promise.all([
            loadFriends(),
            loadPendingRequests(),
            loadAllUsers()
        ]);

        // Set up form handlers
        setupEventHandlers();

        console.log('Friends page initialized successfully');

    } catch (error) {
        console.error('Failed to initialize friends page:', error);
        showMessage('Failed to load friends page', 'error');
    }
}

/**
 * Load current authenticated user
 */
async function loadCurrentUser() {
    try {
        const user = await apiCall('/api/auth/current-user');
        friendsState.currentUser = user;
        console.log('Current user loaded:', user.name);

    } catch (error) {
        console.error('Failed to load current user:', error);
        throw error;
    }
}

/**
 * Load friends list - THIS IS THE KEY FUNCTION FOR AUTO-REFRESH
 */
async function loadFriends() {
    try {
        console.log(`Loading friends for user ${friendsState.currentUser.id}`);

        const friends = await apiCall(`/api/friends/${friendsState.currentUser.id}`);
        friendsState.friends = friends;

        console.log(`Loaded ${friends.length} friends:`, friends);

        // Update the UI immediately
        renderFriendsSection();

    } catch (error) {
        console.error('Failed to load friends:', error);
        document.getElementById('friendsContainer').innerHTML =
            '<div class="error">Failed to load friends</div>';
    }
}

/**
 * Load pending friend requests
 */
async function loadPendingRequests() {
    try {
        const requests = await apiCall(`/api/friends/${friendsState.currentUser.id}/pending`);
        friendsState.pendingRequests = requests;

        console.log(`Loaded ${requests.length} pending requests`);

        // Update the UI
        renderPendingRequestsSection();

    } catch (error) {
        console.error('Failed to load pending requests:', error);
    }
}

/**
 * Load all users for friend discovery
 */
async function loadAllUsers() {
    try {
        const users = await apiCall('/api/users');
        // Filter out current user and existing friends
        friendsState.allUsers = users.filter(user =>
            user.id !== friendsState.currentUser.id &&
            !friendsState.friends.some(friend => friend.id === user.id)
        );

        renderAllUsersSection();

    } catch (error) {
        console.error('Failed to load all users:', error);
    }
}

/**
 * REFRESH FUNCTION - Called by the "Refresh" button
 * This is what fixes your manual refresh issue!
 */
async function refreshFriends() {
    console.log('Refreshing friends data...');

    try {
        // Show loading indicator
        document.getElementById('friendsContainer').innerHTML =
            '<div class="loading">Refreshing...</div>';

        // Reload all data
        await Promise.all([
            loadFriends(),
            loadPendingRequests(),
            loadAllUsers()
        ]);

        showMessage('Friends list refreshed!', 'success');

    } catch (error) {
        console.error('Failed to refresh friends:', error);
        showMessage('Failed to refresh friends list', 'error');
    }
}

/**
 * Render the friends section
 */
function renderFriendsSection() {
    const container = document.getElementById('friendsContainer');

    if (friendsState.friends.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <h4>No friends yet</h4>
                <p>Send some friend requests to get started!</p>
            </div>
        `;
        return;
    }

    let html = '';
    friendsState.friends.forEach(friend => {
        html += `
            <div class="friend-card">
                <div class="friend-info">
                    <img src="${friend.profilePictureUrl || generateAvatar(friend.name)}" 
                         alt="${friend.name}" class="friend-avatar">
                    <div class="friend-details">
                        <h4>${friend.name}</h4>
                        <p class="friend-email">${friend.email}</p>
                        <span class="friend-id">ID: ${friend.id}</span>
                    </div>
                </div>
                <div class="friend-actions">
                    <button onclick="removeFriend(${friend.id})" 
                            class="btn btn-danger btn-small">
                        Remove Friend
                    </button>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

/**
 * Render pending requests section
 */
function renderPendingRequestsSection() {
    // Implementation for pending requests
    // Add this section if you have a specific container for it
}

/**
 * Render all users section for discovery
 */
function renderAllUsersSection() {
    const container = document.getElementById('allUsersContainer');

    if (!container) return;

    if (friendsState.allUsers.length === 0) {
        container.innerHTML = '<div class="loading">No new users to discover</div>';
        return;
    }

    let html = '';
    friendsState.allUsers.forEach(user => {
        html += `
            <div class="user-card">
                <div class="user-info">
                    <img src="${user.profilePictureUrl || generateAvatar(user.name)}" 
                         alt="${user.name}" class="user-avatar">
                    <div class="user-details">
                        <h4>${user.name}</h4>
                        <p>${user.email}</p>
                    </div>
                </div>
                <button onclick="sendFriendRequest(${user.id})" 
                        class="btn btn-primary btn-small">
                    Send Request
                </button>
            </div>
        `;
    });

    container.innerHTML = html;
}

/**
 * Send a friend request with auto-refresh
 */
async function sendFriendRequest(toUserId) {
    try {
        await apiCall(`/api/friends/request?fromUserId=${friendsState.currentUser.id}&toUserId=${toUserId}`, {
            method: 'POST'
        });

        showMessage('Friend request sent!', 'success');

        // AUTO-REFRESH: Reload data to show updated state
        await loadAllUsers(); // Remove the user from available list

    } catch (error) {
        console.error('Failed to send friend request:', error);
        showMessage('Failed to send friend request', 'error');
    }
}

/**
 * Accept a friend request with auto-refresh
 */
async function acceptFriendRequest(requestId) {
    try {
        await apiCall(`/api/friends/${requestId}/accept?userId=${friendsState.currentUser.id}`, {
            method: 'PUT'
        });

        showMessage('Friend request accepted!', 'success');

        // AUTO-REFRESH: Reload all data to show new friend
        await Promise.all([
            loadFriends(),
            loadPendingRequests(),
            loadAllUsers()
        ]);

    } catch (error) {
        console.error('Failed to accept friend request:', error);
        showMessage('Failed to accept friend request', 'error');
    }
}

/**
 * Remove a friend with auto-refresh
 */
async function removeFriend(friendId) {
    if (!confirm('Are you sure you want to remove this friend?')) {
        return;
    }

    try {
        await apiCall(`/api/friends/remove?userId1=${friendsState.currentUser.id}&userId2=${friendId}`, {
            method: 'DELETE'
        });

        showMessage('Friend removed', 'info');

        // AUTO-REFRESH: Reload data to show updated state
        await Promise.all([
            loadFriends(),
            loadAllUsers()
        ]);

    } catch (error) {
        console.error('Failed to remove friend:', error);
        showMessage('Failed to remove friend', 'error');
    }
}

/**
 * Set up event handlers for forms
 */
function setupEventHandlers() {
    // Handle send friend request form
    const sendRequestForm = document.getElementById('sendRequestForm');
    if (sendRequestForm) {
        sendRequestForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const toUserId = document.getElementById('toUserId').value;
            await sendFriendRequest(parseInt(toUserId));
            sendRequestForm.reset();
        });
    }
}

/**
 * Utility Functions
 */

// API call helper
async function apiCall(endpoint, options = {}) {
    const response = await fetch(endpoint, {
        ...options,
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            ...options.headers
        }
    });

    if (!response.ok) {
        throw new Error(`API call failed: ${response.status}`);
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        return await response.json();
    }
    return await response.text();
}

// Generate avatar placeholder
function generateAvatar(name) {
    const initials = name.split(' ').map(n => n[0]).join('').toUpperCase();
    return `data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40"><rect width="40" height="40" fill="%23007bff"/><text x="20" y="25" text-anchor="middle" fill="white" font-family="Arial" font-size="16">${initials}</text></svg>`;
}

// Show loading state
function showLoadingState() {
    const containers = ['friendsContainer', 'allUsersContainer'];
    containers.forEach(id => {
        const container = document.getElementById(id);
        if (container) {
            container.innerHTML = '<div class="loading">Loading...</div>';
        }
    });
}

// Show success/error messages
function showMessage(message, type) {
    // Remove existing messages
    const existingMessages = document.querySelectorAll('.message');
    existingMessages.forEach(msg => msg.remove());

    // Create new message element
    const messageDiv = document.createElement('div');
    messageDiv.className = `message message-${type}`;
    messageDiv.textContent = message;

    // Insert at top of page
    const container = document.getElementById('messageContainer') || document.body;
    container.insertBefore(messageDiv, container.firstChild);

    // Auto-remove after 3 seconds
    setTimeout(() => {
        messageDiv.remove();
    }, 3000);
}

// Export functions for global access (needed for onclick handlers)
window.refreshFriends = refreshFriends;
window.sendFriendRequest = sendFriendRequest;
window.acceptFriendRequest = acceptFriendRequest;
window.removeFriend = removeFriend;