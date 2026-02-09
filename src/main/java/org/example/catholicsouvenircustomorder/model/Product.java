package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int productId;
    private UUID artisanId;
    private String productName;
    private double productPrice;
    private String productDescription;
    private int quantity;
    private boolean status;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product")
    private List<ProductImage> productImages;

    @OneToMany(mappedBy = "product")
    private List<OrderDetail> orderDetails;
    @Version
    private int version;
}
