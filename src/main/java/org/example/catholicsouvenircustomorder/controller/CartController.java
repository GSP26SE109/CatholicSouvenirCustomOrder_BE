package org.example.catholicsouvenircustomorder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.AddToCartRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateCartItemRequest;
import org.example.catholicsouvenircustomorder.dto.response.CartResponse;
import org.example.catholicsouvenircustomorder.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    
    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UUID customerId) {
        
        CartResponse cart = cartService.getCart(customerId);
        
        return ResponseEntity.ok(BaseResponse.<CartResponse>builder()
                .code(200)
                .message("Lấy giỏ hàng thành công")
                .data(cart)
                .build());
    }
    
    @PostMapping("/items")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<CartResponse>> addToCart(
            @AuthenticationPrincipal UUID customerId,
            @Valid @RequestBody AddToCartRequest request) {
        
        CartResponse cart = cartService.addToCart(customerId, request);
        
        return ResponseEntity.ok(BaseResponse.<CartResponse>builder()
                .code(200)
                .message("Thêm vào giỏ hàng thành công")
                .data(cart)
                .build());
    }
    
    @PutMapping("/items/{cartItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal UUID customerId,
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        
        CartResponse cart = cartService.updateCartItem(customerId, cartItemId, request);
        
        return ResponseEntity.ok(BaseResponse.<CartResponse>builder()
                .code(200)
                .message("Cập nhật giỏ hàng thành công")
                .data(cart)
                .build());
    }
    
    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<CartResponse>> removeCartItem(
            @AuthenticationPrincipal UUID customerId,
            @PathVariable UUID cartItemId) {
        
        CartResponse cart = cartService.removeCartItem(customerId, cartItemId);
        
        return ResponseEntity.ok(BaseResponse.<CartResponse>builder()
                .code(200)
                .message("Xóa sản phẩm khỏi giỏ hàng thành công")
                .data(cart)
                .build());
    }
    
    @DeleteMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<CartResponse>> clearCart(
            @AuthenticationPrincipal UUID customerId) {
        
        CartResponse cart = cartService.clearCart(customerId);
        
        return ResponseEntity.ok(BaseResponse.<CartResponse>builder()
                .code(200)
                .message("Xóa toàn bộ giỏ hàng thành công")
                .data(cart)
                .build());
    }
    
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<Integer>> getCartItemCount(
            @AuthenticationPrincipal UUID customerId) {
        
        Integer count = cartService.getCartItemCount(customerId);
        
        return ResponseEntity.ok(BaseResponse.<Integer>builder()
                .code(200)
                .message("Lấy số lượng sản phẩm thành công")
                .data(count)
                .build());
    }
}
