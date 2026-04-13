# Real-time Notification System Guide

## 🎯 Overview

Hệ thống notification real-time sử dụng WebSocket để push notifications đến user ngay lập tức khi có sự kiện xảy ra.

### Các loại notifications:
- 🔔 **NEW_REQUEST** - Request mới được publish (→ All Artisans)
- 💬 **NEW_CONVERSATION** - Artisan quan tâm (→ Customer)
- 📨 **NEW_MESSAGE** - Tin nhắn mới (→ Recipient)
- ✅ **ARTISAN_SELECTED** - Customer chọn artisan (→ Artisan)
- 📦 **ORDER_CREATED** - Order mới (→ Customer)
- 💰 **PAYMENT_RECEIVED** - Thanh toán thành công (→ Artisan)
- 🚚 **SHIPMENT_CREATED** - Đơn hàng được gửi (→ Customer)

---

## 🏗️ Backend Implementation

### 1. Update NotificationController - Add WebSocket Support

```java
// src/main/java/org/example/catholicsouvenircustomorder/controller/NotificationController.java

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    // ... existing REST endpoints ...
    
    /**
     * WebSocket endpoint for real-time notifications
     * Client subscribes to: /topic/notifications/{userId}
     */
    // Note: WebSocket subscription is handled automatically by Spring
    // No @MessageMapping needed for subscription-only topics
}
```

### 2. Update NotificationService - Add Broadcast Method

```java
// src/main/java/org/example/catholicsouvenircustomorder/service/NotificationService.java

public interface NotificationService {
    // ... existing methods ...
    
    /**
     * Send notification and broadcast via WebSocket
     */
    void sendNotification(UUID recipientId, NotificationType type, String title, 
                         String message, UUID relatedEntityId);
    
    /**
     * Broadcast to all artisans
     */
    void broadcastToAllArtisans(NotificationType type, String title, String message, 
                                UUID relatedEntityId);
}
```

### 3. Implement NotificationServiceImp

```java
// src/main/java/org/example/catholicsouvenircustomorder/service/imp/NotificationServiceImp.java

@Service
@RequiredArgsConstructor
public class NotificationServiceImp implements NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Override
    @Transactional
    public void sendNotification(UUID recipientId, NotificationType type, String title, 
                                String message, UUID relatedEntityId) {
        // 1. Save to database
        Account recipient = accountRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
        
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        
        // 2. Broadcast via WebSocket
        NotificationResponse response = mapToResponse(saved);
        String destination = String.format("/topic/notifications/%s", recipientId);
        messagingTemplate.convertAndSend(destination, response);
        
        log.info("Notification sent to user {} via WebSocket", recipientId);
    }
    
    @Override
    @Transactional
    public void broadcastToAllArtisans(NotificationType type, String title, String message, 
                                      UUID relatedEntityId) {
        // Get all artisans
        List<Account> artisans = accountRepository.findByRole_RoleName("ARTISAN");
        
        // Send to each artisan
        artisans.forEach(artisan -> {
            sendNotification(artisan.getAccountId(), type, title, message, relatedEntityId);
        });
        
        log.info("Notification broadcasted to {} artisans", artisans.size());
    }
    
    private NotificationResponse mapToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getNotificationId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setRelatedEntityId(notification.getRelatedEntityId());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
```

### 4. Integrate with Business Logic

#### Example: When Customer Publishes Request

```java
// src/main/java/org/example/catholicsouvenircustomorder/service/imp/CustomRequestServiceImp.java

@Service
@RequiredArgsConstructor
public class CustomRequestServiceImp implements CustomRequestService {
    
    private final NotificationService notificationService;
    
    @Override
    @Transactional
    public void publishRequest(UUID requestId) {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        
        request.setStatus(CustomRequestStatus.PUBLISHED);
        request.setPublishedAt(LocalDateTime.now());
        customRequestRepository.save(request);
        
        // 🔔 Send notification to all artisans
        notificationService.broadcastToAllArtisans(
            NotificationType.NEW_REQUEST,
            "Yêu cầu mới",
            String.format("Yêu cầu mới: %s - Budget: %s VND", 
                         request.getTitle(), 
                         request.getBudget()),
            requestId
        );
    }
}
```

#### Example: When Artisan Creates Conversation

```java
// src/main/java/org/example/catholicsouvenircustomorder/service/imp/ConversationServiceImp.java

@Override
@Transactional
public ConversationResponse startConversation(UUID requestId, UUID artisanId) {
    // ... create conversation logic ...
    
    Conversation saved = conversationRepository.save(conversation);
    
    // 🔔 Send notification to customer
    notificationService.sendNotification(
        customer.getAccountId(),
        NotificationType.NEW_CONVERSATION,
        "Nghệ nhân quan tâm",
        String.format("Nghệ nhân %s quan tâm đến yêu cầu của bạn", 
                     artisan.getAccount().getFullName()),
        saved.getConversationId()
    );
    
    return mapToResponse(saved);
}
```

---

## 💻 Frontend Implementation

### 1. Setup Notification WebSocket Hook

```javascript
// hooks/useNotifications.js
import { useState, useEffect, useCallback } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import apiClient from '../api/client';

export const useNotifications = () => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [connected, setConnected] = useState(false);
  const [stompClient, setStompClient] = useState(null);

  const userId = localStorage.getItem('userId');
  const token = localStorage.getItem('token');

  // Load initial notifications
  useEffect(() => {
    loadNotifications();
    loadUnreadCount();
  }, []);

  // Connect WebSocket
  useEffect(() => {
    if (!userId || !token) return;

    const wsUrl = process.env.REACT_APP_WS_URL || 'http://localhost:8080/ws';
    const socket = new SockJS(wsUrl);
    const client = Stomp.over(socket);

    if (process.env.NODE_ENV === 'production') {
      client.debug = () => {};
    }

    client.connect(
      { Authorization: `Bearer ${token}` },
      () => {
        console.log('✅ Notification WebSocket connected');
        setConnected(true);

        // Subscribe to user's notification channel
        client.subscribe(`/topic/notifications/${userId}`, (message) => {
          const notification = JSON.parse(message.body);
          console.log('🔔 New notification:', notification);

          // Add to notifications list
          setNotifications((prev) => [notification, ...prev]);
          
          // Increment unread count
          setUnreadCount((prev) => prev + 1);
          
          // Show browser notification
          showBrowserNotification(notification);
          
          // Play sound (optional)
          playNotificationSound();
        });

        setStompClient(client);
      },
      (error) => {
        console.error('❌ Notification WebSocket error:', error);
        setConnected(false);
      }
    );

    return () => {
      if (client && client.connected) {
        client.disconnect();
      }
    };
  }, [userId, token]);

  const loadNotifications = async () => {
    try {
      const response = await apiClient.get('/api/notifications?page=0&size=20');
      setNotifications(response.data.data.content);
    } catch (error) {
      console.error('Error loading notifications:', error);
    }
  };

  const loadUnreadCount = async () => {
    try {
      const response = await apiClient.get('/api/notifications/unread-count');
      setUnreadCount(response.data.data);
    } catch (error) {
      console.error('Error loading unread count:', error);
    }
  };

  const markAsRead = async (notificationId) => {
    try {
      await apiClient.post(`/api/notifications/${notificationId}/read`);
      
      // Update local state
      setNotifications((prev) =>
        prev.map((n) =>
          n.notificationId === notificationId ? { ...n, isRead: true } : n
        )
      );
      
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (error) {
      console.error('Error marking as read:', error);
    }
  };

  const markAllAsRead = async () => {
    try {
      await apiClient.post('/api/notifications/read-all');
      
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true }))
      );
      
      setUnreadCount(0);
    } catch (error) {
      console.error('Error marking all as read:', error);
    }
  };

  const showBrowserNotification = (notification) => {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(notification.title, {
        body: notification.message,
        icon: '/logo.png',
        badge: '/badge.png',
      });
    }
  };

  const playNotificationSound = () => {
    const audio = new Audio('/notification-sound.mp3');
    audio.play().catch((e) => console.log('Cannot play sound:', e));
  };

  const requestNotificationPermission = async () => {
    if ('Notification' in window && Notification.permission === 'default') {
      await Notification.requestPermission();
    }
  };

  return {
    notifications,
    unreadCount,
    connected,
    markAsRead,
    markAllAsRead,
    loadNotifications,
    requestNotificationPermission,
  };
};
```

### 2. Notification Bell Component

```jsx
// components/NotificationBell.jsx
import { useState } from 'react';
import { useNotifications } from '../hooks/useNotifications';
import './NotificationBell.css';

function NotificationBell() {
  const {
    notifications,
    unreadCount,
    connected,
    markAsRead,
    markAllAsRead,
  } = useNotifications();
  
  const [isOpen, setIsOpen] = useState(false);

  const handleNotificationClick = (notification) => {
    markAsRead(notification.notificationId);
    
    // Navigate based on notification type
    switch (notification.type) {
      case 'NEW_REQUEST':
        window.location.href = `/requests/${notification.relatedEntityId}`;
        break;
      case 'NEW_CONVERSATION':
        window.location.href = `/conversations/${notification.relatedEntityId}`;
        break;
      case 'NEW_MESSAGE':
        window.location.href = `/chat/${notification.relatedEntityId}`;
        break;
      default:
        break;
    }
    
    setIsOpen(false);
  };

  return (
    <div className="notification-bell">
      <button 
        className="bell-button" 
        onClick={() => setIsOpen(!isOpen)}
      >
        🔔
        {unreadCount > 0 && (
          <span className="badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
        )}
        {!connected && <span className="offline-indicator">⚠️</span>}
      </button>

      {isOpen && (
        <div className="notification-dropdown">
          <div className="dropdown-header">
            <h3>Notifications</h3>
            {unreadCount > 0 && (
              <button onClick={markAllAsRead} className="mark-all-read">
                Mark all as read
              </button>
            )}
          </div>

          <div className="notification-list">
            {notifications.length === 0 ? (
              <div className="empty-state">No notifications</div>
            ) : (
              notifications.map((notification) => (
                <div
                  key={notification.notificationId}
                  className={`notification-item ${!notification.isRead ? 'unread' : ''}`}
                  onClick={() => handleNotificationClick(notification)}
                >
                  <div className="notification-icon">
                    {getNotificationIcon(notification.type)}
                  </div>
                  <div className="notification-content">
                    <h4>{notification.title}</h4>
                    <p>{notification.message}</p>
                    <small>{formatTime(notification.createdAt)}</small>
                  </div>
                  {!notification.isRead && <div className="unread-dot"></div>}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function getNotificationIcon(type) {
  const icons = {
    NEW_REQUEST: '📝',
    NEW_CONVERSATION: '💬',
    NEW_MESSAGE: '📨',
    ARTISAN_SELECTED: '✅',
    ORDER_CREATED: '📦',
    PAYMENT_RECEIVED: '💰',
    SHIPMENT_CREATED: '🚚',
  };
  return icons[type] || '🔔';
}

function formatTime(timestamp) {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;
  
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days < 7) return `${days}d ago`;
  return date.toLocaleDateString();
}

export default NotificationBell;
```

### 3. CSS for Notification Bell

```css
/* components/NotificationBell.css */
.notification-bell {
  position: relative;
}

.bell-button {
  position: relative;
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0.5rem;
}

.bell-button .badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #ff4444;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 0.7rem;
  font-weight: bold;
}

.offline-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  font-size: 0.8rem;
}

.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  width: 400px;
  max-height: 600px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #eee;
}

.dropdown-header h3 {
  margin: 0;
  font-size: 1.1rem;
}

.mark-all-read {
  background: none;
  border: none;
  color: #007bff;
  cursor: pointer;
  font-size: 0.9rem;
}

.notification-list {
  max-height: 500px;
  overflow-y: auto;
}

.empty-state {
  padding: 2rem;
  text-align: center;
  color: #999;
}

.notification-item {
  display: flex;
  gap: 0.75rem;
  padding: 1rem;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.notification-item:hover {
  background: #f8f9fa;
}

.notification-item.unread {
  background: #f0f7ff;
}

.notification-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
}

.notification-content h4 {
  margin: 0 0 0.25rem 0;
  font-size: 0.95rem;
  font-weight: 600;
}

.notification-content p {
  margin: 0 0 0.25rem 0;
  font-size: 0.85rem;
  color: #666;
}

.notification-content small {
  font-size: 0.75rem;
  color: #999;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #007bff;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 0.5rem;
}
```

### 4. Add to App Layout

```jsx
// App.jsx or Layout.jsx
import NotificationBell from './components/NotificationBell';

function App() {
  return (
    <div className="app">
      <header>
        <nav>
          <div className="logo">My App</div>
          <div className="nav-items">
            <NotificationBell />
            <UserMenu />
          </div>
        </nav>
      </header>
      
      <main>
        {/* Your routes */}
      </main>
    </div>
  );
}
```

---

## 🧪 Testing

### Test Notification Flow

```javascript
// Test in browser console
const testNotification = async () => {
  // 1. Request permission
  await Notification.requestPermission();
  
  // 2. Create a test request (as customer)
  const response = await fetch('http://localhost:8080/api/custom-requests', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${customerToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      requestType: 'FREE_FORM',
      title: 'Test Request',
      description: 'Test',
      budget: 1000000,
      deadline: '2024-03-01T00:00:00'
    })
  });
  
  const data = await response.json();
  const requestId = data.data.requestId;
  
  // 3. Publish request
  await fetch(`http://localhost:8080/api/custom-requests/${requestId}/publish`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${customerToken}`
    }
  });
  
  // 4. All artisans should receive notification via WebSocket!
  console.log('✅ Notification sent to all artisans');
};
```

---

## 📊 Notification Types & Actions

| Type | Recipient | Action | Navigate To |
|------|-----------|--------|-------------|
| NEW_REQUEST | All Artisans | View request | `/requests/{id}` |
| NEW_CONVERSATION | Customer | View conversation | `/conversations/{id}` |
| NEW_MESSAGE | Recipient | Open chat | `/chat/{conversationId}` |
| ARTISAN_SELECTED | Artisan | Create order | `/orders/create` |
| ORDER_CREATED | Customer | View order | `/orders/{id}` |
| PAYMENT_RECEIVED | Artisan | Start work | `/orders/{id}` |
| SHIPMENT_CREATED | Customer | Track shipment | `/shipments/{id}` |

---

## 🎯 Summary

Notification real-time hoạt động như sau:

1. **Backend**: Khi có sự kiện → Save notification → Broadcast qua WebSocket
2. **Frontend**: Subscribe `/topic/notifications/{userId}` → Nhận notification real-time
3. **UI**: Hiển thị bell icon với badge → Click để xem chi tiết
4. **Browser**: Show native notification (nếu có permission)

Hệ thống này đảm bảo user luôn nhận được thông báo ngay lập tức!
