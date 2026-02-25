package org.example.catholicsouvenircustomorder.dto.response;

import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.ProductImage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID productId;
    private UUID artisanId;
    private String productName;
    private BigDecimal productPrice;
    private String productDescription;
    private String material;
    private String size;
    private int quantity;
    private boolean status;
    private LocalDateTime createdAt;
    private List<ProductImage> productImages;
}
