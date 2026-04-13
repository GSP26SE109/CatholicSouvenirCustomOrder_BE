package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.CreateOrderRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.model.OrderStatus;
import org.example.catholicsouvenircustomorder.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping()
    public ResponseEntity<BaseResponse> getAllOrders(
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "10") int size,
          @RequestParam(defaultValue = "createAt") String sortBy,
          @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<OrderResponseDTO> orderList = orderService.findAll(pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách order thành công",orderList));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<BaseResponse> getOrderById(@PathVariable String orderId) {
        OrderResponseDTO order = orderService.findById(UUID.fromString(orderId));
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin order thành công",order));
    }

    @GetMapping("/account")
    public ResponseEntity<BaseResponse> getAllOrdersByAccountId(
            @AuthenticationPrincipal UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<OrderResponseDTO> orderList = orderService.findAllByAccountId(accountId,pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách order thành công",orderList));
    }
    @GetMapping("/artisan/{artisanId}")
    public ResponseEntity<BaseResponse> getAllOrdersByArtisanId(
            @PathVariable String artisanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<OrderResponseDTO> orderList = orderService.findAllOrderByArtisanId(UUID.fromString(artisanId),pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách order thành công",orderList));
    }
    @PostMapping()
    public ResponseEntity<BaseResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponseDTO order = orderService.create(request);
        return ResponseEntity.ok(BaseResponse.success("Tạo order thành công",order));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<BaseResponse> updateOrder(@PathVariable String orderId, @RequestBody OrderStatus status) {
        OrderResponseDTO order = orderService.update(UUID.fromString(orderId), status);
        return ResponseEntity.ok(BaseResponse.success("Sửa order thành công",order));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        orderService.delete(UUID.fromString(orderId));
        return ResponseEntity.noContent().build();
    }
}
