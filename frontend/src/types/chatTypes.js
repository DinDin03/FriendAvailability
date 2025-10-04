/**
 * Chat Type Definitions
 *
 * JSDoc type definitions matching backend DTOs for the chat system.
 * These types provide autocomplete and documentation for chat-related data structures.
 *
 * @module chatTypes
 */

/**
 * @typedef {Object} MessageResponseDto
 * @property {number} id - Unique identifier of the message
 * @property {number} senderId - User ID of the message sender
 * @property {string} senderName - Name of the message sender
 * @property {number} roomId - ID of the chat room this message belongs to
 * @property {string} content - Content of the message
 * @property {string} messageType - Type of the message ("TEXT", "SYSTEM_MESSAGE", "IMAGE", "FILE")
 * @property {string} sentAt - When the message was sent (ISO 8601 format)
 */

/**
 * @typedef {Object} SystemMessageDto
 * @property {number} roomId - Room ID
 * @property {string} content - System message content
 * @property {string} messageType - Message type (always "SYSTEM_MESSAGE")
 * @property {string} timestamp - When the system message was created (ISO 8601 format)
 */

/**
 * @typedef {Object} ChatRoomSummaryDTO
 * @property {number} id - Unique identifier of the chat room
 * @property {string} displayName - Display name for the room
 * @property {string} type - Type of chat room ("PRIVATE", "GROUP")
 * @property {number} participantCount - Number of active participants
 * @property {string|null} lastMessageAt - Timestamp of the most recent message (ISO 8601 format)
 * @property {string|null} lastMessagePreview - Preview of the last message (truncated)
 * @property {number} unreadCount - Number of unread messages for the user
 * @property {boolean} isActive - Whether the user is currently online/active in this room
 */

/**
 * @typedef {Object} ChatRoomResponseDTO
 * @property {number} id - Unique identifier of the chat room
 * @property {string|null} name - Name of the chat room (null for private chats)
 * @property {string} displayName - Display name for UI
 * @property {string} type - Type of chat room ("PRIVATE", "GROUP")
 * @property {number} createdBy - User ID of the room creator
 * @property {string} createdByName - Name of the user who created the room
 * @property {string} createdAt - When the room was created (ISO 8601 format)
 * @property {string} updatedAt - When the room was last updated (ISO 8601 format)
 * @property {number} participantCount - Number of active participants in the room
 * @property {string|null} lastMessageAt - Timestamp of the most recent message (ISO 8601 format)
 * @property {string|null} lastMessageContent - Content of the most recent message (truncated)
 * @property {number} unreadCount - Number of unread messages for the requesting user
 * @property {boolean} isParticipant - Whether the requesting user is an active participant
 * @property {string|null} userRole - Role of the requesting user ("ADMIN", "MEMBER")
 */

/**
 * @typedef {Object} ChatRoomListResponseDTO
 * @property {ChatRoomSummaryDTO[]} chatRooms - Array of chat room summaries
 * @property {number} totalElements - Total number of chat rooms
 * @property {number} totalPages - Total number of pages (if paginated)
 * @property {number} currentPage - Current page number (if paginated)
 * @property {number} pageSize - Page size (if paginated)
 * @property {boolean} hasNext - Whether there are more pages
 * @property {boolean} hasPrevious - Whether there are previous pages
 */

/**
 * @typedef {Object} MessageListResponseDTO
 * @property {MessageResponseDto[]} messages - Array of messages
 * @property {number} totalElements - Total number of messages
 * @property {number} totalPages - Total number of pages (if paginated)
 * @property {number} currentPage - Current page number (if paginated)
 * @property {number} pageSize - Page size (if paginated)
 * @property {boolean} hasNext - Whether there are more pages
 * @property {boolean} hasPrevious - Whether there are previous pages
 * @property {number|null} unreadCount - Number of unread messages (if applicable)
 */

/**
 * @typedef {Object} ChatParticipantDTO
 * @property {number} userId - User ID
 * @property {string} userName - User name
 * @property {string} userEmail - User email
 * @property {string} role - Participant role ("ADMIN", "MEMBER")
 * @property {string} joinedAt - When the user joined (ISO 8601 format)
 * @property {boolean} isActive - Whether the participant is active
 */

/**
 * @typedef {Object} ChatParticipantListResponseDTO
 * @property {ChatParticipantDTO[]} participants - Array of participants
 * @property {number} totalParticipants - Total number of participants
 */

/**
 * @typedef {Object} TypingIndicatorDto
 * @property {number} roomId - Room ID
 * @property {number} userId - User ID who is typing
 * @property {boolean} isTyping - Whether user is typing
 */

/**
 * @typedef {Object} TypingResponseDto
 * @property {number} userId - User ID who is typing
 * @property {string} userName - User name who is typing
 * @property {number} roomId - Room ID
 * @property {boolean} isTyping - Whether user is typing
 * @property {string} timestamp - Timestamp (ISO 8601 format)
 */

/**
 * @typedef {Object} MessageReadDto
 * @property {number} roomId - Room ID
 * @property {number} userId - User ID
 */

/**
 * @typedef {Object} ReadReceiptDto
 * @property {number} userId - User ID who read the messages
 * @property {number} roomId - Room ID
 * @property {string} readAt - When messages were read (ISO 8601 format)
 */

/**
 * @typedef {Object} ErrorMessageDto
 * @property {string} error - Error message
 * @property {string} errorCode - Error code
 * @property {string} timestamp - When the error occurred (ISO 8601 format)
 */

/**
 * @typedef {Object} CreatePrivateChatRequest
 * @property {number} userId1 - First user ID
 * @property {number} userId2 - Second user ID
 */

/**
 * @typedef {Object} CreateGroupChatRequest
 * @property {number} creatorId - Creator user ID
 * @property {string} chatName - Group chat name
 * @property {number[]} participantIds - Array of participant user IDs
 */

/**
 * @typedef {Object} ChatMessageDto
 * @property {number} senderId - Sender user ID
 * @property {number} roomId - Room ID
 * @property {string} content - Message content
 */

/**
 * @typedef {Object} UserJoinDto
 * @property {number} roomId - Room ID
 * @property {number} userId - User ID
 */

/**
 * @typedef {Object} UserLeaveDto
 * @property {number} roomId - Room ID
 * @property {number} userId - User ID
 */

// Export empty object to make this a module
export {};
