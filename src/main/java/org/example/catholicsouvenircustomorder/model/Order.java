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
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;
    private LocalDateTime orderDate;
    private BigDecimal total;
    private String status;
    private String paymentMethod;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Account customer;
    
    @ManyToOne
    @JoinColumn(name = "order_group_id")
    @com.fasterxml.jackson.annotation.JsonBackReference("orderGroup-orders")
    private OrderGroup orderGroup;

    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference("order-details")
    private List<OrderDetail> orderDetails = new ArrayList<>();
    
    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference("order-template-details")
    private List<OrderTemplateDetail> templateDetails = new ArrayList<>();
}
