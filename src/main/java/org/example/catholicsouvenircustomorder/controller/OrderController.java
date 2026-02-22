package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.CreateOrderRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping()
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        List<OrderResponseDTO> orderList = orderService.findAll();
        return ResponseEntity.ok().body(orderList);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable String orderId) {
        OrderResponseDTO order = orderService.findById(UUID.fromString(orderId));
        return ResponseEntity.ok().body(order);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrdersByAccountId(@PathVariable String accountId) {
        List<OrderResponseDTO> orderList = orderService.findAllByAccountId(UUID.fromString(accountId));
        return ResponseEntity.ok().body(orderList);
    }

    @PostMapping()
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody CreateOrderRequest request) {
        OrderResponseDTO order = orderService.createOrderWithRetry(request);
        return ResponseEntity.ok().body(order);
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> updateOrder(@PathVariable String orderId, @RequestBody String status) {
        OrderResponseDTO order = orderService.update(UUID.fromString(orderId), status);
        return ResponseEntity.ok().body(order);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        orderService.delete(UUID.fromString(orderId));
        return ResponseEntity.noContent().build();
    }
}
