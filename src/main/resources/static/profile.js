/**
 * Profile and Settings Management JavaScript
 * Handles all profile-related functionality including:
 * - User profile information management
 * - Privacy and sharing settings
 * - Notification preferences
 * - Connected accounts management
 * - Data export and account deletion
 * - Settings persistence and synchronization
 */

// Global state variables
let currentUser = null;
let userSettings = {};
let originalProfileData = {};
let isProfileEditing = false;
let selectedProfilePicture = null;

/**
 * INITIALIZATION
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('Profile page loaded - initializing...');

    checkAuthentication()
        .then(() => {
            initializePage();
        })
        .catch(error => {
            console.error('Authentication failed:', error);
            window.location.href = '/index.html';
        });
});

/**
 * CHECK AUTHENTICATION
 */
async function checkAuthentication() {
    try {
        console.log('Checking user authentication...');

        const response = await fetch('/api/users', {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Authentication required');
        }

        const users = await response.json();
        if (users.length > 0) {
            currentUser = users[0]; // Using first user as demo
            console.log('User authenticated:', currentUser);
            updateUserDisplay();
            return currentUser;
        } else {
            throw new Error('No users found');
        }

    } catch (error) {
        console.error('Authentication check failed:', error);
        throw error;
    }
}

/**
 * UPDATE USER DISPLAY
 */
function updateUserDisplay() {
    const userNameElement = document.getElementById('userName');
    const userAvatarElement = document.getElementById('userAvatar');

    if (currentUser) {
        userNameElement.textContent = currentUser.name || 'User';
        userAvatarElement.src = currentUser.profilePictureUrl || 'https://via.placeholder.com/32';
    }
}

/**
 * INITIALIZE PAGE
 */
async function initializePage() {
    console.log('Initializing profile page...');

    try {
        showLoadingOverlay('Loading your profile...');

        // Load profile data and settings
        await Promise.all([
            loadProfileInformation(),
            loadUserSettings(),
            loadAccountStatistics()
        ]);

        // Set up event listeners
        setupEventListeners();

        // Apply loaded settings to UI
        applySettingsToUI();

        console.log('Profile page initialization complete');

    } catch (error) {
        console.error('Failed to initialize page:', error);
        showMessage('Failed to load profile data. Please refresh the page.', 'error');
    } finally {
        hideLoadingOverlay();
    }
}

/**
 * LOAD PROFILE INFORMATION
 */
async function loadProfileInformation() {
    console.log('Loading profile information...');

    try {
        // In a real app, you'd call GET /api/users/{id} for detailed profile
        // For now, we'll use the current user data and enhance it

        // Fill profile form with current user data
        document.getElementById('profileName').value = currentUser.name || '';
        document.getElementById('profileEmail').value = currentUser.email || '';
        document.getElementById('profilePicture').src = currentUser.profilePictureUrl || 'https://via.placeholder.com/100';
        document.getElementById('currentPicturePreview').src = currentUser.profilePictureUrl || 'https://via.placeholder.com/100';

        // Set default values for fields not in our basic user model
        document.getElementById('profileTimezone').value = 'UTC';
        document.getElementById('profileLanguage').value = 'en';
        document.getElementById('profileBio').value = '';

        // Update member since date
        const memberSince = document.getElementById('memberSince');
        if (currentUser.createdAt) {
            memberSince.textContent = `Member since: ${formatDate(currentUser.createdAt, { year: 'numeric', month: 'long' })}`;
        } else {
            memberSince.textContent = 'Member since: Recently joined';
        }

        // Update Google account status
        const googleEmail = document.getElementById('googleEmail');
        if (currentUser.googleId) {
            googleEmail.textContent = currentUser.email;
        } else {
            googleEmail.textContent = 'Not connected';
        }

        // Store original data for cancel functionality
        originalProfileData = {
            name: currentUser.name || '',
            email: currentUser.email || '',
            timezone: 'UTC',
            language: 'en',
            bio: ''
        };

        console.log('Profile information loaded');

    } catch (error) {
        console.error('Error loading profile information:', error);
        throw error;
    }
}

/**
 * LOAD USER SETTINGS
 */
async function loadUserSettings() {
    console.log('Loading user settings...');

    try {
        // In a real app, you'd call GET /api/settings/{userId}
        // For now, we'll use default settings
        userSettings = {
            privacy: {
                shareAvailability: true,
                shareEventTitles: false,
                shareEventDetails: false,
                shareEmail: true,
                shareLastSeen: true,
                allowDiscovery: true
            },
            notifications: {
                friendRequests: true,
                friendAccepted: true,
                meetingInvites: true,
                scheduleChanges: true,
                reminders: true,
                method: 'email'
            },
            preferences: {
                allowAnalytics: true,
                timezone: 'UTC',
                language: 'en'
            }
        };

        console.log('User settings loaded:', userSettings);

    } catch (error) {
        console.error('Error loading user settings:', error);
        // Continue with default settings if loading fails
        console.log('Using default settings');
    }
}

/**
 * LOAD ACCOUNT STATISTICS
 */
async function loadAccountStatistics() {
    console.log('Loading account statistics...');

    try {
        // Load friend count
        const friendsResponse = await fetch(`/api/friends/${currentUser.id}`, {
            method: 'GET',
            credentials: 'include',
            headers: { 'Accept': 'application/json' }
        });

        let friendCount = 0;
        if (friendsResponse.ok) {
            const friends = await friendsResponse.json();
            friendCount = friends.length;
        }

        // Load availability statistics
        const statsResponse = await fetch(`/api/availability/${currentUser.id}/stats`, {
            method: 'GET',
            credentials: 'include',
            headers: { 'Accept': 'application/json' }
        });

        let eventCount = 0;
        if (statsResponse.ok) {
            const stats = await statsResponse.json();
            eventCount = stats.totalEvents || 0;
        }

        // Calculate days active (mock calculation)
        const joinDate = new Date(currentUser.createdAt || Date.now());
        const today = new Date();
        const daysActive = Math.floor((today - joinDate) / (1000 * 60 * 60 * 24));

        // Update statistics display
        document.getElementById('totalFriends').textContent = friendCount;
        document.getElementById('totalEvents').textContent = eventCount;
        document.getElementById('totalMeetups').textContent = '0'; // Placeholder
        document.getElementById('daysActive').textContent = Math.max(daysActive, 1);

        console.log('Account statistics loaded');

    } catch (error) {
        console.error('Error loading account statistics:', error);
        // Set default values if loading fails
        document.getElementById('totalFriends').textContent = '0';
        document.getElementById('totalEvents').textContent = '0';
        document.getElementById('totalMeetups').textContent = '0';
        document.getElementById('daysActive').textContent = '1';
    }
}

/**
 * APPLY SETTINGS TO UI
 */
function applySettingsToUI() {
    console.log('Applying settings to UI...');

    // Privacy settings
    const privacy = userSettings.privacy;
    document.getElementById('shareAvailability').checked = privacy.shareAvailability;
    document.getElementById('shareEventTitles').checked = privacy.shareEventTitles;
    document.getElementById('shareEventDetails').checked = privacy.shareEventDetails;
    document.getElementById('shareEmail').checked = privacy.shareEmail;
    document.getElementById('shareLastSeen').checked = privacy.shareLastSeen;
    document.getElementById('allowDiscovery').checked = privacy.allowDiscovery;

    // Notification settings
    const notifications = userSettings.notifications;
    document.getElementById('notifyFriendRequests').checked = notifications.friendRequests;
    document.getElementById('notifyFriendAccepted').checked = notifications.friendAccepted;
    document.getElementById('notifyMeetingInvites').checked = notifications.meetingInvites;
    document.getElementById('notifyScheduleChanges').checked = notifications.scheduleChanges;
    document.getElementById('notifyReminders').checked = notifications.reminders;

    // Notification method
    const methodRadio = document.querySelector(`input[name="notificationMethod"][value="${notifications.method}"]`);
    if (methodRadio) {
        methodRadio.checked = true;
    }

    // Other preferences
    document.getElementById('allowAnalytics').checked = userSettings.preferences.allowAnalytics;

    console.log('Settings applied to UI');
}

/**
 * EVENT LISTENERS SETUP
 */
function setupEventListeners() {
    // Profile form submission
    document.getElementById('profileForm').addEventListener('submit', handleProfileUpdate);

    // Picture input change
    document.getElementById('pictureInput').addEventListener('change', handlePictureSelection);

    // Settings change listeners (auto-save)
    setupSettingsChangeListeners();

    console.log('Event listeners set up');
}

/**
 * SETUP SETTINGS CHANGE LISTENERS
 */
function setupSettingsChangeListeners() {
    // Privacy settings
    const privacyCheckboxes = [
        'shareAvailability', 'shareEventTitles', 'shareEventDetails',
        'shareEmail', 'shareLastSeen', 'allowDiscovery'
    ];

    privacyCheckboxes.forEach(id => {
        document.getElementById(id).addEventListener('change', function() {
            userSettings.privacy[id.replace('share', '').replace('allow', '').toLowerCase()] = this.checked;
            saveSettings();
        });
    });

    // Notification settings
    const notificationCheckboxes = [
        'notifyFriendRequests', 'notifyFriendAccepted', 'notifyMeetingInvites',
        'notifyScheduleChanges', 'notifyReminders'
    ];

    notificationCheckboxes.forEach(id => {
        document.getElementById(id).addEventListener('change', function() {
            const key = id.replace('notify', '').replace('Friend', 'friend').replace('Meeting', 'meeting').replace('Schedule', 'schedule');
            userSettings.notifications[key] = this.checked;
            saveSettings();
        });
    });

    // Notification method radio buttons
    document.querySelectorAll('input[name="notificationMethod"]').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                userSettings.notifications.method = this.value;
                saveSettings();
            }
        });
    });

    // Analytics preference
    document.getElementById('allowAnalytics').addEventListener('change', function() {
        userSettings.preferences.allowAnalytics = this.checked;
        saveSettings();
    });
}

/**
 * PROFILE MANAGEMENT
 */
function editProfile() {
    isProfileEditing = true;

    // Enable form fields
    document.getElementById('profileName').readOnly = false;
    document.getElementById('profileTimezone').disabled = false;
    document.getElementById('profileLanguage').disabled = false;
    document.getElementById('profileBio').readOnly = false;

    // Show form actions
    document.getElementById('profileFormActions').style.display = 'block';

    // Update button text
    document.getElementById('editProfileBtn').textContent = 'Cancel';
    document.getElementById('editProfileBtn').onclick = cancelProfileEdit;

    // Focus on name field
    document.getElementById('profileName').focus();

    console.log('Profile editing enabled');
}

function cancelProfileEdit() {
    isProfileEditing = false;

    // Restore original values
    document.getElementById('profileName').value = originalProfileData.name;
    document.getElementById('profileTimezone').value = originalProfileData.timezone;
    document.getElementById('profileLanguage').value = originalProfileData.language;
    document.getElementById('profileBio').value = originalProfileData.bio;

    // Disable form fields
    document.getElementById('profileName').readOnly = true;
    document.getElementById('profileTimezone').disabled = true;
    document.getElementById('profileLanguage').disabled = true;
    document.getElementById('profileBio').readOnly = true;

    // Hide form actions
    document.getElementById('profileFormActions').style.display = 'none';

    // Update button
    document.getElementById('editProfileBtn').textContent = 'Edit';
    document.getElementById('editProfileBtn').onclick = editProfile;

    console.log('Profile editing cancelled');
}

async function handleProfileUpdate(event) {
    event.preventDefault();

    const updatedData = {
        name: document.getElementById('profileName').value,
        timezone: document.getElementById('profileTimezone').value,
        language: document.getElementById('profileLanguage').value,
        bio: document.getElementById('profileBio').value
    };

    console.log('Updating profile:', updatedData);

    try {
        showLoadingOverlay('Updating profile...');

        // Call your UserController PUT /api/users/{id} endpoint
        const response = await fetch(`/api/users/${currentUser.id}`, {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                name: updatedData.name,
                email: currentUser.email // Email can't be changed
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Failed to update profile: ${response.status}`);
        }

        const updatedUser = await response.json();
        console.log('Profile updated successfully:', updatedUser);

        // Update current user data
        currentUser.name = updatedUser.name;

        // Update original data
        originalProfileData = { ...updatedData };

        // Exit editing mode
        cancelProfileEdit();

        // Update header display
        updateUserDisplay();

        showMessage('Profile updated successfully!', 'success');

    } catch (error) {
        console.error('Error updating profile:', error);
        showMessage(`Failed to update profile: ${error.message}`, 'error');
    } finally {
        hideLoadingOverlay();
    }
}

/**
 * PROFILE PICTURE MANAGEMENT
 */
function changePicture() {
    document.getElementById('changePictureModal').classList.add('show');
}

function handlePictureSelection(event) {
    const file = event.target.files[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
        showMessage('Please select a valid image file', 'error');
        return;
    }

    // Validate file size (5MB limit)
    if (file.size > 5 * 1024 * 1024) {
        showMessage('Image must be smaller than 5MB', 'error');
        return;
    }

    // Create preview
    const reader = new FileReader();
    reader.onload = function(e) {
        const preview = document.getElementById('newPicturePreview');
        preview.src = e.target.result;
        preview.style.display = 'block';

        // Hide upload placeholder
        document.querySelector('.upload-placeholder').style.display = 'none';

        // Enable save button
        document.getElementById('savePictureBtn').disabled = false;

        // Store selected file
        selectedProfilePicture = file;
    };

    reader.readAsDataURL(file);
}

async function savePicture() {
    if (!selectedProfilePicture) return;

    try {
        showLoadingOverlay('Uploading profile picture...');

        // In a real app, you'd upload to your backend
        // For now, we'll simulate the upload and use a placeholder
        console.log('Uploading picture:', selectedProfilePicture.name);

        // Simulate upload delay
        await new Promise(resolve => setTimeout(resolve, 1000));

        // Create object URL for preview (in real app, you'd get URL from server)
        const newPictureUrl = URL.createObjectURL(selectedProfilePicture);

        // Update profile picture displays
        document.getElementById('profilePicture').src = newPictureUrl;
        document.getElementById('userAvatar').src = newPictureUrl;
        document.getElementById('currentPicturePreview').src = newPictureUrl;

        // Reset modal
        closeModal('changePictureModal');
        resetPictureModal();

        showMessage('Profile picture updated successfully!', 'success');

        // In a real app, you'd also update the user object with the new URL
        // currentUser.profilePictureUrl = newPictureUrl;

    } catch (error) {
        console.error('Error uploading picture:', error);
        showMessage('Failed to upload profile picture', 'error');
    } finally {
        hideLoadingOverlay();
    }
}

function resetPictureModal() {
    document.getElementById('newPicturePreview').style.display = 'none';
    document.querySelector('.upload-placeholder').style.display = 'block';
    document.getElementById('savePictureBtn').disabled = true;
    document.getElementById('pictureInput').value = '';
    selectedProfilePicture = null;
}

/**
 * SETTINGS MANAGEMENT
 */
async function saveSettings() {
    console.log('Saving settings:', userSettings);

    try {
        // In a real app, you'd call PUT /api/settings/{userId}
        // For now, we'll just store in memory and show success

        // Debounce multiple rapid changes
        if (saveSettings.timeout) {
            clearTimeout(saveSettings.timeout);
        }

        saveSettings.timeout = setTimeout(async () => {
            console.log('Settings auto-saved');
            // showMessage('Settings saved', 'success');
        }, 1000);

    } catch (error) {
        console.error('Error saving settings:', error);
        showMessage('Failed to save settings', 'error');
    }
}

/**
 * CONNECTED ACCOUNTS MANAGEMENT
 */
function manageGoogleConnection() {
    if (currentUser.googleId) {
        if (confirm('Do you want to disconnect your Google account? This will disable automatic calendar sync.')) {
            disconnectGoogleAccount();
        }
    } else {
        connectGoogleAccount();
    }
}

function connectGoogleAccount() {
    // Redirect to Google OAuth
    window.location.href = '/oauth2/authorization/google';
}

async function disconnectGoogleAccount() {
    try {
        showLoadingOverlay('Disconnecting Google account...');

        // In a real app, you'd call an API to disconnect
        console.log('Disconnecting Google account...');

        // Simulate disconnect
        await new Promise(resolve => setTimeout(resolve, 1000));

        // Update UI
        document.getElementById('googleAccountStatus').innerHTML = 'Not connected';

        showMessage('Google account disconnected', 'success');

    } catch (error) {
        console.error('Error disconnecting Google account:', error);
        showMessage('Failed to disconnect Google account', 'error');
    } finally {
        hideLoadingOverlay();
    }
}

function connectGoogleCalendar() {
    // This would request additional calendar permissions
    showMessage('Google Calendar integration coming soon!', 'info');
}

/**
 * DATA MANAGEMENT
 */
async function exportUserData() {
    try {
        showLoadingOverlay('Preparing data export...');

        console.log('Exporting user data...');

        // Collect all user data
        const exportData = {
            profile: {
                id: currentUser.id,
                name: currentUser.name,
                email: currentUser.email,
                createdAt: currentUser.createdAt,
                settings: userSettings
            },
            friends: [], // Would load from API
            events: [], // Would load from API
            exportDate: new Date().toISOString(),
            version: '1.0.0'
        };

        // Create and download file
        const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `linkup-data-export-${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);

        showMessage('Data export downloaded successfully', 'success');

    } catch (error) {
        console.error('Error exporting data:', error);
        showMessage('Failed to export data', 'error');
    } finally {
        hideLoadingOverlay();
    }
}

async function clearAllData() {
    const confirmText = 'CLEAR ALL DATA';
    const userInput = prompt(`This will delete all your events and availability data, but keep your account and friends.\n\nType "${confirmText}" to confirm:`);

    if (userInput !== confirmText) {
        showMessage('Data clearing cancelled', 'info');
        return;
    }

    try {
        showLoadingOverlay('Clearing all data...');

        console.log('Clearing all user data...');

        // In a real app, you'd call DELETE /api/availability/{userId}/all
        // Simulate the operation
        await new Promise(resolve => setTimeout(resolve, 2000));

        showMessage('All data has been cleared successfully', 'success');

        // Reload statistics
        await loadAccountStatistics();

    } catch (error) {
        console.error('Error clearing data:', error);
        showMessage('Failed to clear data', 'error');
    } finally {
        hideLoadingOverlay();
    }
}

async function deleteAccount() {
    const confirmText = 'DELETE MY ACCOUNT';
    const userInput = prompt(`⚠️ WARNING: This will permanently delete your account and ALL associated data.\n\nThis action CANNOT be undone.\n\nType "${confirmText}" to confirm:`);

    if (userInput !== confirmText) {
        showMessage('Account deletion cancelled', 'info');
        return;
    }

    const finalConfirm = confirm('Are you absolutely sure? This will delete everything and cannot be undone.');
    if (!finalConfirm) {
        showMessage('Account deletion cancelled', 'info');
        return;
    }

    try {
        showLoadingOverlay('Deleting account...');

        console.log('Deleting user account...');

        // Call your UserController DELETE /api/users/{id} endpoint
        const response = await fetch(`/api/users/${currentUser.id}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error(`Failed to delete account: ${response.status}`);
        }

        console.log('Account deleted successfully');

        // Show final message and redirect
        showMessage('Account deleted successfully. You will be redirected to the home page.', 'success');

        setTimeout(() => {
            window.location.href = '/index.html';
        }, 3000);

    } catch (error) {
        console.error('Error deleting account:', error);
        showMessage('Failed to delete account. Please try again or contact support.', 'error');
    } finally {
        hideLoadingOverlay();
    }
}

/**
 * HELP AND SUPPORT FUNCTIONS
 */
function viewPrivacyPolicy() {
    // In a real app, this would open your privacy policy
    showMessage('Privacy policy would open here', 'info');
}

function showHelp() {
    showMessage('Help documentation coming soon!', 'info');
}

function showFeedback() {
    const feedback = prompt('Please share your feedback about LinkUp:');
    if (feedback && feedback.trim()) {
        console.log('User feedback:', feedback);
        showMessage('Thank you for your feedback!', 'success');
    }
}

function showChangelog() {
    showMessage('Changelog:\n\n• Version 1.0.0-beta\n• Initial release\n• Friend management\n• Calendar integration\n• Schedule coordination', 'info');
}

function showSupport() {
    showMessage('For technical support, please contact: support@linkup.app', 'info');
}

/**
 * MODAL MANAGEMENT
 */
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    modal.classList.remove('show');

    // Reset picture modal if closing it
    if (modalId === 'changePictureModal') {
        resetPictureModal();
    }
}

/**
 * UTILITY FUNCTIONS
 */
function formatDate(dateString, options = {}) {
    const defaultOptions = {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    };

    const formatOptions = { ...defaultOptions, ...options };

    try {
        return new Date(dateString).toLocaleDateString('en-US', formatOptions);
    } catch (error) {
        console.error('Date formatting error:', error);
        return 'Invalid date';
    }
}

/**
 * EXPORT FUNCTIONS FOR GLOBAL ACCESS
 */
window.editProfile = editProfile;
window.cancelProfileEdit = cancelProfileEdit;
window.changePicture = changePicture;
window.savePicture = savePicture;
window.manageGoogleConnection = manageGoogleConnection;
window.connectGoogleCalendar = connectGoogleCalendar;
window.exportUserData = exportUserData;
window.clearAllData = clearAllData;
window.deleteAccount = deleteAccount;
window.viewPrivacyPolicy = viewPrivacyPolicy;
window.showHelp = showHelp;
window.showFeedback = showFeedback;
window.showChangelog = showChangelog;
window.showSupport = showSupport;
window.closeModal = closeModal;
window.logout = logout;

console.log('Profile JavaScript loaded successfully');