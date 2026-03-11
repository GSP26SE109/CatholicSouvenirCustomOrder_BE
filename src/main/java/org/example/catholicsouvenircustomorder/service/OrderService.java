package org.example.catholicsouvenircustomorder.service;
import java.util.List;
import java.util.UUID;

import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.CreateOrderRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    Page<OrderResponseDTO> findAll(Pageable pageable);
    Page<OrderResponseDTO> findAllByAccountId(UUID accountId,Pageable pageable);
    OrderResponseDTO findById(UUID id);
    OrderResponseDTO create(CreateOrderRequest request);
    OrderResponseDTO update(UUID orderId, String status);
    void delete(UUID orderId);
    Page<OrderResponseDTO> findAllOrderByArtisanId(UUID artisanId,Pageable pageable);
}
