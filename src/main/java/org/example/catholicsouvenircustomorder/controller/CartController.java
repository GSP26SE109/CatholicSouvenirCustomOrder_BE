package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.CartResponse;
import org.example.catholicsouvenircustomorder.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("/{accountId}")
    public ResponseEntity<BaseResponse> getCart(@PathVariable UUID accountId) {
        List<CartResponse>responses= cartService.getCart(accountId);
       return ResponseEntity.ok(BaseResponse.success("Lấy danh sách sản phẩm thành công",responses));
    }
    @DeleteMapping("/{accountId}")
    public void deleteCart(@PathVariable UUID accountId) {
        cartService.clearCart(accountId);
    }
    @DeleteMapping("/{accountId}/{productId}")
    public void removeFromCart(@PathVariable UUID accountId, @PathVariable UUID productId) {
        cartService.removeFromCart(accountId, productId);
    }
    @PostMapping("/{accountId}")
    public void addToCart(@PathVariable UUID accountId, @RequestBody UUID productId,@RequestParam int quantity) {
        cartService.addToCart(accountId, productId, quantity);
    }
    @PutMapping("/{accountId}")
    public void updateCart(@PathVariable UUID accountId, @RequestBody UUID productId,@RequestParam int quantity) {
        cartService.updateCart(accountId, productId, quantity);
    }
}
