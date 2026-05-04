package org.example.catholicsouvenircustomorder.dto.request.Product;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateProductRequest {
    private String productName;

    private String productDescription;

    @Min(value = 0, message = "Price must be greater than 0")
    private BigDecimal productPrice;

    @Min(value = 0, message = "Quantity must be >= 0")
    private Integer quantity;
    private String size;
    private UUID categoryId;
    
    // Initialize with empty lists to handle null/empty cases
    private List<String> tags = new ArrayList<>();
    private List<UUID> deleteImageIds = new ArrayList<>();
    private List<MultipartFile> newImages = new ArrayList<>();
}
