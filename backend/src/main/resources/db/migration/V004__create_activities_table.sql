-- Create activities table for social activity feed functionality
-- This migration creates the activities table that stores user activities
-- for the social feed feature including availability changes, profile updates, etc.

CREATE TABLE activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL COMMENT 'Activity type: availability_change, profile_update, circle_activity, etc.',
    user_id BIGINT NOT NULL COMMENT 'ID of the user who performed the activity',
    timestamp DATETIME NOT NULL COMMENT 'When the activity occurred',
    data TEXT COMMENT 'JSON data containing activity-specific information',
    priority INT NOT NULL DEFAULT 1 COMMENT 'Priority level (1-5, where 5 is highest)',
    visibility VARCHAR(20) NOT NULL DEFAULT 'friends' COMMENT 'Visibility: public, friends, private',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether the activity is still active/relevant',
    related_entity_id BIGINT COMMENT 'Optional reference to related entity (circle ID, event ID, etc.)',
    related_entity_type VARCHAR(50) COMMENT 'Type of related entity: circle, event, user, etc.',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the record was created',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'When the record was last updated'
);

-- Create indexes for optimal query performance
CREATE INDEX idx_activity_user_timestamp ON activities(user_id, timestamp);
CREATE INDEX idx_activity_type ON activities(type);
CREATE INDEX idx_activity_timestamp ON activities(timestamp);
CREATE INDEX idx_activity_visibility ON activities(visibility);
CREATE INDEX idx_activity_active_timestamp ON activities(is_active, timestamp);
CREATE INDEX idx_activity_priority ON activities(priority);
CREATE INDEX idx_activity_related_entity ON activities(related_entity_id, related_entity_type);

-- Add foreign key constraint to users table (assuming it exists)
-- ALTER TABLE activities ADD CONSTRAINT fk_activities_user_id
--     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Sample data types that activities can represent:
-- 'availability_change' - User changed their availability status
-- 'profile_update' - User updated their profile information
-- 'circle_activity' - Activity related to user circles/groups
-- 'social_activity' - General social interactions
-- 'friend_request' - Friend request sent/accepted
-- 'status_update' - User status or mood updates
-- 'location_update' - User location/check-in updates