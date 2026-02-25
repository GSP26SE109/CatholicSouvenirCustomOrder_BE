package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreateProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.request.ProductCreateDTO;
import org.example.catholicsouvenircustomorder.dto.response.ProductResponse;
import org.example.catholicsouvenircustomorder.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProductService {
    List<ProductResponse> findAll();

    Page<ProductResponse> findAllByArtisanId(
            UUID artisanId,
            String status,
            Pageable pageable
    );

    ProductResponse findById(UUID id);

    Product create(CreateProductRequest request);

    ProductResponse update(UUID productId, ProductCreateDTO product);

    void delete(UUID productId);

    Map<UUID, Product> loadAndValidateQuantity(List<OrderItemRequest> items);

    ProductResponse ApproveProduct(UUID productId, String status);
}
