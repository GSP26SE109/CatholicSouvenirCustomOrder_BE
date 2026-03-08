package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(
        name = "cart",uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id"})
)
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID cartId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "account_id")
    private Account account;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();
    private LocalDateTime updatedAt;
}
