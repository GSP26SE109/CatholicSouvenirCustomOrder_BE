package org.example.catholicsouvenircustomorder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CalculateShippingRequest;
import org.example.catholicsouvenircustomorder.dto.request.CheckoutRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.CheckoutResponseDTO;
import org.example.catholicsouvenircustomorder.dto.response.ShippingFeeResponse;
import org.example.catholicsouvenircustomorder.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {
    
    private final CheckoutService checkoutService;
    
    /**
     * Calculate shipping fee before checkout
     */
    @PostMapping("/calculate-shipping")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @Operation(summary = "Calculate shipping fee for cart items", 
               description = "Groups cart items by artisan and calculates separate shipping fees")
    public ResponseEntity<BaseResponse<ShippingFeeResponse>> calculateShipping(
            @AuthenticationPrincipal UUID customerId,
            @Valid @RequestBody CalculateShippingRequest request) {
        ShippingFeeResponse response = checkoutService.calculateShippingFeeForCart(
                customerId, request);
        return ResponseEntity.ok(BaseResponse.<ShippingFeeResponse>builder()
                .code(200)
                .message("Tính phí vận chuyển thành công")
                .data(response)
                .build());
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @Operation(summary = "Checkout cart", 
               description = "Convert cart to orders (one order per artisan) with shipping info")
    public ResponseEntity<BaseResponse<CheckoutResponseDTO>> checkout(
            @AuthenticationPrincipal UUID customerId,
            @Valid @RequestBody CheckoutRequest request) {
        
        CheckoutResponseDTO response = checkoutService.checkout(customerId, request);
        
        return ResponseEntity.ok(BaseResponse.<CheckoutResponseDTO>builder()
                .code(200)
                .message(response.getMessage())
                .data(response)
                .build());
    }
    
    @GetMapping("/validate")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<String>> validateCart(
            @AuthenticationPrincipal UUID customerId) {
        
        checkoutService.validateCart(customerId);
        
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .code(200)
                .message("Giỏ hàng hợp lệ")
                .data("OK")
                .build());
    }
}
