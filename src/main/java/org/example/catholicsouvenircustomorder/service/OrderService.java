package org.example.catholicsouvenircustomorder.service;
import java.util.List;
import java.util.UUID;

import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.CreateOrderRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;

public interface OrderService {
    List<OrderResponseDTO> findAll();
    List<OrderResponseDTO> findAllByAccountId(UUID accountId);
    OrderResponseDTO findById(UUID id);
    OrderResponseDTO create(CreateOrderRequest request);
    OrderResponseDTO update(UUID orderId, String status);
    void delete(UUID orderId);
    List<OrderResponseDTO> findAllOrderByArtisanId(UUID artisanId);
}
