let currentUser = null;

document.addEventListener('DOMContentLoaded', function() {
    console.log('Friend Availability App loaded');
    setupEventListeners();
    loadUsers();
});

function setupEventListeners() {
    document.getElementById('createUserForm').addEventListener('submit', handleCreateUser);
    document.getElementById('sendRequestForm').addEventListener('submit', handleSendRequest);

    console.log('Event listeners set up');
}

function showSection(sectionName) {
    const sections = document.querySelectorAll('.section');
    sections.forEach(section => section.classList.remove('active'));

    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => btn.classList.remove('active'));

    document.getElementById(sectionName).classList.add('active');

    event.target.classList.add('active');

    console.log('Switched to section:', sectionName);
}

async function loadUsers() {
    console.log('Loading users...');
    const usersList = document.getElementById('usersList');
    usersList.innerHTML = '<div class="loading">Loading users...</div>';

    try {
        const response = await fetch('/api/users', {
            credentials: 'include'
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
            <button class="delete-user-btn" data-userid="${user.id}">Delete User</button>
        </div>
    `).join('');

    usersList.innerHTML = usersHTML;

    document.querySelectorAll('.delete-user-btn').forEach(button => {
        button.addEventListener('click', () => {
            const userId = button.dataset.userid;
            if (confirm(`Are you sure you want to delete this user?`)) {
                deleteUser(userId);
            }
        });
    });

}

async function handleCreateUser(event) {
    event.preventDefault();

    const name = document.getElementById('userName').value;
    const email = document.getElementById('userEmail').value;

    console.log('Creating user:', { name, email });

    try {
        const response = await fetch('/api/users', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ name, email }),
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const newUser = await response.json();
        console.log('User created:', newUser);

        showMessage('User created successfully!', 'success');

        document.getElementById('createUserForm').reset();

        loadUsers();

    } catch (error) {
        console.error('Error creating user:', error);
        showMessage('Failed to create user. Please try again.', 'error');
    }
}

async function deleteUser(userId){

    console.log("Deleting user with id: " + userId);

    try{
        await fetch(`/api/friends/${userId}/all`, {
            method: 'DELETE',
        });

        const response = await fetch(`/api/users/${userId}`,{
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        showMessage('User deleted successfully!', 'success');
        loadUsers();

    }catch(error){
        console.error('Error deleting user:', error);
        showMessage('Failed to delete user. Please try again.', 'error');
    }
}

async function handleSendRequest(event) {
    event.preventDefault();

    const fromUserId = document.getElementById('fromUserId').value;
    const toUserId = document.getElementById('toUserId').value;

    console.log('Sending friend request:', { fromUserId, toUserId });

    try {
        const response = await fetch(`/api/friends/request?fromUserId=${fromUserId}&toUserId=${toUserId}`, {
            method: 'POST',
            credentials: 'include'
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

async function loadFriendData() {
    const userId = document.getElementById('currentUserId').value;

    if (!userId) {
        showMessage('Please enter your User ID first', 'error');
        return;
    }

    console.log('Loading friend data for user:', userId);
    const friendsData = document.getElementById('friendsData');
    friendsData.innerHTML = '<div class="loading">Loading your friends and requests...</div>';

    try {
        const [friendsResponse, requestsResponse] = await Promise.all([
            fetch(`/api/friends/${userId}`, { credentials: 'include' }),
            fetch(`/api/friends/${userId}/pending`, { credentials: 'include' })
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
        friendsData.innerHTML = '<div class="error">Failed to load friend data</div>';
    }
}

async function removeFriendship(userId1, userId2) {
    console.log("Removing friendship between users with id: " + userId1 + " and " + userId2);

    try {
        const response = await fetch(`api/friends/remove?userId1=${userId1}&userId2=${userId2}`, {
            method: 'DELETE',
        });

        if (!response.ok) {
            throw new Error('Failed to remove friendship');
        }

        const result = await response.text();
        console.log(result);
        await loadFriendData();
        return result;

    } catch (error) {
        console.error("Error removing friendship:", error);
        throw error;
    }
}

function getCurrentUserId() {
    return document.getElementById('currentUserId')?.value || null;
}

function displayFriendData(friends, pendingRequests) {
    const friendsData = document.getElementById('friendsData');
    const currentUserId = getCurrentUserId();

    let html = '';

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
                    <button class="remove-friendship-btn" 
                            data-userid1="${currentUserId}" 
                            data-userid2="${friend.id}">
                            Remove Friend
                    </button>
                </div>
            `;
        });
    }

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

    document.querySelectorAll('.remove-friendship-btn').forEach(button => {
        button.addEventListener('click', () => {
            const userId1 = button.getAttribute('data-userid1');
            const userId2 = button.getAttribute('data-userid2');

            if (confirm(`Are you sure you want to delete this friendship?`)) {
                removeFriendship(userId1, userId2);
            }
        });
    });

}

async function respondToRequest(friendshipId, action) {
    const userId = document.getElementById('currentUserId').value;

    if (!userId) {
        showMessage('Please enter your User ID first', 'error');
        return;
    }

    console.log(`${action}ing friend request:`, { friendshipId, userId });

    try {
        const response = await fetch(`/api/friends/${friendshipId}/${action}?userId=${userId}`, {
            method: 'PUT',
            credentials: 'include'
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        const result = await response.json();
        console.log(`Friend request ${action}ed:`, result);

        showMessage(`Friend request ${action}ed successfully!`, 'success');

        loadFriendData();

    } catch (error) {
        console.error(`Error ${action}ing friend request:`, error);
        showMessage(`Failed to ${action} friend request: ${error.message}`, 'error');
    }
}

function showMessage(message, type) {
    const existingMessages = document.querySelectorAll('.success, .error');
    existingMessages.forEach(msg => msg.remove());

    const messageDiv = document.createElement('div');
    messageDiv.className = type;
    messageDiv.textContent = message;

    const activeSection = document.querySelector('.section.active');
    activeSection.insertBefore(messageDiv, activeSection.firstChild);

    setTimeout(() => {
        messageDiv.remove();
    }, 5000);
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function debugAPI() {
    console.log('=== API Debug Information ===');
    console.log('Base URL:', window.location.origin);
    console.log('Users endpoint:', '/api/users');
    console.log('Friends endpoint:', '/api/friends');

    fetch('/api/users', { credentials: 'include' })
        .then(response => {
            console.log('API Status:', response.ok ? 'Working' : 'Error');
            console.log('Response status:', response.status);
        })
        .catch(error => {
            console.log('API Error:', error);
        });
}

debugAPI();