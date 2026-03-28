package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@Table(name="order_detail",
indexes = {
        @Index(name="idx_orders_detail_product_id",columnList = "product_id"),
        @Index(name="idx_orders_detail_order_id",columnList = "order_id")
})
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
    private int discount;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
