package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "conversations")
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID conversationId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "request_id")
    private CustomRequest request;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Account customer;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "artisan_id")
    private Artisan artisan;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sentAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
