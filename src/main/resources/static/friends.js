/**
 * Friends Management JavaScript
 * Handles all friend-related functionality including:
 * - Loading friends list
 * - Managing friend requests
 * - Sending new requests
 * - User search and discovery
 */

// Global variables to store application state
let currentUser = null;
let allUsers = [];
let userFriends = [];
let pendingRequests = [];

/**
 * INITIALIZATION
 * This runs when the page loads
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('Friends page loaded - initializing...');

    // Check if user is authenticated
    checkAuthentication()
        .then(() => {
            // If authenticated, load all friend data
            initializePage();
        })
        .catch(error => {
            console.error('Authentication failed:', error);
            // Redirect to login page if not authenticated
            window.location.href = '/index.html';
        });
});

/**
 * CHECK AUTHENTICATION
 * Verifies user is logged in by calling backend
 */
async function checkAuthentication() {
    try {
        console.log('Checking user authentication...');

        // Call your backend to get current user info
        const response = await fetch('/api/users/current', {
            method: 'GET',
            credentials: 'include', // Include session cookies
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Authentication failed: ${response.status}`);
        }

        currentUser = await response.json();
        console.log('User authenticated:', currentUser);

        // Update UI with user info
        updateUserDisplay();

        return currentUser;
    } catch (error) {
        console.error('Authentication check failed:', error);
        throw error;
    }
}

/**
 * UPDATE USER DISPLAY
 * Updates the header with current user information
 */
function updateUserDisplay() {
    const userNameElement = document.getElementById('userName');
    const userAvatarElement = document.getElementById('userAvatar');
    const fromUserIdElement = document.getElementById('fromUserId');

    if (currentUser) {
        userNameElement.textContent = currentUser.name || 'User';
        userAvatarElement.src = currentUser.profilePictureUrl || 'https://via.placeholder.com/32';
        fromUserIdElement.value = currentUser.id;

        console.log('User display updated for:', currentUser.name);
    }
}

/**
 * INITIALIZE PAGE
 * Loads all necessary data and sets up event listeners
 */
async function initializePage() {
    console.log('Initializing friends page...');

    try {
        // Show loading state
        showLoadingOverlay('Loading your friends data...');

        // Load all data in parallel for better performance
        await Promise.all([
            loadUserFriends(),
            loadPendingRequests(),
            loadAllUsers()
        ]);

        // Update statistics
        updateStatistics();

        // Set up event listeners
        setupEventListeners();

        console.log('Friends page initialization complete');

    } catch (error) {
        console.error('Failed to initialize page:', error);
        showMessage('Failed to load friends data. Please refresh the page.', 'error');
    } finally {
        hideLoadingOverlay();
    }
}

/**
 * LOAD USER FRIENDS
 * Fetches the current user's friends from backend
 */
async function loadUserFriends() {
    console.log('Loading user friends...');

    try {
        // Call your FriendController GET /api/friends/{userId} endpoint
        const response = await fetch(`/api/friends/${currentUser.id}`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load friends: ${response.status}`);
        }

        userFriends = await response.json();
        console.log('Loaded friends:', userFriends);

        // Update the friends display
        displayFriends(userFriends);

        return userFriends;

    } catch (error) {
        console.error('Error loading friends:', error);
        document.getElementById('friendsContainer').innerHTML =
            '<div class="error">Failed to load friends</div>';
        throw error;
    }
}

/**
 * LOAD PENDING REQUESTS
 * Fetches pending friend requests for current user
 */
async function loadPendingRequests() {
    console.log('Loading pending requests...');

    try {
        // Call your FriendController GET /api/friends/{userId}/pending endpoint
        const response = await fetch(`/api/friends/${currentUser.id}/pending`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load requests: ${response.status}`);
        }

        pendingRequests = await response.json();
        console.log('Loaded pending requests:', pendingRequests);

        // Update the requests display
        displayRequests(pendingRequests);

        return pendingRequests;

    } catch (error) {
        console.error('Error loading pending requests:', error);
        document.getElementById('requestsContainer').innerHTML =
            '<div class="error">Failed to load requests</div>';
        throw error;
    }
}

/**
 * LOAD ALL USERS
 * Fetches all users for friend discovery
 */
async function loadAllUsers() {
    console.log('Loading all users...');

    try {
        // Call your UserController GET /api/users endpoint
        const response = await fetch('/api/users', {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load users: ${response.status}`);
        }

        allUsers = await response.json();
        console.log('Loaded all users:', allUsers.length);

        // Filter out current user and existing friends
        const availableUsers = filterAvailableUsers(allUsers);

        // Update the users display
        displayAllUsers(availableUsers);

        return allUsers;

    } catch (error) {
        console.error('Error loading all users:', error);
        document.getElementById('allUsersContainer').innerHTML =
            '<div class="error">Failed to load users</div>';
        throw error;
    }
}

/**
 * FILTER AVAILABLE USERS
 * Removes current user and existing friends from user list
 */
function filterAvailableUsers(users) {
    const friendIds = userFriends.map(friend => friend.id);
    const currentUserId = currentUser.id;

    return users.filter(user =>
        user.id !== currentUserId &&
        !friendIds.includes(user.id)
    );
}

/**
 * DISPLAY FUNCTIONS
 * These functions update the UI with data from backend
 */

function displayFriends(friends) {
    const container = document.getElementById('friendsContainer');

    if (friends.length === 0) {
        container.innerHTML = '<div class="empty-state">No friends yet. Send some friend requests!</div>';
        return;
    }

    const friendsHTML = friends.map(friend => `
        <div class="friend-card">
            <div class="friend-avatar">
                <img src="${friend.profilePictureUrl || 'https://via.placeholder.com/50'}" alt="${friend.name}">
            </div>
            <div class="friend-info">
                <h4>${friend.name}</h4>
                <p class="friend-email">${friend.email}</p>
                <p class="friend-id">ID: ${friend.id}</p>
            </div>
            <div class="friend-actions">
                <button onclick="viewFriendAvailability(${friend.id})" class="btn btn-secondary btn-small">
                    View Calendar
                </button>
                <button onclick="removeFriend(${friend.id})" class="btn btn-danger btn-small">
                    Remove Friend
                </button>
            </div>
        </div>
    `).join('');

    container.innerHTML = friendsHTML;
}

function displayRequests(requests) {
    const container = document.getElementById('requestsContainer');

    if (requests.length === 0) {
        container.innerHTML = '<div class="empty-state">No pending friend requests</div>';
        return;
    }

    const requestsHTML = requests.map(request => `
        <div class="request-card">
            <div class="request-info">
                <h4>Friend Request</h4>
                <p><strong>From User ID:</strong> ${request.userId}</p>
                <p><strong>Status:</strong> <span class="status-badge status-${request.status.toLowerCase()}">${request.status}</span></p>
                <p><strong>Received:</strong> ${formatDate(request.createdAt)}</p>
            </div>
            <div class="request-actions">
                <button onclick="respondToRequest(${request.id}, 'accept')" class="btn btn-primary btn-small">
                    Accept
                </button>
                <button onclick="respondToRequest(${request.id}, 'reject')" class="btn btn-secondary btn-small">
                    Reject
                </button>
            </div>
        </div>
    `).join('');

    container.innerHTML = requestsHTML;
}

function displayAllUsers(users) {
    const container = document.getElementById('allUsersContainer');

    if (users.length === 0) {
        container.innerHTML = '<div class="empty-state">All users are already your friends!</div>';
        return;
    }

    const usersHTML = users.slice(0, 10).map(user => `
        <div class="user-card">
            <div class="user-avatar">
                <img src="${user.profilePictureUrl || 'https://via.placeholder.com/40'}" alt="${user.name}">
            </div>
            <div class="user-info">
                <h4>${user.name}</h4>
                <p class="user-email">${user.email}</p>
                <p class="user-id">ID: ${user.id}</p>
            </div>
            <div class="user-actions">
                <button onclick="sendQuickRequest(${user.id})" class="btn btn-primary btn-small">
                    Send Request
                </button>
            </div>
        </div>
    `).join('');

    container.innerHTML = usersHTML;
}

/**
 * FRIEND REQUEST FUNCTIONS
 * Handle sending, accepting, and rejecting friend requests
 */

async function sendFriendRequest(fromUserId, toUserId) {
    console.log(`Sending friend request from ${fromUserId} to ${toUserId}`);

    try {
        showLoadingOverlay('Sending friend request...');

        // Call your FriendController POST /api/friends/request endpoint
        const response = await fetch(`/api/friends/request?fromUserId=${fromUserId}&toUserId=${toUserId}`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Request failed: ${response.status}`);
        }

        const result = await response.json();
        console.log('Friend request sent:', result);

        showMessage('Friend request sent successfully!', 'success');

        // Refresh the data
        await loadAllUsers();
        updateStatistics();

    } catch (error) {
        console.error('Error sending friend request:', error);
        showMessage(`Failed to send friend request: ${error.message}`, 'error');
    } finally {
        hideLoadingOverlay();
    }
}

async function respondToRequest(requestId, action) {
    console.log(`${action}ing friend request ${requestId}`);

    try {
        showLoadingOverlay(`${action}ing friend request...`);

        // Call your FriendController PUT endpoints
        const response = await fetch(`/api/friends/${requestId}/${action}?userId=${currentUser.id}`, {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `${action} failed: ${response.status}`);
        }

        const result = await response.json();
        console.log(`Friend request ${action}ed:`, result);

        showMessage(`Friend request ${action}ed successfully!`, 'success');

        // Refresh data
        await Promise.all([
            loadPendingRequests(),
            loadUserFriends(),
            loadAllUsers()
        ]);
        updateStatistics();

    } catch (error) {
        console.error(`Error ${action}ing request:`, error);
        showMessage(`Failed to ${action} friend request: ${error.message}`, 'error');
    } finally {
        hideLoadingOverlay();
    }
}

/**
 * EVENT HANDLERS
 * Handle user interactions
 */

function setupEventListeners() {
    // Friend request form submission
    document.getElementById('sendRequestForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const fromUserId = document.getElementById('fromUserId').value;
        const toUserId = document.getElementById('toUserId').value;

        if (!fromUserId || !toUserId) {
            showMessage('Please fill in all fields', 'error');
            return;
        }

        await sendFriendRequest(parseInt(fromUserId), parseInt(toUserId));

        // Clear the form
        document.getElementById('toUserId').value = '';
    });

    // Search functionality
    document.getElementById('friendSearch').addEventListener('input', function(e) {
        const searchTerm = e.target.value.toLowerCase();
        filterFriendsDisplay(searchTerm);
    });
}

function filterFriendsDisplay(searchTerm) {
    const friendCards = document.querySelectorAll('.friend-card');

    friendCards.forEach(card => {
        const name = card.querySelector('h4').textContent.toLowerCase();
        const email = card.querySelector('.friend-email').textContent.toLowerCase();

        if (name.includes(searchTerm) || email.includes(searchTerm)) {
            card.style.display = 'block';
        } else {
            card.style.display = 'none';
        }
    });
}

/**
 * QUICK ACTION FUNCTIONS
 */

async function sendQuickRequest(toUserId) {
    await sendFriendRequest(currentUser.id, toUserId);
}

async function removeFriend(friendId) {
    if (!confirm('Are you sure you want to remove this friend?')) {
        return;
    }

    try {
        showLoadingOverlay('Removing friend...');

        // Call your FriendController DELETE endpoint
        const response = await fetch(`/api/friends/remove?userId1=${currentUser.id}&userId2=${friendId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error(`Failed to remove friend: ${response.status}`);
        }

        showMessage('Friend removed successfully', 'success');

        // Refresh data
        await Promise.all([
            loadUserFriends(),
            loadAllUsers()
        ]);
        updateStatistics();

    } catch (error) {
        console.error('Error removing friend:', error);
        showMessage('Failed to remove friend', 'error');
    } finally {
        hideLoadingOverlay();
    }
}

function viewFriendAvailability(friendId) {
    // Navigate to availability page with friend filter
    window.location.href = `availability.html?friendId=${friendId}`;
}

/**
 * REFRESH FUNCTIONS
 */

async function refreshFriends() {
    await loadUserFriends();
    updateStatistics();
    showMessage('Friends list refreshed', 'success');
}

async function refreshRequests() {
    await loadPendingRequests();
    updateStatistics();
    showMessage('Requests refreshed', 'success');
}

/**
 * STATISTICS UPDATE
 */

function updateStatistics() {
    document.getElementById('totalFriends').textContent = userFriends.length;
    document.getElementById('pendingRequests').textContent = pendingRequests.length;
    document.getElementById('sentRequests').textContent = '0'; // You can implement this
    document.getElementById('onlineFriends').textContent = '0'; // You can implement this
}

/**
 * UTILITY FUNCTIONS
 */

function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function logout() {
    window.location.href = '/logout';
}

// Make functions available globally for onclick handlers
window.respondToRequest = respondToRequest;
window.sendQuickRequest = sendQuickRequest;
window.removeFriend = removeFriend;
window.viewFriendAvailability = viewFriendAvailability;
window.refreshFriends = refreshFriends;
window.refreshRequests = refreshRequests;
window.logout = logout;