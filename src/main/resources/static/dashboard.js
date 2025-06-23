/**
 * Dashboard Management JavaScript
 *
 * PURPOSE: This file manages the main dashboard of the LinkUp application.
 * It's the central hub where authenticated users see their overview and navigate to other features.
 *
 * KEY BACKEND CONCEPTS DEMONSTRATED:
 * - Session management and authentication verification
 * - RESTful API integration patterns
 * - Error handling and user feedback
 * - Asynchronous programming with async/await
 * - State management in frontend applications
 * - Data fetching and caching strategies
 */

// =====================================
// GLOBAL STATE VARIABLES
// =====================================

/**
 * APPLICATION STATE
 *
 * In backend development, understanding state management is crucial.
 * These variables hold the current state of our application.
 *
 * Think of this as your application's "memory" - it remembers:
 * - Who is currently logged in
 * - What data we've loaded
 * - What the user is currently doing
 */
let currentUser = null;           // Stores authenticated user information
let dashboardStats = {};          // Stores statistics for dashboard widgets
let recentActivity = [];          // Stores recent user activity
let upcomingEvents = [];          // Stores upcoming events for quick view
let isDataLoading = false;        // Prevents multiple simultaneous data loads

/**
 * CONFIGURATION CONSTANTS
 *
 * In production backend systems, these would typically come from:
 * - Environment variables
 * - Configuration files
 * - Database settings
 */
const DASHBOARD_CONFIG = {
    REFRESH_INTERVAL: 300000,     // 5 minutes in milliseconds
    MAX_RECENT_ACTIVITIES: 10,    // Maximum activities to show
    MAX_UPCOMING_EVENTS: 5,       // Maximum upcoming events to display
    CACHE_DURATION: 60000         // 1 minute cache for API calls
};

// =====================================
// INITIALIZATION & AUTHENTICATION
// =====================================

/**
 * MAIN INITIALIZATION FUNCTION
 *
 * This is the entry point of our dashboard. In backend terms, this is like
 * the "main" method that starts everything.
 *
 * FLOW:
 * 1. Wait for page to load completely (DOM ready)
 * 2. Check if user is authenticated
 * 3. If authenticated, load dashboard data
 * 4. If not authenticated, redirect to login
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Dashboard initializing...');

    // Start the authentication and initialization process
    initializeDashboard()
        .then(() => {
            console.log('✅ Dashboard initialization complete');
        })
        .catch(error => {
            console.error('❌ Dashboard initialization failed:', error);
            handleInitializationError(error);
        });
});

/**
 * MAIN DASHBOARD INITIALIZATION
 *
 * This function demonstrates several key backend concepts:
 * - Asynchronous programming (async/await)
 * - Error handling with try/catch
 * - Sequential execution of dependent operations
 * - State management
 */
async function initializeDashboard() {
    try {
        // STEP 1: Verify Authentication
        console.log('🔐 Checking user authentication...');
        await verifyUserAuthentication();

        // STEP 2: Load Dashboard Data
        console.log('📊 Loading dashboard data...');
        await loadDashboardData();

        // STEP 3: Set Up Event Listeners
        console.log('🎧 Setting up event listeners...');
        setupEventListeners();

        // STEP 4: Start Periodic Updates
        console.log('🔄 Starting periodic updates...');
        startPeriodicUpdates();

        // STEP 5: Show Success State
        showDashboardReady();

    } catch (error) {
        console.error('Failed to initialize dashboard:', error);
        throw error; // Re-throw to be handled by caller
    }
}

/**
 * USER AUTHENTICATION VERIFICATION
 *
 * This function demonstrates:
 * - HTTP requests to backend APIs
 * - Error handling for authentication failures
 * - Session management concepts
 * - Redirect logic for unauthorized users
 */
async function verifyUserAuthentication() {
    try {
        console.log('Making authentication request to backend...');

        // This is a REST API call to your Spring Boot backend
        // The '/api/users' endpoint requires authentication
        const response = await fetch('/api/users', {
            method: 'GET',
            credentials: 'include',    // IMPORTANT: Include session cookies
            headers: {
                'Accept': 'application/json',
                'Cache-Control': 'no-cache'  // Always check fresh authentication
            }
        });

        /**
         * HTTP STATUS CODE HANDLING
         *
         * Understanding HTTP status codes is crucial for backend development:
         * - 200: Success
         * - 401: Unauthorized (not logged in)
         * - 403: Forbidden (logged in but no permission)
         * - 500: Server error
         */
        if (!response.ok) {
            if (response.status === 401) {
                console.log('❌ User not authenticated - redirecting to login');
                redirectToLogin();
                return;
            }
            throw new Error(`Authentication check failed: ${response.status}`);
        }

        // Parse the JSON response from your backend
        const users = await response.json();

        /**
         * TEMPORARY DEMO LOGIC
         *
         * In a real application, you'd have a proper endpoint like:
         * GET /api/auth/current-user
         *
         * For now, we're using the first user as a demo
         */
        if (users && users.length > 0) {
            currentUser = users[0];
            console.log('✅ User authenticated:', currentUser.name);
            updateUserDisplay();
        } else {
            throw new Error('No user data received');
        }

    } catch (error) {
        console.error('Authentication verification failed:', error);

        // On any authentication error, redirect to login
        redirectToLogin();
        throw error;
    }
}

/**
 * REDIRECT TO LOGIN
 *
 * This function handles authentication failures by redirecting users
 * to the login page. In production systems, you might also:
 * - Clear any cached data
 * - Log the redirect event
 * - Show a brief message to the user
 */
function redirectToLogin() {
    console.log('🔄 Redirecting to login page...');

    // Add a slight delay for better user experience
    setTimeout(() => {
        window.location.href = '/index.html';
    }, 1000);
}

/**
 * UPDATE USER DISPLAY
 *
 * This function updates the UI with the authenticated user's information.
 * It demonstrates DOM manipulation and data binding concepts.
 */
function updateUserDisplay() {
    try {
        // Update user name in header
        const userNameElement = document.getElementById('userName');
        if (userNameElement) {
            userNameElement.textContent = currentUser.name || 'User';
        }

        // Update user avatar
        const userAvatarElement = document.getElementById('userAvatar');
        if (userAvatarElement) {
            userAvatarElement.src = currentUser.profilePictureUrl || 'https://via.placeholder.com/32x32?text=👤';
            userAvatarElement.alt = `${currentUser.name}'s avatar`;
        }

        console.log('✅ User display updated');

    } catch (error) {
        console.error('Error updating user display:', error);
        // Non-critical error - don't throw, just log
    }
}

// =====================================
// DATA LOADING & MANAGEMENT
// =====================================

/**
 * LOAD DASHBOARD DATA
 *
 * This function orchestrates loading all the data needed for the dashboard.
 * It demonstrates:
 * - Parallel API calls for better performance
 * - Error handling for individual data sources
 * - Loading state management
 */
async function loadDashboardData() {
    if (isDataLoading) {
        console.log('⏳ Data loading already in progress...');
        return;
    }

    isDataLoading = true;
    showLoadingState();

    try {
        console.log('📥 Loading dashboard data...');

        /**
         * PARALLEL DATA LOADING
         *
         * Using Promise.allSettled() instead of Promise.all() because:
         * - Promise.all() fails if ANY request fails
         * - Promise.allSettled() waits for all requests, even if some fail
         * - This gives us a better user experience
         */
        const dataPromises = [
            loadUserStatistics(),
            loadRecentActivity(),
            loadUpcomingEvents(),
            loadFriendsData()
        ];

        const results = await Promise.allSettled(dataPromises);

        // Process results and handle any failures
        results.forEach((result, index) => {
            if (result.status === 'rejected') {
                console.error(`Data loading failed for request ${index}:`, result.reason);
            }
        });

        // Update the dashboard display
        updateDashboardDisplay();

        console.log('✅ Dashboard data loaded successfully');

    } catch (error) {
        console.error('Failed to load dashboard data:', error);
        showErrorState('Failed to load dashboard data');
        throw error;

    } finally {
        isDataLoading = false;
        hideLoadingState();
    }
}

/**
 * LOAD USER STATISTICS
 *
 * This function loads statistical data for the dashboard widgets.
 * It demonstrates how to make multiple API calls and aggregate data.
 */
async function loadUserStatistics() {
    try {
        console.log('📊 Loading user statistics...');

        // Initialize default stats
        dashboardStats = {
            totalFriends: 0,
            totalEvents: 0,
            upcomingEvents: 0,
            pendingRequests: 0
        };

        // Load friends count
        try {
            const friendsResponse = await fetch(`/api/friends/${currentUser.id}`, {
                method: 'GET',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });

            if (friendsResponse.ok) {
                const friends = await friendsResponse.json();
                dashboardStats.totalFriends = friends.length;
            }
        } catch (error) {
            console.warn('Failed to load friends count:', error);
        }

        // Load events count (if availability endpoint exists)
        try {
            const statsResponse = await fetch(`/api/availability/${currentUser.id}/stats`, {
                method: 'GET',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });

            if (statsResponse.ok) {
                const stats = await statsResponse.json();
                dashboardStats.totalEvents = stats.totalEvents || 0;
            }
        } catch (error) {
            console.warn('Failed to load events stats:', error);
        }

        // Load pending requests count
        try {
            const requestsResponse = await fetch(`/api/friends/${currentUser.id}/pending`, {
                method: 'GET',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });

            if (requestsResponse.ok) {
                const requests = await requestsResponse.json();
                dashboardStats.pendingRequests = requests.length;
            }
        } catch (error) {
            console.warn('Failed to load pending requests:', error);
        }

        console.log('📊 Statistics loaded:', dashboardStats);

    } catch (error) {
        console.error('Error loading user statistics:', error);
        throw error;
    }
}

/**
 * LOAD RECENT ACTIVITY
 *
 * This function would load recent user activity.
 * For now, we'll simulate this data since the backend doesn't have
 * an activity tracking system yet.
 */
async function loadRecentActivity() {
    try {
        console.log('📋 Loading recent activity...');

        // TODO: Replace with actual API call when activity tracking is implemented
        // const response = await fetch(`/api/activity/${currentUser.id}/recent`);

        // Simulated recent activity for now
        recentActivity = [
            {
                id: 1,
                type: 'friend_request_sent',
                description: 'Sent friend request to John Doe',
                timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000), // 2 hours ago
                icon: '👥'
            },
            {
                id: 2,
                type: 'event_created',
                description: 'Created event "Team Meeting"',
                timestamp: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000), // 1 day ago
                icon: '📅'
            },
            {
                id: 3,
                type: 'profile_updated',
                description: 'Updated profile information',
                timestamp: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000), // 3 days ago
                icon: '👤'
            }
        ];

        console.log('📋 Recent activity loaded:', recentActivity.length, 'items');

    } catch (error) {
        console.error('Error loading recent activity:', error);
        throw error;
    }
}

/**
 * LOAD UPCOMING EVENTS
 *
 * This function loads upcoming events for the dashboard preview.
 */
async function loadUpcomingEvents() {
    try {
        console.log('⏰ Loading upcoming events...');

        // Try to load from availability API
        try {
            const response = await fetch(`/api/availability/${currentUser.id}/upcoming`, {
                method: 'GET',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });

            if (response.ok) {
                const events = await response.json();
                upcomingEvents = events.slice(0, DASHBOARD_CONFIG.MAX_UPCOMING_EVENTS);
                console.log('⏰ Upcoming events loaded:', upcomingEvents.length, 'events');
                return;
            }
        } catch (error) {
            console.warn('Failed to load upcoming events from API:', error);
        }

        // Fallback to empty array if API fails
        upcomingEvents = [];

    } catch (error) {
        console.error('Error loading upcoming events:', error);
        throw error;
    }
}

/**
 * LOAD FRIENDS DATA
 *
 * This function loads basic friends data for dashboard display.
 */
async function loadFriendsData() {
    try {
        console.log('👥 Loading friends data...');

        const response = await fetch(`/api/friends/${currentUser.id}`, {
            method: 'GET',
            credentials: 'include',
            headers: { 'Accept': 'application/json' }
        });

        if (response.ok) {
            const friends = await response.json();
            console.log('👥 Friends data loaded:', friends.length, 'friends');
            // Store friends data if needed for dashboard display
        } else {
            console.warn('Failed to load friends data:', response.status);
        }

    } catch (error) {
        console.error('Error loading friends data:', error);
        // Non-critical error - don't throw
    }
}

// =====================================
// UI UPDATE & DISPLAY FUNCTIONS
// =====================================

/**
 * UPDATE DASHBOARD DISPLAY
 *
 * This is the main UI update function that refreshes all dashboard components.
 * It demonstrates:
 * - DOM manipulation patterns
 * - Data binding concepts
 * - Error handling in UI updates
 * - Conditional rendering based on data state
 */
function updateDashboardDisplay() {
    try {
        console.log('🎨 Updating dashboard display...');

        // Update each dashboard section
        updateStatisticsCards();
        updateRecentActivitySection();
        updateUpcomingEventsSection();
        updateQuickActionsSection();

        console.log('✅ Dashboard display updated successfully');

    } catch (error) {
        console.error('❌ Error updating dashboard display:', error);
        showErrorMessage('Failed to update dashboard display');
    }
}

/**
 * UPDATE STATISTICS CARDS
 *
 * This function updates the statistic cards at the top of the dashboard.
 * It demonstrates:
 * - Data formatting for display
 * - DOM element selection and manipulation
 * - Null safety patterns
 * - Animation/transition concepts
 */
function updateStatisticsCards() {
    try {
        console.log('📊 Updating statistics cards...');

        // Update Total Friends
        const totalFriendsElement = document.getElementById('totalFriends');
        if (totalFriendsElement) {
            animateNumberChange(totalFriendsElement, dashboardStats.totalFriends);
        }

        // Update Total Events
        const totalEventsElement = document.getElementById('totalEvents');
        if (totalEventsElement) {
            animateNumberChange(totalEventsElement, dashboardStats.totalEvents);
        }

        // Update Upcoming Events Count
        const upcomingEventsElement = document.getElementById('upcomingEvents');
        if (upcomingEventsElement) {
            animateNumberChange(upcomingEventsElement, upcomingEvents.length);
        }

        // Update Pending Requests (with notification styling)
        const pendingRequestsElement = document.getElementById('pendingRequests');
        if (pendingRequestsElement) {
            animateNumberChange(pendingRequestsElement, dashboardStats.pendingRequests);

            // Add visual indicator for pending requests
            const statCard = pendingRequestsElement.closest('.stat-card');
            if (statCard) {
                if (dashboardStats.pendingRequests > 0) {
                    statCard.classList.add('has-notifications');
                } else {
                    statCard.classList.remove('has-notifications');
                }
            }
        }

        console.log('📊 Statistics cards updated');

    } catch (error) {
        console.error('Error updating statistics cards:', error);
    }
}

/**
 * ANIMATE NUMBER CHANGE
 *
 * This function provides smooth animation when numbers change.
 * It demonstrates:
 * - DOM manipulation
 * - CSS class management
 * - Animation concepts
 * - User experience enhancements
 */
function animateNumberChange(element, newValue) {
    if (!element) return;

    const currentValue = parseInt(element.textContent) || 0;

    // Only animate if the value actually changed
    if (currentValue !== newValue) {
        // Add animation class
        element.classList.add('stat-updating');

        // Update the value
        element.textContent = newValue;

        // Remove animation class after animation completes
        setTimeout(() => {
            element.classList.remove('stat-updating');
        }, 300);
    }
}

/**
 * UPDATE RECENT ACTIVITY SECTION
 *
 * This function updates the recent activity list.
 * It demonstrates:
 * - Dynamic HTML generation
 * - Template string usage
 * - Date formatting
 * - Conditional rendering
 */
function updateRecentActivitySection() {
    try {
        console.log('📋 Updating recent activity section...');

        const activityContainer = document.getElementById('recentActivity');
        if (!activityContainer) {
            console.warn('Recent activity container not found');
            return;
        }

        // Check if we have any activity data
        if (!recentActivity || recentActivity.length === 0) {
            activityContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📭</div>
                    <h4>No Recent Activity</h4>
                    <p>Start using LinkUp to see your activity here!</p>
                </div>
            `;
            return;
        }

        // Generate HTML for each activity item
        const activityHTML = recentActivity
            .slice(0, DASHBOARD_CONFIG.MAX_RECENT_ACTIVITIES)
            .map(activity => generateActivityItemHTML(activity))
            .join('');

        activityContainer.innerHTML = `
            <div class="activity-list">
                ${activityHTML}
            </div>
        `;

        console.log('📋 Recent activity section updated');

    } catch (error) {
        console.error('Error updating recent activity section:', error);
    }
}

/**
 * GENERATE ACTIVITY ITEM HTML
 *
 * This function generates HTML for a single activity item.
 * It demonstrates:
 * - Template generation patterns
 * - Data sanitization
 * - Relative time formatting
 * - CSS class management
 */
function generateActivityItemHTML(activity) {
    // Sanitize the description to prevent XSS attacks
    const safeDescription = escapeHtml(activity.description);

    // Format the timestamp for display
    const timeAgo = getRelativeTimeString(activity.timestamp);

    // Determine the CSS class based on activity type
    const activityClass = getActivityTypeClass(activity.type);

    return `
        <div class="activity-item ${activityClass}">
            <div class="activity-icon">
                ${activity.icon}
            </div>
            <div class="activity-content">
                <div class="activity-description">${safeDescription}</div>
                <div class="activity-time">${timeAgo}</div>
            </div>
        </div>
    `;
}

/**
 * UPDATE UPCOMING EVENTS SECTION
 *
 * This function updates the upcoming events preview.
 * It demonstrates:
 * - Data transformation
 * - Date/time formatting
 * - Conditional content rendering
 * - Event handling preparation
 */
function updateUpcomingEventsSection() {
    try {
        console.log('⏰ Updating upcoming events section...');

        const eventsContainer = document.getElementById('upcomingEventsContainer');
        if (!eventsContainer) {
            console.warn('Upcoming events container not found');
            return;
        }

        // Check if we have any events
        if (!upcomingEvents || upcomingEvents.length === 0) {
            eventsContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📅</div>
                    <h4>No Upcoming Events</h4>
                    <p>Your schedule is clear! <a href="availability.html">Add some events</a> to get started.</p>
                </div>
            `;
            return;
        }

        // Generate HTML for each event
        const eventsHTML = upcomingEvents
            .slice(0, DASHBOARD_CONFIG.MAX_UPCOMING_EVENTS)
            .map(event => generateEventItemHTML(event))
            .join('');

        eventsContainer.innerHTML = `
            <div class="events-list">
                ${eventsHTML}
            </div>
            <div class="events-footer">
                <a href="availability.html" class="view-all-link">View All Events →</a>
            </div>
        `;

        console.log('⏰ Upcoming events section updated');

    } catch (error) {
        console.error('Error updating upcoming events section:', error);
    }
}

/**
 * GENERATE EVENT ITEM HTML
 *
 * This function creates HTML for a single event item.
 * It demonstrates:
 * - Complex data formatting
 * - Date/time handling
 * - Status indication
 * - Interactive element preparation
 */
function generateEventItemHTML(event) {
    const eventDate = new Date(event.startTime);
    const eventEndDate = new Date(event.endTime);

    // Format date and time for display
    const dateStr = formatEventDate(eventDate);
    const timeStr = formatEventTimeRange(eventDate, eventEndDate);

    // Determine event type styling
    const eventTypeClass = event.isBusy ? 'event-busy' : 'event-free';
    const eventIcon = event.isBusy ? '🔴' : '🟢';

    // Sanitize event data
    const safeTitle = escapeHtml(event.title || 'Untitled Event');
    const safeLocation = event.location ? escapeHtml(event.location) : null;

    return `
        <div class="event-item ${eventTypeClass}" data-event-id="${event.id}">
            <div class="event-date">
                <div class="event-day">${eventDate.getDate()}</div>
                <div class="event-month">${eventDate.toLocaleDateString('en-US', { month: 'short' })}</div>
            </div>
            <div class="event-details">
                <div class="event-title">
                    <span class="event-status">${eventIcon}</span>
                    ${safeTitle}
                </div>
                <div class="event-time">${timeStr}</div>
                ${safeLocation ? `<div class="event-location">📍 ${safeLocation}</div>` : ''}
            </div>
            <div class="event-actions">
                <button class="btn-icon" onclick="viewEvent(${event.id})" title="View Event">
                    👁️
                </button>
            </div>
        </div>
    `;
}

/**
 * UPDATE QUICK ACTIONS SECTION
 *
 * This function updates the quick actions available to the user.
 * It demonstrates:
 * - Dynamic content generation
 * - User permission checking
 * - Action availability logic
 * - Progressive enhancement
 */
function updateQuickActionsSection() {
    try {
        console.log('⚡ Updating quick actions section...');

        const actionsContainer = document.getElementById('quickActions');
        if (!actionsContainer) {
            console.warn('Quick actions container not found');
            return;
        }

        // Define available quick actions based on user state
        const quickActions = getAvailableQuickActions();

        // Generate HTML for quick actions
        const actionsHTML = quickActions
            .map(action => generateQuickActionHTML(action))
            .join('');

        actionsContainer.innerHTML = `
            <div class="quick-actions-grid">
                ${actionsHTML}
            </div>
        `;

        console.log('⚡ Quick actions section updated');

    } catch (error) {
        console.error('Error updating quick actions section:', error);
    }
}

/**
 * GET AVAILABLE QUICK ACTIONS
 *
 * This function determines which quick actions should be available
 * based on the current user state and application data.
 */
function getAvailableQuickActions() {
    const actions = [
        {
            id: 'add-event',
            title: 'Add Event',
            description: 'Create a new calendar event',
            icon: '📅',
            action: 'navigateToAddEvent',
            available: true
        },
        {
            id: 'find-friends',
            title: 'Find Friends',
            description: 'Connect with more people',
            icon: '👥',
            action: 'navigateToFriends',
            available: true
        },
        {
            id: 'schedule-meetup',
            title: 'Schedule Meetup',
            description: 'Find common free time',
            icon: '🤝',
            action: 'navigateToSchedule',
            available: dashboardStats.totalFriends > 0
        },
        {
            id: 'view-calendar',
            title: 'View Calendar',
            description: 'See your full availability',
            icon: '📊',
            action: 'navigateToCalendar',
            available: true
        },
        {
            id: 'manage-requests',
            title: 'Friend Requests',
            description: `${dashboardStats.pendingRequests} pending`,
            icon: '📨',
            action: 'navigateToRequests',
            available: dashboardStats.pendingRequests > 0,
            hasNotification: dashboardStats.pendingRequests > 0
        }
    ];

    // Filter to only available actions
    return actions.filter(action => action.available);
}

/**
 * GENERATE QUICK ACTION HTML
 *
 * This function creates HTML for a quick action button.
 */
function generateQuickActionHTML(action) {
    const notificationClass = action.hasNotification ? 'has-notification' : '';

    return `
        <div class="quick-action-item ${notificationClass}" onclick="${action.action}()">
            <div class="action-icon">${action.icon}</div>
            <div class="action-content">
                <h4 class="action-title">${action.title}</h4>
                <p class="action-description">${action.description}</p>
            </div>
        </div>
    `;
}

// =====================================
// EVENT LISTENERS & USER INTERACTIONS
// =====================================

/**
 * SETUP EVENT LISTENERS
 *
 * This function sets up all event listeners for dashboard interactions.
 * It demonstrates:
 * - Event delegation patterns
 * - Form handling
 * - Navigation event handling
 * - Keyboard accessibility
 */
function setupEventListeners() {
    try {
        console.log('🎧 Setting up event listeners...');

        // Navigation event listeners
        setupNavigationListeners();

        // Search functionality
        setupSearchListeners();

        // Refresh button
        setupRefreshListener();

        // Keyboard shortcuts
        setupKeyboardShortcuts();

        // Window/page event listeners
        setupWindowEventListeners();

        console.log('✅ Event listeners set up successfully');

    } catch (error) {
        console.error('Error setting up event listeners:', error);
    }
}

/**
 * SETUP NAVIGATION LISTENERS
 *
 * This function handles navigation between different sections of the app.
 */
function setupNavigationListeners() {
    // Section navigation buttons
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(button => {
        button.addEventListener('click', handleSectionNavigation);
    });

    // Logout button
    const logoutBtn = document.querySelector('[onclick="logout()"]');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }
}

/**
 * HANDLE SECTION NAVIGATION
 *
 * This function manages navigation between different sections.
 * It demonstrates:
 * - Event handling patterns
 * - Active state management
 * - URL management
 * - User experience optimization
 */
function handleSectionNavigation(event) {
    event.preventDefault();

    const target = event.currentTarget;
    const sectionName = target.getAttribute('data-section') ||
        target.getAttribute('href')?.replace('.html', '') ||
        target.textContent.toLowerCase().trim();

    console.log('🧭 Navigating to section:', sectionName);

    // Update active state
    updateNavigationActiveState(target);

    // Show the requested section
    showSection(sectionName);
}

/**
 * UPDATE NAVIGATION ACTIVE STATE
 *
 * This function updates the visual active state of navigation elements.
 */
function updateNavigationActiveState(activeElement) {
    // Remove active class from all nav buttons
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    // Add active class to clicked element
    activeElement.classList.add('active');
}

/**
 * SETUP SEARCH LISTENERS
 *
 * This function handles search functionality across the dashboard.
 * It demonstrates:
 * - Debouncing techniques for performance
 * - Real-time search implementation
 * - Input event handling
 */
function setupSearchListeners() {
    const searchInput = document.getElementById('dashboardSearch');
    if (searchInput) {
        // Use debouncing to avoid excessive API calls
        let searchTimeout;

        searchInput.addEventListener('input', function(event) {
            clearTimeout(searchTimeout);

            searchTimeout = setTimeout(() => {
                handleDashboardSearch(event.target.value);
            }, 300); // 300ms delay
        });
    }
}

/**
 * SETUP REFRESH LISTENER
 *
 * This function handles manual refresh requests from the user.
 */
function setupRefreshListener() {
    const refreshBtn = document.getElementById('refreshDashboard');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', function(event) {
            event.preventDefault();
            console.log('🔄 Manual refresh requested');
            refreshDashboardData();
        });
    }
}

/**
 * SETUP KEYBOARD SHORTCUTS
 *
 * This function enables keyboard shortcuts for power users.
 * It demonstrates:
 * - Keyboard event handling
 * - Accessibility considerations
 * - User experience enhancements
 */
function setupKeyboardShortcuts() {
    document.addEventListener('keydown', function(event) {
        // Only trigger if no input field is focused
        if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
            return;
        }

        // Handle keyboard shortcuts
        if (event.ctrlKey || event.metaKey) {
            switch (event.key.toLowerCase()) {
                case 'r':
                    event.preventDefault();
                    refreshDashboardData();
                    break;
                case 'f':
                    event.preventDefault();
                    navigateToFriends();
                    break;
                case 'c':
                    event.preventDefault();
                    navigateToCalendar();
                    break;
            }
        }
    });
}

/**
 * SETUP WINDOW EVENT LISTENERS
 *
 * This function handles window-level events like focus/blur for
 * optimizing data refresh and user experience.
 */
function setupWindowEventListeners() {
    // Refresh data when window regains focus
    window.addEventListener('focus', function() {
        console.log('👁️ Window focused - checking for updates');
        checkForUpdates();
    });

    // Handle visibility changes (tab switching)
    document.addEventListener('visibilitychange', function() {
        if (!document.hidden) {
            console.log('👁️ Tab visible - checking for updates');
            checkForUpdates();
        }
    });

    // Handle browser back/forward buttons
    window.addEventListener('popstate', function(event) {
        console.log('🔙 Browser navigation detected');
        handleBrowserNavigation(event);
    });
}

// =====================================
// NAVIGATION FUNCTIONS
// =====================================

/**
 * NAVIGATION FUNCTIONS
 *
 * These functions handle navigation to different parts of the application.
 * They demonstrate:
 * - Single Page Application (SPA) concepts
 * - State management during navigation
 * - URL management
 * - User experience optimization
 */

function showSection(sectionName) {
    console.log('📍 Showing section:', sectionName);

    try {
        // Hide all sections
        const sections = document.querySelectorAll('.section');
        sections.forEach(section => {
            section.classList.remove('active');
        });

        // Show the requested section
        const targetSection = document.getElementById(sectionName);
        if (targetSection) {
            targetSection.classList.add('active');

            // Update page title
            updatePageTitle(sectionName);

            // Load section-specific data if needed
            loadSectionData(sectionName);
        } else {
            console.warn('Section not found:', sectionName);
        }

    } catch (error) {
        console.error('Error showing section:', error);
    }
}

/**
 * QUICK ACTION NAVIGATION FUNCTIONS
 *
 * These functions are called when users click quick action buttons.
 * In a production app, these might use a router library.
 */

function navigateToAddEvent() {
    console.log('📅 Navigating to add event');
    window.location.href = 'availability.html#create-event';
}

function navigateToFriends() {
    console.log('👥 Navigating to friends');
    window.location.href = 'friends.html';
}

function navigateToSchedule() {
    console.log('🤝 Navigating to schedule meetup');
    window.location.href = 'schedule.html';
}

function navigateToCalendar() {
    console.log('📊 Navigating to calendar');
    window.location.href = 'availability.html';
}

function navigateToRequests() {
    console.log('📨 Navigating to friend requests');
    window.location.href = 'friends.html#requests';
}

function viewEvent(eventId) {
    console.log('👁️ Viewing event:', eventId);
    window.location.href = `availability.html#event-${eventId}`;
}

// =====================================
// PERIODIC UPDATES & BACKGROUND TASKS
// =====================================

/**
 * START PERIODIC UPDATES
 *
 * This function starts background tasks that keep the dashboard fresh.
 * It demonstrates:
 * - Background processing concepts
 * - Interval management
 * - Resource cleanup
 * - Performance considerations
 */
function startPeriodicUpdates() {
    console.log('🔄 Starting periodic updates...');

    // Store interval IDs for cleanup
    window.dashboardIntervals = {
        dataRefresh: setInterval(() => {
            if (!document.hidden && !isDataLoading) {
                refreshDashboardData();
            }
        }, DASHBOARD_CONFIG.REFRESH_INTERVAL),

        timeUpdates: setInterval(() => {
            updateRelativeTimes();
        }, 60000) // Update times every minute
    };

    // Cleanup intervals when page unloads
    window.addEventListener('beforeunload', function() {
        stopPeriodicUpdates();
    });
}

/**
 * STOP PERIODIC UPDATES
 *
 * This function cleans up background tasks to prevent memory leaks.
 */
function stopPeriodicUpdates() {
    console.log('⏹️ Stopping periodic updates...');

    if (window.dashboardIntervals) {
        Object.values(window.dashboardIntervals).forEach(intervalId => {
            clearInterval(intervalId);
        });
        delete window.dashboardIntervals;
    }
}

/**
 * REFRESH DASHBOARD DATA
 *
 * This function refreshes all dashboard data, typically called
 * periodically or when the user manually requests a refresh.
 */
async function refreshDashboardData() {
    try {
        console.log('🔄 Refreshing dashboard data...');

        // Show refresh indicator
        showRefreshIndicator();

        // Reload all dashboard data
        await loadDashboardData();

        // Show success message
        showSuccessMessage('Dashboard refreshed successfully');

    } catch (error) {
        console.error('Failed to refresh dashboard:', error);
        showErrorMessage('Failed to refresh dashboard data');
    } finally {
        hideRefreshIndicator();
    }
}

/**
 * CHECK FOR UPDATES
 *
 * This function checks if updates are needed without forcing a full refresh.
 */
async function checkForUpdates() {
    // Only check if we haven't updated recently
    const lastUpdate = localStorage.getItem('lastDashboardUpdate');
    const now = Date.now();

    if (!lastUpdate || (now - parseInt(lastUpdate)) > DASHBOARD_CONFIG.CACHE_DURATION) {
        await refreshDashboardData();
        localStorage.setItem('lastDashboardUpdate', now.toString());
    }
}

// =====================================
// UTILITY FUNCTIONS
// =====================================

/**
 * UTILITY FUNCTIONS
 *
 * These functions provide common functionality used throughout the dashboard.
 * They demonstrate:
 * - Code reusability principles
 * - Data formatting standards
 * - Security best practices
 * - Performance optimization
 */

/**
 * ESCAPE HTML
 *
 * This function prevents XSS attacks by escaping HTML characters.
 * CRITICAL for security in production applications.
 */
function escapeHtml(text) {
    if (!text) return '';

    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * GET RELATIVE TIME STRING
 *
 * This function converts timestamps to human-readable relative times.
 * It demonstrates date/time manipulation and internationalization concepts.
 */
function getRelativeTimeString(timestamp) {
    const now = new Date();
    const date = new Date(timestamp);
    const diffMs = now - date;

    const diffSeconds = Math.floor(diffMs / 1000);
    const diffMinutes = Math.floor(diffSeconds / 60);
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffSeconds < 60) {
        return 'Just now';
    } else if (diffMinutes < 60) {
        return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
    } else if (diffHours < 24) {
        return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else if (diffDays < 7) {
        return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    } else {
        return date.toLocaleDateString();
    }
}

/**
 * FORMAT EVENT DATE
 *
 * This function formats event dates for consistent display.
 */
function formatEventDate(date) {
    return date.toLocaleDateString('en-US', {
        weekday: 'short',
        month: 'short',
        day: 'numeric'
    });
}

/**
 * FORMAT EVENT TIME RANGE
 *
 * This function formats time ranges for event display.
 */
function formatEventTimeRange(startDate, endDate) {
    const startTime = startDate.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
    });

    const endTime = endDate.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
    });

    return `${startTime} - ${endTime}`;
}

/**
 * GET ACTIVITY TYPE CLASS
 *
 * This function returns CSS classes based on activity type.
 */
function getActivityTypeClass(activityType) {
    const typeMap = {
        'friend_request_sent': 'activity-social',
        'friend_request_received': 'activity-social',
        'event_created': 'activity-calendar',
        'event_updated': 'activity-calendar',
        'profile_updated': 'activity-profile'
    };

    return typeMap[activityType] || 'activity-general';
}

/**
 * UPDATE RELATIVE TIMES
 *
 * This function updates all relative time displays on the page.
 */
function updateRelativeTimes() {
    const timeElements = document.querySelectorAll('[data-timestamp]');

    timeElements.forEach(element => {
        const timestamp = element.getAttribute('data-timestamp');
        if (timestamp) {
            element.textContent = getRelativeTimeString(new Date(timestamp));
        }
    });
}

/**
 * UPDATE PAGE TITLE
 *
 * This function updates the browser tab title based on current section.
 */
function updatePageTitle(sectionName) {
    const titles = {
        'dashboard': 'Dashboard - LinkUp',
        'friends': 'Friends - LinkUp',
        'calendar': 'Calendar - LinkUp',
        'schedule': 'Schedule - LinkUp',
        'profile': 'Profile - LinkUp'
    };

    document.title = titles[sectionName] || 'LinkUp';
}

// =====================================
// UI STATE MANAGEMENT
// =====================================

/**
 * UI STATE FUNCTIONS
 *
 * These functions manage different UI states and user feedback.
 * They demonstrate:
 * - User experience design principles
 * - State management patterns
 * - Accessibility considerations
 * - Error handling strategies
 */

function showDashboardReady() {
    console.log('✅ Dashboard ready');

    // Remove any loading states
    hideLoadingState();

    // Show dashboard content
    const dashboard = document.querySelector('.dashboard-container');
    if (dashboard) {
        dashboard.classList.add('dashboard-ready');
    }

    // Show welcome message for first-time users
    if (isFirstTimeUser()) {
        showWelcomeMessage();
    }
}

function showLoadingState() {
    const loadingOverlay = document.getElementById('loadingOverlay');
    if (loadingOverlay) {
        loadingOverlay.style.display = 'flex';
    }
}

function hideLoadingState() {
    const loadingOverlay = document.getElementById('loadingOverlay');
    if (loadingOverlay) {
        loadingOverlay.style.display = 'none';
    }
}

function showRefreshIndicator() {
    const refreshBtn = document.getElementById('refreshDashboard');
    if (refreshBtn) {
        refreshBtn.classList.add('refreshing');
        refreshBtn.disabled = true;
    }
}

function hideRefreshIndicator() {
    const refreshBtn = document.getElementById('refreshDashboard');
    if (refreshBtn) {
        refreshBtn.classList.remove('refreshing');
        refreshBtn.disabled = false;
    }
}

function showErrorState(message) {
    console.error('Dashboard error:', message);
    showErrorMessage(message);
}

function showSuccessMessage(message) {
    console.log('Success:', message);

    // Create or update success message element
    let messageEl = document.getElementById('successMessage');
    if (!messageEl) {
        messageEl = document.createElement('div');
        messageEl.id = 'successMessage';
        messageEl.className = 'success-message';
        document.body.appendChild(messageEl);
    }

    messageEl.textContent = message;
    messageEl.style.display = 'block';

    // Auto-hide after 3 seconds
    setTimeout(() => {
        messageEl.style.display = 'none';
    }, 3000);
}

function showErrorMessage(message) {
    console.error('Error:', message);

    // Create or update error message element
    let messageEl = document.getElementById('errorMessage');
    if (!messageEl) {
        messageEl = document.createElement('div');
        messageEl.id = 'errorMessage';
        messageEl.className = 'error-message';
        document.body.appendChild(messageEl);
    }

    messageEl.textContent = message;
    messageEl.style.display = 'block';

    // Auto-hide after 5 seconds
    setTimeout(() => {
        messageEl.style.display = 'none';
    }, 5000);
}

// =====================================
// ERROR HANDLING & CLEANUP
// =====================================

/**
 * HANDLE INITIALIZATION ERROR
 *
 * This function handles errors that occur during dashboard initialization.
 */
function handleInitializationError(error) {
    console.error('Dashboard initialization error:', error);

    // Show user-friendly error message
    const errorContainer = document.createElement('div');
    errorContainer.className = 'initialization-error';
    errorContainer.innerHTML = `
        <div class="error-content">
            <h2>🚫 Unable to Load Dashboard</h2>
            <p>We're having trouble loading your dashboard. This might be due to:</p>
            <ul>
                <li>Network connectivity issues</li>
                <li>Server maintenance</li>
                <li>Authentication problems</li>
            </ul>
            <div class="error-actions">
                <button onclick="location.reload()" class="btn btn-primary">Try Again</button>
                <button onclick="redirectToLogin()" class="btn btn-secondary">Sign In Again</button>
            </div>
        </div>
    `;

    document.body.appendChild(errorContainer);
}

/**
 * HANDLE LOGOUT
 *
 * This function handles user logout process.
 */
async function handleLogout(event) {
    if (event) {
        event.preventDefault();
    }

    try {
        console.log('🚪 Logging out user...');

        // Stop all background tasks
        stopPeriodicUpdates();

        // Clear local data
        clearLocalData();

        // Call logout API
        await fetch('/logout', {
            method: 'POST',
            credentials: 'include'
        });

        // Redirect to home page
        window.location.href = '/index.html';

    } catch (error) {
        console.error('Logout error:', error);
        // Force redirect even if logout call fails
        window.location.href = '/index.html';
    }
}

/**
 * CLEAR LOCAL DATA
 *
 * This function clears any locally stored data when user logs out.
 */
function clearLocalData() {
    // Clear dashboard-specific data
    localStorage.removeItem('lastDashboardUpdate');

    // Clear any cached data
    currentUser = null;
    dashboardStats = {};
    recentActivity = [];
    upcomingEvents = [];
}

/**
 * UTILITY HELPER FUNCTIONS
 */

function isFirstTimeUser() {
    return !localStorage.getItem('hasVisitedDashboard');
}

function showWelcomeMessage() {
    localStorage.setItem('hasVisitedDashboard', 'true');
    showSuccessMessage(`Welcome to LinkUp, ${currentUser.name}! 🎉`);
}

function handleDashboardSearch(query) {
    console.log('🔍 Dashboard search:', query);
    // Implement search functionality here
}

function loadSectionData(sectionName) {
    console.log('📊 Loading section data for:', sectionName);
    // Load section-specific data if needed
}

function handleBrowserNavigation(event) {
    console.log('🔙 Browser navigation:', event);
    // Handle browser back/forward navigation
}

// =====================================
// GLOBAL FUNCTION EXPORTS
// =====================================

/**
 * MAKE FUNCTIONS GLOBALLY AVAILABLE
 *
 * These functions need to be accessible from HTML onclick handlers
 * and other JavaScript files.
 */

// Navigation functions
window.navigateToAddEvent = navigateToAddEvent;
window.navigateToFriends = navigateToFriends;
window.navigateToSchedule = navigateToSchedule;
window.navigateToCalendar = navigateToCalendar;
window.navigateToRequests = navigateToRequests;
window.viewEvent = viewEvent;
window.showSection = showSection;

// User actions
window.handleLogout = handleLogout;
window.refreshDashboardData = refreshDashboardData;

// Utility functions
window.logout = handleLogout; // Alias for compatibility

// =====================================
// FINAL INITIALIZATION LOG
// =====================================

console.log('✅ Dashboard.js fully loaded and ready!');
console.log('🎯 Features available:');
console.log('   - User authentication verification');
console.log('   - Real-time data loading and updates');
console.log('   - Interactive statistics and widgets');
console.log('   - Navigation and quick actions');
console.log('   - Periodic background updates');
console.log('   - Comprehensive error handling');
console.log('   - Keyboard shortcuts and accessibility');
console.log('📚 This demonstrates key backend concepts for your Atlassian internship!');