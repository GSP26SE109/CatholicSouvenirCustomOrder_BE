package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.request.ProductCreateDTO;
import org.example.catholicsouvenircustomorder.model.Product;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProductService {
    List<Product> findAll();
    List<Product> findAllByArtisanId(UUID artisanId);
    Product findById(UUID id);
    Product create(ProductCreateDTO product);
    Product update(UUID productId,ProductCreateDTO product);
    void delete(UUID productId);
    public Map<UUID, Product> loadAndValidateQuantity(List<OrderItemRequest> items);
}
