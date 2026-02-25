package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.request.ProductCreateDTO;
import org.example.catholicsouvenircustomorder.dto.response.ProductResponse;
import org.example.catholicsouvenircustomorder.model.Product;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProductService {
    List<ProductResponse> findAll();
    List<ProductResponse> findAllByArtisanId(UUID artisanId);
    ProductResponse findById(UUID id);
    ProductResponse create(ProductCreateDTO product);
    ProductResponse update(UUID productId,ProductCreateDTO product);
    void delete(UUID productId);
    public Map<UUID, Product> loadAndValidateQuantity(List<OrderItemRequest> items);
}
