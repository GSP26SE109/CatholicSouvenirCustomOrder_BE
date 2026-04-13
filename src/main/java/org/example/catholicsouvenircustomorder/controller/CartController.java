package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CartItemRequest;
import org.example.catholicsouvenircustomorder.dto.response.Cart.CartResponse;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping()
    public ResponseEntity<BaseResponse> getCart(@AuthenticationPrincipal UUID accountId) {
        CartResponse responses = cartService.getCart(accountId);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách sản phẩm thành công", responses));
    }

    @PostMapping()
    public ResponseEntity<BaseResponse> addToCart(
            @AuthenticationPrincipal UUID accountId,
            @ModelAttribute CartItemRequest request) {
        cartService.addToCart(accountId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(BaseResponse.success("Thêm sản phẩm thành công", null));
    }

    @PutMapping()
    public ResponseEntity<BaseResponse> updateCart(
            @AuthenticationPrincipal UUID accountId,
            @ModelAttribute CartItemRequest request) {
        cartService.updateCart(accountId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(BaseResponse.success("Cập nhật giỏ hàng thành công", null));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<BaseResponse> removeFromCart(
            @AuthenticationPrincipal UUID accountId,
            @PathVariable UUID productId) {
        cartService.removeFromCart(accountId, productId);
        return ResponseEntity.ok(BaseResponse.success("Xóa sản phẩm thành công", null));
    }

    @DeleteMapping()
    public ResponseEntity<BaseResponse> clearCart(@AuthenticationPrincipal UUID accountId) {
        cartService.clearCart(accountId);
        return ResponseEntity.ok(BaseResponse.success("Xóa giỏ hàng thành công", null));
    }
    @PostMapping("/check_out")
    public ResponseEntity<BaseResponse> checkout(
            @AuthenticationPrincipal UUID accountId,
            @RequestBody List<UUID> selectedProductIds){
        List<OrderResponseDTO> orders= cartService.checkout(accountId, selectedProductIds);
        return ResponseEntity.ok(BaseResponse.success("Check out thành công", orders));
    }
}

