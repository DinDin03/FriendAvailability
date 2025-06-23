/**
 * Availability/Calendar Management JavaScript
 * Handles all calendar-related functionality including:
 * - Loading and displaying calendar events
 * - Creating, editing, and deleting events
 * - Managing different calendar views (today, week, month)
 * - Complete calendar view with inferred free time
 * - Statistics and quick actions
 */

// Global state variables
let currentUser = null;
let currentView = 'month'; // 'today', 'week', 'month'
let currentDate = new Date();
let allEvents = [];
let todayEvents = [];
let upcomingEvents = [];
let calendarStats = {};

/**
 * INITIALIZATION
 * Runs when the page loads
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('Availability page loaded - initializing...');

    // Check authentication and initialize
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
 * Verify user is logged in
 */
async function checkAuthentication() {
    try {
        console.log('Checking user authentication...');

        // For now, we'll use a workaround since we don't have a direct current-user endpoint
        // We'll try to access the users endpoint and assume authentication if it works
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

        // Get the first user for demo purposes (in real app, you'd have proper user session)
        const users = await response.json();
        if (users.length > 0) {
            currentUser = users[0]; // Using first user as demo
            console.log('User authenticated (demo):', currentUser);
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
 * Update header with user information
 */
function updateUserDisplay() {
    const userNameElement = document.getElementById('userName');
    const userAvatarElement = document.getElementById('userAvatar');

    if (currentUser) {
        userNameElement.textContent = currentUser.name || 'User';
        userAvatarElement.src = currentUser.profilePictureUrl || 'https://via.placeholder.com/32';
        console.log('User display updated for:', currentUser.name);
    }
}

/**
 * INITIALIZE PAGE
 * Set up the page with initial data
 */
async function initializePage() {
    console.log('Initializing availability page...');

    try {
        showLoadingOverlay('Loading your calendar...');

        // Set up initial date
        setupInitialDate();

        // Load all calendar data
        await Promise.all([
            loadCalendarView(),
            loadTodaySchedule(),
            loadUpcomingEvents(),
            loadStatistics()
        ]);

        // Set up event listeners
        setupEventListeners();

        console.log('Availability page initialization complete');

    } catch (error) {
        console.error('Failed to initialize page:', error);
        showMessage('Failed to load calendar data. Please refresh the page.', 'error');
    } finally {
        hideLoadingOverlay();
    }
}

/**
 * SETUP INITIAL DATE
 * Configure date picker and today's date display
 */
function setupInitialDate() {
    const dateInput = document.getElementById('calendarDate');
    const todayDateElement = document.getElementById('todayDate');

    // Set date input to today
    const today = new Date();
    dateInput.value = today.toISOString().split('T')[0];

    // Update today's date display
    todayDateElement.textContent = formatDate(today, {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });

    currentDate = today;
}

/**
 * LOAD CALENDAR VIEW
 * Load events based on current view (today/week/month)
 */
async function loadCalendarView() {
    console.log(`Loading calendar view: ${currentView}`);

    try {
        let events = [];

        switch (currentView) {
            case 'today':
                events = await loadTodayEvents();
                break;
            case 'week':
                events = await loadWeekEvents();
                break;
            case 'month':
                events = await loadMonthEvents();
                break;
        }

        allEvents = events;
        displayCalendar(events);

    } catch (error) {
        console.error('Error loading calendar view:', error);
        showElementError('calendarContainer', 'Failed to load calendar');
    }
}

/**
 * LOAD TODAY EVENTS
 * Load events for today using your backend endpoint
 */
async function loadTodayEvents() {
    console.log('Loading today events...');

    try {
        // Call your AvailabilityController GET /api/availability/{userId}/today
        const response = await fetch(`/api/availability/${currentUser.id}/today`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load today events: ${response.status}`);
        }

        const events = await response.json();
        console.log('Today events loaded:', events);
        return events;

    } catch (error) {
        console.error('Error loading today events:', error);
        throw error;
    }
}

/**
 * LOAD WEEK EVENTS
 * Load events for the current week
 */
async function loadWeekEvents() {
    console.log('Loading week events...');

    const startOfWeek = getStartOfWeek(currentDate);
    const endOfWeek = getEndOfWeek(currentDate);

    return loadEventsForRange(startOfWeek, endOfWeek);
}

/**
 * LOAD MONTH EVENTS
 * Load events for the current month using your backend endpoint
 */
async function loadMonthEvents() {
    console.log('Loading month events...');

    try {
        const year = currentDate.getFullYear();
        const month = currentDate.getMonth() + 1; // getMonth() returns 0-11

        // Call your AvailabilityController GET /api/availability/{userId}/month
        const response = await fetch(`/api/availability/${currentUser.id}/month?year=${year}&month=${month}`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load month events: ${response.status}`);
        }

        const events = await response.json();
        console.log('Month events loaded:', events);
        return events;

    } catch (error) {
        console.error('Error loading month events:', error);
        throw error;
    }
}

/**
 * LOAD EVENTS FOR RANGE
 * Load events for a specific date range
 */
async function loadEventsForRange(startDate, endDate) {
    console.log(`Loading events from ${startDate.toISOString()} to ${endDate.toISOString()}`);

    try {
        const startISO = startDate.toISOString();
        const endISO = endDate.toISOString();

        // Call your AvailabilityController GET /api/availability/{userId}
        const response = await fetch(`/api/availability/${currentUser.id}?start=${startISO}&end=${endISO}`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load events: ${response.status}`);
        }

        const events = await response.json();
        console.log('Range events loaded:', events);
        return events;

    } catch (error) {
        console.error('Error loading events for range:', error);
        throw error;
    }
}

/**
 * LOAD TODAY SCHEDULE
 * Load and display today's schedule in sidebar
 */
async function loadTodaySchedule() {
    console.log('Loading today schedule...');

    try {
        const events = await loadTodayEvents();
        todayEvents = events;
        displayTodaySchedule(events);

    } catch (error) {
        console.error('Error loading today schedule:', error);
        showElementError('todaySchedule', 'Failed to load today\'s schedule');
    }
}

/**
 * LOAD UPCOMING EVENTS
 * Load and display upcoming events
 */
async function loadUpcomingEvents() {
    console.log('Loading upcoming events...');

    try {
        // Call your AvailabilityController GET /api/availability/{userId}/upcoming
        const response = await fetch(`/api/availability/${currentUser.id}/upcoming`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load upcoming events: ${response.status}`);
        }

        const events = await response.json();
        upcomingEvents = events;
        console.log('Upcoming events loaded:', events);

        displayUpcomingEvents(events);

    } catch (error) {
        console.error('Error loading upcoming events:', error);
        showElementError('upcomingEventsContainer', 'Failed to load upcoming events');
    }
}

/**
 * LOAD STATISTICS
 * Load calendar statistics
 */
async function loadStatistics() {
    console.log('Loading calendar statistics...');

    try {
        // Call your AvailabilityController GET /api/availability/{userId}/stats
        const response = await fetch(`/api/availability/${currentUser.id}/stats`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load statistics: ${response.status}`);
        }

        const stats = await response.json();
        calendarStats = stats;
        console.log('Statistics loaded:', stats);

        updateStatistics(stats);

    } catch (error) {
        console.error('Error loading statistics:', error);
        // Set default stats if loading fails
        updateStatistics({
            totalEvents: 0,
            freeTimeSlots: 0,
            busyTimeSlots: 0
        });
    }
}

/**
 * DISPLAY FUNCTIONS
 * Update UI with loaded data
 */

function displayCalendar(events) {
    const container = document.getElementById('calendarContainer');

    if (!events || events.length === 0) {
        container.innerHTML = '<div class="empty-state">No events for this period</div>';
        return;
    }

    // Generate calendar HTML based on current view
    let calendarHTML = '';

    switch (currentView) {
        case 'today':
            calendarHTML = generateTodayView(events);
            break;
        case 'week':
            calendarHTML = generateWeekView(events);
            break;
        case 'month':
            calendarHTML = generateMonthView(events);
            break;
    }

    container.innerHTML = calendarHTML;
}

function generateTodayView(events) {
    const hours = Array.from({length: 24}, (_, i) => i);

    return `
        <div class="today-view">
            <div class="time-grid">
                ${hours.map(hour => {
        const hourEvents = events.filter(event => {
            const eventStart = new Date(event.startTime);
            return eventStart.getHours() === hour;
        });

        return `
                        <div class="time-slot" data-hour="${hour}">
                            <div class="time-label">${formatHour(hour)}</div>
                            <div class="events-column">
                                ${hourEvents.map(event => `
                                    <div class="event-block ${event.isBusy ? 'busy' : 'free'}" onclick="editEvent(${event.id})">
                                        <span class="event-title">${event.title || 'Untitled'}</span>
                                        <span class="event-time">${formatTime(event.startTime)} - ${formatTime(event.endTime)}</span>
                                    </div>
                                `).join('')}
                            </div>
                        </div>
                    `;
    }).join('')}
            </div>
        </div>
    `;
}

function generateWeekView(events) {
    // Simplified week view - you can enhance this later
    return `
        <div class="week-view">
            <div class="week-header">Week View</div>
            <div class="events-list">
                ${events.map(event => `
                    <div class="event-item ${event.isBusy ? 'busy' : 'free'}" onclick="editEvent(${event.id})">
                        <div class="event-title">${event.title || 'Untitled'}</div>
                        <div class="event-time">${formatDate(event.startTime)} ${formatTime(event.startTime)} - ${formatTime(event.endTime)}</div>
                        ${event.location ? `<div class="event-location">📍 ${event.location}</div>` : ''}
                    </div>
                `).join('')}
            </div>
        </div>
    `;
}

function generateMonthView(events) {
    // Simplified month view - you can enhance this later
    return `
        <div class="month-view">
            <div class="month-header">${formatDate(currentDate, { month: 'long', year: 'numeric' })}</div>
            <div class="events-grid">
                ${events.map(event => `
                    <div class="event-card ${event.isBusy ? 'busy' : 'free'}" onclick="editEvent(${event.id})">
                        <div class="event-date">${formatDate(event.startTime, { day: 'numeric', month: 'short' })}</div>
                        <div class="event-title">${event.title || 'Untitled'}</div>
                        <div class="event-time">${formatTime(event.startTime)} - ${formatTime(event.endTime)}</div>
                        ${event.location ? `<div class="event-location">📍 ${event.location}</div>` : ''}
                    </div>
                `).join('')}
            </div>
        </div>
    `;
}

function displayTodaySchedule(events) {
    const container = document.getElementById('todaySchedule');

    if (!events || events.length === 0) {
        container.innerHTML = '<div class="empty-state">No events today</div>';
        return;
    }

    const scheduleHTML = events.map(event => `
        <div class="schedule-item ${event.isBusy ? 'busy' : 'free'}" onclick="editEvent(${event.id})">
            <div class="schedule-time">
                <span class="start-time">${formatTime(event.startTime)}</span>
                <span class="end-time">${formatTime(event.endTime)}</span>
            </div>
            <div class="schedule-details">
                <h4>${event.title || 'Untitled Event'}</h4>
                ${event.description ? `<p>${event.description}</p>` : ''}
                ${event.location ? `<p class="location">📍 ${event.location}</p>` : ''}
            </div>
            <div class="schedule-status">
                <span class="status-badge ${event.isBusy ? 'busy' : 'free'}">
                    ${event.isBusy ? '🔴 Busy' : '🟢 Free'}
                </span>
            </div>
        </div>
    `).join('');

    container.innerHTML = scheduleHTML;
}

function displayUpcomingEvents(events) {
    const container = document.getElementById('upcomingEventsContainer');

    if (!events || events.length === 0) {
        container.innerHTML = '<div class="empty-state">No upcoming events</div>';
        return;
    }

    const upcomingHTML = events.slice(0, 5).map(event => `
        <div class="upcoming-event" onclick="editEvent(${event.id})">
            <div class="event-date">
                <span class="day">${formatDate(event.startTime, { day: 'numeric' })}</span>
                <span class="month">${formatDate(event.startTime, { month: 'short' })}</span>
            </div>
            <div class="event-info">
                <h4>${event.title || 'Untitled Event'}</h4>
                <p class="event-time">${formatTime(event.startTime)} - ${formatTime(event.endTime)}</p>
                ${event.location ? `<p class="event-location">📍 ${event.location}</p>` : ''}
            </div>
            <div class="event-status">
                <span class="status-dot ${event.isBusy ? 'busy' : 'free'}"></span>
            </div>
        </div>
    `).join('');

    container.innerHTML = upcomingHTML;
}

function updateStatistics(stats) {
    document.getElementById('totalEvents').textContent = stats.totalEvents || 0;
    document.getElementById('freeSlots').textContent = stats.freeTimeSlots || 0;
    document.getElementById('busySlots').textContent = stats.busyTimeSlots || 0;
    document.getElementById('upcomingEvents').textContent = upcomingEvents.length || 0;
}

/**
 * EVENT MANAGEMENT FUNCTIONS
 * Create, edit, and delete events
 */

async function createEvent(eventData) {
    console.log('Creating event:', eventData);

    try {
        showLoadingOverlay('Creating event...');

        // Call your AvailabilityController POST /api/availability
        const response = await fetch('/api/availability', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(eventData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Failed to create event: ${response.status}`);
        }

        const newEvent = await response.json();
        console.log('Event created:', newEvent);

        showMessage('Event created successfully!', 'success');

        // Refresh all data
        await refreshCalendar();

        return newEvent;

    } catch (error) {
        console.error('Error creating event:', error);
        showMessage(`Failed to create event: ${error.message}`, 'error');
        throw error;
    } finally {
        hideLoadingOverlay();
    }
}

async function updateEvent(eventId, eventData) {
    console.log('Updating event:', eventId, eventData);

    try {
        showLoadingOverlay('Updating event...');

        // Call your AvailabilityController PUT /api/availability/{id}
        const response = await fetch(`/api/availability/${eventId}`, {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(eventData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Failed to update event: ${response.status}`);
        }

        const updatedEvent = await response.json();
        console.log('Event updated:', updatedEvent);

        showMessage('Event updated successfully!', 'success');

        // Refresh all data
        await refreshCalendar();

        return updatedEvent;

    } catch (error) {
        console.error('Error updating event:', error);
        showMessage(`Failed to update event: ${error.message}`, 'error');
        throw error;
    } finally {
        hideLoadingOverlay();
    }
}

async function deleteEvent() {
    const eventId = document.getElementById('editEventId').value;

    if (!eventId) {
        showMessage('No event selected for deletion', 'error');
        return;
    }

    if (!confirm('Are you sure you want to delete this event?')) {
        return;
    }

    console.log('Deleting event:', eventId);

    try {
        showLoadingOverlay('Deleting event...');

        // Call your AvailabilityController DELETE /api/availability/{id}
        const response = await fetch(`/api/availability/${eventId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error(`Failed to delete event: ${response.status}`);
        }

        console.log('Event deleted successfully');
        showMessage('Event deleted successfully!', 'success');

        // Close modal and refresh data
        closeModal('editEventModal');
        await refreshCalendar();

    } catch (error) {
        console.error('Error deleting event:', error);
        showMessage('Failed to delete event', 'error');
    } finally {
        hideLoadingOverlay();
    }
}

/**
 * MODAL AND FORM HANDLING
 */

function showCreateEventModal() {
    const modal = document.getElementById('createEventModal');
    modal.classList.add('show');

    // Pre-fill with current date/time
    const now = new Date();
    const startTime = new Date(now.getTime() + 60 * 60 * 1000); // 1 hour from now
    const endTime = new Date(startTime.getTime() + 60 * 60 * 1000); // 1 hour duration

    document.getElementById('eventStartTime').value = formatDateTimeLocal(startTime);
    document.getElementById('eventEndTime').value = formatDateTimeLocal(endTime);
}

function editEvent(eventId) {
    const event = allEvents.find(e => e.id === eventId);
    if (!event) {
        showMessage('Event not found', 'error');
        return;
    }

    // Fill edit form with event data
    document.getElementById('editEventId').value = event.id;
    document.getElementById('editEventTitle').value = event.title || '';
    document.getElementById('editEventType').value = event.isBusy ? 'busy' : 'free';
    document.getElementById('editEventStartTime').value = formatDateTimeLocal(new Date(event.startTime));
    document.getElementById('editEventEndTime').value = formatDateTimeLocal(new Date(event.endTime));
    document.getElementById('editEventDescription').value = event.description || '';
    document.getElementById('editEventLocation').value = event.location || '';
    document.getElementById('editEventAllDay').checked = event.isAllDay || false;
    document.getElementById('editEventReminder').value = event.reminderMinutes || 30;

    // Show modal
    const modal = document.getElementById('editEventModal');
    modal.classList.add('show');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    modal.classList.remove('show');
}

/**
 * EVENT LISTENERS AND HANDLERS
 */

function setupEventListeners() {
    // Create event form
    document.getElementById('createEventForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const eventData = {
            userId: currentUser.id,
            title: document.getElementById('eventTitle').value,
            startTime: document.getElementById('eventStartTime').value,
            endTime: document.getElementById('eventEndTime').value,
            description: document.getElementById('eventDescription').value,
            location: document.getElementById('eventLocation').value,
            isBusy: document.getElementById('eventType').value === 'busy',
            isAllDay: document.getElementById('eventAllDay').checked,
            reminderMinutes: parseInt(document.getElementById('eventReminder').value)
        };

        try {
            await createEvent(eventData);
            closeModal('createEventModal');
            // Reset form
            document.getElementById('createEventForm').reset();
        } catch (error) {
            // Error already handled in createEvent function
        }
    });

    // Edit event form
    document.getElementById('editEventForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const eventId = document.getElementById('editEventId').value;
        const eventData = {
            title: document.getElementById('editEventTitle').value,
            startTime: document.getElementById('editEventStartTime').value,
            endTime: document.getElementById('editEventEndTime').value,
            description: document.getElementById('editEventDescription').value,
            location: document.getElementById('editEventLocation').value,
            isBusy: document.getElementById('editEventType').value === 'busy',
            isAllDay: document.getElementById('editEventAllDay').checked,
            reminderMinutes: parseInt(document.getElementById('editEventReminder').value)
        };

        try {
            await updateEvent(eventId, eventData);
            closeModal('editEventModal');
        } catch (error) {
            // Error already handled in updateEvent function
        }
    });
}

/**
 * CALENDAR NAVIGATION
 */

function setCalendarView(view) {
    currentView = view;

    // Update active button
    document.querySelectorAll('.view-selector .btn').forEach(btn => btn.classList.remove('active'));
    document.getElementById(view + 'Btn').classList.add('active');

    // Reload calendar
    loadCalendarView();
}

function navigateCalendar(direction) {
    switch (currentView) {
        case 'today':
            currentDate.setDate(currentDate.getDate() + (direction === 'next' ? 1 : -1));
            break;
        case 'week':
            currentDate.setDate(currentDate.getDate() + (direction === 'next' ? 7 : -7));
            break;
        case 'month':
            currentDate.setMonth(currentDate.getMonth() + (direction === 'next' ? 1 : -1));
            break;
    }

    // Update date input
    document.getElementById('calendarDate').value = currentDate.toISOString().split('T')[0];

    // Reload calendar
    loadCalendarView();
}

function onDateChange() {
    const dateInput = document.getElementById('calendarDate');
    currentDate = new Date(dateInput.value);
    loadCalendarView();
}

/**
 * QUICK ACTIONS
 */

function addQuickEvent(type) {
    const now = new Date();
    const startTime = new Date(now.getTime() + 60 * 60 * 1000); // 1 hour from now
    const endTime = new Date(startTime.getTime() + 60 * 60 * 1000); // 1 hour duration

    const templates = {
        meeting: {
            title: 'Meeting',
            isBusy: true,
            description: 'Team meeting'
        },
        break: {
            title: 'Break',
            isBusy: false,
            description: 'Coffee break'
        },
        lunch: {
            title: 'Lunch',
            isBusy: true,
            description: 'Lunch time'
        },
        free: {
            title: 'Available',
            isBusy: false,
            description: 'Free time'
        },
        busy: {
            title: 'Busy',
            isBusy: true,
            description: 'Not available'
        }
    };

    const template = templates[type];
    if (!template) return;

    const eventData = {
        userId: currentUser.id,
        title: template.title,
        description: template.description,
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
        isBusy: template.isBusy,
        isAllDay: false,
        reminderMinutes: 15
    };

    createEvent(eventData);
}

/**
 * COMPLETE CALENDAR VIEW
 */

async function viewCompleteCalendar() {
    const modal = document.getElementById('completeCalendarModal');
    modal.classList.add('show');

    // Set default date range (next 7 days)
    const today = new Date();
    const nextWeek = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000);

    document.getElementById('completeStartDate').value = today.toISOString().split('T')[0];
    document.getElementById('completeEndDate').value = nextWeek.toISOString().split('T')[0];
}

async function loadCompleteView() {
    const startDate = document.getElementById('completeStartDate').value;
    const endDate = document.getElementById('completeEndDate').value;

    if (!startDate || !endDate) {
        showMessage('Please select both start and end dates', 'error');
        return;
    }

    console.log('Loading complete calendar view...');

    try {
        showElementLoading('completeCalendarContainer', 'Loading complete view with free time...');

        const startISO = new Date(startDate).toISOString();
        const endISO = new Date(endDate + 'T23:59:59').toISOString();

        // Call your AvailabilityController GET /api/availability/{userId}/complete
        const response = await fetch(`/api/availability/${currentUser.id}/complete?start=${startISO}&end=${endISO}`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load complete view: ${response.status}`);
        }

        const completeView = await response.json();
        console.log('Complete view loaded:', completeView);

        displayCompleteView(completeView);

    } catch (error) {
        console.error('Error loading complete view:', error);
        showElementError('completeCalendarContainer', 'Failed to load complete view');
    }
}

function displayCompleteView(events) {
    const container = document.getElementById('completeCalendarContainer');

    if (!events || events.length === 0) {
        container.innerHTML = '<div class="empty-state">No events in selected date range</div>';
        return;
    }

    const completeHTML = `
        <div class="complete-view">
            <div class="complete-header">
                <h4>Complete Schedule (${events.length} slots)</h4>
                <p>Showing all events and inferred free time</p>
            </div>
            <div class="complete-timeline">
                ${events.map(event => `
                    <div class="timeline-event ${event.isBusy ? 'busy' : 'free'}" ${event.id ? `onclick="editEvent(${event.id})"` : ''}>
                        <div class="event-time">
                            <span class="start">${formatDate(event.startTime, { month: 'short', day: 'numeric' })}</span>
                            <span class="duration">${formatTime(event.startTime)} - ${formatTime(event.endTime)}</span>
                        </div>
                        <div class="event-content">
                            <h5>${event.title || 'Untitled'}</h5>
                            ${event.description ? `<p>${event.description}</p>` : ''}
                            <div class="event-meta">
                                <span class="event-type ${event.isBusy ? 'busy' : 'free'}">
                                    ${event.isBusy ? '🔴 Busy' : '🟢 Available'}
                                </span>
                                ${event.location ? `<span class="event-location">📍 ${event.location}</span>` : ''}
                                <span class="event-duration">${calculateDuration(event.startTime, event.endTime)}</span>
                            </div>
                        </div>
                    </div>
                `).join('')}
            </div>
        </div>
    `;

    container.innerHTML = completeHTML;
}

/**
 * UTILITY FUNCTIONS
 */

function refreshCalendar() {
    return Promise.all([
        loadCalendarView(),
        loadTodaySchedule(),
        loadUpcomingEvents(),
        loadStatistics()
    ]);
}

function formatHour(hour) {
    const ampm = hour >= 12 ? 'PM' : 'AM';
    const displayHour = hour % 12 || 12;
    return `${displayHour}:00 ${ampm}`;
}

function formatDateTimeLocal(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function getStartOfWeek(date) {
    const start = new Date(date);
    const day = start.getDay();
    const diff = start.getDate() - day;
    start.setDate(diff);
    start.setHours(0, 0, 0, 0);
    return start;
}

function getEndOfWeek(date) {
    const end = new Date(date);
    const day = end.getDay();
    const diff = end.getDate() - day + 6;
    end.setDate(diff);
    end.setHours(23, 59, 59, 999);
    return end;
}

function calculateDuration(startTime, endTime) {
    const start = new Date(startTime);
    const end = new Date(endTime);
    const diffMs = end - start;
    const diffMins = Math.floor(diffMs / 60000);
    const hours = Math.floor(diffMins / 60);
    const minutes = diffMins % 60;

    if (hours > 0) {
        return minutes > 0 ? `${hours}h ${minutes}m` : `${hours}h`;
    } else {
        return `${minutes}m`;
    }
}

function getCurrentEvents() {
    return todayEvents.filter(event => {
        const now = new Date();
        const start = new Date(event.startTime);
        const end = new Date(event.endTime);
        return now >= start && now <= end;
    });
}

function getNextEvent() {
    const now = new Date();
    const futureEvents = upcomingEvents.filter(event => new Date(event.startTime) > now);
    return futureEvents.length > 0 ? futureEvents[0] : null;
}

/**
 * EXPORT FUNCTIONS
 * Make functions available globally for onclick handlers
 */

// Modal functions
window.showCreateEventModal = showCreateEventModal;
window.editEvent = editEvent;
window.deleteEvent = deleteEvent;
window.closeModal = closeModal;

// Calendar navigation
window.setCalendarView = setCalendarView;
window.navigateCalendar = navigateCalendar;
window.onDateChange = onDateChange;

// Quick actions
window.addQuickEvent = addQuickEvent;
window.viewCompleteCalendar = viewCompleteCalendar;
window.loadCompleteView = loadCompleteView;

// Utility functions
window.refreshCalendar = refreshCalendar;
window.logout = logout;

console.log('Availability JavaScript loaded successfully');