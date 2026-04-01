package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.Product.CreateProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductRequest;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProductService {
    Page<ProductResponse> findAll(Pageable pageable);

    Page<ProductResponse> findAllByArtisanId(
            UUID artisanId,
            String status,
            Pageable pageable
    );

    ProductResponse findById(UUID id);

    ProductResponse create(CreateProductRequest request, UUID artisanId);

    ProductResponse update(UUID artisanId,UUID productId, UpdateProductRequest dto);

    void delete(UUID productId);

    Map<UUID, Product> loadAndValidateQuantity(List<OrderItemRequest> items);

    ProductResponse ApproveProduct(UUID productId, org.example.catholicsouvenircustomorder.dto.request.ApproveProductRequest request);
}
