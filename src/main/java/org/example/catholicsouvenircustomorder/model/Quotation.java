package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;
import org.example.catholicsouvenircustomorder.dto.QuotationStageDTO;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "quotations")
public class Quotation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID quotationId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "request_id")
    private CustomRequest request;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "artisan_id")
    private Artisan artisan;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(nullable = false)
    private Integer estimatedDays;
    
    @Column(columnDefinition = "TEXT")
    private String proposal;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<QuotationStageDTO> stages;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotationStatus status = QuotationStatus.PENDING;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
