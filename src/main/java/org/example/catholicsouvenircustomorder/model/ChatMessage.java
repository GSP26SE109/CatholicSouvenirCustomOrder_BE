package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID messageId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private Account sender;
    
    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private Account receiver; // Nullable for public messages
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType = MessageType.TEXT;
    
    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private boolean isRead = false;
    
    // Lombok @Data tự động tạo isRead() và setRead()
    // Nhưng một số code có thể dùng getIsRead() và setIsRead()
    public boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }
}
