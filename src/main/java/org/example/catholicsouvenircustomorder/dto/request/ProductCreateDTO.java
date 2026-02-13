package org.example.catholicsouvenircustomorder.dto.request;


import lombok.Data;
import org.example.catholicsouvenircustomorder.model.OrderDetail;
import org.example.catholicsouvenircustomorder.model.ProductImage;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductCreateDTO {
    private UUID artisanId;
    private String productName;
    private BigDecimal productPrice;
    private String productDescription;
    private int quantity;
    private boolean status;
    private List<ProductImage> productImages;
    private List<OrderDetail> orderDetails;
}
