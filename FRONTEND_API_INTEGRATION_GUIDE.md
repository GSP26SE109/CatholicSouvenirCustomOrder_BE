# Frontend API Integration Guide - Chat Flow

## 🎯 Overview cho Frontend Developer

Hệ thống chat hoạt động theo luồng:
1. Customer tạo và publish CustomRequest
2. Artisan xem requests và tạo Conversation
3. Cả 2 chat qua WebSocket hoặc REST API
4. Customer chọn Artisan và tiếp tục flow

---

## 📱 UI Components cần thiết

### 1. Customer Side
- **CreateRequestPage** - Tạo custom request
- **MyRequestsPage** - Xem danh sách requests của mình
- **ConversationsListPage** - Xem artisans quan tâm
- **ChatPage** - Chat với artisan

### 2. Artisan Side
- **BrowseRequestsPage** - Xem published requests
- **MyConversationsPage** - Xem conversations của mình
- **ChatPage** - Chat với customer

---

## 🔗 API Endpoints Summary

| Endpoint | Method | Role | Mục đích |
|----------|--------|------|----------|
| `/api/custom-requests` | POST | Customer | Tạo request |
| `/api/custom-requests/{id}/publish` | POST | Customer | Publish request |
| `/api/custom-requests/published` | GET | Artisan | Xem published requests |
| `/api/conversations/start` | POST | Artisan | Tạo conversation |
| `/api/conversations/request/{requestId}` | GET | Customer | Xem artisans quan tâm |
| `/api/conversations/my-conversations` | GET | Artisan | Xem conversations |
| `/api/chat/send` | POST | Both | Gửi tin nhắn |
| `/api/chat/conversation/{id}/messages` | GET | Both | Lấy lịch sử chat |
| `/api/chat/conversation/{id}/mark-read` | POST | Both | Đánh dấu đã đọc |

---

## 💻 Code Examples

### Setup API Client

```javascript
// api/client.js
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## 👤 CUSTOMER FLOW

### 1. Create Custom Request

```jsx
// pages/CreateRequestPage.jsx
import { useState } from 'react';
import apiClient from '../api/client';

function CreateRequestPage() {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    budget: '',
    deadline: '',
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await apiClient.post('/api/custom-requests', {
        requestType: 'FREE_FORM',
        title: formData.title,
        description: formData.description,
        budget: parseInt(formData.budget),
        deadline: new Date(formData.deadline).toISOString(),
        referenceImages: [],
      });

      const requestId = response.data.data.requestId;
      
      // Auto publish
      await apiClient.post(`/api/custom-requests/${requestId}/publish`);
      
      alert('Request created and published successfully!');
      // Navigate to my requests page
      window.location.href = `/my-requests/${requestId}`;
    } catch (error) {
      alert('Error: ' + error.response?.data?.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="create-request-page">
      <h1>Create Custom Request</h1>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Title"
          value={formData.title}
          onChange={(e) => setFormData({ ...formData, title: e.target.value })}
          required
        />
        <textarea
          placeholder="Description"
          value={formData.description}
          onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          required
        />
        <input
          type="number"
          placeholder="Budget (VND)"
          value={formData.budget}
          onChange={(e) => setFormData({ ...formData, budget: e.target.value })}
          required
        />
        <input
          type="date"
          value={formData.deadline}
          onChange={(e) => setFormData({ ...formData, deadline: e.target.value })}
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Creating...' : 'Create & Publish'}
        </button>
      </form>
    </div>
  );
}

export default CreateRequestPage;
```

### 2. View Interested Artisans

```jsx
// pages/ConversationsListPage.jsx
import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiClient from '../api/client';

function ConversationsListPage() {
  const { requestId } = useParams();
  const navigate = useNavigate();
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadConversations();
  }, [requestId]);

  const loadConversations = async () => {
    try {
      const response = await apiClient.get(`/api/conversations/request/${requestId}`);
      setConversations(response.data.data);
    } catch (error) {
      console.error('Error loading conversations:', error);
    } finally {
      setLoading(false);
    }
  };

  const openChat = (conversationId, artisanName) => {
    navigate(`/chat/${conversationId}`, { 
      state: { artisanName } 
    });
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="conversations-list">
      <h1>Interested Artisans</h1>
      {conversations.length === 0 ? (
        <p>No artisans interested yet. Please wait...</p>
      ) : (
        <div className="artisan-list">
          {conversations.map((conv) => (
            <div key={conv.conversationId} className="artisan-card">
              <h3>{conv.artisanName}</h3>
              <p>Created: {new Date(conv.createdAt).toLocaleString()}</p>
              <button onClick={() => openChat(conv.conversationId, conv.artisanName)}>
                Chat Now
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default ConversationsListPage;
```

---

## 🎨 ARTISAN FLOW

### 1. Browse Published Requests

```jsx
// pages/BrowseRequestsPage.jsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../api/client';

function BrowseRequestsPage() {
  const navigate = useNavigate();
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadRequests();
  }, []);

  const loadRequests = async () => {
    try {
      const response = await apiClient.get('/api/custom-requests/published?page=0&size=20');
      setRequests(response.data.data.content);
    } catch (error) {
      console.error('Error loading requests:', error);
    } finally {
      setLoading(false);
    }
  };

  const showInterest = async (requestId) => {
    try {
      const response = await apiClient.post(`/api/conversations/start?requestId=${requestId}`);
      const conversationId = response.data.data.conversationId;
      
      alert('Conversation created! You can now chat with the customer.');
      navigate(`/chat/${conversationId}`);
    } catch (error) {
      alert('Error: ' + error.response?.data?.message);
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="browse-requests">
      <h1>Published Requests</h1>
      <div className="request-list">
        {requests.map((request) => (
          <div key={request.requestId} className="request-card">
            <h3>{request.title}</h3>
            <p>{request.description}</p>
            <p><strong>Budget:</strong> {request.budget.toLocaleString()} VND</p>
            <p><strong>Deadline:</strong> {new Date(request.deadline).toLocaleDateString()}</p>
            <p><strong>Customer:</strong> {request.customerName}</p>
            <button onClick={() => showInterest(request.requestId)}>
              Show Interest & Chat
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

export default BrowseRequestsPage;
```

### 2. View My Conversations

```jsx
// pages/MyConversationsPage.jsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../api/client';

function MyConversationsPage() {
  const navigate = useNavigate();
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadConversations();
  }, []);

  const loadConversations = async () => {
    try {
      const response = await apiClient.get('/api/conversations/my-conversations');
      setConversations(response.data.data);
    } catch (error) {
      console.error('Error loading conversations:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="my-conversations">
      <h1>My Conversations</h1>
      <div className="conversation-list">
        {conversations.map((conv) => (
          <div 
            key={conv.conversationId} 
            className="conversation-card"
            onClick={() => navigate(`/chat/${conv.conversationId}`)}
          >
            <h3>{conv.customerName}</h3>
            <p><strong>Request:</strong> {conv.requestTitle}</p>
            <p><small>Created: {new Date(conv.createdAt).toLocaleString()}</small></p>
          </div>
        ))}
      </div>
    </div>
  );
}

export default MyConversationsPage;
```

---

## 💬 CHAT PAGE (Both Customer & Artisan)

### Complete Chat Component with WebSocket

```jsx
// pages/ChatPage.jsx
import { useState, useEffect, useRef } from 'react';
import { useParams, useLocation } from 'react-router-dom';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import apiClient from '../api/client';

function ChatPage() {
  const { conversationId } = useParams();
  const location = useLocation();
  const [messages, setMessages] = useState([]);
  const [messageInput, setMessageInput] = useState('');
  const [stompClient, setStompClient] = useState(null);
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const messagesEndRef = useRef(null);

  const otherParticipantName = location.state?.artisanName || location.state?.customerName || 'User';

  // Load chat history
  useEffect(() => {
    loadChatHistory();
  }, [conversationId]);

  // Connect WebSocket
  useEffect(() => {
    connectWebSocket();
    return () => {
      if (stompClient) {
        stompClient.disconnect();
      }
    };
  }, [conversationId]);

  // Auto scroll to bottom
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const loadChatHistory = async () => {
    try {
      const response = await apiClient.get(
        `/api/chat/conversation/${conversationId}/messages?page=0&size=100`
      );
      setMessages(response.data.data.content);
      
      // Mark as read
      await apiClient.post(`/api/chat/conversation/${conversationId}/mark-read`);
    } catch (error) {
      console.error('Error loading chat history:', error);
    } finally {
      setLoading(false);
    }
  };

  const connectWebSocket = () => {
    const token = localStorage.getItem('token');
    const wsUrl = process.env.REACT_APP_WS_URL || 'http://localhost:8080/ws';
    
    const socket = new SockJS(wsUrl);
    const client = Stomp.over(socket);

    // Disable debug in production
    if (process.env.NODE_ENV === 'production') {
      client.debug = () => {};
    }

    client.connect(
      { Authorization: `Bearer ${token}` },
      () => {
        console.log('✅ WebSocket connected');
        setConnected(true);

        // Subscribe to conversation
        client.subscribe(`/topic/chat/${conversationId}`, (message) => {
          const newMessage = JSON.parse(message.body);
          setMessages((prev) => [...prev, newMessage]);
          
          // Mark as read if not my message
          const currentUserId = localStorage.getItem('userId');
          if (newMessage.senderId !== currentUserId) {
            apiClient.post(`/api/chat/conversation/${conversationId}/mark-read`);
          }
        });

        setStompClient(client);
      },
      (error) => {
        console.error('❌ WebSocket error:', error);
        setConnected(false);
      }
    );
  };

  const sendMessage = async () => {
    const content = messageInput.trim();
    if (!content) return;

    if (stompClient && connected) {
      // Send via WebSocket
      stompClient.send(
        `/app/chat/${conversationId}`,
        {},
        JSON.stringify({
          conversationId: conversationId,
          content: content,
          messageType: 'TEXT',
        })
      );
      setMessageInput('');
    } else {
      // Fallback to REST API
      try {
        const response = await apiClient.post('/api/chat/send', {
          conversationId: conversationId,
          content: content,
          messageType: 'TEXT',
        });
        setMessages((prev) => [...prev, response.data.data]);
        setMessageInput('');
      } catch (error) {
        alert('Error sending message: ' + error.response?.data?.message);
      }
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  if (loading) return <div>Loading chat...</div>;

  return (
    <div className="chat-page">
      {/* Header */}
      <div className="chat-header">
        <h2>Chat with {otherParticipantName}</h2>
        <div className="connection-status">
          {connected ? '🟢 Connected' : '🔴 Disconnected'}
        </div>
      </div>

      {/* Messages */}
      <div className="messages-container">
        {messages.map((msg) => {
          const isMyMessage = msg.senderId === localStorage.getItem('userId');
          return (
            <div
              key={msg.messageId}
              className={`message ${isMyMessage ? 'my-message' : 'other-message'}`}
            >
              <div className="message-sender">{msg.senderName}</div>
              <div className="message-content">{msg.content}</div>
              <div className="message-time">
                {new Date(msg.sentAt).toLocaleTimeString()}
                {isMyMessage && (msg.isRead ? ' ✓✓' : ' ✓')}
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="message-input-container">
        <textarea
          value={messageInput}
          onChange={(e) => setMessageInput(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="Type a message..."
          rows="3"
        />
        <button onClick={sendMessage} disabled={!messageInput.trim()}>
          Send
        </button>
      </div>
    </div>
  );
}

export default ChatPage;
```

### CSS for Chat Page

```css
/* styles/ChatPage.css */
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 800px;
  margin: 0 auto;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #ddd;
  background: #f5f5f5;
}

.connection-status {
  font-size: 0.9rem;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  background: #fff;
}

.message {
  margin-bottom: 1rem;
  padding: 0.75rem;
  border-radius: 8px;
  max-width: 70%;
}

.my-message {
  margin-left: auto;
  background: #007bff;
  color: white;
  text-align: right;
}

.other-message {
  margin-right: auto;
  background: #f1f1f1;
  color: #333;
}

.message-sender {
  font-weight: bold;
  font-size: 0.85rem;
  margin-bottom: 0.25rem;
}

.message-content {
  margin-bottom: 0.25rem;
}

.message-time {
  font-size: 0.75rem;
  opacity: 0.7;
}

.message-input-container {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  border-top: 1px solid #ddd;
  background: #f5f5f5;
}

.message-input-container textarea {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: none;
  font-family: inherit;
}

.message-input-container button {
  padding: 0.75rem 1.5rem;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.message-input-container button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
```

---

## 🔄 Complete User Flow Diagram

```
CUSTOMER SIDE:
1. CreateRequestPage → Create & Publish
2. MyRequestsPage → View request status
3. ConversationsListPage → See interested artisans
4. ChatPage → Chat with artisan
5. Select artisan → Continue to order

ARTISAN SIDE:
1. BrowseRequestsPage → View published requests
2. Click "Show Interest" → Create conversation
3. ChatPage → Chat with customer
4. Negotiate price & details
5. Create CustomOrder with stages
```

---

## 📦 Package Dependencies

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "axios": "^1.6.0",
    "sockjs-client": "^1.6.1",
    "@stomp/stompjs": "^7.0.0"
  }
}
```

Install:
```bash
npm install axios sockjs-client @stomp/stompjs
```

---

## 🎯 Quick Start Checklist

- [ ] Setup API client with axios
- [ ] Add token to localStorage after login
- [ ] Create routing for all pages
- [ ] Implement CreateRequestPage
- [ ] Implement BrowseRequestsPage
- [ ] Implement ConversationsListPage
- [ ] Implement ChatPage with WebSocket
- [ ] Test chat real-time
- [ ] Handle connection errors
- [ ] Add loading states
- [ ] Style components

---

## 🐛 Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| WebSocket not connecting | Check token in localStorage, verify WS_URL |
| Messages not real-time | Check WebSocket connection status |
| 401 Unauthorized | Token expired, redirect to login |
| Can't send message | Check conversationId is correct |
| Messages not loading | Check API endpoint and pagination |

---

Với guide này, Frontend developer có thể dễ dàng implement toàn bộ chat flow!
