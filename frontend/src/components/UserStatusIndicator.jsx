import React from 'react';
import { Circle, Clock, Minus, AlertCircle } from 'lucide-react';

/**
 * UserStatusIndicator - Visual status indicator component
 *
 * Displays user online/offline/busy/away status with appropriate colors and icons
 * Supports different sizes and tooltip display for enhanced UX
 */
export const UserStatusIndicator = ({
  status = 'OFFLINE',
  size = 'sm',
  showTooltip = true,
  showLabel = false,
  className = '',
  lastSeen = null
}) => {
  // Status configuration with colors, icons, and labels
  const statusConfig = {
    ONLINE: {
      color: 'bg-green-500',
      borderColor: 'border-green-600',
      icon: Circle,
      label: 'Online',
      description: 'Available to chat'
    },
    OFFLINE: {
      color: 'bg-gray-400',
      borderColor: 'border-gray-500',
      icon: Circle,
      label: 'Offline',
      description: 'Not available'
    },
    BUSY: {
      color: 'bg-red-500',
      borderColor: 'border-red-600',
      icon: Minus,
      label: 'Busy',
      description: 'Do not disturb'
    },
    AWAY: {
      color: 'bg-yellow-500',
      borderColor: 'border-yellow-600',
      icon: Clock,
      label: 'Away',
      description: 'Away from computer'
    },
    DO_NOT_DISTURB: {
      color: 'bg-purple-500',
      borderColor: 'border-purple-600',
      icon: AlertCircle,
      label: 'Do Not Disturb',
      description: 'Focusing on work'
    }
  };

  // Size configurations
  const sizeConfig = {
    xs: {
      container: 'w-2 h-2',
      icon: 'w-1.5 h-1.5',
      text: 'text-xs'
    },
    sm: {
      container: 'w-3 h-3',
      icon: 'w-2 h-2',
      text: 'text-sm'
    },
    md: {
      container: 'w-4 h-4',
      icon: 'w-3 h-3',
      text: 'text-base'
    },
    lg: {
      container: 'w-6 h-6',
      icon: 'w-4 h-4',
      text: 'text-lg'
    }
  };

  const config = statusConfig[status] || statusConfig.OFFLINE;
  const sizeClasses = sizeConfig[size] || sizeConfig.sm;
  const IconComponent = config.icon;

  // Format last seen time for tooltip
  const formatLastSeen = (lastSeenDate) => {
    if (!lastSeenDate) return '';

    const now = new Date();
    const lastSeen = new Date(lastSeenDate);
    const diffInMinutes = Math.floor((now - lastSeen) / (1000 * 60));

    if (diffInMinutes < 1) return 'Just now';
    if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
    if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}h ago`;
    return `${Math.floor(diffInMinutes / 1440)}d ago`;
  };

  const tooltipContent = showTooltip ? (
    <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-2 py-1 bg-gray-900 text-white text-xs rounded shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none z-10 whitespace-nowrap">
      <div className="font-medium">{config.label}</div>
      <div className="text-gray-300">{config.description}</div>
      {lastSeen && status !== 'ONLINE' && (
        <div className="text-gray-400">Last seen: {formatLastSeen(lastSeen)}</div>
      )}
      <div className="absolute top-full left-1/2 transform -translate-x-1/2 w-0 h-0 border-l-2 border-r-2 border-t-2 border-transparent border-t-gray-900"></div>
    </div>
  ) : null;

  if (showLabel) {
    return (
      <div className={`relative group inline-flex items-center space-x-2 ${className}`}>
        <div className={`relative ${sizeClasses.container}`}>
          <div className={`
            ${sizeClasses.container}
            ${config.color}
            rounded-full
            border-2
            ${config.borderColor}
            flex items-center justify-center
          `}>
            <IconComponent className={`${sizeClasses.icon} text-white`} />
          </div>

          {/* Pulse animation for online status */}
          {status === 'ONLINE' && (
            <div className={`
              absolute inset-0
              ${config.color}
              rounded-full
              animate-ping
              opacity-75
            `}></div>
          )}
        </div>

        <span className={`${sizeClasses.text} text-gray-700 font-medium`}>
          {config.label}
        </span>

        {tooltipContent}
      </div>
    );
  }

  return (
    <div className={`relative group inline-block ${className}`}>
      <div className={`relative ${sizeClasses.container}`}>
        <div className={`
          ${sizeClasses.container}
          ${config.color}
          rounded-full
          border-2
          ${config.borderColor}
          flex items-center justify-center
          cursor-help
        `}>
          <IconComponent className={`${sizeClasses.icon} text-white`} />
        </div>

        {/* Pulse animation for online status */}
        {status === 'ONLINE' && (
          <div className={`
            absolute inset-0
            ${config.color}
            rounded-full
            animate-ping
            opacity-75
          `}></div>
        )}
      </div>

      {tooltipContent}
    </div>
  );
};

/**
 * UserStatusSelector - Dropdown component for changing user status
 */
export const UserStatusSelector = ({
  currentStatus = 'OFFLINE',
  onStatusChange,
  disabled = false,
  className = ''
}) => {
  const statusOptions = [
    { value: 'ONLINE', label: 'Online', description: 'Available to chat' },
    { value: 'AWAY', label: 'Away', description: 'Away from computer' },
    { value: 'BUSY', label: 'Busy', description: 'Do not disturb' },
    { value: 'DO_NOT_DISTURB', label: 'Do Not Disturb', description: 'Focusing on work' },
    { value: 'OFFLINE', label: 'Offline', description: 'Not available' }
  ];

  return (
    <div className={`relative ${className}`}>
      <select
        value={currentStatus}
        onChange={(e) => onStatusChange && onStatusChange(e.target.value)}
        disabled={disabled}
        className="
          w-full px-3 py-2
          border border-gray-300 rounded-lg
          focus:ring-2 focus:ring-blue-500 focus:border-blue-500
          disabled:opacity-50 disabled:cursor-not-allowed
          bg-white text-gray-900
        "
      >
        {statusOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label} - {option.description}
          </option>
        ))}
      </select>

      <div className="absolute left-3 top-1/2 transform -translate-y-1/2 pointer-events-none">
        <UserStatusIndicator
          status={currentStatus}
          size="sm"
          showTooltip={false}
        />
      </div>
    </div>
  );
};

export default UserStatusIndicator;