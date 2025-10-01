import React, { useState, useEffect } from 'react';
import { AlertTriangle, CheckCircle, X, Trash2, UserMinus, AlertCircle } from 'lucide-react';

/**
 * ConfirmationDialog - Reusable confirmation dialog component
 *
 * Supports different types of confirmations with appropriate styling and icons
 * Includes undo functionality for non-destructive reversible actions
 */
export const ConfirmationDialog = ({
  isOpen,
  onClose,
  onConfirm,
  onUndo = null,
  title,
  message,
  type = 'default', // default, warning, danger, success
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  undoText = 'Undo',
  showUndo = false,
  undoTimeout = 5000, // 5 seconds for undo
  loading = false,
  children = null
}) => {
  const [showUndoOption, setShowUndoOption] = useState(false);
  const [undoCountdown, setUndoCountdown] = useState(0);

  // Start undo countdown when showUndo is enabled
  useEffect(() => {
    if (showUndo && onUndo) {
      setShowUndoOption(true);
      setUndoCountdown(Math.ceil(undoTimeout / 1000));

      const interval = setInterval(() => {
        setUndoCountdown(prev => {
          if (prev <= 1) {
            setShowUndoOption(false);
            clearInterval(interval);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(interval);
    }
  }, [showUndo, onUndo, undoTimeout]);

  // Dialog type configurations
  const typeConfig = {
    default: {
      icon: AlertCircle,
      iconColor: 'text-blue-600',
      iconBg: 'bg-blue-100',
      confirmButton: 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-500',
      borderColor: 'border-blue-200'
    },
    warning: {
      icon: AlertTriangle,
      iconColor: 'text-yellow-600',
      iconBg: 'bg-yellow-100',
      confirmButton: 'bg-yellow-600 hover:bg-yellow-700 focus:ring-yellow-500',
      borderColor: 'border-yellow-200'
    },
    danger: {
      icon: Trash2,
      iconColor: 'text-red-600',
      iconBg: 'bg-red-100',
      confirmButton: 'bg-red-600 hover:bg-red-700 focus:ring-red-500',
      borderColor: 'border-red-200'
    },
    success: {
      icon: CheckCircle,
      iconColor: 'text-green-600',
      iconBg: 'bg-green-100',
      confirmButton: 'bg-green-600 hover:bg-green-700 focus:ring-green-500',
      borderColor: 'border-green-200'
    }
  };

  const config = typeConfig[type] || typeConfig.default;
  const IconComponent = config.icon;

  const handleConfirm = async () => {
    await onConfirm();
    if (!showUndo) {
      onClose();
    }
  };

  const handleUndo = async () => {
    if (onUndo) {
      await onUndo();
      setShowUndoOption(false);
      onClose();
    }
  };

  if (!isOpen && !showUndoOption) return null;

  // Undo toast notification
  if (showUndoOption && !isOpen) {
    return (
      <div className="fixed bottom-4 right-4 z-50 max-w-sm">
        <div className="bg-gray-900 text-white rounded-lg shadow-lg p-4 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <CheckCircle className="w-5 h-5 text-green-400" />
            <div>
              <p className="text-sm font-medium">Action completed</p>
              <p className="text-xs text-gray-300">
                Undo in {undoCountdown} seconds
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-2 ml-4">
            <button
              onClick={handleUndo}
              className="text-sm text-blue-400 hover:text-blue-300 font-medium"
            >
              {undoText}
            </button>
            <button
              onClick={() => setShowUndoOption(false)}
              className="text-gray-400 hover:text-gray-300"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Main confirmation dialog
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center">
        {/* Background overlay */}
        <div
          className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
          onClick={onClose}
        ></div>

        {/* Dialog */}
        <div className={`
          relative inline-block w-full max-w-md p-6 my-8 text-left
          bg-white rounded-lg shadow-xl transform transition-all
          border-2 ${config.borderColor}
        `}>
          {/* Header */}
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-3">
              <div className={`
                flex items-center justify-center w-10 h-10 rounded-full
                ${config.iconBg}
              `}>
                <IconComponent className={`w-6 h-6 ${config.iconColor}`} />
              </div>
              <h3 className="text-lg font-medium text-gray-900">
                {title}
              </h3>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Content */}
          <div className="mb-6">
            <p className="text-gray-600 leading-relaxed">
              {message}
            </p>
            {children && (
              <div className="mt-4">
                {children}
              </div>
            )}
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end space-x-3">
            <button
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-500 disabled:opacity-50 transition-colors"
            >
              {cancelText}
            </button>
            <button
              onClick={handleConfirm}
              disabled={loading}
              className={`
                px-4 py-2 text-white rounded-lg font-medium
                focus:outline-none focus:ring-2 focus:ring-offset-2
                disabled:opacity-50 transition-colors
                ${config.confirmButton}
                ${loading ? 'cursor-not-allowed' : 'cursor-pointer'}
              `}
            >
              {loading ? (
                <div className="flex items-center space-x-2">
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Processing...</span>
                </div>
              ) : (
                confirmText
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

/**
 * Hook for managing confirmation dialogs
 */
export const useConfirmation = () => {
  const [confirmationState, setConfirmationState] = useState({
    isOpen: false,
    title: '',
    message: '',
    type: 'default',
    onConfirm: null,
    onUndo: null,
    showUndo: false,
    confirmText: 'Confirm',
    cancelText: 'Cancel',
    undoText: 'Undo'
  });

  const [loading, setLoading] = useState(false);

  const showConfirmation = (options) => {
    setConfirmationState({
      isOpen: true,
      title: options.title || 'Confirm Action',
      message: options.message || 'Are you sure you want to proceed?',
      type: options.type || 'default',
      onConfirm: options.onConfirm,
      onUndo: options.onUndo || null,
      showUndo: options.showUndo || false,
      confirmText: options.confirmText || 'Confirm',
      cancelText: options.cancelText || 'Cancel',
      undoText: options.undoText || 'Undo'
    });
  };

  const hideConfirmation = () => {
    setConfirmationState(prev => ({ ...prev, isOpen: false }));
    setLoading(false);
  };

  const handleConfirm = async () => {
    if (confirmationState.onConfirm) {
      setLoading(true);
      try {
        await confirmationState.onConfirm();
        if (confirmationState.showUndo) {
          // Keep dialog state for undo functionality
          setConfirmationState(prev => ({ ...prev, isOpen: false }));
        } else {
          hideConfirmation();
        }
      } catch (error) {
        console.error('Confirmation action failed:', error);
        setLoading(false);
      }
    } else {
      hideConfirmation();
    }
  };

  const handleUndo = async () => {
    if (confirmationState.onUndo) {
      try {
        await confirmationState.onUndo();
      } catch (error) {
        console.error('Undo action failed:', error);
      }
    }
    hideConfirmation();
  };

  const ConfirmationComponent = () => (
    <ConfirmationDialog
      isOpen={confirmationState.isOpen}
      onClose={hideConfirmation}
      onConfirm={handleConfirm}
      onUndo={confirmationState.onUndo ? handleUndo : null}
      title={confirmationState.title}
      message={confirmationState.message}
      type={confirmationState.type}
      confirmText={confirmationState.confirmText}
      cancelText={confirmationState.cancelText}
      undoText={confirmationState.undoText}
      showUndo={confirmationState.showUndo}
      loading={loading}
    />
  );

  return {
    showConfirmation,
    hideConfirmation,
    ConfirmationComponent
  };
};

/**
 * Pre-configured confirmation functions for common actions
 */
export const confirmations = {
  removeFriend: (friendName, onConfirm, onUndo) => ({
    title: 'Remove Friend',
    message: `Are you sure you want to remove ${friendName} from your friends list? This action can be undone within 5 seconds.`,
    type: 'warning',
    confirmText: 'Remove Friend',
    onConfirm,
    onUndo,
    showUndo: true
  }),

  blockUser: (userName, onConfirm) => ({
    title: 'Block User',
    message: `Are you sure you want to block ${userName}? They will no longer be able to send you messages or friend requests.`,
    type: 'danger',
    confirmText: 'Block User',
    onConfirm
  }),

  deleteFriendRequest: (onConfirm, onUndo) => ({
    title: 'Cancel Friend Request',
    message: 'Are you sure you want to cancel this friend request? This action can be undone.',
    type: 'warning',
    confirmText: 'Cancel Request',
    onConfirm,
    onUndo,
    showUndo: true
  }),

  leaveCircle: (circleName, onConfirm) => ({
    title: 'Leave Circle',
    message: `Are you sure you want to leave "${circleName}"? You'll need to be re-invited to join again.`,
    type: 'warning',
    confirmText: 'Leave Circle',
    onConfirm
  }),

  deleteMessage: (onConfirm, onUndo) => ({
    title: 'Delete Message',
    message: 'Are you sure you want to delete this message? This action can be undone within 5 seconds.',
    type: 'danger',
    confirmText: 'Delete Message',
    onConfirm,
    onUndo,
    showUndo: true
  })
};

export default ConfirmationDialog;