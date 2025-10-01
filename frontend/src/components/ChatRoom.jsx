import { useState, useEffect, useRef } from 'react';
import { ArrowLeft, Send, Loader, AlertCircle, Wifi, WifiOff } from 'lucide-react';
import { chatService } from '../services/chatService';
import toast from 'react-hot-toast';

/**
 * ChatRoom - Real-time chat interface component
 *
 * Features:
 * - Display message history with chronological ordering
 * - Send and receive messages in real-time via WebSocket
 * - Distinguish own messages from others
 * - System messages for join/leave notifications
 * - Auto-scroll to bottom on new messages
 * - Keyboard shortcuts (Enter to send)
 * - Connection status indicator
 * - Proper cleanup on unmount
 *
 * @param {Object} props
 * @param {number} props.roomId - Chat room ID
 * @param {number} props.userId - Current user ID
 * @param {Function} props.onBack - Callback to return to rooms list
 */
export const ChatRoom = ({ roomId, userId, onBack }) => {
  // Refs
  const messagesEndRef = useRef(null);
  const messageInputRef = useRef(null);
  const pollingIntervalRef = useRef(null);
  const lastMessageTimeRef = useRef(null);

  // State management
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [roomDetails, setRoomDetails] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);
  const [error, setError] = useState(null);

  /**
   * Initialize chat room on mount
   */
  useEffect(() => {
    if (roomId && userId) {
      initializeChatRoom();
    }

    // Cleanup on unmount
    return () => {
      cleanupChatRoom();
    };
  }, [roomId, userId]);

  /**
   * Auto-scroll to bottom when new messages arrive
   */
  useEffect(() => {
    // Small delay to ensure DOM is updated
    const timer = setTimeout(() => {
      scrollToBottom();
    }, 100);
    return () => clearTimeout(timer);
  }, [messages]);

  /**
   * Sync missed messages when tab becomes visible
   */
  useEffect(() => {
    const handleVisibilityChange = async () => {
      if (document.visibilityState === 'visible' && roomId && userId) {
        console.log('Tab became visible - syncing missed messages');
        await syncMissedMessages();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [roomId, userId]);

  /**
   * Periodic polling for missed messages (fallback)
   * Polls every 30 seconds when chat is active
   */
  useEffect(() => {
    if (!roomId || !userId) return;

    // Start polling interval
    pollingIntervalRef.current = setInterval(async () => {
      console.log('Polling for new messages...');
      await syncMissedMessages();
    }, 30000); // 30 seconds

    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
        pollingIntervalRef.current = null;
      }
    };
  }, [roomId, userId]);

  /**
   * Initialize chat room with full setup
   */
  const initializeChatRoom = async () => {
    try {
      setIsLoading(true);
      setError(null);

      console.log('Initializing chat room:', roomId);

      // 1. Load room details
      const details = await chatService.getChatRoomDetails(roomId, userId);
      setRoomDetails(details);
      console.log('Room details loaded:', details);

      // 2. Load recent messages
      const response = await chatService.getRecentMessages(roomId, userId, 50);
      const loadedMessages = response.messages || [];
      setMessages(loadedMessages);
      console.log('Messages loaded:', loadedMessages.length);

      // Track last message time for sync
      if (loadedMessages.length > 0) {
        const lastMsg = loadedMessages[loadedMessages.length - 1];
        lastMessageTimeRef.current = lastMsg.sentAt;
        console.log('Last message time set:', lastMsg.sentAt);
      }

      // 3. Initialize WebSocket if not connected
      if (!chatService.isWebSocketConnected()) {
        console.log('Initializing WebSocket...');
        await chatService.initializeWebSocket();
      }

      // 4. Set room sync data for reconnection handling
      chatService.setRoomSyncData(roomId, userId, lastMessageTimeRef.current);

      // 5. Subscribe to room messages
      console.log('Subscribing to room:', roomId);
      await chatService.subscribeToRoom(roomId, handleNewMessage);
      setWsConnected(true);

      // 5. Send join notification
      console.log('Sending join notification...');
      chatService.joinRoom(roomId, userId);

      // 6. Mark messages as read
      await chatService.markMessagesAsRead(roomId, userId);

      console.log('Chat room initialized successfully');

    } catch (error) {
      console.error('Failed to initialize chat room:', error);
      setError(error.message || 'Failed to load chat');
      toast.error(error.message || 'Failed to load chat');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Cleanup chat room on unmount
   */
  const cleanupChatRoom = () => {
    if (roomId && userId) {
      console.log('Cleaning up chat room:', roomId);

      // Clear polling interval
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
        pollingIntervalRef.current = null;
      }

      // Unsubscribe from room messages
      chatService.unsubscribeFromRoom(roomId);

      // Send leave notification
      chatService.leaveRoom(roomId, userId);

      setWsConnected(false);
    }
  };

  /**
   * Sync missed messages
   * Fetches messages sent after the last known message
   */
  const syncMissedMessages = async () => {
    if (!lastMessageTimeRef.current) {
      console.log('No last message time, skipping sync');
      return;
    }

    try {
      console.log('Syncing missed messages since:', lastMessageTimeRef.current);

      const newMessages = await chatService.syncMissedMessages(
        roomId,
        userId,
        lastMessageTimeRef.current
      );

      if (newMessages.length > 0) {
        console.log(`Found ${newMessages.length} missed messages`);

        // Add new messages with deduplication
        setMessages(prev => {
          const existingIds = new Set(prev.map(m => m.id));
          const uniqueNewMessages = newMessages.filter(m => !existingIds.has(m.id));

          if (uniqueNewMessages.length > 0) {
            console.log(`Adding ${uniqueNewMessages.length} unique messages`);
            // Update last message time
            const latestMsg = uniqueNewMessages[uniqueNewMessages.length - 1];
            if (latestMsg.sentAt) {
              lastMessageTimeRef.current = latestMsg.sentAt;
            }
            return [...prev, ...uniqueNewMessages];
          }

          return prev;
        });
      } else {
        console.log('No new messages found');
      }
    } catch (error) {
      console.error('Failed to sync missed messages:', error);
      // Don't show error to user - this is a background operation
    }
  };

  /**
   * Handle new message from WebSocket
   * Handles both MessageResponseDto and SystemMessageDto
   * Includes deduplication to prevent duplicate messages
   */
  const handleNewMessage = (message) => {
    console.log('Received new message:', message);

    // Update last message time
    if (message.sentAt) {
      lastMessageTimeRef.current = message.sentAt;
    }

    // Add message with deduplication
    setMessages(prev => {
      // Check if message already exists (by ID)
      const messageExists = prev.some(m => m.id === message.id);
      if (messageExists) {
        console.log('Duplicate message detected, skipping:', message.id);
        return prev;
      }
      return [...prev, message];
    });
  };

  /**
   * Handle sending a message
   */
  const handleSendMessage = async () => {
    const messageContent = newMessage.trim();

    // Validate message
    if (!messageContent) {
      return;
    }

    // Check WebSocket connection
    if (!wsConnected) {
      toast.error('Not connected to chat. Please refresh.');
      return;
    }

    try {
      setIsSending(true);

      console.log('Sending message:', messageContent);

      // Send via WebSocket
      chatService.sendMessage(roomId, userId, messageContent);

      // Clear input immediately
      setNewMessage('');

      // Focus back on input
      messageInputRef.current?.focus();

    } catch (error) {
      console.error('Failed to send message:', error);
      toast.error(error.message || 'Failed to send message');

      // Keep the message in input on error
      setNewMessage(messageContent);
    } finally {
      setIsSending(false);
    }
  };

  /**
   * Handle keyboard shortcuts
   */
  const handleKeyPress = (e) => {
    // Enter without Shift: Send message
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
    // Shift + Enter: Allow new line (default behavior)
  };

  /**
   * Auto-scroll to bottom
   */
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  /**
   * Format message timestamp
   */
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return '';

    try {
      const date = new Date(timestamp);
      return date.toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      console.warn('Failed to format timestamp:', error);
      return '';
    }
  };

  /**
   * Retry loading chat room
   */
  const handleRetry = () => {
    initializeChatRoom();
  };

  // Loading State
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Loader className="w-12 h-12 text-blue-600 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Loading chat...</p>
        </div>
      </div>
    );
  }

  // Error State
  if (error && !roomDetails) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-sm p-8 max-w-md w-full">
          <AlertCircle className="w-12 h-12 text-red-600 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-gray-900 text-center mb-2">
            Failed to load chat
          </h3>
          <p className="text-gray-600 text-center mb-6">{error}</p>
          <div className="flex space-x-3">
            <button
              onClick={onBack}
              className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Back to Chats
            </button>
            <button
              onClick={handleRetry}
              className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Header - Fixed */}
      <header className="bg-white shadow-sm border-b sticky top-0 z-10">
        <div className="max-w-4xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <button
                onClick={onBack}
                className="text-blue-600 hover:text-blue-700 transition-colors"
                title="Back to chats"
              >
                <ArrowLeft className="w-6 h-6" />
              </button>
              <div className="min-w-0">
                <h2 className="font-semibold text-gray-900 truncate">
                  {roomDetails?.displayName || roomDetails?.name || 'Chat'}
                </h2>
                {roomDetails?.type === 'GROUP' && roomDetails?.participantCount && (
                  <p className="text-sm text-gray-600">
                    {roomDetails.participantCount} {roomDetails.participantCount === 1 ? 'participant' : 'participants'}
                  </p>
                )}
              </div>
            </div>

            {/* Connection Status */}
            <div className="flex items-center space-x-2">
              {wsConnected ? (
                <>
                  <Wifi className="w-4 h-4 text-green-600" />
                  <span className="text-xs text-green-600 hidden sm:inline">Connected</span>
                </>
              ) : (
                <>
                  <WifiOff className="w-4 h-4 text-red-600" />
                  <span className="text-xs text-red-600 hidden sm:inline">Disconnected</span>
                </>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Messages Area - Scrollable */}
      <main className="flex-1 overflow-y-auto p-4">
        <div className="max-w-4xl mx-auto">
          {messages.length === 0 ? (
            <div className="text-center text-gray-500 mt-12">
              <p className="text-lg mb-2">No messages yet</p>
              <p className="text-sm">Start the conversation!</p>
            </div>
          ) : (
            messages.map((message, index) => {
              const prevMessage = index > 0 ? messages[index - 1] : null;
              return (
                <MessageBubble
                  key={message.id || `msg-${index}`}
                  message={message}
                  prevMessage={prevMessage}
                  userId={userId}
                  formatTimestamp={formatTimestamp}
                />
              );
            })
          )}
          {/* Auto-scroll anchor */}
          <div ref={messagesEndRef} />
        </div>
      </main>

      {/* Message Input - Fixed */}
      <footer className="bg-white border-t p-4">
        <div className="max-w-4xl mx-auto flex items-end space-x-2">
          <textarea
            ref={messageInputRef}
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Type a message..."
            rows={1}
            disabled={!wsConnected}
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-600 focus:border-blue-600 resize-none disabled:opacity-50 disabled:cursor-not-allowed max-h-32"
            style={{ minHeight: '42px' }}
          />
          <button
            onClick={handleSendMessage}
            disabled={!newMessage.trim() || isSending || !wsConnected}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center space-x-2 h-[42px]"
            title={!wsConnected ? 'Not connected' : 'Send message (Enter)'}
          >
            {isSending ? (
              <Loader className="w-5 h-5 animate-spin" />
            ) : (
              <Send className="w-5 h-5" />
            )}
            <span className="hidden sm:inline">Send</span>
          </button>
        </div>
        {!wsConnected && (
          <div className="max-w-4xl mx-auto mt-2">
            <p className="text-xs text-red-600 text-center">
              Disconnected from chat. Messages cannot be sent.
            </p>
          </div>
        )}
      </footer>
    </div>
  );
};

/**
 * MessageBubble - Individual message component
 * Handles regular messages and system messages
 */
const MessageBubble = ({ message, prevMessage, userId, formatTimestamp }) => {
  // Determine message type
  const isSystemMessage = message.messageType === 'SYSTEM_MESSAGE' || !message.senderId;
  const isOwnMessage = message.senderId === userId;

  // Calculate time gap (in minutes) between this message and previous
  const getTimeGapMinutes = () => {
    if (!prevMessage || !message.sentAt || !prevMessage.sentAt) return Infinity;
    const currentTime = new Date(message.sentAt).getTime();
    const prevTime = new Date(prevMessage.sentAt).getTime();
    return (currentTime - prevTime) / (1000 * 60); // Convert to minutes
  };

  const timeGapMinutes = getTimeGapMinutes();
  const showTimestamp = timeGapMinutes > 3; // Show timestamp if gap > 3 minutes
  const marginTop = showTimestamp ? 'mt-4' : 'mt-1'; // Reduced gap between consecutive messages

  // System Message (join/leave notifications)
  if (isSystemMessage) {
    return (
      <div className="flex justify-center my-4">
        <div className="bg-gray-100 text-gray-600 text-sm px-4 py-2 rounded-full italic">
          {message.content}
        </div>
      </div>
    );
  }

  // Regular Message (own or other)
  return (
    <div className={`flex items-end ${isOwnMessage ? 'justify-end' : 'justify-start'} ${marginTop}`}>
      {/* Timestamp on the left for received messages */}
      {!isOwnMessage && (
        <span className="text-xs text-gray-400 mb-1 mr-3 min-w-[3rem] text-right">
          {showTimestamp ? formatTimestamp(message.sentAt || message.timestamp) : ''}
        </span>
      )}

      {/* Message bubble */}
      <div
        className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
          isOwnMessage
            ? 'bg-blue-600 text-white rounded-br-none'
            : 'bg-white text-gray-900 shadow-sm border border-gray-200 rounded-bl-none'
        }`}
      >
        {/* Message Content */}
        <p className="break-words whitespace-pre-wrap">{message.content}</p>
      </div>

      {/* Timestamp on the right for sent messages */}
      {isOwnMessage && (
        <span className="text-xs text-gray-400 mb-1 ml-3 min-w-[3rem] text-left">
          {showTimestamp ? formatTimestamp(message.sentAt || message.timestamp) : ''}
        </span>
      )}
    </div>
  );
};

export default ChatRoom;