package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.CreateOrderRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
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
    public ResponseEntity<BaseResponse> getAllOrders() {
        List<OrderResponseDTO> orderList = orderService.findAll();
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách order thành công",orderList));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<BaseResponse> getOrderById(@PathVariable String orderId) {
        OrderResponseDTO order = orderService.findById(UUID.fromString(orderId));
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin order thành công",order));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<BaseResponse> getAllOrdersByAccountId(@PathVariable String accountId) {
        List<OrderResponseDTO> orderList = orderService.findAllByAccountId(UUID.fromString(accountId));
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách order thành công",orderList));
    }

    @PostMapping()
    public ResponseEntity<BaseResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponseDTO order = orderService.create(request);
        return ResponseEntity.ok(BaseResponse.success("Tạo order thành công",order));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<BaseResponse> updateOrder(@PathVariable String orderId, @RequestBody String status) {
        OrderResponseDTO order = orderService.update(UUID.fromString(orderId), status);
        return ResponseEntity.ok(BaseResponse.success("Sửa order thành công",order));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        orderService.delete(UUID.fromString(orderId));
        return ResponseEntity.noContent().build();
    }
}
