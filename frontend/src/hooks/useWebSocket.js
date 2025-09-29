import { useState, useEffect, useRef, useCallback } from 'react';

/**
 * useWebSocket Hook - WebSocket connection management
 *
 * This hook provides a robust WebSocket connection with automatic reconnection,
 * connection status tracking, message queuing, and error handling.
 *
 * Features:
 * - Automatic connection management
 * - Exponential backoff reconnection
 * - Message queuing during disconnections
 * - Connection status tracking
 * - Cleanup on unmount
 */
export const useWebSocket = (url, options = {}) => {
  const {
    protocols = [],
    onOpen,
    onClose,
    onError,
    onMessage,
    shouldReconnect = true,
    maxReconnectAttempts = 5,
    reconnectInterval = 1000,
    heartbeatInterval = 30000,
    messageQueueSize = 100
  } = options;

  // Connection state
  const [connectionStatus, setConnectionStatus] = useState('Disconnected');
  const [lastMessage, setLastMessage] = useState(null);
  const [connectionAttempts, setConnectionAttempts] = useState(0);

  // Refs for persistent values
  const websocketRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const heartbeatTimeoutRef = useRef(null);
  const messageQueueRef = useRef([]);
  const reconnectAttemptsRef = useRef(0);
  const isUnmountedRef = useRef(false);

  /**
   * Connect to WebSocket
   */
  const connect = useCallback(() => {
    if (isUnmountedRef.current || !url) return;

    try {
      // Close existing connection
      if (websocketRef.current) {
        websocketRef.current.close();
      }

      setConnectionStatus('Connecting');

      const ws = new WebSocket(url, protocols);
      websocketRef.current = ws;

      ws.onopen = (event) => {
        if (isUnmountedRef.current) return;

        setConnectionStatus('Connected');
        setConnectionAttempts(0);
        reconnectAttemptsRef.current = 0;

        // Send queued messages
        while (messageQueueRef.current.length > 0) {
          const queuedMessage = messageQueueRef.current.shift();
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(queuedMessage);
          }
        }

        // Start heartbeat
        if (heartbeatInterval > 0) {
          startHeartbeat();
        }

        onOpen?.(event);
      };

      ws.onclose = (event) => {
        if (isUnmountedRef.current) return;

        setConnectionStatus('Disconnected');
        stopHeartbeat();

        onClose?.(event);

        // Attempt reconnection if enabled and not a clean close
        if (shouldReconnect && !event.wasClean && reconnectAttemptsRef.current < maxReconnectAttempts) {
          scheduleReconnect();
        }
      };

      ws.onerror = (event) => {
        if (isUnmountedRef.current) return;

        setConnectionStatus('Error');
        onError?.(event);
      };

      ws.onmessage = (event) => {
        if (isUnmountedRef.current) return;

        setLastMessage(event);
        onMessage?.(event);
      };

    } catch (error) {
      console.error('WebSocket connection failed:', error);
      setConnectionStatus('Error');
      onError?.(error);

      if (shouldReconnect && reconnectAttemptsRef.current < maxReconnectAttempts) {
        scheduleReconnect();
      }
    }
  }, [url, protocols, onOpen, onClose, onError, onMessage, shouldReconnect, maxReconnectAttempts, heartbeatInterval]);

  /**
   * Schedule reconnection with exponential backoff
   */
  const scheduleReconnect = useCallback(() => {
    if (isUnmountedRef.current) return;

    reconnectAttemptsRef.current += 1;
    setConnectionAttempts(reconnectAttemptsRef.current);

    const delay = reconnectInterval * Math.pow(2, reconnectAttemptsRef.current - 1);

    reconnectTimeoutRef.current = setTimeout(() => {
      if (!isUnmountedRef.current) {
        connect();
      }
    }, Math.min(delay, 30000)); // Cap at 30 seconds

  }, [connect, reconnectInterval]);

  /**
   * Start heartbeat to keep connection alive
   */
  const startHeartbeat = useCallback(() => {
    if (heartbeatInterval <= 0) return;

    heartbeatTimeoutRef.current = setInterval(() => {
      if (websocketRef.current?.readyState === WebSocket.OPEN) {
        websocketRef.current.send(JSON.stringify({ type: 'ping' }));
      }
    }, heartbeatInterval);
  }, [heartbeatInterval]);

  /**
   * Stop heartbeat
   */
  const stopHeartbeat = useCallback(() => {
    if (heartbeatTimeoutRef.current) {
      clearInterval(heartbeatTimeoutRef.current);
      heartbeatTimeoutRef.current = null;
    }
  }, []);

  /**
   * Send message through WebSocket
   */
  const sendMessage = useCallback((message) => {
    if (!websocketRef.current || isUnmountedRef.current) return false;

    const messageString = typeof message === 'string' ? message : JSON.stringify(message);

    if (websocketRef.current.readyState === WebSocket.OPEN) {
      websocketRef.current.send(messageString);
      return true;
    } else {
      // Queue message if connection is not open
      if (messageQueueRef.current.length < messageQueueSize) {
        messageQueueRef.current.push(messageString);
      } else {
        // Remove oldest message if queue is full
        messageQueueRef.current.shift();
        messageQueueRef.current.push(messageString);
      }
      return false;
    }
  }, [messageQueueSize]);

  /**
   * Manually disconnect
   */
  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    stopHeartbeat();

    if (websocketRef.current) {
      websocketRef.current.close(1000, 'Manual disconnect');
      websocketRef.current = null;
    }

    setConnectionStatus('Disconnected');
  }, [stopHeartbeat]);

  /**
   * Manually reconnect
   */
  const reconnect = useCallback(() => {
    disconnect();
    reconnectAttemptsRef.current = 0;
    setConnectionAttempts(0);
    connect();
  }, [disconnect, connect]);

  /**
   * Get current connection state
   */
  const getReadyState = useCallback(() => {
    return websocketRef.current?.readyState ?? WebSocket.CLOSED;
  }, []);

  // Initialize connection on mount or URL change
  useEffect(() => {
    if (url) {
      connect();
    }

    return () => {
      isUnmountedRef.current = true;
      disconnect();
    };
  }, [url, connect, disconnect]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      isUnmountedRef.current = true;

      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }

      stopHeartbeat();

      if (websocketRef.current) {
        websocketRef.current.close();
      }
    };
  }, [stopHeartbeat]);

  return {
    connectionStatus,
    lastMessage,
    sendMessage,
    disconnect,
    reconnect,
    getReadyState,
    connectionAttempts,
    isConnected: connectionStatus === 'Connected',
    isConnecting: connectionStatus === 'Connecting',
    isDisconnected: connectionStatus === 'Disconnected',
    hasError: connectionStatus === 'Error'
  };
};