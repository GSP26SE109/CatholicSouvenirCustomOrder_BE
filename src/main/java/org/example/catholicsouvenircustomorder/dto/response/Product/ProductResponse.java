package org.example.catholicsouvenircustomorder.dto.response.Product;

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
    private String productName;
    private BigDecimal productPrice;
    private String productDescription;
    private String material;
    private String size;
    private int quantity;
    private UUID artisanId;
    private String artisanName;
    private String status;
    private LocalDateTime createdAt;
    private List<ProductImageResponse> images;
}
