package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_stages")
@Data
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID stageId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private String name;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    private String status;

    private Integer stageOrder;

    @Version
    private Long version;
}

