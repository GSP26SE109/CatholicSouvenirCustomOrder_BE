package org.example.catholicsouvenircustomorder.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "custom_orders")
public class CustomOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID customOrderId;
    
    @OneToOne(optional = false)
    @JoinColumn(name = "request_id", unique = true)
    private CustomRequest request;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "artisan_id")
    private Artisan artisan;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomOrderStatus status = CustomOrderStatus.PENDING_CONFIRMATION;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPrice;
    
    @OneToMany(mappedBy = "customOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stageOrder ASC")
    @JsonManagedReference
    private List<CustomOrderStage> stages = new ArrayList<>();
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
