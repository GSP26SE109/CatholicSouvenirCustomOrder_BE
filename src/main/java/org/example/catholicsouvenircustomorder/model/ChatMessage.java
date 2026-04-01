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
    @JoinColumn(name = "request_id")
    private CustomRequest customRequest;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private Account sender;
    
    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private Account receiver;
    
    @Column(nullable = false, length = 2000)
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType = MessageType.TEXT;
    
    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
    
    private Boolean isRead = false;
}
