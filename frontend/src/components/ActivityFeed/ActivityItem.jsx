import { memo, useState, useCallback } from 'react';
import {
  Clock, Calendar, User, Users, UserPlus, Camera, MessageCircle,
  CalendarPlus, PlusCircle, Activity, Eye, EyeOff, MoreHorizontal
} from 'lucide-react';
import {
  generateActivityMessage,
  getActivityIcon,
  getActivityColors,
  getActivityInteractions,
  shouldHighlightActivity
} from '../../lib/activityUtils';
import { formatRelativeTime } from '../../lib/timeUtils';

// Icon component mapping for dynamic icon rendering
const IconComponents = {
  Clock,
  Calendar,
  User,
  Users,
  UserPlus,
  Camera,
  MessageCircle,
  CalendarPlus,
  PlusCircle,
  Activity,
  Eye,
  EyeOff,
  MoreHorizontal
};

/**
 * ActivityItem Component - Individual activity item with optimizations
 *
 * This component renders a single activity item with proper styling,
 * interactions, and performance optimizations using React.memo.
 *
 * Features:
 * - React.memo optimization for re-render prevention
 * - Dynamic icon rendering based on activity type
 * - Interactive elements (mark as read, view details)
 * - Responsive design with proper styling
 * - Accessibility support
 */
const ActivityItem = memo(({
  activity,
  onMarkAsRead,
  onInteraction,
  showTimestamp = true,
  showAvatar = true,
  showInteractions = true,
  className = '',
  size = 'default' // 'compact', 'default', 'detailed'
}) => {
  const [showMenu, setShowMenu] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  // Generate activity message and metadata
  const message = generateActivityMessage(activity, {
    short: size === 'compact'
  });

  const iconName = getActivityIcon(activity.type, activity.data);
  const colors = getActivityColors(activity.type, activity.data);
  const interactions = getActivityInteractions(activity, activity.userId);
  const shouldHighlight = shouldHighlightActivity(activity, {
    highlightUnread: true,
    highlightRecent: true,
    recentHours: 2
  });

  // Get the appropriate icon component
  const IconComponent = IconComponents[iconName] || Activity;

  /**
   * Handle activity interaction (mark as read, etc.)
   */
  const handleInteraction = useCallback(async (interactionType, event) => {
    event?.stopPropagation();

    if (isProcessing) return;

    try {
      setIsProcessing(true);

      switch (interactionType) {
        case 'mark_read':
        case 'mark_unread':
          await onMarkAsRead?.(activity.id);
          break;

        default:
          await onInteraction?.(activity, interactionType);
      }
    } catch (error) {
      console.error('Activity interaction failed:', error);
    } finally {
      setIsProcessing(false);
      setShowMenu(false);
    }
  }, [activity, onMarkAsRead, onInteraction, isProcessing]);

  /**
   * Handle activity click for detailed view
   */
  const handleActivityClick = useCallback(() => {
    if (!activity.isRead) {
      handleInteraction('mark_read');
    }
    onInteraction?.(activity, 'view_details');
  }, [activity, handleInteraction, onInteraction]);

  // Size-specific styling
  const sizeClasses = {
    compact: {
      container: 'p-3',
      avatar: 'w-8 h-8',
      icon: 'w-3 h-3',
      iconContainer: 'w-6 h-6',
      text: 'text-sm',
      timestamp: 'text-xs'
    },
    default: {
      container: 'p-4',
      avatar: 'w-10 h-10',
      icon: 'w-4 h-4',
      iconContainer: 'w-8 h-8',
      text: 'text-sm',
      timestamp: 'text-xs'
    },
    detailed: {
      container: 'p-5',
      avatar: 'w-12 h-12',
      icon: 'w-5 h-5',
      iconContainer: 'w-10 h-10',
      text: 'text-base',
      timestamp: 'text-sm'
    }
  };

  const styles = sizeClasses[size];

  return (
    <div
      className={`
        group relative border rounded-lg transition-all duration-200 cursor-pointer
        ${shouldHighlight
          ? `${colors.border} ${colors.bg} shadow-sm ring-1 ring-blue-200`
          : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
        }
        ${!activity.isRead ? 'border-l-4 border-l-blue-500' : ''}
        ${className}
      `}
      onClick={handleActivityClick}
    >
      <div className={styles.container}>
        <div className="flex items-start space-x-3">
          {/* Friend Avatar */}
          {showAvatar && (
            <div className="flex-shrink-0">
              {activity.friend?.avatar ? (
                <img
                  src={activity.friend.avatar}
                  alt={activity.friend.name}
                  className={`${styles.avatar} rounded-full object-cover`}
                />
              ) : (
                <div className={`${styles.avatar} bg-gray-200 rounded-full flex items-center justify-center`}>
                  <User className="w-1/2 h-1/2 text-gray-500" />
                </div>
              )}
            </div>
          )}

          {/* Activity Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between">
              {/* Activity Message */}
              <div className="flex-1 pr-2">
                <div className="flex items-center space-x-2 mb-1">
                  <div
                    className={`
                      ${styles.iconContainer} rounded-full flex items-center justify-center flex-shrink-0
                      ${shouldHighlight ? colors.bg : 'bg-gray-100'}
                    `}
                  >
                    <IconComponent
                      className={`${styles.icon} ${shouldHighlight ? colors.icon : 'text-gray-600'}`}
                    />
                  </div>

                  {!activity.isRead && (
                    <div className="w-2 h-2 bg-blue-500 rounded-full flex-shrink-0"></div>
                  )}
                </div>

                <p className={`${styles.text} text-gray-900 leading-relaxed`}>
                  {message}
                </p>

                {/* Activity Details for Detailed View */}
                {size === 'detailed' && activity.data && (
                  <div className="mt-2 text-sm text-gray-600">
                    {activity.type === 'availability_change' && activity.data.message && (
                      <p className="italic">"{activity.data.message}"</p>
                    )}
                    {activity.type === 'circle_activity' && activity.data.description && (
                      <p className="italic">"{activity.data.description}"</p>
                    )}
                  </div>
                )}

                {/* Timestamp */}
                {showTimestamp && (
                  <div className="flex items-center space-x-2 mt-2">
                    <time
                      className={`${styles.timestamp} text-gray-500`}
                      dateTime={activity.timestamp}
                      title={new Date(activity.timestamp).toLocaleString()}
                    >
                      {formatRelativeTime(activity.timestamp)}
                    </time>

                    {activity.priority >= 3 && (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-orange-100 text-orange-800">
                        Important
                      </span>
                    )}
                  </div>
                )}
              </div>

              {/* Interaction Menu */}
              {showInteractions && interactions.length > 0 && (
                <div className="relative flex-shrink-0">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setShowMenu(!showMenu);
                    }}
                    className={`
                      p-1 rounded-full transition-colors
                      ${showMenu
                        ? 'bg-gray-200 text-gray-700'
                        : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100 opacity-0 group-hover:opacity-100'
                      }
                    `}
                  >
                    <MoreHorizontal className="w-4 h-4" />
                  </button>

                  {/* Dropdown Menu */}
                  {showMenu && (
                    <div className="absolute right-0 top-8 z-10 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1">
                      {interactions.map((interaction) => {
                        const InteractionIcon = IconComponents[interaction.icon] || Activity;

                        return (
                          <button
                            key={interaction.type}
                            onClick={(e) => handleInteraction(interaction.type, e)}
                            disabled={isProcessing}
                            className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center space-x-2 disabled:opacity-50"
                          >
                            <InteractionIcon className="w-4 h-4" />
                            <span>{interaction.label}</span>
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Loading Overlay */}
        {isProcessing && (
          <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center rounded-lg">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
          </div>
        )}
      </div>

      {/* Click outside handler for menu */}
      {showMenu && (
        <div
          className="fixed inset-0 z-5"
          onClick={() => setShowMenu(false)}
        />
      )}
    </div>
  );
}, (prevProps, nextProps) => {
  // Custom comparison for React.memo optimization
  // Only re-render if relevant props change
  return (
    prevProps.activity.id === nextProps.activity.id &&
    prevProps.activity.isRead === nextProps.activity.isRead &&
    prevProps.activity.timestamp === nextProps.activity.timestamp &&
    prevProps.showTimestamp === nextProps.showTimestamp &&
    prevProps.showAvatar === nextProps.showAvatar &&
    prevProps.showInteractions === nextProps.showInteractions &&
    prevProps.size === nextProps.size &&
    prevProps.className === nextProps.className
  );
});

ActivityItem.displayName = 'ActivityItem';

export { ActivityItem };