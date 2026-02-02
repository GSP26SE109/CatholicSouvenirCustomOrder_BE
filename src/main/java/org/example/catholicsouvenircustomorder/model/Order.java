package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;
    private LocalDateTime orderDate;
    private double total;
    private String status;
    private String paymentMethod;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Account customer;

    @OneToMany(mappedBy = "order")
    private List<OrderDetail> orderDetails;


}
