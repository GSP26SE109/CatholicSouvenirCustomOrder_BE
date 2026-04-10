package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@Table(name = "custom_requests")
public class CustomRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID requestId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Account customer;
    
    // ProductTemplate relationship removed - template-based uses separate Order flow
    
    @ManyToOne
    @JoinColumn(name = "selected_artisan_id")
    private Artisan selectedArtisan;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String aiConceptImageUrl;
    
    @Column(columnDefinition = "TEXT")
    private String aiImagePrompt;
    
    @Column(nullable = false)
    private Integer imageGenCount = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomRequestStatus status = CustomRequestStatus.DRAFT;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType;
    
    @Column(precision = 18, scale = 2)
    private BigDecimal minBudget;
    
    @Column(precision = 18, scale = 2)
    private BigDecimal maxBudget;
    
    @OneToOne(mappedBy = "request")
    private CustomOrder customOrder;
    
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Conversation> conversations = new ArrayList<>();
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
