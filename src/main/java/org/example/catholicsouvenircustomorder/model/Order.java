package org.example.catholicsouvenircustomorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;
    private LocalDateTime orderDate;
    private BigDecimal total;
    private BigDecimal shippingFee;
    private String status;
    private String paymentMethod;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    
    /**
     * Date when locked balance will be released to artisan (7 days after payment)
     * Used for complaint protection period
     */
    private LocalDateTime unlockDate;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Account customer;
    
    @ManyToOne
    @JoinColumn(name = "order_group_id")
    @JsonIgnore
    private OrderGroup orderGroup;

    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<OrderDetail> orderDetails = new ArrayList<>();
    
    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private List<OrderTemplateDetail> templateDetails = new ArrayList<>();
}
