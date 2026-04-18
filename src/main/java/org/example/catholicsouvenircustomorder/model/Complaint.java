package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "complaints")
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID complaintId;
    
    // Relationship to Order (nullable for CustomOrder)
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    // Relationship to CustomOrder (nullable for Order)
    @ManyToOne
    @JoinColumn(name = "custom_order_id")
    private CustomOrder customOrder;
    
    // Customer who filed the complaint
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Account customer;
    
    // Artisan responsible for the product
    @ManyToOne(optional = false)
    @JoinColumn(name = "artisan_id")
    private Artisan artisan;
    
    // Complaint details
    @Column(nullable = false, length = 1000)
    private String reason;
    
    @ElementCollection
    @CollectionTable(name = "complaint_images", joinColumns = @JoinColumn(name = "complaint_id"))
    @Column(name = "image_url")
    private List<String> evidenceImages = new ArrayList<>();
    
    // Artisan response
    @Column(length = 1000)
    private String artisanResponse;
    
    @Column(nullable = false)
    private Boolean requireReturn = false;
    
    private LocalDateTime artisanResponseAt;
    
    // Admin decision
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.PENDING;
    
    @Column(precision = 18, scale = 2)
    private BigDecimal refundAmount;
    
    @Column(length = 500)
    private String adminNote;
    
    @Column(length = 500)
    private String rejectionReason;
    
    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private Account reviewedBy;
    
    private LocalDateTime reviewedAt;
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Relationship to RefundTransaction
    @OneToOne(mappedBy = "complaint", cascade = CascadeType.ALL)
    private RefundTransaction refundTransaction;
    
    // Relationship to Return Shipment (reuse existing Shipment entity)
    @OneToOne(mappedBy = "complaint", cascade = CascadeType.ALL)
    private Shipment returnShipment;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
