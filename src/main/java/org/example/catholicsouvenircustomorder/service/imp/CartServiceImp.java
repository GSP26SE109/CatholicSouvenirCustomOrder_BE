package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.CreateOrderRequest;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.response.Cart.CartItemResponse;
import org.example.catholicsouvenircustomorder.dto.response.Cart.CartResponse;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Cart;
import org.example.catholicsouvenircustomorder.model.CartItem;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.CartService;
import org.example.catholicsouvenircustomorder.service.OrderService;
import org.example.catholicsouvenircustomorder.service.ProductService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImp implements CartService {
    private final StringRedisTemplate redisTemplate;
    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AccountRepository accountRepository;
    private final ProductService productService;
    private String buildKey(UUID accountId) {
        return "cart:" + accountId;
    }

    @Transactional
    public void addToCart(UUID accountId, UUID productId, int quantity) {

        redisTemplate.opsForHash()
                .increment(buildKey(accountId), productId.toString(), quantity);

        Cart cart = cartRepository.findByAccount_AccountId(accountId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setAccount(accountRepository.findById(accountId)
                            .orElseThrow(() -> new ResourceNotFoundException("Account not found")));
                    return cartRepository.save(newCart);
                });

        CartItem item = cartItemRepository
                .findByCart_CartIdAndProduct_ProductId(cart.getCartId(), productId)
                .orElse(null);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found")));
            item.setQuantity(quantity);

        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }

        cartItemRepository.save(item);
    }

    @Override
    public CartResponse getCart(UUID accountId) {

        String key = buildKey(accountId);

        Map<Object, Object> redisCart =
                redisTemplate.opsForHash().entries(key);

        if (redisCart.isEmpty()) {

            List<CartItem> items = cartRepository.findByAccount_AccountId(accountId).get().getItems();

            Map<String, String> map = new HashMap<>();

            for (CartItem item : items) {
                map.put(
                        item.getProduct().getProductId().toString(),
                        String.valueOf(item.getQuantity())
                );
            }

            if (!map.isEmpty()) {
                redisTemplate.opsForHash().putAll(key, map);
            }

            redisCart = redisTemplate.opsForHash().entries(key);
        }

        List<UUID> productIds = redisCart.keySet()
                .stream()
                .map(id -> UUID.fromString(id.toString()))
                .toList();

        Map<UUID, Product> productMap =
                productRepository.findAllById(productIds)
                        .stream()
                        .collect(Collectors.toMap(Product::getProductId, p -> p));
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal total = new BigDecimal(0);
        for (Map.Entry<Object, Object> entry : redisCart.entrySet()) {

            UUID productId = UUID.fromString(entry.getKey().toString());
            int quantity = Integer.parseInt(entry.getValue().toString());

            Product product = productMap.get(productId);

            BigDecimal subtotal = product.getProductPrice()
                    .multiply(BigDecimal.valueOf(quantity));

            total = total.add(subtotal);
            if (product != null) {
                itemResponses.add(
                        CartItemResponse.builder()
                                .productId(productId)
                                .productName(product.getProductName())
                                .productPrice(product.getProductPrice())
                                .quantity(quantity)
                                .images(product.getImages()
                                        .stream()
                                        .map(ProductImage::getImage_url)
                                        .toList())
                                .build());
            }
        }

        return CartResponse.builder()
                .items(itemResponses)
                .totalPrice(total)
                .build();
    }

    @Override
    public void clearCart(UUID accountId) {
        redisTemplate.delete(buildKey(accountId));
    }

    @Override
    public void removeFromCart(UUID accountId, UUID productId) {
        redisTemplate.opsForHash()
                .delete(buildKey(accountId), productId.toString());
    }

    @Override
    public void updateCart(UUID accountId, UUID productId, int quantity) {
        String cartKey = "cart:" + accountId;

        if (quantity <= 0) {
            redisTemplate.opsForHash()
                    .delete(cartKey, productId.toString());
        } else {
            redisTemplate.opsForHash()
                    .put(cartKey, productId.toString(), String.valueOf(quantity));
        }
    }

    @Override
    @Transactional
    public List<OrderResponseDTO> checkout(UUID accountId, List<UUID> selectedProductIds) {

        Map<Object, Object> cartItems = redisTemplate.opsForHash().entries(buildKey(accountId));

        Map<UUID, Integer> selectedItems = cartItems.entrySet().stream()
                .filter(entry -> selectedProductIds.contains(UUID.fromString(entry.getKey().toString())))
                .collect(Collectors.toMap(
                        entry -> UUID.fromString(entry.getKey().toString()),
                        entry -> Integer.parseInt(entry.getValue().toString())
                ));

        if (selectedItems.isEmpty()) {
            throw new ResourceNotFoundException("No selected items found in cart");
        }

        // Build a flat list of ALL items across all artisans first
        List<OrderItemRequest> allItems = selectedItems.entrySet().stream()
                .map(entry -> OrderItemRequest.builder()
                        .productId(entry.getKey())
                        .quantity(entry.getValue())
                        .build())
                .toList();

        // Validate stock for ALL items before creating any order
        Map<UUID, Product> validatedProducts = productService.loadAndValidateQuantity(allItems);

        Map<UUID, List<OrderItemRequest>> groupedByArtisan = new HashMap<>();

        for (OrderItemRequest item : allItems) {
            UUID artisanId = validatedProducts.get(item.getProductId())
                    .getArtisan()
                    .getArtisanUuid();

            groupedByArtisan
                    .computeIfAbsent(artisanId, k -> new ArrayList<>())
                    .add(item);
        }
        List<OrderResponseDTO> orders = new ArrayList<>();

        for (Map.Entry<UUID, List<OrderItemRequest>> entry : groupedByArtisan.entrySet()) {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .accountId(accountId)
                    .orderDate(LocalDateTime.now())
                    .items(entry.getValue())
                    .build();

            orders.add(orderService.create(request));
        }

        selectedProductIds.forEach(productId ->
                redisTemplate.opsForHash().delete(buildKey(accountId), productId.toString())
        );

        return orders;
    }
}
