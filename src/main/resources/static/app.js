// Global variables
let currentUser = null;
let isAuthenticated = false;

// Initialize the app when page loads
document.addEventListener('DOMContentLoaded', function() {
    console.log('Friend Availability App loaded');
    checkAuthStatus(); // Check authentication on load
    setupEventListeners();
    loadUsers(); // Load users on startup
});

// Check authentication status
async function checkAuthStatus() {
    try {
        const response = await fetch('/api/auth/status', {
            method: 'GET',
            credentials: 'include', // IMPORTANT: Always include credentials
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const status = await response.json();
            console.log('Auth Status:', status);

            isAuthenticated = status.loggedIn;
            updateUIBasedOnAuth(status);

            // If authenticated, get full user info
            if (isAuthenticated) {
                await getUserInfo();
            }
        }
    } catch (error) {
        console.error('Error checking auth status:', error);
    }
}

// Get current user information
async function getUserInfo() {
    try {
        const response = await fetch('/api/auth/user', {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const userInfo = await response.json();
            console.log('User Info:', userInfo);
            currentUser = userInfo;
            displayUserInfo(userInfo);
        }
    } catch (error) {
        console.error('Error getting user info:', error);
    }
}

// Update UI based on authentication status
function updateUIBasedOnAuth(status) {
    const authStatusDiv = document.getElementById('authStatus') || createAuthStatusDiv();

    if (status.loggedIn) {
        authStatusDiv.innerHTML = `
            <div class="auth-info">
                <span>Logged in as: <strong>${status.name || 'User'}</strong></span>
                <button onclick="handleLogout()" class="btn btn-secondary">Logout</button>
            </div>
        `;
    } else {
        authStatusDiv.innerHTML = `
            <div class="auth-info">
                <span>Not logged in</span>
                <button onclick="handleLogin()" class="btn btn-primary">Login with Google</button>
            </div>
        `;
    }
}

// Create auth status div if it doesn't exist
function createAuthStatusDiv() {
    const header = document.querySelector('header');
    const authStatusDiv = document.createElement('div');
    authStatusDiv.id = 'authStatus';
    authStatusDiv.className = 'auth-status';
    header.appendChild(authStatusDiv);
    return authStatusDiv;
}

// Display user information
function displayUserInfo(userInfo) {
    if (userInfo.authenticated) {
        console.log('Authenticated user:', userInfo.name);
        // You can add more UI updates here
    }
}

// Handle login
function handleLogin() {
    window.location.href = '/oauth2/authorization/google';
}

// Handle logout
async function handleLogout() {
    try {
        const response = await fetch('/logout', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            }
        });

        if (response.ok || response.redirected) {
            window.location.href = '/index.html?logout=success';
        }
    } catch (error) {
        console.error('Error during logout:', error);
    }
}

// Set up all event listeners
function setupEventListeners() {
    // Form submissions
    document.getElementById('createUserForm').addEventListener('submit', handleCreateUser);
    document.getElementById('sendRequestForm').addEventListener('submit', handleSendRequest);

    console.log('Event listeners set up');
}

// Navigation function to show/hide sections
function showSection(sectionName) {
    // Hide all sections
    const sections = document.querySelectorAll('.section');
    sections.forEach(section => section.classList.remove('active'));

    // Remove active class from all nav buttons
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => btn.classList.remove('active'));

    // Show selected section
    document.getElementById(sectionName).classList.add('active');

    // Highlight active nav button
    event.target.classList.add('active');

    console.log('Switched to section:', sectionName);
}

// ============================================
// USER MANAGEMENT FUNCTIONS
// ============================================

// Load all users from API
async function loadUsers() {
    console.log('Loading users...');
    const usersList = document.getElementById('usersList');
    usersList.innerHTML = '<div class="loading">Loading users...</div>';

    try {
        const response = await fetch('/api/users', {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const users = await response.json();
        console.log('Users loaded:', users);
        displayUsers(users);

    } catch (error) {
        console.error('Error loading users:', error);
        usersList.innerHTML = '<div class="error">Failed to load users. Is the server running?</div>';
    }
}

// Display users in the UI
function displayUsers(users) {
    const usersList = document.getElementById('usersList');

    if (users.length === 0) {
        usersList.innerHTML = '<div class="loading">No users found. Create the first user!</div>';
        return;
    }

    const usersHTML = users.map(user => `
        <div class="user-item">
            <h4>${user.name}</h4>
            <p><strong>ID:</strong> ${user.id}</p>
            <p><strong>Email:</strong> ${user.email}</p>
            <p><strong>Google ID:</strong> ${user.googleId || 'Not set'}</p>
        </div>
    `).join('');

    usersList.innerHTML = usersHTML;
}

// Handle create user form submission
async function handleCreateUser(event) {
    event.preventDefault();

    const name = document.getElementById('userName').value;
    const email = document.getElementById('userEmail').value;

    console.log('Creating user:', { name, email });

    try {
        const response = await fetch('/api/users', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ name, email }),
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const newUser = await response.json();
        console.log('User created:', newUser);

        // Show success message
        showMessage('User created successfully!', 'success');

        // Clear form
        document.getElementById('createUserForm').reset();

        // Reload users list
        loadUsers();

    } catch (error) {
        console.error('Error creating user:', error);
        showMessage('Failed to create user. Please try again.', 'error');
    }
}

// ============================================
// FRIEND MANAGEMENT FUNCTIONS
// ============================================

// Handle send friend request form submission
async function handleSendRequest(event) {
    event.preventDefault();

    if (!isAuthenticated) {
        showMessage('Please login to send friend requests', 'error');
        return;
    }

    const fromUserId = document.getElementById('fromUserId').value;
    const toUserId = document.getElementById('toUserId').value;

    console.log('Sending friend request:', { fromUserId, toUserId });

    try {
        const response = await fetch(`/api/friends/request?fromUserId=${fromUserId}&toUserId=${toUserId}`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        const friendship = await response.json();
        console.log('Friend request sent:', friendship);

        showMessage('Friend request sent successfully!', 'success');
        document.getElementById('sendRequestForm').reset();

    } catch (error) {
        console.error('Error sending friend request:', error);
        showMessage(`Failed to send friend request: ${error.message}`, 'error');
    }
}

// Load all friend data for a specific user
async function loadFriendData() {
    if (!isAuthenticated) {
        showMessage('Please login to view friend data', 'error');
        return;
    }

    const userId = document.getElementById('currentUserId').value;

    if (!userId) {
        showMessage('Please enter your User ID first', 'error');
        return;
    }

    console.log('Loading friend data for user:', userId);
    const friendsData = document.getElementById('friendsData');
    friendsData.innerHTML = '<div class="loading">Loading your friends and requests...</div>';

    try {
        // Load friends and pending requests in parallel
        const [friendsResponse, requestsResponse] = await Promise.all([
            fetch(`/api/friends/${userId}`, {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            }),
            fetch(`/api/friends/${userId}/pending`, {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            })
        ]);

        if (!friendsResponse.ok || !requestsResponse.ok) {
            throw new Error('Failed to load friend data');
        }

        const friends = await friendsResponse.json();
        const pendingRequests = await requestsResponse.json();

        console.log('Friends:', friends);
        console.log('Pending requests:', pendingRequests);

        displayFriendData(friends, pendingRequests);

    } catch (error) {
        console.error('Error loading friend data:', error);
        friendsData.innerHTML = '<div class="error">Failed to load friend data. Please login first.</div>';
    }
}

// Display friends and pending requests
function displayFriendData(friends, pendingRequests) {
    const friendsData = document.getElementById('friendsData');

    let html = '';

    // Display friends
    html += '<h4>Your Friends</h4>';
    if (friends.length === 0) {
        html += '<p class="loading">No friends yet. Send some friend requests!</p>';
    } else {
        friends.forEach(friend => {
            html += `
                <div class="friend-item">
                    <h4>${friend.name}</h4>
                    <p><strong>Email:</strong> ${friend.email}</p>
                    <p><strong>ID:</strong> ${friend.id}</p>
                </div>
            `;
        });
    }

    // Display pending requests
    html += '<h4 style="margin-top: 30px;">Pending Friend Requests</h4>';
    if (pendingRequests.length === 0) {
        html += '<p class="loading">No pending requests</p>';
    } else {
        pendingRequests.forEach(request => {
            html += `
                <div class="request-item">
                    <h4>Friend Request</h4>
                    <p><strong>From User ID:</strong> ${request.userId}</p>
                    <p><strong>Status:</strong> <span class="status-badge status-pending">${request.status}</span></p>
                    <p><strong>Received:</strong> ${new Date(request.createdAt).toLocaleDateString()}</p>
                    <div style="margin-top: 10px;">
                        <button class="btn btn-primary" onclick="respondToRequest(${request.id}, 'accept')">
                            Accept
                        </button>
                        <button class="btn btn-secondary" onclick="respondToRequest(${request.id}, 'reject')" style="margin-left: 10px;">
                            Reject
                        </button>
                    </div>
                </div>
            `;
        });
    }

    friendsData.innerHTML = html;
}

// Respond to friend request (accept or reject)
async function respondToRequest(friendshipId, action) {
    if (!isAuthenticated) {
        showMessage('Please login to respond to friend requests', 'error');
        return;
    }

    const userId = document.getElementById('currentUserId').value;

    if (!userId) {
        showMessage('Please enter your User ID first', 'error');
        return;
    }

    console.log(`${action}ing friend request:`, { friendshipId, userId });

    try {
        const response = await fetch(`/api/friends/${friendshipId}/${action}?userId=${userId}`, {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        const result = await response.json();
        console.log(`Friend request ${action}ed:`, result);

        showMessage(`Friend request ${action}ed successfully!`, 'success');

        // Reload friend data to show updated state
        loadFriendData();

    } catch (error) {
        console.error(`Error ${action}ing friend request:`, error);
        showMessage(`Failed to ${action} friend request: ${error.message}`, 'error');
    }
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

// Show success/error messages to user
function showMessage(message, type) {
    // Remove existing messages
    const existingMessages = document.querySelectorAll('.success, .error');
    existingMessages.forEach(msg => msg.remove());

    // Create new message element
    const messageDiv = document.createElement('div');
    messageDiv.className = type;
    messageDiv.textContent = message;

    // Insert at top of current section
    const activeSection = document.querySelector('.section.active');
    activeSection.insertBefore(messageDiv, activeSection.firstChild);

    // Auto-remove after 5 seconds
    setTimeout(() => {
        messageDiv.remove();
    }, 5000);
}

// Format date for display
function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Debug function to log API responses
async function debugAPI() {
    console.log('=== API Debug Information ===');
    console.log('Base URL:', window.location.origin);
    console.log('Users endpoint:', '/api/users');
    console.log('Friends endpoint:', '/api/friends');
    console.log('Auth endpoint:', '/api/auth');

    // Test auth status
    try {
        const authResponse = await fetch('/api/auth/status', {
            credentials: 'include',
            headers: { 'Accept': 'application/json' }
        });
        const authStatus = await authResponse.json();
        console.log('Auth Status Response:', authStatus);

        // Also check debug endpoint
        const debugResponse = await fetch('/api/auth/debug', {
            credentials: 'include',
            headers: { 'Accept': 'application/json' }
        });
        const debugInfo = await debugResponse.json();
        console.log('Debug Info:', debugInfo);
    } catch (error) {
        console.log('Auth Debug Error:', error);
    }

    // Test if API is reachable
    fetch('/api/users', {
        credentials: 'include',
        headers: { 'Accept': 'application/json' }
    })
        .then(response => {
            console.log('API Status:', response.ok ? 'Working' : 'Error');
            console.log('Response status:', response.status);
        })
        .catch(error => {
            console.log('API Error:', error);
        });
}

// Check for login/logout success messages in URL
const urlParams = new URLSearchParams(window.location.search);
if (urlParams.get('login') === 'success') {
    showMessage('Successfully logged in with Google!', 'success');
    // Clean URL
    window.history.replaceState({}, document.title, window.location.pathname);
}
if (urlParams.get('logout') === 'success') {
    showMessage('Successfully logged out!', 'success');
    // Clean URL
    window.history.replaceState({}, document.title, window.location.pathname);
}

// Run debug on page load
debugAPI();