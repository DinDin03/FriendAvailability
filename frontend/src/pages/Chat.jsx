import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { MessageCircle, Users, User, RefreshCw, ArrowLeft, AlertCircle, UserPlus, Loader } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { chatService } from '../services/chatService';
import { formatRelativeTime } from '../lib/timeUtils';
import { ChatRoom } from '../components/ChatRoom';
import toast from 'react-hot-toast';

/**
 * Chat - Main chat rooms list page
 *
 * Features:
 * - Display user's chat rooms with room details
 * - Show unread message counts
 * - Room selection for navigation to chat interface
 * - Loading, error, and empty states
 * - Manual refresh capability
 * - Real-time updates ready
 */
export const Chat = () => {
  const { user } = useAuth();

  // State management
  const [chatRooms, setChatRooms] = useState([]);
  const [selectedRoomId, setSelectedRoomId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Create new chat state
  const [targetUserId, setTargetUserId] = useState('');
  const [isCreatingChat, setIsCreatingChat] = useState(false);

  /**
   * Load chat rooms from the backend
   */
  const loadChatRooms = async () => {
    if (!user?.id) return;

    try {
      setIsLoading(true);
      setError(null);

      const response = await chatService.getUserChatRooms(user.id, {
        page: 0,
        size: 0 // Get all rooms
      });

      // Backend returns ChatRoomListResponseDTO with chatRooms array
      const rooms = response.chatRooms || [];
      setChatRooms(rooms);

      console.log('Chat rooms loaded:', rooms.length);

    } catch (error) {
      console.error('Failed to load chat rooms:', error);
      setError(error.message || 'Failed to load chats');
      toast.error(error.message || 'Failed to load chats');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Handle manual refresh
   */
  const handleRefresh = async () => {
    try {
      setIsRefreshing(true);

      const response = await chatService.getUserChatRooms(user.id, {
        page: 0,
        size: 0
      });

      const rooms = response.chatRooms || [];
      setChatRooms(rooms);

      toast.success('Chats refreshed!');
    } catch (error) {
      console.error('Failed to refresh chats:', error);
      toast.error(error.message || 'Failed to refresh chats');
    } finally {
      setIsRefreshing(false);
    }
  };

  /**
   * Handle room selection
   */
  const handleRoomClick = (roomId) => {
    setSelectedRoomId(roomId);
    console.log('Room selected:', roomId);
  };

  /**
   * Handle back from chat room
   */
  const handleBackToRoomsList = () => {
    setSelectedRoomId(null);
    // Refresh rooms list to update unread counts
    loadChatRooms();
  };

  /**
   * Handle creating a new private chat
   */
  const handleCreatePrivateChat = async () => {
    const userId = targetUserId.trim();

    // Validation: Empty input
    if (!userId) {
      toast.error('Please enter a user ID');
      return;
    }

    // Validation: Numeric check
    const targetUserIdNum = parseInt(userId);
    if (isNaN(targetUserIdNum)) {
      toast.error('Please enter a valid numeric user ID');
      return;
    }

    // Validation: Cannot create chat with self
    if (targetUserIdNum === user?.id) {
      toast.error('You cannot create a chat with yourself');
      return;
    }

    try {
      setIsCreatingChat(true);

      console.log('Creating private chat with user:', targetUserIdNum);

      // Create or get existing private chat
      const chatRoom = await chatService.createPrivateChat(user.id, targetUserIdNum);

      console.log('Chat created/retrieved:', chatRoom);

      toast.success(`Chat with user ${targetUserIdNum} is ready!`);

      // Clear input
      setTargetUserId('');

      // Refresh chat rooms list
      await loadChatRooms();

      // Auto-open the created/existing chat
      setSelectedRoomId(chatRoom.id);

    } catch (error) {
      console.error('Failed to create chat:', error);

      // Handle specific error messages
      let errorMessage = error.message || 'Failed to create chat';

      if (error.message?.includes('404') || error.message?.includes('not found')) {
        errorMessage = `User ${targetUserIdNum} not found`;
      } else if (error.message?.includes('403') || error.message?.includes('permission')) {
        errorMessage = 'You do not have permission to create this chat';
      }

      toast.error(errorMessage);
    } finally {
      setIsCreatingChat(false);
    }
  };

  /**
   * Handle Enter key press in create chat input
   */
  const handleCreateChatKeyPress = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleCreatePrivateChat();
    }
  };

  // Load chat rooms on mount
  useEffect(() => {
    if (user?.id) {
      loadChatRooms();
    }
  }, [user?.id]);

  // Show ChatRoom component if a room is selected
  if (selectedRoomId) {
    return (
      <ChatRoom
        roomId={selectedRoomId}
        userId={user?.id}
        onBack={handleBackToRoomsList}
      />
    );
  }

  // Loading State
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading your chats...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b sticky top-0 z-10">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-4">
              <Link
                to="/dashboard"
                className="text-gray-600 hover:text-gray-900 transition-colors flex items-center space-x-1"
              >
                <ArrowLeft className="w-5 h-5" />
                <span className="hidden sm:inline">Dashboard</span>
              </Link>
              <div className="flex items-center space-x-2">
                <MessageCircle className="w-6 h-6 text-purple-600" />
                <h1 className="text-2xl font-bold text-gray-900">My Chats</h1>
              </div>
            </div>
            <button
              onClick={handleRefresh}
              disabled={isRefreshing}
              className="flex items-center space-x-1 text-purple-600 hover:text-purple-700 transition-colors disabled:opacity-50"
              title="Refresh chats"
            >
              <RefreshCw className={`w-5 h-5 ${isRefreshing ? 'animate-spin' : ''}`} />
              <span className="hidden sm:inline">Refresh</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Create New Chat Section */}
        <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
          <div className="flex items-center space-x-3 mb-4">
            <UserPlus className="w-6 h-6 text-blue-600" />
            <div>
              <h3 className="text-lg font-semibold text-gray-900">Create New Chat</h3>
              <p className="text-sm text-gray-600">Start a conversation with a user</p>
            </div>
          </div>

          <div className="flex space-x-4">
            <div className="flex-1">
              <label htmlFor="targetUserId" className="block text-sm font-medium text-gray-700 mb-2">
                User ID
              </label>
              <input
                id="targetUserId"
                type="text"
                value={targetUserId}
                onChange={(e) => setTargetUserId(e.target.value)}
                onKeyPress={handleCreateChatKeyPress}
                placeholder="Enter user ID"
                disabled={isCreatingChat}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              />
            </div>
            <div className="flex items-end">
              <button
                onClick={handleCreatePrivateChat}
                disabled={isCreatingChat || !targetUserId.trim()}
                className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
              >
                {isCreatingChat ? (
                  <>
                    <Loader className="w-4 h-4 animate-spin" />
                    <span>Creating...</span>
                  </>
                ) : (
                  <>
                    <UserPlus className="w-4 h-4" />
                    <span>Create</span>
                  </>
                )}
              </button>
            </div>
          </div>

          <p className="text-sm text-gray-500 mt-3">
            Enter a user ID to create a private chat. Press Enter or click Create.
          </p>
        </div>

        {/* Error State */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-6 mb-6">
            <div className="flex items-center space-x-3">
              <div className="flex-shrink-0">
                <AlertCircle className="w-6 h-6 text-red-600" />
              </div>
              <div className="flex-1">
                <h3 className="text-sm font-medium text-red-800">Failed to load chats</h3>
                <p className="text-sm text-red-700 mt-1">{error}</p>
              </div>
              <button
                onClick={loadChatRooms}
                className="text-sm text-red-600 hover:text-red-700 font-medium"
              >
                Retry
              </button>
            </div>
          </div>
        )}

        {/* Empty State */}
        {!error && chatRooms.length === 0 && (
          <div className="bg-white rounded-lg shadow-sm p-12 text-center">
            <MessageCircle className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No chats yet</h3>
            <p className="text-gray-600 mb-6">
              Start a conversation with your friends to see your chats here.
            </p>
            <Link
              to="/dashboard"
              className="inline-flex items-center space-x-2 bg-purple-600 text-white px-6 py-3 rounded-lg hover:bg-purple-700 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back to Dashboard</span>
            </Link>
          </div>
        )}

        {/* Chat Rooms List */}
        {!error && chatRooms.length > 0 && (
          <div className="space-y-3">
            {chatRooms.map((room) => (
              <RoomCard
                key={room.id}
                room={room}
                isSelected={selectedRoomId === room.id}
                onClick={() => handleRoomClick(room.id)}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  );
};

/**
 * RoomCard - Individual chat room card component
 */
const RoomCard = ({ room, isSelected, onClick }) => {
  const isGroupChat = room.type === 'GROUP';
  const hasUnread = room.unreadCount > 0;

  // Get avatar icon based on room type
  const AvatarIcon = isGroupChat ? Users : User;
  const avatarBgColor = isGroupChat ? 'bg-purple-100' : 'bg-blue-100';
  const avatarIconColor = isGroupChat ? 'text-purple-600' : 'text-blue-600';

  // Format timestamp
  const formattedTime = room.lastMessageAt
    ? formatRelativeTime(room.lastMessageAt)
    : '';

  return (
    <div
      onClick={onClick}
      className={`bg-white rounded-lg border-2 p-4 cursor-pointer transition-all hover:shadow-md ${
        isSelected
          ? 'border-purple-500 shadow-md'
          : 'border-gray-200 hover:border-purple-300'
      }`}
    >
      <div className="flex items-start space-x-4">
        {/* Avatar */}
        <div className={`flex-shrink-0 w-12 h-12 rounded-full ${avatarBgColor} flex items-center justify-center`}>
          <AvatarIcon className={`w-6 h-6 ${avatarIconColor}`} />
        </div>

        {/* Room Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-1">
            <h3 className="text-base font-semibold text-gray-900 truncate">
              {room.displayName || room.name || `Chat ${room.id}`}
            </h3>
            {hasUnread && (
              <span className="ml-2 flex-shrink-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white bg-red-500 rounded-full">
                {room.unreadCount > 99 ? '99+' : room.unreadCount}
              </span>
            )}
          </div>

          {/* Last Message Preview */}
          {room.lastMessagePreview && (
            <p className="text-sm text-gray-600 truncate mb-1">
              {room.lastMessagePreview}
            </p>
          )}

          {/* Metadata */}
          <div className="flex items-center space-x-2 text-xs text-gray-500">
            {formattedTime && (
              <span>{formattedTime}</span>
            )}
            {isGroupChat && room.participantCount && (
              <>
                <span>•</span>
                <span>{room.participantCount} members</span>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Chat;