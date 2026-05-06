package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.AddToCartRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateCartItemRequest;
import org.example.catholicsouvenircustomorder.dto.response.CartItemResponse;
import org.example.catholicsouvenircustomorder.dto.response.CartResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.CartService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImp implements CartService {
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AccountRepository accountRepository;
    private final ProductRepository productRepository;
    private final ProductTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CART_KEY = "cart:";
    private static final long CACHE_TTL = 7;
    
    @Override
    public CartResponse getCart(UUID customerId) {
        String cacheKey = CART_KEY + customerId;
        CartResponse cached = (CartResponse) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        Cart cart = findOrCreateCart(customerId);
        
        // ===== NEW: Validate stock for all items =====
        validateCartStock(cart);
        
        CartResponse response = mapToResponse(cart);
        
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL, TimeUnit.DAYS);
        return response;
    }
    
    /**
     * Validate stock availability for all cart items
     * Marks items as out of stock if quantity is 0
     */
    private void validateCartStock(Cart cart) {
        for (CartItem item : cart.getItems()) {
            if (item.isProduct()) {
                Product product = item.getProduct();
                // Refresh product from DB to get latest quantity
                Product freshProduct = productRepository.findById(product.getProductId())
                        .orElse(product);
                item.setProduct(freshProduct);
            }
        }
    }
    
    @Override
    @Transactional
    public CartResponse addToCart(UUID customerId, AddToCartRequest request) {
        Cart cart = findOrCreateCart(customerId);
        
        Optional<CartItem> existing = findExisting(cart, request);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            
            // Validate stock for products
            if (item.isProduct()) {
                Product product = item.getProduct();
                
                if (product.getAvailableQuantity() == 0) {
                    throw new BadRequestException(
                        String.format("Sản phẩm '%s' đã hết hàng", product.getProductName())
                    );
                }
                
                if (!product.hasAvailableStock(newQuantity)) {
                    throw new BadRequestException(
                        String.format("Sản phẩm '%s' chỉ còn %d sản phẩm có sẵn. Bạn đã có %d trong giỏ hàng", 
                            product.getProductName(), product.getAvailableQuantity(), item.getQuantity())
                    );
                }
            }
            
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem cartItem = createCartItem(cart, request);
            cart.addItem(cartItem);
            cartItemRepository.save(cartItem);
        }
        
        cart = cartRepository.save(cart);
        CartResponse response = mapToResponse(cart);
        
        updateCache(customerId, response);
        return response;
    }
    
    @Override
    @Transactional
    public CartResponse updateCartItem(UUID customerId, UUID cartItemId, UpdateCartItemRequest request) {
        Cart cart = findCartByCustomer(customerId);
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng"));
        
        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new BadRequestException("Sản phẩm không thuộc giỏ hàng của bạn");
        }
        
        // Validate stock for products
        if (cartItem.isProduct()) {
            Product product = cartItem.getProduct();
            
            if (product.getAvailableQuantity() == 0) {
                throw new BadRequestException(
                    String.format("Sản phẩm '%s' đã hết hàng", product.getProductName())
                );
            }
            
            if (!product.hasAvailableStock(request.getQuantity())) {
                throw new BadRequestException(
                    String.format("Sản phẩm '%s' chỉ còn %d sản phẩm có sẵn", 
                        product.getProductName(), product.getAvailableQuantity())
                );
            }
        }
        
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        
        cart = cartRepository.save(cart);
        CartResponse response = mapToResponse(cart);
        
        updateCache(customerId, response);
        return response;
    }
    
    @Override
    @Transactional
    public CartResponse removeCartItem(UUID customerId, UUID cartItemId) {
        Cart cart = findCartByCustomer(customerId);
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng"));
        
        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new BadRequestException("Sản phẩm không thuộc giỏ hàng của bạn");
        }
        
        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);
        
        cart = cartRepository.save(cart);
        CartResponse response = mapToResponse(cart);
        
        updateCache(customerId, response);
        return response;
    }
    
    @Override
    @Transactional
    public CartResponse clearCart(UUID customerId) {
        Cart cart = findCartByCustomer(customerId);
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        cart.clearItems();
        
        cart = cartRepository.save(cart);
        CartResponse response = mapToResponse(cart);
        
        updateCache(customerId, response);
        return response;
    }
    
    @Override
    public Integer getCartItemCount(UUID customerId) {
        String cacheKey = CART_KEY + customerId + ":count";
        Integer cached = (Integer) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        Optional<Cart> cart = cartRepository.findByCustomer_AccountId(customerId);
        Integer count = cart.map(Cart::getTotalItems).orElse(0);
        
        redisTemplate.opsForValue().set(cacheKey, count, CACHE_TTL, TimeUnit.DAYS);
        return count;
    }
    
    @Override
    public void invalidateCartCache(UUID customerId) {
        String cartKey = CART_KEY + customerId;
        String countKey = CART_KEY + customerId + ":count";
        
        redisTemplate.delete(cartKey);
        redisTemplate.delete(countKey);
    }
    
    private void updateCache(UUID customerId, CartResponse response) {
        String cacheKey = CART_KEY + customerId;
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL, TimeUnit.DAYS);
        
        String countKey = CART_KEY + customerId + ":count";
        redisTemplate.opsForValue().set(countKey, response.getTotalItems(), CACHE_TTL, TimeUnit.DAYS);
    }
    
    private Cart findOrCreateCart(UUID customerId) {
        return cartRepository.findByCustomer_AccountId(customerId)
                .orElseGet(() -> {
                    Account customer = accountRepository.findById(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    return cartRepository.save(cart);
                });
    }
    
    private Cart findCartByCustomer(UUID customerId) {
        return cartRepository.findByCustomer_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));
    }
    
    private CartItem createCartItem(Cart cart, AddToCartRequest request) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setType(request.getType());
        item.setQuantity(request.getQuantity());
        
        if (request.getType() == CartItemType.PRODUCT) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
            
            // Check if product is out of stock
            if (product.getQuantity() == 0) {
                throw new BadRequestException(
                    String.format("Sản phẩm '%s' đã hết hàng", product.getProductName())
                );
            }
            
            // Check if requested quantity exceeds available stock
            if (product.getQuantity() < request.getQuantity()) {
                throw new BadRequestException(
                    String.format("Sản phẩm '%s' chỉ còn %d sản phẩm trong kho", 
                        product.getProductName(), product.getQuantity())
                );
            }
            
            item.setProduct(product);
            item.setPrice(product.getProductPrice());
            
        } else if (request.getType() == CartItemType.TEMPLATE) {
            ProductTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy template"));
            
            item.setTemplate(template);
            item.setPrice(calculatePrice(template, request.getCustomizationData()));
            
            if (request.getCustomizationData() != null) {
                try {
                    item.setCustomizationData(objectMapper.writeValueAsString(request.getCustomizationData()));
                } catch (JsonProcessingException e) {
                    throw new BadRequestException("Dữ liệu customization không hợp lệ");
                }
            }
        }
        
        return item;
    }
    
    private Optional<CartItem> findExisting(Cart cart, AddToCartRequest request) {
        if (request.getType() == CartItemType.PRODUCT) {
            return cartItemRepository.findByCart_CartIdAndProduct_ProductId(
                    cart.getCartId(), request.getProductId());
        }
        return Optional.empty();
    }
    
    private BigDecimal calculatePrice(ProductTemplate template, Map<String, String> customization) {
        BigDecimal totalPrice = template.getBasePrice();
        
        if (customization != null && !customization.isEmpty()) {
            // Get all zones for this template
            List<TemplateCustomZone> zones = template.getCustomZones();
            
            for (TemplateCustomZone zone : zones) {
                String zoneIdStr = zone.getZoneId().toString();
                String inputValue = customization.get(zoneIdStr);
                
                // Only add extra price if zone has input value
                if (inputValue != null && !inputValue.trim().isEmpty()) {
                    if (zone.getExtraPrice() != null && zone.getExtraPrice().compareTo(BigDecimal.ZERO) > 0) {
                        totalPrice = totalPrice.add(zone.getExtraPrice());
                    }
                }
            }
        }
        
        return totalPrice;
    }
    
    private CartResponse mapToResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());
        
        BigDecimal total = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return CartResponse.builder()
                .cartId(cart.getCartId())
                .customerId(cart.getCustomer().getAccountId())
                .items(items)
                .totalItems(cart.getTotalItems())
                .totalAmount(total)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
    
    private CartItemResponse mapItemToResponse(CartItem item) {
        CartItemResponse.CartItemResponseBuilder builder = CartItemResponse.builder()
                .cartItemId(item.getCartItemId())
                .type(item.getType())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(item.getSubtotal())
                .addedAt(item.getAddedAt());
        
        if (item.isProduct()) {
            Product product = item.getProduct();
            boolean isAvailable = product.getQuantity() > 0;
            boolean hasEnoughStock = product.getQuantity() >= item.getQuantity();
            
            builder.productId(product.getProductId())
                    .productName(product.getProductName())
                    .productImage(product.getImages().isEmpty() ? null : product.getImages().get(0).getImage_url())
                    .availableStock(product.getQuantity())
                    .isAvailable(isAvailable && hasEnoughStock);
        } else if (item.isTemplate()) {
            ProductTemplate template = item.getTemplate();
            builder.templateId(template.getTemplateId())
                    .templateName(template.getName())
                    .templateImage(template.getBaseImages().isEmpty() ? null : template.getBaseImages().get(0))
                    .isAvailable(true); // Templates always available
            
            if (item.getCustomizationData() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> data = objectMapper.readValue(item.getCustomizationData(), Map.class);
                    builder.customizationData(data);
                } catch (JsonProcessingException e) {
                    // Ignore parse error
                }
            }
        }
        
        return builder.build();
    }
}
