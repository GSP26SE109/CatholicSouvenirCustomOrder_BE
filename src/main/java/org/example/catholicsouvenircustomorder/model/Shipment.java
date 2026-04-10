package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "shipments")
public class Shipment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID shipmentId;
    
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @OneToOne
    @JoinColumn(name = "custom_order_id")
    private CustomOrder customOrder;
    
    @Column(unique = true)
    private String ghnOrderCode;
    
    @Column(unique = true)
    private String trackingNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShippingStatus status = ShippingStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String pickAddress;
    
    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;
    
    private String recipientName;
    private String recipientPhone;
    
    private BigDecimal shippingFee;
    private BigDecimal insuranceFee;
    
    private LocalDateTime estimatedDelivery;
    private LocalDateTime actualDelivery;
    
    @Column(columnDefinition = "TEXT")
    private String note;
    
    @Column(columnDefinition = "TEXT")
    private String statusHistory;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
