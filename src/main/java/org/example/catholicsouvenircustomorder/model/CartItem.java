package org.example.catholicsouvenircustomorder.model;


import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;
@Entity
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;
}