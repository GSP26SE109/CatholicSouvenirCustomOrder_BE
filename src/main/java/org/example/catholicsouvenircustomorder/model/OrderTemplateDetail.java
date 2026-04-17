package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@Table(name = "order_template_details")
public class OrderTemplateDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderTemplateDetailId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    @com.fasterxml.jackson.annotation.JsonBackReference("order-template-details")
    private Order order;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "template_id")
    private ProductTemplate template;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> customizations;
    
    @Column(nullable = false)
    private Integer quantity = 1;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (subtotal == null && unitPrice != null && quantity != null) {
            subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
