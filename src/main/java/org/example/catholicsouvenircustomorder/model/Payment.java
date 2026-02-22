package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(optional = true)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    private String status;

    @Column(nullable = false)
    private String providerTransactionId;

    private LocalDateTime createdAt;
}
