package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, length = 2000)
    private String description;
    
    @Column(length = 1000)
    private String referenceImageUrl;
    
    @Column(length = 1000)
    private String aiGeneratedImageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomRequestStatus status = CustomRequestStatus.PENDING;
    
    @ManyToMany
    @JoinTable(
        name = "request_selected_artisans",
        joinColumns = @JoinColumn(name = "request_id"),
        inverseJoinColumns = @JoinColumn(name = "artisan_id")
    )
    private List<Artisan> selectedArtisans = new ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "confirmed_artisan_id")
    private Artisan confirmedArtisan;
    
    @OneToMany(mappedBy = "customRequest", cascade = CascadeType.ALL)
    private List<Quotation> quotations = new ArrayList<>();
    
    @OneToOne(mappedBy = "customRequest")
    private CustomOrder customOrder;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
