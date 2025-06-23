// =====================================
// CORE SCHEDULING ALGORITHM
// =====================================

/**
 * FIND COMMON TIME SLOTS
 *
 * This is the main scheduling algorithm that finds common free time
 * among selected friends. It demonstrates:
 * - Complex algorithm implementation
 * - Time complexity optimization
 * - Data processing and transformation
 * - Parallel API calls and data aggregation
 * - Result ranking and scoring
 */
async function findCommonTime() {
    console.log('🔍 Starting common time search...');

    try {
        // Validation checks
        if (!validateSearchInput()) {
            return;
        }

        // Set loading state
        setSearchLoadingState(true);

        // Load availability data for all selected friends
        console.log('📊 Loading availability data...');
        const availabilityData = await loadAllAvailabilityData();

        // Run the scheduling algorithm
        console.log('⚙️ Running scheduling algorithm...');
        const commonSlots = await calculateCommonTimeSlots(availabilityData);

        // Score and rank the results
        console.log('📈 Scoring and ranking results...');
        const rankedSlots = scoreAndRankSlots(commonSlots);

        // Store results and update UI
        schedulingState.commonTimeSlots = rankedSlots;
        schedulingState.lastSearchResults = {
            timestamp: Date.now(),
            criteria: { ...schedulingState.searchCriteria },
            selectedFriends: Array.from(schedulingState.selectedFriends),
            results: rankedSlots
        };

        // Display results
        displaySearchResults(rankedSlots);

        // Save to search history
        saveToSearchHistory();

        // Update statistics
        updateStatistics();

        console.log('✅ Search completed successfully');
        showSuccessMessage(`Found ${rankedSlots.length} available time slots!`);

    } catch (error) {
        console.error('❌ Search failed:', error);
        showErrorMessage('Failed to find common time slots. Please try again.');
        displaySearchError(error);
    } finally {
        setSearchLoadingState(false);
    }
}

/**
 * VALIDATE SEARCH INPUT
 *
 * Validates that all required inputs are provided and valid.
 */
function validateSearchInput() {
    console.log('✅ Validating search input...');

    // Check friend selection
    if (schedulingState.selectedFriends.size === 0) {
        showErrorMessage('Please select at least one friend');
        return false;
    }

    if (schedulingState.selectedFriends.size < schedulingState.searchCriteria.minParticipants) {
        showErrorMessage(`Please select at least ${schedulingState.searchCriteria.minParticipants} friends`);
        return false;
    }

    // Update search criteria from form
    updateSearchCriteria();

    // Validate time preferences
    if (!validateTimePreferences()) {
        showErrorMessage('Please fix the time preference errors above');
        return false;
    }

    console.log('✅ Input validation passed');
    return true;
}

/**
 * LOAD ALL AVAILABILITY DATA
 *
 * This function loads availability data for all selected friends.
 * It demonstrates:
 * - Parallel API calls for performance
 * - Error handling for individual failures
 * - Data caching and optimization
 * - Timeout handling
 */
async function loadAllAvailabilityData() {
    console.log('📥 Loading availability data for selected friends...');

    const selectedFriendIds = Array.from(schedulingState.selectedFriends);
    const userId = schedulingState.currentUser.id;

    // Include current user in the search
    const allUserIds = [userId, ...selectedFriendIds];

    // Create promises for parallel loading
    const loadPromises = allUserIds.map(async (friendId) => {
        try {
            return await loadUserAvailability(friendId);
        } catch (error) {
            console.warn(`Failed to load availability for user ${friendId}:`, error);
            return { userId: friendId, availability: [], error: error.message };
        }
    });

    // Wait for all requests with timeout
    const results = await Promise.allSettled(loadPromises);

    // Process results and handle failures
    const availabilityMap = new Map();
    let failedLoads = 0;

    results.forEach((result, index) => {
        const userId = allUserIds[index];

        if (result.status === 'fulfilled' && result.value && !result.value.error) {
            availabilityMap.set(userId, result.value.availability || []);
            console.log(`✅ Loaded availability for user ${userId}: ${result.value.availability.length} slots`);
        } else {
            failedLoads++;
            availabilityMap.set(userId, []); // Empty availability for failed loads
            console.warn(`❌ Failed to load availability for user ${userId}`);
        }
    });

    // Warn if some data failed to load
    if (failedLoads > 0) {
        showWarningMessage(`Could not load availability for ${failedLoads} users. Results may be incomplete.`);
    }

    console.log(`📊 Loaded availability data for ${availabilityMap.size} users`);
    return availabilityMap;
}

/**
 * LOAD USER AVAILABILITY
 *
 * Loads availability data for a specific user within the search date range.
 */
async function loadUserAvailability(userId) {
    const criteria = schedulingState.searchCriteria;

    // Check cache first
    const cacheKey = `${userId}-${criteria.startDate.toISOString()}-${criteria.endDate.toISOString()}`;
    const cached = schedulingState.dataCache.get(cacheKey);
    const lastUpdate = schedulingState.lastCacheUpdate.get(cacheKey);

    if (cached && lastUpdate && (Date.now() - lastUpdate) < SCHEDULING_CONFIG.CACHE_DURATION) {
        console.log(`📋 Using cached data for user ${userId}`);
        return {userId, availability: cached};
    }

    // Load from API
    const startISO = criteria.startDate.toISOString();
    const endISO =/**
     * Schedule/Meetup Management JavaScript
     *
     * PURPOSE: This file manages the meetup scheduling functionality of LinkUp.
     * It handles finding common free time slots among multiple friends and scheduling events.
     *
     * KEY BACKEND CONCEPTS DEMONSTRATED:
     * - Algorithm design and optimization (scheduling algorithms)
     * - Data processing and transformation (availability calculations)
     * - Complex state management (multi-user coordination)
     * - Search and filtering algorithms (time slot optimization)
     * - Data visualization and presentation (calendar views)
     * - Performance optimization (caching, debouncing)
     * - Concurrent data processing (parallel API calls)
     * - Export and sharing functionality (data serialization)
     */

// =====================================
// GLOBAL STATE VARIABLES
// =====================================

        /**
         * SCHEDULING STATE MANAGEMENT
         *
         * This complex state management demonstrates patterns used in
         * enterprise applications for handling multi-user, multi-step processes.
         *
         * These concepts are crucial for backend development because they show:
         * - How to manage complex application state
         * - How to handle concurrent user interactions
         * - How to optimize data structures for performance
         */
        let
    schedulingState = {
        // User and friend data
        currentUser: null,
        allFriends: [],
        selectedFriends: new Set(),

        // Time preferences and constraints
        searchCriteria: {
            startDate: null,
            endDate: null,
            duration: 60,                    // Minutes
            preferredStartTime: '09:00',
            preferredEndTime: '17:00',
            allowedDays: [1, 2, 3, 4, 5],    // Monday-Friday
            minParticipants: 2
        },

        // Results and calculations
        availabilityData: new Map(),         // User ID -> availability slots
        commonTimeSlots: [],
        selectedTimeSlot: null,

        // UI and processing state
        isSearching: false,
        isProcessingSchedule: false,
        lastSearchResults: null,
        searchHistory: [],

        // Caching and performance
        dataCache: new Map(),
        lastCacheUpdate: new Map(),
        pendingRequests: new Set()
    };

    /**
     * ALGORITHM CONFIGURATION
     *
     * These constants control the scheduling algorithms.
     * In production systems, these would be:
     * - Configurable per organization
     * - Stored in database settings
     * - Adjustable via admin interfaces
     */
    const SCHEDULING_CONFIG = {
        // Performance optimization
        MAX_FRIENDS_PER_SEARCH: 10,         // Prevent exponential complexity
        MAX_DAYS_TO_SEARCH: 30,             // Limit search scope
        CACHE_DURATION: 300000,             // 5 minutes
        DEBOUNCE_DELAY: 500,                // UI responsiveness

        // Algorithm parameters
        MIN_SLOT_DURATION: 15,              // Minimum meaningful meeting time
        MAX_SLOT_DURATION: 480,             // 8 hours maximum
        OVERLAP_THRESHOLD: 0.8,             // 80% of friends must be available
        SCHEDULING_BUFFER: 15,              // Buffer between meetings

        // Data processing limits
        MAX_SEARCH_RESULTS: 50,             // Prevent UI overload
        PARALLEL_REQUEST_LIMIT: 5,          // Concurrent API calls
        REQUEST_TIMEOUT: 10000              // 10 second timeout
    };

    /**
     * TIME SLOT SCORING WEIGHTS
     *
     * This demonstrates algorithmic optimization - how to score
     * and rank potential meeting times based on multiple criteria.
     * This type of multi-criteria optimization is common in enterprise software.
     */
    const SLOT_SCORING = {
        FRIEND_AVAILABILITY: 0.4,           // 40% weight for friend participation
        TIME_PREFERENCE: 0.3,               // 30% weight for preferred time
        DAY_PREFERENCE: 0.2,                // 20% weight for preferred days
        DURATION_MATCH: 0.1                 // 10% weight for duration fit
    };

// =====================================
// INITIALIZATION & AUTHENTICATION
// =====================================

    /**
     * MAIN INITIALIZATION FUNCTION
     *
     * This demonstrates proper initialization patterns for complex applications:
     * - Dependency checking and loading
     * - Authentication verification
     * - Data preloading for performance
     * - Error handling and fallback strategies
     */
    document.addEventListener('DOMContentLoaded', function () {
        console.log('📅 Schedule page initializing...');

        try {
            initializeSchedulePage()
                .then(() => {
                    console.log('✅ Schedule page initialization complete');
                })
                .catch(error => {
                    console.error('❌ Schedule page initialization failed:', error);
                    handleInitializationError(error);
                });
        } catch (error) {
            console.error('💥 Critical error during schedule page setup:', error);
            showCriticalError();
        }
    });

    /**
     * INITIALIZE SCHEDULE PAGE
     *
     * This function demonstrates complex application initialization:
     * - Sequential dependency loading
     * - Parallel data fetching where possible
     * - State restoration from cache/localStorage
     * - Progressive enhancement
     */
    async function initializeSchedulePage() {
        try {
            // STEP 1: Verify user authentication
            console.log('🔐 Verifying authentication...');
            await verifyAuthentication();

            // STEP 2: Load initial data in parallel
            console.log('📊 Loading initial data...');
            await loadInitialData();

            // STEP 3: Set up event listeners and UI
            console.log('🎧 Setting up interactions...');
            setupEventListeners();

            // STEP 4: Initialize UI components
            console.log('🎨 Initializing UI components...');
            initializeUIComponents();

            // STEP 5: Restore previous state if available
            console.log('🔄 Restoring previous state...');
            restorePreviousState();

            // STEP 6: Set up real-time features
            console.log('⚡ Setting up real-time features...');
            setupRealTimeFeatures();

            console.log('🎉 Schedule page ready for use');

        } catch (error) {
            console.error('Failed to initialize schedule page:', error);
            throw error;
        }
    }

    /**
     * VERIFY AUTHENTICATION
     *
     * Ensures user is logged in before allowing access to scheduling features.
     */
    async function verifyAuthentication() {
        try {
            const response = await fetch('/api/users', {
                method: 'GET',
                credentials: 'include',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                throw new Error('Authentication required');
            }

            const users = await response.json();
            if (users && users.length > 0) {
                schedulingState.currentUser = users[0];
                updateUserDisplay();
                console.log('✅ User authenticated:', schedulingState.currentUser.name);
            } else {
                throw new Error('No user data available');
            }

        } catch (error) {
            console.error('Authentication failed:', error);
            redirectToLogin();
            throw error;
        }
    }

    /**
     * LOAD INITIAL DATA
     *
     * This function demonstrates parallel data loading for better performance.
     * It loads friends data and initializes the scheduling interface.
     */
    async function loadInitialData() {
        try {
            // Load data in parallel for better performance
            const dataPromises = [
                loadUserFriends(),
                loadUserStatistics(),
                loadSearchHistory()
            ];

            const results = await Promise.allSettled(dataPromises);

            // Process results and handle any failures gracefully
            results.forEach((result, index) => {
                if (result.status === 'rejected') {
                    console.warn(`Data loading failed for request ${index}:`, result.reason);
                }
            });

            // Update UI with loaded data
            updateInitialUI();

        } catch (error) {
            console.error('Error loading initial data:', error);
            throw error;
        }
    }

    /**
     * LOAD USER FRIENDS
     *
     * Loads the user's friends for friend selection in scheduling.
     */
    async function loadUserFriends() {
        try {
            console.log('👥 Loading user friends...');

            const response = await fetch(`/api/friends/${schedulingState.currentUser.id}`, {
                method: 'GET',
                credentials: 'include',
                headers: {'Accept': 'application/json'}
            });

            if (response.ok) {
                schedulingState.allFriends = await response.json();
                console.log('👥 Loaded friends:', schedulingState.allFriends.length);
            } else {
                console.warn('Failed to load friends:', response.status);
                schedulingState.allFriends = [];
            }

        } catch (error) {
            console.error('Error loading friends:', error);
            schedulingState.allFriends = [];
        }
    }

    /**
     * LOAD USER STATISTICS
     *
     * Loads statistics for the dashboard widgets.
     */
    async function loadUserStatistics() {
        try {
            const stats = {
                totalFriends: schedulingState.allFriends.length,
                selectedFriends: 0,
                commonSlots: 0,
                searchDays: 7
            };

            updateStatistics(stats);

        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    }

    /**
     * LOAD SEARCH HISTORY
     *
     * Loads previous search history from localStorage for better UX.
     */
    function loadSearchHistory() {
        try {
            const savedHistory = localStorage.getItem('linkup_search_history');
            if (savedHistory) {
                schedulingState.searchHistory = JSON.parse(savedHistory);
                console.log('📚 Loaded search history:', schedulingState.searchHistory.length, 'items');
            }
        } catch (error) {
            console.warn('Failed to load search history:', error);
            schedulingState.searchHistory = [];
        }
    }

// =====================================
// FRIEND SELECTION MANAGEMENT
// =====================================

    /**
     * FRIEND SELECTION FUNCTIONS
     *
     * These functions handle the complex logic of friend selection
     * for meetup scheduling. They demonstrate:
     * - Set operations for tracking selections
     * - Real-time UI updates
     * - Data validation and constraints
     * - Performance optimization
     */

    function displayFriends() {
        console.log('👥 Displaying friends for selection...');

        const friendsGrid = document.getElementById('friendsGrid');
        if (!friendsGrid) {
            console.error('Friends grid container not found');
            return;
        }

        if (schedulingState.allFriends.length === 0) {
            friendsGrid.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">👤</div>
                <h4>No Friends Yet</h4>
                <p>You need friends to schedule meetups!</p>
                <a href="friends.html" class="btn btn-primary">Add Friends</a>
            </div>
        `;
            return;
        }

        // Generate friend selection UI
        const friendsHTML = schedulingState.allFriends.map(friend =>
            generateFriendSelectionHTML(friend)
        ).join('');

        friendsGrid.innerHTML = `
        <div class="friends-selection-grid">
            ${friendsHTML}
        </div>
    `;

        // Update selection display
        updateSelectionDisplay();
    }

    /**
     * GENERATE FRIEND SELECTION HTML
     *
     * Creates the HTML for a friend selection card.
     */
    function generateFriendSelectionHTML(friend) {
        const isSelected = schedulingState.selectedFriends.has(friend.id);
        const selectedClass = isSelected ? 'selected' : '';

        return `
        <div class="friend-selector-card ${selectedClass}" data-friend-id="${friend.id}">
            <div class="friend-avatar">
                <img src="${friend.profilePictureUrl || 'https://via.placeholder.com/50'}" alt="${friend.name}">
                <div class="selection-indicator">
                    <span class="checkmark">✓</span>
                </div>
            </div>
            <div class="friend-info">
                <h4>${friend.name}</h4>
                <p class="friend-email">${friend.email}</p>
            </div>
            <div class="friend-actions">
                <button class="btn-select" onclick="toggleFriendSelection(${friend.id})">
                    ${isSelected ? 'Remove' : 'Select'}
                </button>
                <button class="btn-preview" onclick="previewFriendAvailability(${friend.id})" title="Preview availability">
                    👁️
                </button>
            </div>
        </div>
    `;
    }

    /**
     * TOGGLE FRIEND SELECTION
     *
     * This function demonstrates Set operations and real-time UI updates.
     */
    function toggleFriendSelection(friendId) {
        console.log('🎯 Toggling friend selection:', friendId);

        try {
            // Check selection limits
            if (!schedulingState.selectedFriends.has(friendId) &&
                schedulingState.selectedFriends.size >= SCHEDULING_CONFIG.MAX_FRIENDS_PER_SEARCH) {
                showWarningMessage(`Maximum ${SCHEDULING_CONFIG.MAX_FRIENDS_PER_SEARCH} friends can be selected`);
                return;
            }

            // Toggle selection using Set operations
            if (schedulingState.selectedFriends.has(friendId)) {
                schedulingState.selectedFriends.delete(friendId);
                console.log('➖ Friend removed from selection');
            } else {
                schedulingState.selectedFriends.add(friendId);
                console.log('➕ Friend added to selection');
            }

            // Update UI immediately
            updateFriendSelectionUI(friendId);
            updateSelectionDisplay();
            updateStatistics();

            // Clear previous search results since selection changed
            clearSearchResults();

        } catch (error) {
            console.error('Error toggling friend selection:', error);
            showErrorMessage('Failed to update friend selection');
        }
    }

    /**
     * UPDATE FRIEND SELECTION UI
     *
     * Updates the visual state of a friend selection card.
     */
    function updateFriendSelectionUI(friendId) {
        const friendCard = document.querySelector(`[data-friend-id="${friendId}"]`);
        if (!friendCard) return;

        const isSelected = schedulingState.selectedFriends.has(friendId);
        const selectButton = friendCard.querySelector('.btn-select');

        if (isSelected) {
            friendCard.classList.add('selected');
            if (selectButton) selectButton.textContent = 'Remove';
        } else {
            friendCard.classList.remove('selected');
            if (selectButton) selectButton.textContent = 'Select';
        }
    }

    /**
     * UPDATE SELECTION DISPLAY
     *
     * Updates the selection summary display.
     */
    function updateSelectionDisplay() {
        const selectedCount = schedulingState.selectedFriends.size;
        const selectionDisplay = document.getElementById('selectedFriendsDisplay');

        if (selectionDisplay) {
            const countElement = selectionDisplay.querySelector('.selection-count');
            if (countElement) {
                if (selectedCount === 0) {
                    countElement.textContent = 'None selected';
                } else {
                    const selectedNames = Array.from(schedulingState.selectedFriends)
                        .map(id => {
                            const friend = schedulingState.allFriends.find(f => f.id === id);
                            return friend ? friend.name : 'Unknown';
                        })
                        .slice(0, 3) // Show first 3 names
                        .join(', ');

                    const remainingCount = selectedCount - 3;
                    const displayText = selectedCount <= 3
                        ? selectedNames
                        : `${selectedNames} and ${remainingCount} more`;

                    countElement.textContent = `${selectedCount} selected: ${displayText}`;
                }
            }
        }
    }

    /**
     * CLEAR FRIEND SELECTION
     *
     * Clears all selected friends.
     */
    function clearSelection() {
        console.log('🧹 Clearing friend selection...');

        schedulingState.selectedFriends.clear();

        // Update all friend cards
        document.querySelectorAll('.friend-selector-card').forEach(card => {
            card.classList.remove('selected');
            const selectButton = card.querySelector('.btn-select');
            if (selectButton) selectButton.textContent = 'Select';
        });

        updateSelectionDisplay();
        updateStatistics();
        clearSearchResults();

        showInfoMessage('Friend selection cleared');
    }

// =====================================
// TIME PREFERENCE MANAGEMENT
// =====================================

    /**
     * TIME PREFERENCE FUNCTIONS
     *
     * These functions handle the complex logic of time preferences
     * and constraints for meeting scheduling.
     */

    function setupTimePreferences() {
        console.log('⏰ Setting up time preferences...');

        // Set default date range (next 7 days)
        const today = new Date();
        const nextWeek = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000);

        const startDateInput = document.getElementById('searchStartDate');
        const endDateInput = document.getElementById('searchEndDate');

        if (startDateInput) {
            startDateInput.value = formatDateForInput(today);
            startDateInput.min = formatDateForInput(today); // Can't schedule in the past
        }

        if (endDateInput) {
            endDateInput.value = formatDateForInput(nextWeek);
            endDateInput.min = formatDateForInput(today);
        }

        // Set up day of week preferences
        setupDayPreferences();

        // Update search criteria
        updateSearchCriteria();
    }

    /**
     * SETUP DAY PREFERENCES
     *
     * Initializes the day of week selection interface.
     */
    function setupDayPreferences() {
        const dayCheckboxes = document.querySelectorAll('#dayPreferences input[type="checkbox"]');

        dayCheckboxes.forEach(checkbox => {
            checkbox.addEventListener('change', function () {
                updateSearchCriteria();
                validateTimePreferences();
            });
        });
    }

    /**
     * UPDATE SEARCH CRITERIA
     *
     * Updates the search criteria based on user input.
     */
    function updateSearchCriteria() {
        try {
            const criteria = schedulingState.searchCriteria;

            // Date range
            const startDateInput = document.getElementById('searchStartDate');
            const endDateInput = document.getElementById('searchEndDate');

            if (startDateInput && endDateInput) {
                criteria.startDate = new Date(startDateInput.value);
                criteria.endDate = new Date(endDateInput.value);
            }

            // Duration
            const durationSelect = document.getElementById('meetingDuration');
            if (durationSelect) {
                criteria.duration = parseInt(durationSelect.value);
            }

            // Time preferences
            const startTimeSelect = document.getElementById('preferredStartTime');
            const endTimeSelect = document.getElementById('preferredEndTime');

            if (startTimeSelect) criteria.preferredStartTime = startTimeSelect.value;
            if (endTimeSelect) criteria.preferredEndTime = endTimeSelect.value;

            // Day preferences
            const dayCheckboxes = document.querySelectorAll('#dayPreferences input[type="checkbox"]:checked');
            criteria.allowedDays = Array.from(dayCheckboxes).map(cb => parseInt(cb.value));

            console.log('⚙️ Search criteria updated:', criteria);

            // Update statistics
            updateSearchDaysStatistic();

        } catch (error) {
            console.error('Error updating search criteria:', error);
        }
    }

    /**
     * VALIDATE TIME PREFERENCES
     *
     * Validates time preferences and shows warnings for potential issues.
     */
    function validateTimePreferences() {
        const criteria = schedulingState.searchCriteria;
        const issues = [];

        // Date range validation
        if (criteria.startDate && criteria.endDate) {
            if (criteria.startDate >= criteria.endDate) {
                issues.push('End date must be after start date');
            }

            const daysDiff = Math.ceil((criteria.endDate - criteria.startDate) / (1000 * 60 * 60 * 24));
            if (daysDiff > SCHEDULING_CONFIG.MAX_DAYS_TO_SEARCH) {
                issues.push(`Search range too large (max ${SCHEDULING_CONFIG.MAX_DAYS_TO_SEARCH} days)`);
            }
        }

        // Time range validation
        if (criteria.preferredStartTime && criteria.preferredEndTime) {
            if (criteria.preferredStartTime >= criteria.preferredEndTime) {
                issues.push('End time must be after start time');
            }
        }

        // Day selection validation
        if (criteria.allowedDays.length === 0) {
            issues.push('At least one day must be selected');
        }

        // Duration validation
        if (criteria.duration < SCHEDULING_CONFIG.MIN_SLOT_DURATION) {
            issues.push(`Duration too short (minimum ${SCHEDULING_CONFIG.MIN_SLOT_DURATION} minutes)`);
        }

        if (criteria.duration > SCHEDULING_CONFIG.MAX_SLOT_DURATION) {
            issues.push(`Duration too long (maximum ${SCHEDULING_CONFIG.MAX_SLOT_DURATION} minutes)`);
        }

        // Show validation results
        const validationContainer = document.getElementById('timeValidation');
        if (validationContainer) {
            if (issues.length > 0) {
                validationContainer.innerHTML = `
                <div class="validation-errors">
                    ${issues.map(issue => `<div class="validation-error">⚠️ ${issue}</div>`).join('')}
                </div>
            `;
                validationContainer.style.display = 'block';
            } else {
                validationContainer.style.display = 'none';
            }
        }

        return issues.length === 0;
    }

// I'll continue with the core scheduling algorithm and search functionality...
// This is getting quite comprehensive, so let me continue with the most important parts.

    console.log('📝 Schedule.js - Part 1 loaded (Initialization & Friend Selection)');
}